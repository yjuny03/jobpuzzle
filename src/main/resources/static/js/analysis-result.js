// analysis-result.js — 완료된 맞춤 분석을 읽기 전용 리포트로 표시
(function () {
  'use strict';

  var root = document.getElementById('analysis-result-root');
  var caseId = root && root.dataset.analysisCaseId;
  var levelLabel = { HIGH: '충족', MEDIUM: '일부 충족', LOW: '낮은 연결', NONE: '근거 부족', INSUFFICIENT: '판단 보류' };
  var typeLabel = {
    REQUIRED: '필수', PREFERRED: '우대', EXPERIENCE: '경험', SKILL: '기술',
    PROBLEM_SOLVING: '문제 해결', COMPANY_FIT: '직무 적합', GENERAL: '일반'
  };
  var readinessLabel = {
    SUFFICIENT: '지원 근거가 충분히 연결됐어요',
    PARTIAL: '보완하면 더 강해질 수 있어요',
    CANDIDATE_LACK: '지원 경험을 조금 더 보완해 주세요',
    POSTING_LACK: '공고 요구사항을 충분히 확인하기 어려워요',
    GUIDE_LACK: '직무 기준을 충분히 적용하기 어려워요'
  };

  function node(tag, className, text) {
    var value = document.createElement(tag);
    if (className) value.className = className;
    if (text != null) value.textContent = text;
    return value;
  }

  function api(path) {
    return fetch(path, { credentials: 'same-origin' }).then(function (response) {
      return response.json().catch(function () { return {}; }).then(function (body) {
        if (!response.ok || !body.success) {
          var error = new Error(body.message || '결과를 불러오지 못했습니다.');
          error.status = response.status;
          error.code = body.code;
          throw error;
        }
        return body.data;
      });
    });
  }

  function countByLevel(matches) {
    return matches.reduce(function (result, match) {
      var key = match.matchLevel || 'NONE';
      result[key] = (result[key] || 0) + 1;
      return result;
    }, { HIGH: 0, MEDIUM: 0, LOW: 0, NONE: 0, INSUFFICIENT: 0 });
  }

  // 충족도별 연결 강도를 반영해 전체 요구사항 연결 점수를 계산합니다.
  function calculateConnectionScore(counts, total) {
    if (!total) return 0;
    // LOW와 MEDIUM을 완전 충족으로 세지 않아 약한 연결이 점수를 과도하게 높이지 않도록 합니다.
    var weightedSum = counts.HIGH * 100 + counts.MEDIUM * 50 + counts.LOW * 25;
    return Math.round(weightedSum / total);
  }

  function categoryText(category) {
    if (!category) return '직무 정보 없음';
    return [category.mainCategory, category.subCategory].filter(Boolean).join(' · ') || '직무 정보 없음';
  }

  function isConnectedLevel(level) {
    return level === 'HIGH' || level === 'MEDIUM' || level === 'LOW';
  }

  function sourceTypeLabel(type) {
    return {
      RESUME: '이력서', COVER_LETTER: '자기소개서', PORTFOLIO: '포트폴리오',
      EXPERIENCE_NOTE: '경험정리', JOB_POSTING: '채용공고', COMPANY_INFO: '회사정보'
    }[type] || type;
  }

  function candidateSourceTypes(matches) {
    var types = [];
    matches.forEach(function (match) {
      (Array.isArray(match.candidateSourceRefs) ? match.candidateSourceRefs : []).forEach(function (ref) {
        var label = sourceTypeLabel(ref.documentType || '지원 자료');
        if (label && types.indexOf(label) < 0) types.push(label);
      });
    });
    return types;
  }

  function section(title, description, count) {
    var card = node('section', 'result-card');
    var head = node('header', 'result-card__head');
    var copy = node('div');
    copy.appendChild(node('h2', null, title));
    if (description) copy.appendChild(node('p', null, description));
    head.appendChild(copy);
    if (count != null) head.appendChild(node('span', 'result-count', String(count)));
    card.appendChild(head);
    return card;
  }

  function appendTextLine(parent, label, value) {
    var line = node('p');
    line.appendChild(node('strong', null, label + ' · '));
    line.appendChild(document.createTextNode(value || '확인된 내용이 없습니다.'));
    parent.appendChild(line);
  }

  function focusText(value) {
    if (Array.isArray(value)) return value.join(' · ');
    if (value && typeof value === 'object') return Object.keys(value).map(function (key) { return value[key]; }).join(' · ');
    return value ? String(value) : '별도 평가 포인트 없음';
  }

  function renderTop(result, counts, questions) {
    var topbar = node('div', 'result-topbar');
    var back = node('a', 'result-breadcrumb', '면접 준비로 돌아가기');
    back.href = window.JobPuzzleRoutes.path('/interview');
    topbar.appendChild(back);
    topbar.appendChild(node('span', 'result-date', '분석 #' + result.analysisCaseId + ' · 저장된 결과'));
    root.appendChild(topbar);

    var total = (result.requirementMatches || []).length;
    var percent = calculateConnectionScore(counts, total);
    var hero = node('section', 'result-hero');
    var copy = node('div', 'result-hero__copy');
    copy.appendChild(node('p', 'result-eyebrow', 'YOUR JOB FIT REPORT'));
    copy.appendChild(node('h1', null, readinessLabel[result.readiness && result.readiness.status] || '지원 분석 결과가 준비됐어요'));
    copy.appendChild(node('p', 'result-hero__reason',
      result.readiness && result.readiness.reason || '채용공고와 지원 자료에서 확인된 연결 근거를 정리했습니다.'));
    var meta = node('div', 'result-hero__meta');
    [categoryText(result.jobCategory), '질문 ' + questions.length + '개']
      .forEach(function (text) { meta.appendChild(node('span', 'result-meta-chip', text)); });
    copy.appendChild(meta);
    hero.appendChild(copy);
    var puzzle = node('canvas', 'result-puzzle-canvas');
    puzzle.dataset.jobPuzzleScene = '';
    puzzle.dataset.resultPuzzle = '';
    puzzle.setAttribute('aria-hidden', 'true');
    hero.appendChild(puzzle);
    var scoreTone = percent >= 70 ? 'good' : percent >= 40 ? 'medium' : 'low';
    var scoreWrap = node('div', 'result-score-wrap');
    var score = node('div', 'result-score result-score--' + scoreTone);
    score.style.setProperty('--result-score', percent);
    score.appendChild(node('strong', null, percent + '%'));
    scoreWrap.appendChild(score);
    scoreWrap.appendChild(node('span', 'result-score-label', '요구사항 연결'));
    hero.appendChild(scoreWrap);
    root.appendChild(hero);

    root.appendChild(renderDistribution(result, counts, questions));
  }

  function renderDistribution(result, counts, questions) {
    var matches = result.requirementMatches || [];
    var total = matches.length;
    var required = matches.filter(function (match) { return match.requirementType === 'REQUIRED'; });
    var preferred = matches.filter(function (match) { return match.requirementType === 'PREFERRED'; });
    var requiredConnected = required.filter(function (match) { return isConnectedLevel(match.matchLevel); }).length;
    var preferredConnected = preferred.filter(function (match) { return isConnectedLevel(match.matchLevel); }).length;
    var sourceTypes = candidateSourceTypes(matches);
    var gapCount = counts.NONE + counts.INSUFFICIENT;
    var groups = [
      ['high', '충족', counts.HIGH],
      ['medium', '일부 충족', counts.MEDIUM],
      ['low', '낮은 연결', counts.LOW],
      ['gap', '근거 부족', gapCount]
    ];
    var card = node('section', 'result-distribution');
    var head = node('div', 'result-distribution__head');
    var copy = node('div');
    copy.appendChild(node('p', 'result-distribution__eyebrow', 'REQUIREMENT COVERAGE'));
    copy.appendChild(node('h2', null, '전체 요구사항 ' + total + '개'));
    copy.appendChild(node('p', null, '지원 자료와 연결된 정도를 한눈에 확인할 수 있어요.'));
    head.appendChild(copy);
    var questionBadge = node('div', 'result-question-badge');
    questionBadge.appendChild(node('span', null, '예상 질문'));
    questionBadge.appendChild(node('strong', null, questions.length + '개'));
    head.appendChild(questionBadge);
    card.appendChild(head);

    var bar = node('div', 'result-distribution__bar');
    bar.setAttribute('role', 'img');
    bar.setAttribute('aria-label', '요구사항 연결 분포');
    groups.forEach(function (group) {
      var ratio = total ? Math.round(group[2] / total * 100) : 0;
      var segment = node('span', 'result-distribution__segment result-distribution__segment--' + group[0]);
      segment.style.width = ratio + '%';
      segment.title = group[1] + ' ' + group[2] + '개 · ' + ratio + '%';
      bar.appendChild(segment);
    });
    card.appendChild(bar);

    var legend = node('div', 'result-distribution__legend');
    groups.forEach(function (group) {
      var ratio = total ? Math.round(group[2] / total * 100) : 0;
      var item = node('div', 'result-distribution__legend-item');
      item.appendChild(node('i', 'result-distribution__dot result-distribution__dot--' + group[0]));
      item.appendChild(node('span', null, group[1]));
      item.appendChild(node('strong', null, group[2] + '개'));
      item.appendChild(node('small', null, ratio + '%'));
      legend.appendChild(item);
    });
    card.appendChild(legend);

    var facts = node('div', 'result-fact-grid');
    [
      ['필수 요구사항', requiredConnected + ' / ' + required.length + '개 연결'],
      ['우대 요구사항', preferredConnected + ' / ' + preferred.length + '개 연결'],
      ['사용된 지원 자료', sourceTypes.length ? sourceTypes.join(' · ') : '연결 자료 없음']
    ].forEach(function (fact) {
      var item = node('article', 'result-fact');
      item.appendChild(node('span', null, fact[0]));
      item.appendChild(node('strong', null, fact[1]));
      facts.appendChild(item);
    });
    card.appendChild(facts);

    var limitations = result.readiness && result.readiness.limitations || [];
    if (limitations.length) {
      var warning = node('div', 'result-limitations');
      warning.appendChild(node('span', 'result-limitations__icon', '!'));
      var warningCopy = node('div');
      warningCopy.appendChild(node('strong', null, '분석할 때 함께 확인해 주세요'));
      var warningList = node('ul');
      limitations.forEach(function (item) { warningList.appendChild(node('li', null, item)); });
      warningCopy.appendChild(warningList);
      warning.appendChild(warningCopy);
      card.appendChild(warning);
    }
    return card;
  }

  function evidence(label, value, className) {
    var box = node('div', 'result-evidence' + (className ? ' ' + className : ''));
    box.appendChild(node('b', null, label));
    box.appendChild(node('p', null, value || '확인된 내용이 없습니다.'));
    return box;
  }

  function sourceSummary(refs) {
    if (!Array.isArray(refs) || !refs.length) return '연결된 원문 위치가 없습니다.';
    return refs.map(function (ref) {
      var sourceType = {
        RESUME: '이력서', COVER_LETTER: '자기소개서', PORTFOLIO: '포트폴리오',
        EXPERIENCE_NOTE: '경험정리', JOB_POSTING: '채용공고', COMPANY_INFO: '회사정보'
      }[ref.documentType] || ref.documentType || '지원 자료';
      return sourceType + (ref.pageNumber ? ' · ' + ref.pageNumber + '페이지' : '');
    }).filter(function (value, index, all) {
      return all.indexOf(value) === index;
    }).join(', ');
  }

  function renderMatches(result) {
    var matches = result.requirementMatches || [];
    var questions = result.questionSet && result.questionSet.questions || [];
    var card = section('요구사항 연결 분석', '공고의 조건과 내 자료에서 확인된 근거를 함께 비교해 보세요.', matches.length);
    var filters = node('div', 'result-filters');
    var list = node('div', 'result-match-list');
    [
      ['ALL', '전체 ' + matches.length],
      ['HIGH', '충족 ' + matches.filter(function (match) { return match.matchLevel === 'HIGH'; }).length],
      ['MEDIUM', '일부 충족 ' + matches.filter(function (match) { return match.matchLevel === 'MEDIUM' || match.matchLevel === 'LOW'; }).length],
      ['GAP', '근거 부족 ' + matches.filter(function (match) { return match.matchLevel === 'NONE' || match.matchLevel === 'INSUFFICIENT'; }).length],
      ['REQUIRED', '필수 ' + matches.filter(function (match) { return match.requirementType === 'REQUIRED'; }).length],
      ['PREFERRED', '우대 ' + matches.filter(function (match) { return match.requirementType === 'PREFERRED'; }).length]
    ].forEach(function (filter, index) {
      var button = node('button', 'result-filter' + (index === 0 ? ' is-active' : ''), filter[1]);
      button.type = 'button';
      button.addEventListener('click', function () {
        filters.querySelectorAll('.result-filter').forEach(function (item) { item.classList.remove('is-active'); });
        button.classList.add('is-active');
        list.querySelectorAll('.result-match').forEach(function (item) {
          var matchesFilter = filter[0] === 'ALL'
            || item.dataset.level === filter[0]
            || item.dataset.type === filter[0]
            || (filter[0] === 'GAP' && /^(NONE|INSUFFICIENT)$/.test(item.dataset.level));
          item.hidden = !matchesFilter;
        });
      });
      filters.appendChild(button);
    });
    card.appendChild(filters);
    if (!matches.length) {
      card.appendChild(node('p', 'result-empty', '표시할 연결 분석이 없습니다.'));
      return card;
    }
    matches.forEach(function (match, index) {
      var item = node('article', 'result-match');
      item.dataset.level = match.matchLevel || 'NONE';
      item.dataset.type = match.requirementType || '';
      var toggle = node('button', 'result-match__toggle');
      toggle.type = 'button';
      toggle.setAttribute('aria-expanded', 'false');
      toggle.appendChild(node('span', 'result-match__index', String(index + 1).padStart(2, '0')));
      var copy = node('span');
      copy.appendChild(node('strong', null, match.requirement || '요구사항'));
      var relatedCount = questions.filter(function (question) {
        return question.relatedRequirementId === match.requirementId;
      }).length;
      copy.appendChild(node('small', null,
        (typeLabel[match.requirementType] || match.requirementType || '요구사항')
        + ' · 연결 자료 ' + (Array.isArray(match.candidateSourceRefs) ? match.candidateSourceRefs.length : 0) + '개'
        + (relatedCount ? ' · 관련 질문 ' + relatedCount + '개' : '')));
      toggle.appendChild(copy);
      var aside = node('span', 'result-match__aside');
      aside.appendChild(node('span', 'result-level result-level--' + (match.matchLevel || 'NONE'),
        levelLabel[match.matchLevel] || '근거 부족'));
      var expand = node('span', 'result-match__expand');
      expand.appendChild(node('span', 'result-match__expand-text', '상세 보기'));
      expand.appendChild(node('i', 'result-match__chevron'));
      aside.appendChild(expand);
      toggle.appendChild(aside);
      item.appendChild(toggle);
      var detail = node('div', 'result-match__detail');
      detail.appendChild(evidence('확인된 지원자 근거', match.candidateEvidence, 'result-evidence--confirmed'));
      detail.appendChild(evidence('부족한 근거', match.missingPoint, 'result-evidence--missing'));
      detail.appendChild(evidence('판단 이유', match.reason, 'result-evidence--reason result-evidence--wide'));
      detail.appendChild(evidence('사용된 자료', sourceSummary(match.candidateSourceRefs), 'result-evidence--source result-evidence--wide'));
      item.appendChild(detail);
      toggle.addEventListener('click', function () {
        var open = item.classList.toggle('is-open');
        toggle.setAttribute('aria-expanded', String(open));
        expand.querySelector('.result-match__expand-text').textContent = open ? '상세 닫기' : '상세 보기';
      });
      list.appendChild(item);
    });
    card.appendChild(list);
    return card;
  }

  function renderQuestions(result) {
    var questions = result.questionSet && result.questionSet.questions || [];
    var card = section('예상 면접 질문', '분석 근거를 바탕으로 실제 면접에서 확인할 가능성이 높은 질문입니다.', questions.length);
    if (!questions.length) {
      card.appendChild(node('p', 'result-empty', '현재 자료에서는 생성된 질문이 없습니다.'));
      return card;
    }
    var filters = node('div', 'result-filters');
    var list = node('div', 'result-question-list');
    var types = ['ALL'].concat(questions.map(function (question) { return question.questionType; })
      .filter(function (value, index, all) { return all.indexOf(value) === index; }));
    types.forEach(function (type, index) {
      var count = type === 'ALL'
        ? questions.length
        : questions.filter(function (question) { return question.questionType === type; }).length;
      var button = node('button', 'result-filter' + (index === 0 ? ' is-active' : ''),
        (type === 'ALL' ? '전체' : typeLabel[type] || type) + ' ' + count);
      button.type = 'button';
      button.addEventListener('click', function () {
        filters.querySelectorAll('.result-filter').forEach(function (item) { item.classList.remove('is-active'); });
        button.classList.add('is-active');
        list.querySelectorAll('.result-question').forEach(function (item) {
          item.hidden = type !== 'ALL' && item.dataset.type !== type;
        });
      });
      filters.appendChild(button);
    });
    card.appendChild(filters);
    questions.slice().sort(function (a, b) { return a.displayOrder - b.displayOrder; }).forEach(function (question, index) {
      var item = node('article', 'result-question');
      item.dataset.type = question.questionType || '';
      item.appendChild(node('span', 'result-question__no', String(index + 1).padStart(2, '0')));
      item.appendChild(node('span', 'result-question__type result-question__type--' + (question.questionType || 'GENERAL'),
        typeLabel[question.questionType] || question.questionType || '질문'));
      item.appendChild(node('h3', null, question.question));
      var details = node('details');
      details.appendChild(node('summary', null, '질문 의도와 평가 포인트'));
      var detail = node('div', 'result-question__detail');
      appendTextLine(detail, '질문 의도', question.intent || '지원자의 경험을 구체적으로 확인합니다.');
      appendTextLine(detail, '평가 포인트', focusText(question.evaluationFocus));
      if (question.relatedRequirementId) {
        appendTextLine(detail, '관련 요구사항', question.relatedRequirementId);
      }
      details.appendChild(detail);
      item.appendChild(details);
      list.appendChild(item);
    });
    card.appendChild(list);
    var actions = node('div', 'result-question-actions');
    var interviewLink = node('a', 'result-question-actions__button', '질문을 선택하고 면접 시작');
    interviewLink.href = window.JobPuzzleRoutes.path('/interview?analysisCaseId=' + encodeURIComponent(caseId));
    actions.appendChild(interviewLink);
    card.appendChild(actions);
    return card;
  }

  function renderPlans(result) {
    var plans = result.actionPlans || [];
    var card = section('보완 과제', '분석 결과를 바탕으로 추천된 과제입니다. 일정과 완료 상태는 액션플랜에서 관리할 수 있습니다.', plans.length);
    if (!plans.length) {
      card.appendChild(node('p', 'result-empty', '이 분석에서 생성된 액션플랜이 없습니다.'));
      return card;
    }
    var list = node('div', 'result-action-plan-list');
    // 상단 개수와 실제 목록이 어긋나지 않도록 이 분석에서 생성된 과제를 모두 표시한다.
    plans.forEach(function (plan, index) {
      var item = node('article', 'result-action-plan');
      item.appendChild(node('span', 'result-action-plan__number', String(index + 1)));
      var copy = node('div', 'result-action-plan__copy');
      copy.appendChild(node('strong', null, plan.suggestion || '보완 과제'));
      if (plan.matchLevel) {
        copy.appendChild(node('span',
          'result-action-plan__level result-action-plan__level--' + String(plan.matchLevel).toLowerCase(),
          plan.matchLevel));
      }
      copy.appendChild(node('p', null, plan.missingPoint || '보완이 필요한 내용을 확인해 주세요.'));
      item.appendChild(copy);
      list.appendChild(item);
    });
    card.appendChild(list);
    var allLink = node('a', 'result-action-plan__all', '액션플랜에서 일정 관리하기 →');
    allLink.href = window.JobPuzzleRoutes.path('/action-plans') + '?analysisCaseId=' + encodeURIComponent(result.analysisCaseId);
    card.appendChild(allLink);
    return card;
  }

  function render(result) {
    root.replaceChildren();
    root.setAttribute('aria-busy', 'false');
    var matches = result.requirementMatches || [];
    var questions = result.questionSet && result.questionSet.questions || [];
    var counts = countByLevel(matches);
    renderTop(result, counts, questions);
    var layout = node('div', 'result-layout');
    var main = node('div', 'result-main');
    main.appendChild(renderMatches(result));
    main.appendChild(renderQuestions(result));
    main.appendChild(renderPlans(result));
    layout.appendChild(main);
    root.appendChild(layout);
  }

  function renderUnavailable(message, href, label) {
    root.replaceChildren();
    root.setAttribute('aria-busy', 'false');
    var card = node('section', 'result-error');
    card.appendChild(node('h1', null, '분석 결과를 아직 열 수 없어요'));
    card.appendChild(node('p', null, message));
    var link = node('a', 'btn btn--primary', label);
    link.href = href;
    link.style.textDecoration = 'none';
    card.appendChild(link);
    root.appendChild(card);
  }

  if (!root || !caseId || !/^\d+$/.test(caseId) || Number(caseId) <= 0) {
    if (root) renderUnavailable('유효하지 않은 분석 결과 주소입니다.', window.JobPuzzleRoutes.path('/interview'), '면접 준비로 이동');
    return;
  }

  api(window.JobPuzzleRoutes.path('/analysis/cases/' + caseId + '/status'))
    .then(function (status) {
      if (status.analysisCaseStatus !== 'COMPLETED') {
        renderUnavailable('분석이 진행 중이거나 결과가 아직 준비되지 않았습니다.',
          window.JobPuzzleRoutes.path('/analysis/' + encodeURIComponent(caseId)), '분석 상태 확인');
        return null;
      }
      return api(window.JobPuzzleRoutes.path('/analysis/cases/' + caseId + '/result'));
    })
    .then(function (result) { if (result) render(result); })
    .catch(function (error) {
      var message = error.status === 403
        ? '이 분석 결과를 볼 권한이 없습니다.'
        : '잠시 후 다시 시도해 주세요.';
      renderUnavailable(message, window.JobPuzzleRoutes.path('/interview'), '면접 준비로 이동');
    });
})();
