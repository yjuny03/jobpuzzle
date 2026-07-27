(function () {
  'use strict';

  var sessionId = new URLSearchParams(window.location.search).get('sessionId');
  var CATEGORY_LABELS = {
    intentMatch: '질문 의도 이해',
    specificity: '경험 구체성',
    ownRole: '본인 역할',
    problemSolving: '문제 해결 과정',
    resultExpression: '성과 및 결과 표현',
    requirementConnection: '공고 요구사항 연결',
    guideAlignment: '직무 가이드 적합',
    deliveryClarity: '답변 전달력'
  };

  function api(path) {
    return fetch(path, { credentials: 'same-origin' }).then(function (response) {
      return response.json().then(function (body) {
        if (!response.ok || !body.success) {
          throw new Error(body.message || '결과를 불러오지 못했습니다.');
        }
        return body.data;
      });
    });
  }

  function esc(value) {
    var element = document.createElement('div');
    element.textContent = value == null ? '' : String(value);
    return element.innerHTML;
  }

  function modeLabel(mode) {
    if (mode === 'BASIC') return '기본 모의면접';
    if (mode === 'COMPANY_FIT') return '회사 맞춤 면접';
    return '약점 보완 면접';
  }

  function scoreTone(score) {
    if (score >= 80) return 'score-good';
    if (score >= 70) return 'score-pass';
    return 'score-needs-work';
  }

  function renderDonut(value) {
    var overall = value == null ? 0 : value;
    var deg = Math.round(overall * 3.6);
    document.getElementById('overall-donut').innerHTML =
      '<div class="score-donut" style="--score-angle:' + deg + 'deg">' +
      '<div><strong>' + (value == null ? '-' : overall) + '</strong><span>100점</span></div></div>';
    document.getElementById('overall-score-label').textContent =
      value == null ? '점수 집계 대상 없음' : overall + '점';
  }

  function dimensionItems(scores, counts) {
    var entries = Object.keys(scores || {}).map(function (key) {
      return { key: key, value: scores[key] };
    });
    if (!entries.length) {
      return '<p class="result-empty-copy">평가에 성공한 관점이 없습니다.</p>';
    }
    return entries.map(function (item) {
      var evaluationCount = counts && counts[item.key] ? counts[item.key] : 1;
      var basis = evaluationCount === 1
        ? '제출한 답변 1회의 관점 점수'
        : '답변 과정 ' + evaluationCount + '회의 동일 관점 평균';
      return '<div class="dimension-row"><div><strong>' +
        esc(CATEGORY_LABELS[item.key] || item.key) + '</strong>' +
        '<span><em>질문 평가 관점</em> ' + esc(basis) + '</span></div>' +
        '<b class="' + scoreTone(item.value) + '">' + item.value + '점</b></div>';
    }).join('');
  }

  function conversationTimeline(question) {
    var messages = (question.conversation || []).filter(function (message) {
      return message.messageType !== 'ORIGINAL_QUESTION';
    });
    if (!messages.length) {
      return '<div class="result-conversation-empty">저장된 답변 기록이 없습니다.</div>';
    }
    return '<section class="result-conversation"><div class="result-section-title">' +
      '<span>답변 과정</span><small>점수에 반영된 실제 면접 대화</small></div>' +
      '<div class="result-conversation-list">' + messages.map(function (message, index) {
        var isUser = message.sender === 'USER';
        var label = message.messageType === 'ORIGINAL_ANSWER'
          ? '첫 답변'
          : message.messageType === 'FOLLOW_UP_QUESTION'
            ? 'AI 심화 질문'
            : '심화 답변';
        var unanswered = message.messageType === 'FOLLOW_UP_QUESTION' &&
          (!messages[index + 1] || messages[index + 1].messageType !== 'FOLLOW_UP_ANSWER');
        return '<article class="result-conversation-item ' + (isUser ? 'is-user' : 'is-ai') + '">' +
          '<div><strong>' + label + '</strong>' +
          (unanswered ? '<em>미응답 · 점수 제외</em>' : '') + '</div>' +
          '<p>' + esc(message.messageText) + '</p></article>';
      }).join('') + '</div></section>';
  }

  function renderQuestionDetail(question, questionScore, score) {
    var detail = document.getElementById('q-detail');
    var dimensionScores = questionScore ? questionScore.dimensionScores : {};
    var dimensionCounts = questionScore ? questionScore.dimensionEvaluationCounts : {};
    var dimensionCount = Object.keys(dimensionScores || {}).length;
    var answerEvaluationCount = Object.keys(dimensionCounts || {}).reduce(function (max, key) {
      return Math.max(max, dimensionCounts[key] || 0);
    }, 0);
    detail.innerHTML =
      '<div class="question-detail-head"><span>질문 ' + question.displayOrder + '</span>' +
      '<strong class="' + scoreTone(questionScore && questionScore.finalScore || 0) + '">' +
      (questionScore && questionScore.finalScore != null ? questionScore.finalScore + '점' : '미평가') +
      '</strong></div>' +
      '<h2>' + esc(question.questionText) + '</h2>' +
      '<p class="question-intent">' + esc(question.intent || '질문 의도가 기록되지 않았습니다.') + '</p>' +
      conversationTimeline(question) +
      '<div class="question-score-formula"><div><span>① 답변 분석</span><strong>' +
      answerEvaluationCount + '회</strong></div><i>→</i><div><span>② 핵심 평가 기준</span><strong>' +
      dimensionCount + '개</strong></div><i>→</i><div><span>③ 질문 종합 점수</span><strong>' +
      (questionScore && questionScore.finalScore != null ? questionScore.finalScore + '점' : '미평가') +
      '</strong></div></div>' +
      '<div class="dimension-list">' +
      dimensionItems(dimensionScores, dimensionCounts) + '</div>' +
      '<div class="progress-summary"><div><strong>' + score.submittedQuestionCount +
      '</strong><span>답변한 질문</span></div><div><strong>' + score.evaluatedQuestionCount +
      '</strong><span>평가 성공</span></div><div><strong>' + score.skippedQuestionCount +
      '</strong><span>건너뜀</span></div><div><strong>' + score.completionRate +
      '%</strong><span>완료율</span></div></div>';
  }

  function renderSession(score, questions) {
    document.getElementById('result-title').textContent = modeLabel(score.mode) + ' 결과';
    document.getElementById('result-meta').textContent =
      '질문 ' + score.totalQuestionCount + '개 · 답변 ' + score.submittedQuestionCount +
      '개 · 평가 성공 ' + score.evaluatedQuestionCount + '개';
    renderDonut(score.overallScore);

    var scoreByQuestion = {};
    (score.questionScores || []).forEach(function (item) {
      scoreByQuestion[item.sessionQuestionId] = item;
    });
    var list = document.getElementById('q-list');
    list.innerHTML = questions.map(function (question, index) {
      var item = scoreByQuestion[question.sessionQuestionId];
      return '<button type="button" class="q-list-item' + (index === 0 ? ' is-active' : '') +
        '" data-question-index="' + index + '"><span class="q-list-item__num">' +
        (index + 1) + '</span><span class="q-list-item__summary">' +
        esc(question.questionText) + '</span><strong>' +
        (item && item.finalScore != null ? item.finalScore + '점' : '미평가') + '</strong></button>';
    }).join('');
    if (questions.length) {
      renderQuestionDetail(questions[0], scoreByQuestion[questions[0].sessionQuestionId], score);
    } else {
      document.getElementById('q-detail').innerHTML =
        '<p class="result-empty-copy">표시할 질문이 없습니다.</p>';
    }
    list.querySelectorAll('[data-question-index]').forEach(function (button) {
      button.addEventListener('click', function () {
        list.querySelectorAll('.q-list-item').forEach(function (item) {
          item.classList.toggle('is-active', item === button);
        });
        var question = questions[Number(button.dataset.questionIndex)];
        renderQuestionDetail(question, scoreByQuestion[question.sessionQuestionId], score);
      });
    });

    var categories = Object.keys(score.categoryScores || {}).map(function (key) {
      var questionCount = (score.questionScores || []).filter(function (questionScore) {
        return questionScore.dimensionScores &&
          questionScore.dimensionScores[key] != null;
      }).length;
      return { key: key, value: score.categoryScores[key], questionCount: questionCount };
    }).sort(function (a, b) { return a.value - b.value; });
    document.getElementById('score-method').innerHTML =
      '<div><span>1</span><strong>답변마다 분석</strong><p>첫 답변과 심화 답변을 핵심 기준으로 평가</p></div>' +
      '<i>→</i><div><span>2</span><strong>질문별 점수</strong><p>같은 기준의 답변 점수를 평균</p></div>' +
      '<i>→</i><div><span>3</span><strong>세션 종합</strong><p>질문별 동일 관점과 최종점수를 평균</p></div>';
    document.getElementById('eval-list').innerHTML = categories.length
      ? categories.map(function (item) {
        return '<article class="eval-item-card"><div><strong>' +
          esc(CATEGORY_LABELS[item.key] || item.key) + '</strong>' +
          '<span class="eval-scope">세션 종합 관점</span>' +
          '<p>이 관점이 적용된 질문 ' + item.questionCount +
          '개의 최종 관점점수를 평균했습니다.</p></div>' +
          '<b class="' + scoreTone(item.value) + '">' + item.value + '점</b></article>';
      }).join('')
      : '<p class="result-empty-copy">평가에 성공한 관점이 없습니다.</p>';

    var weaknessList = document.getElementById('weakness-list');
    var weaknessButton = document.getElementById('weakness-practice-link');
    var weaknessHeading = document.getElementById('weakness-heading');
    if (score.mode === 'BASIC') {
      if (weaknessHeading) weaknessHeading.textContent = '이번 연습 안내';
      weaknessList.innerHTML =
        '<p class="weakness-info-title">기본 모드는 약점 태그를 생성하지 않습니다.</p>' +
        '<p class="weakness-info-copy">직무 공통 질문의 답변 점수만 확인할 수 있습니다.</p>';
      if (weaknessButton) weaknessButton.hidden = true;
    } else {
      if (weaknessHeading) weaknessHeading.textContent =
        score.mode === 'WEAKNESS_REVIEW' ? '약점 보완 결과' : '발견된 보완 포인트';
      weaknessList.innerHTML =
        '<p class="weakness-info-copy">70점 미만 평가에서 확인된 약점은 약점 보완 모드에서 연습할 수 있습니다.</p>';
    }
  }

  function formatDate(value) {
    if (!value) return '';
    return new Date(value).toLocaleString('ko-KR', {
      year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit'
    });
  }

  function renderHistory(items) {
    document.getElementById('result-title').textContent = '완료한 면접';
    document.getElementById('result-meta').textContent =
      items.length ? '완료한 면접을 선택해 질문별 점수와 진행 결과를 확인하세요.' :
        '아직 완료한 면접이 없습니다.';
    document.querySelector('.result-layout').classList.add('result-layout--history');
    document.querySelector('.result-sidebar').hidden = true;
    document.querySelector('.result-tabs').hidden = true;
    document.getElementById('q-detail').hidden = true;
    var list = document.getElementById('q-list');
    list.className = 'history-list';
    list.innerHTML = items.length ? items.map(function (item) {
      return '<a class="history-card" href="/interview-result.html?sessionId=' +
        encodeURIComponent(item.sessionId) + '"><div><span>' + esc(modeLabel(item.mode)) +
        '</span><h2>' + item.questionCount + '개 질문 면접</h2><p>' +
        esc(formatDate(item.completedAt)) +
        (item.targetWeaknessTag ? ' · #' + esc(item.targetWeaknessTag) : '') +
        '</p></div><strong>결과 보기</strong></a>';
    }).join('') :
      '<div class="history-empty"><h2>첫 면접을 시작해 보세요</h2>' +
      '<p>답변을 하나 이상 제출하고 정상 완료하면 이곳에 결과가 저장됩니다.</p>' +
      '<a class="btn btn--primary" href="/interview.html">면접 시작</a></div>';
  }

  document.addEventListener('DOMContentLoaded', function () {
    if (!sessionId) {
      api('/api/interview-sessions/history').then(renderHistory).catch(function (error) {
        document.getElementById('result-meta').textContent = error.message;
      });
      return;
    }
    Promise.all([
      api('/api/interview-sessions/' + sessionId + '/score'),
      api('/api/interview-sessions/' + sessionId + '/questions')
    ]).then(function (values) {
      renderSession(values[0], values[1]);
    }).catch(function (error) {
      document.getElementById('overall-score-label').textContent = error.message;
    });
  });
})();
