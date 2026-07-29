(function () {
  'use strict';

  function notify(type, message, duration) {
    if (typeof window.showToast === 'function') {
      return window.showToast(type, message, duration);
    }
    window.alert(message);
    return null;
  }

  var params = new URLSearchParams(window.location.search);
  var analysisCaseId = params.get('analysisCaseId');
  var resumeSessionId = params.get('resumeSessionId');
  var requestedMode = params.get('mode');

  var root;
  var questionSet;
  var session;
  var questions = [];
  var currentIndex = 0;
  var followUpMessageId = null;
  var activeSection;
  var reviewSection;
  var activeSessions = [];
  var analysisSection;
  var analysisAttentionSection;
  var preparedAnalysisSection;
  var completing = false;
  var speechRecognition = null;
  var speechListening = false;
  var selectionModal = null;
  var selectionHistoryActive = false;

  function esc(value) {
    var element = document.createElement('div');
    element.textContent = value == null ? '' : String(value);
    return element.innerHTML;
  }

  function api(path, options) {
    options = options || {};
    var fetchOptions = {
      method: options.method || 'GET',
      credentials: 'same-origin',
      headers: {}
    };
    if (options.json !== undefined) {
      fetchOptions.headers['Content-Type'] = 'application/json; charset=UTF-8';
      fetchOptions.body = JSON.stringify(options.json);
    }
    return fetch(path, fetchOptions).then(function (response) {
      return response.json().then(function (body) {
        if (!response.ok || !body.success) throw new Error(body.message || '요청에 실패했습니다.');
        return body.data;
      });
    });
  }

  function message(text, error) {
    hideActiveSection();
    root.innerHTML = '<div class="card card--pad-lg" style="text-align:center;padding:48px 20px;">' +
      '<p style="font-size:14px;color:' + (error ? '#B5433D' : '#5B6370') + ';">' + esc(text) + '</p>' +
      (error ? '<a class="btn btn--primary" href="/interview.html" style="text-decoration:none;">면접 준비로 돌아가기</a>' : '') +
      '</div>';
  }

  function modeLabel(mode) {
    if (mode === 'BASIC') return '기본 질문';
    if (mode === 'COMPANY_FIT') return '회사 맞춤';
    return '약점 보완';
  }

  function hideActiveSection() {
    if (activeSection) activeSection.hidden = true;
  }

  function stopSpeechInput() {
    if (speechRecognition && speechListening) {
      speechRecognition.stop();
    }
  }

  function setupSpeechInput() {
    var button = document.getElementById('voice-input-toggle');
    var textarea = document.getElementById('live-answer');
    var status = document.getElementById('voice-input-status');
    if (!button || !textarea || !status) return;

    var Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!Recognition) {
      button.disabled = true;
      status.textContent = '이 브라우저는 음성 입력을 지원하지 않습니다. 키보드로 답변해 주세요.';
      return;
    }

    speechRecognition = new Recognition();
    speechRecognition.lang = 'ko-KR';
    speechRecognition.continuous = true;
    speechRecognition.interimResults = true;
    speechRecognition.maxAlternatives = 1;

    var baseText = '';
    var finalText = '';

    speechRecognition.onstart = function () {
      speechListening = true;
      button.classList.add('is-listening');
      button.setAttribute('aria-pressed', 'true');
      button.textContent = '음성 입력 중지';
      status.textContent = '듣고 있습니다. 자연스럽게 답변해 주세요.';
    };
    speechRecognition.onresult = function (event) {
      var interimText = '';
      for (var i = event.resultIndex; i < event.results.length; i += 1) {
        var transcript = event.results[i][0].transcript.trim();
        if (event.results[i].isFinal) {
          finalText += (finalText ? ' ' : '') + transcript;
        } else {
          interimText += (interimText ? ' ' : '') + transcript;
        }
      }
      textarea.value = [baseText, finalText, interimText].filter(Boolean).join(' ').trim();
      status.textContent = interimText
        ? '인식 중: ' + interimText
        : '음성이 텍스트로 입력되었습니다. 계속 말하거나 입력을 종료할 수 있습니다.';
    };
    speechRecognition.onerror = function (event) {
      var copy = event.error === 'not-allowed'
        ? '마이크 권한이 필요합니다. 브라우저 주소창의 마이크 권한을 허용해 주세요.'
        : event.error === 'no-speech'
          ? '음성이 들리지 않았습니다. 다시 시도해 주세요.'
          : '음성 인식이 중단되었습니다. 다시 시도해 주세요.';
      status.textContent = copy;
    };
    speechRecognition.onend = function () {
      speechListening = false;
      button.classList.remove('is-listening');
      button.setAttribute('aria-pressed', 'false');
      button.textContent = '음성으로 답변';
      if (!status.textContent || status.textContent.indexOf('인식 중:') === 0) {
        status.textContent = '음성 입력이 종료되었습니다. 변환된 문장을 확인해 주세요.';
      }
    };

    button.addEventListener('click', function () {
      if (speechListening) {
        speechRecognition.stop();
        return;
      }
      baseText = textarea.value.trim();
      finalText = '';
      try {
        speechRecognition.start();
      } catch (error) {
        status.textContent = '음성 인식을 바로 다시 시작할 수 없습니다. 잠시 후 시도해 주세요.';
      }
    });
  }

  function load() {
    message('분석 상태와 질문 세트를 확인하고 있습니다.');
    api('/api/analysis/cases/' + analysisCaseId + '/status').then(function (status) {
      if (status.analysisCaseStatus !== 'COMPLETED') {
        throw new Error('분석이 아직 완료되지 않았습니다. 분석 화면에서 상태를 확인해주세요.');
      }
      if (!status.canGenerateQuestions || !status.questionSetAvailable) {
        throw new Error('현재 분석 결과로는 회사 맞춤 질문을 생성할 수 없습니다.');
      }
      return api('/api/analysis/cases/' + analysisCaseId + '/question-set');
    }).then(function (set) {
      if (!set.canGenerateQuestions || !set.questions || !set.questions.length) {
        throw new Error('선택할 수 있는 회사 맞춤 질문이 없습니다.');
      }
      questionSet = set;
      renderSelection();
    }).catch(function (error) {
      message(error.message, true);
    });
  }

  function startBasic() {
    message('직무 기준에 맞는 기본 질문을 생성하고 있습니다.');
    Promise.all([api('/api/user/me'), api('/api/job-category')]).then(function (values) {
      var user = values[0];
      var categories = values[1] || [];
      var category = categories.filter(function (item) {
        return String(item.jobCategoryId) === String(user.defaultJobCategoryId);
      })[0];
      if (!category) throw new Error('마이페이지에서 희망 직무를 먼저 설정해주세요.');
      return api('/api/question-sets/basic', {
        method: 'POST',
        json: { jobCategoryId: category.jobCategoryId, careerLevel: category.careerLevel }
      });
    }).then(function (set) {
      questionSet = set;
      renderSelection();
    }).catch(function (error) { message(error.message, true); });
  }

  function startWeakness(tag) {
    message('선택한 약점의 최근 평가를 바탕으로 질문을 생성하고 있습니다.');
    api('/api/question-sets/weakness', {
      method: 'POST',
      json: { targetWeaknessTag: tag }
    }).then(function (set) {
      questionSet = set;
      renderSelection();
    }).catch(function (error) { message(error.message, true); });
  }

  function renderSelection() {
    var rows = questionSet.questions.map(function (question) {
      return '<label class="q-gen-item" style="cursor:pointer;">' +
        '<input type="checkbox" name="question" value="' + question.questionId + '" checked>' +
        '<span class="q-gen-item__num">' + question.displayOrder + '</span>' +
        '<span class="q-gen-item__text">' + esc(question.question) + '</span></label>';
    }).join('');
    if (window.InterviewPage && window.InterviewPage.showModes) window.InterviewPage.showModes();
    if (activeSection) activeSection.hidden = false;
    closeSelectionModal(true);
    selectionModal = document.createElement('div');
    selectionModal.className = 'question-selection-modal';
    selectionModal.innerHTML =
      '<div class="question-selection-backdrop" data-close-selection></div>' +
      '<section class="question-selection-dialog" role="dialog" aria-modal="true" aria-labelledby="question-selection-title">' +
      '<header><div><span class="question-selection-mode">' + esc(modeLabel(questionSet.mode)) +
      '</span><h2 id="question-selection-title">연습할 질문을 골라주세요</h2>' +
      '<p>선택한 순서가 아니라, 아래 표시 순서대로 면접이 진행됩니다.</p></div>' +
      '<button type="button" class="question-selection-close" data-close-selection aria-label="질문 선택 닫기">×</button></header>' +
      '<div class="q-gen-list">' + rows + '</div>' +
      '<footer><span>질문은 하나 이상 선택해야 합니다.</span>' +
      '<button id="create-live-session" class="btn btn--primary">선택한 질문으로 시작</button></footer></section>';
    document.body.appendChild(selectionModal);
    document.body.classList.add('has-selection-modal');
    window.history.pushState({ interviewSelection: true }, '', '#question-selection');
    selectionHistoryActive = true;
    selectionModal.querySelectorAll('[data-close-selection]').forEach(function (button) {
      button.addEventListener('click', function () {
        if (analysisCaseId) {
          window.location.href = '/api/analysis/' + encodeURIComponent(analysisCaseId);
        } else if (selectionHistoryActive) window.history.back();
        else closeSelectionModal(true);
      });
    });
    document.getElementById('create-live-session').addEventListener('click', createSession);
  }

  function closeSelectionModal(fromHistory) {
    if (selectionModal) selectionModal.remove();
    selectionModal = null;
    document.body.classList.remove('has-selection-modal');
    if (fromHistory) selectionHistoryActive = false;
  }

  function createSession() {
    var selected = Array.prototype.slice.call(
      (selectionModal || document).querySelectorAll('input[name="question"]:checked')
    )
      .map(function (input) { return Number(input.value); });
    if (!selected.length) {
      notify('warning', '질문을 하나 이상 선택해주세요.');
      return;
    }
    closeSelectionModal(true);
    window.history.replaceState({}, '', window.location.pathname + window.location.search);
    message('면접을 준비하고 있습니다.');
    api('/api/interview-sessions', {
      method: 'POST',
      json: { questionSetId: questionSet.questionSetId, selectedQuestionIds: selected }
    }).then(function (created) {
      session = created;
      localStorage.setItem('jobpuzzle_interview_session_id', String(session.sessionId));
      return api('/api/interview-sessions/' + session.sessionId + '/questions');
    }).then(function (items) {
      questions = items;
      currentIndex = 0;
      renderQuestion();
    }).catch(function (error) {
      message(error.message, true);
    });
  }

  function resumeSession(active) {
    hideActiveSection();
    session = active;
    api('/api/interview-sessions/' + session.sessionId + '/questions').then(function (items) {
      questions = items;
      currentIndex = 0;
      for (var i = 0; i < questions.length; i += 1) {
        if (questions[i].status === 'PENDING' || questions[i].status === 'IN_PROGRESS') {
          currentIndex = i;
          break;
        }
      }
      renderQuestion();
    }).catch(function (error) { message(error.message, true); });
  }

  function renderQuestion() {
    stopSpeechInput();
    var question = questions[currentIndex];
    if (!question) {
      renderFinish();
      return;
    }
    followUpMessageId = question.pendingFollowUpMessageId || null;
    var followUpCount = question.followUpCount || 0;
    var hasAnyAnswer = questions.some(function (item) { return item.answerSubmitted; });
    var actionLabel = hasAnyAnswer ? '여기서 면접 완료' : '답변 전 세션 취소';
    var submitLabel = followUpMessageId ? '심화 답변 제출' : '첫 답변 제출';
    var conversation = (question.conversation || []).filter(function (item) {
      return item.messageType !== 'ORIGINAL_QUESTION';
    });
    var conversationHtml = conversation.length
      ? '<div class="live-conversation" aria-label="현재 질문의 답변 과정">' +
        conversation.map(function (item) {
          var isUser = item.sender === 'USER';
          var label = item.messageType === 'ORIGINAL_ANSWER'
            ? '첫 답변'
            : item.messageType === 'FOLLOW_UP_QUESTION'
              ? 'AI 심화 질문'
              : '심화 답변';
          return '<div class="live-conversation-turn ' + (isUser ? 'is-user' : 'is-ai') + '">' +
            '<span>' + label + '</span><p>' + esc(item.messageText) + '</p></div>';
        }).join('') + '</div>'
      : '';
    root.innerHTML = '<div class="card card--pad-lg live-interview-card">' +
      '<div class="live-question-heading"><span>' + (currentIndex + 1) + ' / ' + questions.length +
      '</span><strong>' + esc(modeLabel(session.mode)) + '</strong></div>' +
      '<p class="live-question-label">첫 질문</p>' +
      '<p class="live-original-question">' + esc(question.questionText) + '</p>' +
      conversationHtml +
      '<div id="live-follow-up" class="live-follow-up"' + (followUpMessageId ? '' : ' hidden') + '>' +
      '<span>AI 심화 질문 ' + followUpCount + ' / 최대 2</span>' +
      '<p>' + esc(question.pendingFollowUpQuestion || '') + '</p>' +
      '<small>답변을 조금 더 확인할 필요가 있을 때만 이어집니다. 여기서 연습을 마쳐도 됩니다.</small></div>' +
      '<label class="live-answer-label" for="live-answer">' +
      (followUpMessageId ? '심화 질문에 답변하기' : '첫 답변 작성하기') + '</label>' +
      '<textarea id="live-answer" placeholder="상황, 본인 역할, 행동, 결과를 중심으로 답변해 주세요"></textarea>' +
      '<div class="voice-input-row"><button type="button" id="voice-input-toggle" ' +
      'class="voice-input-button" aria-pressed="false">음성으로 답변</button>' +
      '<p id="voice-input-status" aria-live="polite">버튼을 누르면 한국어 음성을 텍스트로 변환합니다.</p></div>' +
      '<div style="display:flex;justify-content:space-between;margin-top:14px;">' +
      '<button id="finish-live-session" class="btn btn--ghost">' + actionLabel + '</button>' +
      '<button id="submit-live-answer" class="btn btn--primary">' + submitLabel + '</button></div></div>';
    document.getElementById('submit-live-answer').addEventListener('click', submitAnswer);
    document.getElementById('finish-live-session').addEventListener('click', finishOrCancel);
    setupSpeechInput();
  }

  function submitAnswer() {
    stopSpeechInput();
    var textarea = document.getElementById('live-answer');
    var text = textarea.value.trim();
    if (!text) return;
    var button = document.getElementById('submit-live-answer');
    button.disabled = true;
    button.textContent = 'AI가 평가 중입니다';
    api('/api/interview-session-questions/' + questions[currentIndex].sessionQuestionId + '/answers', {
      method: 'POST',
      json: {
        messageText: text,
        answerType: followUpMessageId ? 'FOLLOW_UP_ANSWER' : 'ORIGINAL_ANSWER',
        parentQuestionMessageId: followUpMessageId
      }
    }).then(function (result) {
      questions[currentIndex].answerSubmitted = true;
      questions[currentIndex].conversation = questions[currentIndex].conversation || [];
      questions[currentIndex].conversation.push({
        sender: 'USER',
        messageType: followUpMessageId ? 'FOLLOW_UP_ANSWER' : 'ORIGINAL_ANSWER',
        messageText: text
      });
      if (result.evaluationFailed) {
        notify('warning', result.summary);
      }
      if (result.followUpQuestion) {
        followUpMessageId = result.followUpQuestionMessageId;
        questions[currentIndex].pendingFollowUpMessageId = result.followUpQuestionMessageId;
        questions[currentIndex].pendingFollowUpQuestion = result.followUpQuestion;
        questions[currentIndex].followUpCount = (questions[currentIndex].followUpCount || 0) + 1;
        questions[currentIndex].conversation.push({
          sender: 'AI',
          messageType: 'FOLLOW_UP_QUESTION',
          messageText: result.followUpQuestion
        });
        renderQuestion();
        return;
      }
      followUpMessageId = null;
      questions[currentIndex].status = 'COMPLETED';
      questions[currentIndex].pendingFollowUpMessageId = null;
      questions[currentIndex].pendingFollowUpQuestion = null;
      currentIndex += 1;
      renderQuestion();
    }).catch(function (error) {
      button.disabled = false;
      button.textContent = '다시 제출';
      notify('error', error.message);
    });
  }

  function renderFinish() {
    if (!session) return;
    stopSpeechInput();
    window.location.href = '/interview-result.html?sessionId=' +
      encodeURIComponent(session.sessionId) + '&interim=true';
  }

  function completeSession() {
    if (!session || completing) return;
    stopSpeechInput();
    completing = true;
    root.innerHTML = '<div class="card card--pad-lg live-completing">' +
      '<span class="live-completing__pulse" aria-hidden="true"></span>' +
      '<div><strong>면접 결과를 정리하고 있습니다</strong>' +
      '<p>답변 평가와 관점별 점수를 집계한 뒤 결과 화면으로 바로 이동합니다.</p></div></div>';
    api('/api/interview-sessions/' + session.sessionId + '/complete', { method: 'POST' })
      .then(function () {
        window.location.href = '/interview-result.html?sessionId=' + encodeURIComponent(session.sessionId);
      }).catch(function (error) {
        completing = false;
        message(error.message, true);
      });
  }

  function finishOrCancel() {
    if (!session) return;
    var hasAnyAnswer = questions.some(function (item) { return item.answerSubmitted; });
    if (hasAnyAnswer) {
      renderFinish();
      return;
    }
    if (!window.confirm('아직 제출한 답변이 없습니다. 이 세션을 취소할까요?')) return;
    message('면접 세션을 취소하고 있습니다.');
    api('/api/interview-sessions/' + session.sessionId + '/cancel', { method: 'POST' })
      .then(function () {
        localStorage.removeItem('jobpuzzle_interview_session_id');
        window.location.href = '/interview.html';
      })
      .catch(function (error) { message(error.message, true); });
  }

  function renderActiveSessions(items) {
    activeSessions = items || [];
    if (!items || !items.length || analysisCaseId) return;
    activeSection = document.createElement('section');
    activeSection.className = 'active-session-section';
    activeSection.innerHTML =
      '<div class="active-session-heading"><div><h2>진행 중인 면접</h2>' +
      '<p>새 모드를 선택하거나, 저장된 위치에서 이어갈 수 있습니다.</p></div>' +
      '<span class="session-status-count is-interview">' + items.length + '개 진행 중</span></div>' +
      '<div class="active-session-list">' + items.map(function (item) {
        var created = item.createdAt ? new Date(item.createdAt).toLocaleString('ko-KR', {
          month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit'
        }) : '';
        var questionOrder = item.currentQuestionOrder || 1;
        var qaDepth = item.currentQaDepth || 0;
        var qaLabel = qaDepth === 0 ? '첫 답변 작성 전'
          : qaDepth === 1 ? '첫 답변 완료 · 심화 질문 확인 단계'
            : qaDepth === 2 ? '첫 번째 심화 답변 완료'
              : '두 번째 심화 답변 완료 · 결과 확인 가능';
        var displayDepth = Math.min(3, Math.max(1, qaDepth));
        var progress = item.questionCount
          ? Math.round((item.completedQuestionCount || 0) / item.questionCount * 100)
          : 0;
        return '<article class="active-session-card mode-' + String(item.mode).toLowerCase().replace('_', '-') +
          '" tabindex="0">' +
          '<div class="active-session-main"><span class="active-session-mode">' +
          esc(modeLabel(item.mode)) + '</span>' +
          '<h3>' + esc(item.questionCount) + '개 질문으로 진행 중</h3>' +
          '<p>' + esc(created) + (item.targetWeaknessTag ? ' · #' + esc(item.targetWeaknessTag) : '') + '</p></div>' +
          '<div class="active-session-actions"><button class="btn btn--primary" data-resume-session="' +
          item.sessionId + '">이어서 진행</button>' +
          (item.answerSubmitted ? '' : '<button class="btn btn--ghost" data-cancel-session="' +
          item.sessionId + '">답변 전 취소</button>') + '</div>' +
          '<div class="active-session-insight" role="status">' +
          '<div class="active-session-progress-head"><strong>현재 진행 위치</strong><span>' +
          progress + '%</span></div><div class="active-session-progress"><i style="width:' +
          progress + '%"></i></div><dl><div><dt>질문</dt><dd>' + questionOrder + ' / ' +
          item.questionCount + '</dd></div><div><dt>답변 단계</dt><dd>' + displayDepth +
          ' / 최대 3단계</dd></div><div><dt>현재 위치</dt><dd>' + esc(qaLabel) +
          '</dd></div></dl></div></article>';
      }).join('') + '</div>';
    root.insertAdjacentElement('afterend', activeSection);
    activeSection.querySelectorAll('[data-resume-session]').forEach(function (button) {
      button.addEventListener('click', function () {
        var selected = items.find(function (item) {
          return String(item.sessionId) === button.dataset.resumeSession;
        });
        resumeSession(selected);
      });
    });
    activeSection.querySelectorAll('[data-cancel-session]').forEach(function (button) {
      button.addEventListener('click', function () {
        if (!window.confirm('답변을 제출하지 않은 세션만 취소할 수 있습니다. 취소할까요?')) return;
        api('/api/interview-sessions/' + button.dataset.cancelSession + '/cancel', { method: 'POST' })
          .then(function () { activeSection.remove(); activeSection = null; loadActiveSessions(); })
          .catch(function (error) { notify('error', error.message); });
      });
    });
  }

  function loadActiveSessions() {
    Promise.all([
      api('/api/interview-sessions/active-list'),
      api('/api/interview-sessions/review-list')
    ]).then(function (values) {
      renderActiveSessions(values[0]);
      renderReviewReadySessions(values[1]);
    }).catch(function () {});
  }

  function renderReviewReadySessions(items) {
    if (reviewSection) reviewSection.remove();
    reviewSection = null;
    if (!items || !items.length || analysisCaseId) return;
    reviewSection = document.createElement('section');
    reviewSection.className = 'review-ready-section';
    reviewSection.innerHTML =
      '<div class="active-session-heading"><div><h2>결과 검토 중인 면접</h2>' +
      '<p>선택한 질문의 답변은 끝났습니다. 남은 질문을 더 연습하거나 현재 결과를 확정할 수 있어요.</p></div>' +
      '<span class="session-status-count is-review">' + items.length + '개 검토 중</span></div><div class="review-ready-list">' +
      items.map(function (item) {
        var created = item.createdAt ? new Date(item.createdAt).toLocaleString('ko-KR') : '';
        return '<article class="review-ready-card"><div><span>' + esc(modeLabel(item.mode)) +
          '</span><h3>' + esc(item.completedQuestionCount) + '개 질문 답변 완료</h3><p>' +
          esc(created) + '</p></div><a class="btn btn--primary" href="/interview-result.html?sessionId=' +
          encodeURIComponent(item.sessionId) + '">중간 결과 확인</a></article>';
      }).join('') + '</div>';
    var anchor = activeSection || root;
    anchor.insertAdjacentElement('afterend', reviewSection);
  }

  function analysisStatusLabel(status) {
    if (status === 'ANALYZING') return '분석 진행 중';
    if (status === 'FAILED') return '분석 확인 필요';
    return '분석 시작 전';
  }

  function analysisStageLabel(stage) {
    var labels = {
      JOB_POSTING_ANALYSIS: '채용공고 분석',
      CANDIDATE_MATERIAL_ANALYSIS: '지원자 자료 분석',
      GUIDE_CONTEXT_BUILD: '직무 가이드 적용',
      CUSTOMIZED_SYNTHESIS: '맞춤 결과 생성'
    };
    return labels[stage] || '맞춤 결과 생성';
  }

  function analysisStages(item) {
    return [
      { key: 'jobPostingAnalysisStatus', label: '채용공고 분석' },
      { key: 'candidateMaterialAnalysisStatus', label: '지원자 자료 분석' },
      { key: 'guideContextStatus', label: '직무 가이드 적용' },
      { key: 'customizedAnalysisStatus', label: '맞춤 결과 생성' }
    ].map(function (stage) {
      return {
        label: stage.label,
        status: item[stage.key] || 'NOT_STARTED'
      };
    });
  }

  function renderActiveAnalyses(items) {
    if (analysisSection) analysisSection.remove();
    if (!items.length || analysisCaseId) return;
    analysisSection = document.createElement('section');
    analysisSection.className = 'active-analysis-section';
    analysisSection.innerHTML =
      '<div class="active-session-heading"><div><h2>진행 중인 맞춤 분석</h2>' +
      '<p>자료를 분석하는 동안 다른 화면을 이용해도 괜찮아요.</p></div>' +
      '<span class="session-status-count is-analyzing">' + items.length + '개 확인 중</span></div>' +
      '<div class="active-analysis-list">' + items.map(function (item) {
        var label = [item.mainCategory, item.subCategory].filter(Boolean).join(' · ') || '회사 맞춤 면접';
        var steps = analysisStages(item);
        var completed = steps.filter(function (stage) { return stage.status === 'SUCCEEDED'; }).length;
        var runningIndex = steps.findIndex(function (stage) { return stage.status === 'RUNNING'; });
        var currentIndex = runningIndex >= 0 ? runningIndex : Math.min(completed, steps.length - 1);
        var current = steps[currentIndex];
        var progress = item.status === 'INPUT_CONFIRMED' ? 0 : Math.round((completed + (runningIndex >= 0 ? .45 : 0)) / steps.length * 100);
        var detail = steps.map(function (stage, index) {
          var stateClass = stage.status === 'SUCCEEDED' ? 'is-done' :
            stage.status === 'RUNNING' ? 'is-running' : '';
          return '<li class="' + stateClass + '"><i>' + (index + 1) + '</i><span>' +
            esc(stage.label) + '</span></li>';
        }).join('');
        return '<article class="active-analysis-card ' + (item.status === 'INPUT_CONFIRMED' ? 'is-waiting' : '') + '">' +
          '<div class="active-analysis-visual"><i></i><span>AI</span></div>' +
          '<div class="active-analysis-copy"><span>' + analysisStatusLabel(item.status) + '</span>' +
          '<h3>' + esc(label) + '</h3><p><strong>' + esc(current.label) + '</strong> 단계입니다. ' +
          (item.status === 'INPUT_CONFIRMED' ? '분석 시작을 기다리고 있어요.' : '자료와 근거를 차근차근 확인하고 있어요.') + '</p>' +
          '<ol class="active-analysis-stages">' + detail + '</ol>' +
          '<div class="active-analysis-progress" aria-label="분석 진행률 ' + progress + '%"><i style="width:' + progress + '%"></i></div></div>' +
          '<a class="btn btn--primary" href="/api/analysis/' + item.analysisCaseId + '">상태 확인</a></article>';
      }).join('') + '</div>';
    var anchor = activeSection || root;
    anchor.insertAdjacentElement('afterend', analysisSection);
  }

  function renderAnalysisAttention(items) {
    if (analysisAttentionSection) analysisAttentionSection.remove();
    analysisAttentionSection = null;
    if (!items.length || analysisCaseId) return;

    analysisAttentionSection = document.createElement('section');
    analysisAttentionSection.className = 'analysis-attention-section';
    analysisAttentionSection.innerHTML =
      '<div class="active-session-heading"><div><h2>분석 확인 필요</h2>' +
      '<p>멈춘 분석입니다. 선택한 자료와 앞선 분석 결과는 보존되어 있어요.</p></div>' +
      '<span class="session-status-count is-attention">' + items.length + '개 확인 필요</span></div>' +
      '<div class="analysis-attention-list">' + items.map(function (item) {
        var label = [item.mainCategory, item.subCategory].filter(Boolean).join(' · ') || '회사 맞춤 면접';
        return '<article class="analysis-attention-card">' +
          '<div class="analysis-attention-visual" aria-hidden="true">!</div>' +
          '<div class="analysis-attention-copy"><span>' + esc(analysisStageLabel(item.latestFailureStage)) + ' 단계 확인 필요</span>' +
          '<h3>' + esc(label) + '</h3><p>' + esc(item.userMessage || '분석 결과를 생성하지 못했어요.') + '</p>' +
          '<small>자료를 다시 선택하지 않아도 상태 화면에서 다시 실행할 수 있습니다.</small></div>' +
          '<a class="btn btn--outline" href="/api/analysis/' + encodeURIComponent(item.analysisCaseId) + '">상태 확인</a></article>';
      }).join('') + '</div>';
    var anchor = analysisSection || activeSection || root;
    anchor.insertAdjacentElement('afterend', analysisAttentionSection);
  }

  function renderPreparedAnalyses(items) {
    if (preparedAnalysisSection) preparedAnalysisSection.remove();
    preparedAnalysisSection = null;
    if (!items.length || analysisCaseId) return;
    preparedAnalysisSection = document.createElement('section');
    preparedAnalysisSection.className = 'prepared-analysis-section';
    preparedAnalysisSection.innerHTML =
      '<div class="active-session-heading"><div><h2>면접 질문이 준비된 맞춤 분석</h2>' +
      '<p>분석 결과를 다시 확인하거나, 준비된 질문으로 면접을 이어갈 수 있어요.</p></div>' +
      '<span class="session-status-count is-prepared prepared-analysis-count">' + items.length + '개 준비됨</span></div>' +
      '<div class="prepared-analysis-list">' + items.map(function (item) {
        var label = [item.mainCategory, item.subCategory].filter(Boolean).join(' · ') || '회사 맞춤 면접';
        var created = item.createdAt ? new Date(item.createdAt).toLocaleString('ko-KR') : '';
        var sourceNames = (item.sources || []).map(function (source) {
          return source.displayName;
        }).filter(Boolean);
        var detail = [label, item.careerLevel, sourceNames.join(', ')].filter(Boolean).join(' | ');
        return '<article class="prepared-analysis-card"><div class="prepared-analysis-check">✓</div>' +
          '<div class="prepared-analysis-copy" tabindex="0"><span>분석 완료</span><h3>' + esc(label) + '</h3>' +
          '<p>' + esc(created) + ' · ' + esc(item.careerLevel || '경력 기준 미지정') + '</p>' +
          '<small class="prepared-analysis-detail">' + esc(detail) + '</small></div>' +
          '<div class="prepared-analysis-actions"><a class="btn btn--outline" href="/api/analysis/' +
          encodeURIComponent(item.analysisCaseId) + '">분석 결과 보기</a>' +
          '<a class="btn btn--primary" href="/interview.html?analysisCaseId=' +
          encodeURIComponent(item.analysisCaseId) + '">질문 선택</a></div></article>';
      }).join('') + '</div>';
    var anchor = analysisAttentionSection || analysisSection || activeSection || root;
    anchor.insertAdjacentElement('afterend', preparedAnalysisSection);
  }

  function loadActiveAnalyses() {
    var stored;
    try { stored = JSON.parse(localStorage.getItem('jobpuzzle_analysis_cases') || '[]'); }
    catch (ignore) { stored = []; }
    if (!stored.length) return;
    Promise.all(stored.map(function (item) {
      return api('/api/analysis/cases/' + item.analysisCaseId + '/status')
        .then(function (status) {
        item.status = status.analysisCaseStatus;
        item.latestFailureStage = status.latestFailureStage;
        item.userMessage = status.userMessage;
        item.questionSetAvailable = status.questionSetAvailable;
        item.jobPostingAnalysisStatus = status.jobPostingAnalysisStatus;
        item.candidateMaterialAnalysisStatus = status.candidateMaterialAnalysisStatus;
        item.guideContextStatus = status.guideContextStatus;
        item.customizedAnalysisStatus = status.customizedAnalysisStatus;
        return item;
        }).catch(function () { return null; });
    })).then(function (items) {
      var activeItems = items.filter(function (item) {
        return item && (item.status === 'INPUT_CONFIRMED' || item.status === 'ANALYZING');
      });
      var failedItems = items.filter(function (item) {
        return item && item.status === 'FAILED';
      });
      localStorage.setItem(
        'jobpuzzle_analysis_cases',
        JSON.stringify(activeItems.concat(failedItems))
      );
      renderActiveAnalyses(activeItems);
      renderAnalysisAttention(failedItems);
    });
  }

  function loadPreparedAnalysesFromServer() {
    api('/api/analysis-cases/completed').then(function (cases) {
      return Promise.all((cases || []).map(function (item) {
        return api('/api/analysis/cases/' + item.analysisCaseId + '/status')
          .then(function (status) {
            item.questionSetAvailable = status.questionSetAvailable;
            return item;
          })
          .catch(function () { return null; });
      }));
    }).then(function (items) {
      renderPreparedAnalyses(items.filter(function (item) {
        return item && item.questionSetAvailable;
      }));
    }).catch(function () {});
  }

  function bindResultTabGuard() {
    var tab = document.getElementById('interview-result-tab');
    if (!tab) return;
    tab.addEventListener('click', function (event) {
      if (!session || !root.querySelector('.live-interview-card')) return;
      var shouldLeave = window.confirm(
        '진행 중인 면접이 있습니다.\n\n작성한 내용은 저장되어 있어 나중에 이어서 진행할 수 있습니다. 답변 평가 화면으로 이동할까요?'
      );
      if (!shouldLeave) event.preventDefault();
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    root = document.getElementById('step-root');
    var evaluationGuide = document.getElementById('evaluation-guide');
    if (evaluationGuide && root) root.parentNode.insertBefore(evaluationGuide, root);
    if (analysisCaseId) {
      load();
    } else if (resumeSessionId) {
      api('/api/interview-sessions/' + resumeSessionId)
        .then(resumeSession)
        .catch(function (error) { message(error.message, true); });
    } else {
      loadActiveSessions();
      loadActiveAnalyses();
      loadPreparedAnalysesFromServer();
      if (requestedMode === 'weakness') {
        window.setTimeout(function () {
          var weaknessCard = document.querySelector('[data-mode="weakness"]');
          if (weaknessCard) weaknessCard.click();
        }, 0);
      }
    }
    bindResultTabGuard();
  });
  window.addEventListener('popstate', function () {
    if (!selectionModal) return;
    if (analysisCaseId) {
      window.location.replace('/api/analysis/' + encodeURIComponent(analysisCaseId));
      return;
    }
    closeSelectionModal(true);
  });
  window.InterviewLive = { startBasic: startBasic, startWeakness: startWeakness };
})();
