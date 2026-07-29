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

  function categoryText(category) {
    if (!category) return '직무 정보 없음';
    return [category.mainCategory, category.subCategory].filter(Boolean).join(' · ') || '직무 정보 없음';
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
    back.href = '/interview.html';
    topbar.appendChild(back);
    topbar.appendChild(node('span', 'result-date', '분석 #' + result.analysisCaseId + ' · 저장된 결과'));
    root.appendChild(topbar);

    var connected = counts.HIGH + counts.MEDIUM + counts.LOW;
    var total = (result.requirementMatches || []).length;
    var percent = total ? Math.round(connected / total * 100) : 0;
    var hero = node('section', 'result-hero');
    var copy = node('div', 'result-hero__copy');
    copy.appendChild(node('p', 'result-eyebrow', 'YOUR JOB FIT REPORT'));
    copy.appendChild(node('h1', null, readinessLabel[result.readiness && result.readiness.status] || '지원 분석 결과가 준비됐어요'));
    copy.appendChild(node('p', 'result-hero__reason',
      result.readiness && result.readiness.reason || '채용공고와 지원 자료에서 확인된 연결 근거를 정리했습니다.'));
    var meta = node('div', 'result-hero__meta');
    [categoryText(result.jobCategory), '질문 ' + questions.length + '개', '보완 과제 ' + (result.actionPlans || []).length + '개']
      .forEach(function (text) { meta.appendChild(node('span', 'result-meta-chip', text)); });
    copy.appendChild(meta);
    hero.appendChild(copy);
    var score = node('div', 'result-score');
    score.appendChild(node('strong', null, percent + '%'));
    score.appendChild(node('span', null, '요구사항 연결'));
    hero.appendChild(score);
    root.appendChild(hero);

    var stats = node('section', 'result-stat-grid', null);
    [
      ['전체 요구사항', total, ''],
      ['충족', counts.HIGH, ' result-stat--good'],
      ['일부 연결', counts.MEDIUM + counts.LOW, ' result-stat--warn'],
      ['예상 질문', questions.length, '']
    ].forEach(function (item) {
      var stat = node('article', 'result-stat' + item[2]);
      stat.appendChild(node('span', null, item[0]));
      stat.appendChild(node('strong', null, String(item[1])));
      stats.appendChild(stat);
    });
    root.appendChild(stats);
  }

  function evidence(label, value, className) {
    var box = node('div', 'result-evidence' + (className ? ' ' + className : ''));
    box.appendChild(node('b', null, label));
    box.appendChild(node('p', null, value || '확인된 내용이 없습니다.'));
    return box;
  }

  function renderMatches(result) {
    var matches = result.requirementMatches || [];
    var questions = result.questionSet && result.questionSet.questions || [];
    var card = section('요구사항 연결 분석', '공고의 조건과 내 자료에서 확인된 근거를 함께 비교해 보세요.', matches.length);
    var filters = node('div', 'result-filters');
    var list = node('div', 'result-match-list');
    [
      ['ALL', '전체'], ['HIGH', '충족'], ['MEDIUM', '일부 충족'],
      ['GAP', '근거 부족'], ['REQUIRED', '필수'], ['PREFERRED', '우대']
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
    matches.forEach(function (match) {
      var item = node('article', 'result-match');
      item.dataset.level = match.matchLevel || 'NONE';
      item.dataset.type = match.requirementType || '';
      var toggle = node('button', 'result-match__toggle');
      toggle.type = 'button';
      toggle.setAttribute('aria-expanded', 'false');
      var copy = node('span');
      copy.appendChild(node('strong', null, match.requirement || '요구사항'));
      var relatedCount = questions.filter(function (question) {
        return question.relatedRequirementId === match.requirementId;
      }).length;
      copy.appendChild(node('small', null,
        (typeLabel[match.requirementType] || match.requirementType || '요구사항')
        + (relatedCount ? ' · 관련 질문 ' + relatedCount + '개' : '')));
      toggle.appendChild(copy);
      toggle.appendChild(node('span', 'result-level result-level--' + (match.matchLevel || 'NONE'),
        levelLabel[match.matchLevel] || '근거 부족'));
      item.appendChild(toggle);
      var detail = node('div', 'result-match__detail');
      detail.appendChild(evidence('확인된 지원자 근거', match.candidateEvidence));
      detail.appendChild(evidence('부족한 근거', match.missingPoint, 'result-evidence--missing'));
      detail.appendChild(evidence('판단 이유', match.reason, 'result-evidence--wide'));
      item.appendChild(detail);
      toggle.addEventListener('click', function () {
        var open = item.classList.toggle('is-open');
        toggle.setAttribute('aria-expanded', String(open));
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
      var button = node('button', 'result-filter' + (index === 0 ? ' is-active' : ''),
        type === 'ALL' ? '전체' : typeLabel[type] || type);
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
      item.appendChild(node('span', 'result-question__type', typeLabel[question.questionType] || question.questionType || '질문'));
      item.appendChild(node('h3', null, question.question));
      var details = node('details');
      details.appendChild(node('summary', null, '질문 의도와 평가 포인트'));
      var detail = node('div', 'result-question__detail');
      appendTextLine(detail, '질문 의도', question.intent || '지원자의 경험을 구체적으로 확인합니다.');
      appendTextLine(detail, '평가 포인트', focusText(question.evaluationFocus));
      details.appendChild(detail);
      item.appendChild(details);
      list.appendChild(item);
    });
    card.appendChild(list);
    return card;
  }

  function insight(requirement, message, warning) {
    var item = node('div', 'result-insight' + (warning ? ' result-insight--warn' : ''));
    item.appendChild(node('span', 'result-insight__icon', warning ? '!' : '✓'));
    var copy = node('div');
    copy.appendChild(node('strong', null, requirement || (warning ? '보완할 근거' : '연결된 강점')));
    copy.appendChild(node('p', null, message || '세부 분석에서 내용을 확인해 주세요.'));
    item.appendChild(copy);
    return item;
  }

  function renderInsights(result) {
    var matches = result.requirementMatches || [];
    var card = section('핵심 인사이트', '먼저 확인할 강점과 보완점입니다.');
    var list = node('div', 'result-insight-list');
    matches.filter(function (match) { return match.matchLevel === 'HIGH'; }).slice(0, 2)
      .forEach(function (match) { list.appendChild(insight(match.requirement, match.candidateEvidence, false)); });
    matches.filter(function (match) { return match.matchLevel === 'NONE' || match.matchLevel === 'INSUFFICIENT'; }).slice(0, 2)
      .forEach(function (match) { list.appendChild(insight(match.requirement, match.missingPoint, true)); });
    if (!list.children.length) list.appendChild(node('p', 'result-empty', '표시할 핵심 인사이트가 없습니다.'));
    card.appendChild(list);
    return card;
  }

  function renderPlans(result) {
    var plans = result.actionPlans || [];
    var card = section('우선 보완할 내용', '면접 전에 준비하면 좋은 순서입니다.', plans.length);
    var list = node('div', 'result-plan-list');
    plans.slice(0, 4).forEach(function (plan, index) {
      var item = node('article', 'result-plan');
      item.appendChild(node('small', null, 'PRIORITY ' + String(index + 1).padStart(2, '0')));
      item.appendChild(node('strong', null, plan.missingPoint || '보완할 근거'));
      item.appendChild(node('p', null, plan.suggestion || '관련 경험을 구체적인 사례로 정리해 보세요.'));
      list.appendChild(item);
    });
    if (!plans.length) list.appendChild(node('p', 'result-empty', '현재 등록된 보완 과제가 없습니다.'));
    card.appendChild(list);
    return card;
  }

  function renderCta(result, questionCount) {
    var card = node('section', 'result-cta');
    var canStart = questionCount > 0 && result.interviewStartAllowed === true;
    var linkedSessionId = result.linkedSessionId;
    var completed = result.linkedSessionStatus === 'COMPLETED';
    card.appendChild(node('h2', null, canStart
      ? '분석을 연습으로 연결해 보세요'
      : linkedSessionId ? '이 분석으로 진행한 면접이 있어요' : '분석 결과를 확인했어요'));
    card.appendChild(node('p', null, canStart
      ? '생성된 ' + questionCount + '개 질문으로 맞춤 면접을 시작할 수 있어요.'
      : linkedSessionId
        ? (completed ? '완료된 면접 결과에서 답변과 평가를 다시 확인할 수 있어요.'
          : '분석 결과를 확인한 뒤 진행 중인 면접으로 돌아갈 수 있어요.')
        : '자료를 보완한 뒤 새로운 맞춤 분석을 진행해 보세요.'));
    var link = node('a', 'result-cta__button',
      canStart ? '맞춤 면접 시작'
        : linkedSessionId ? (completed ? '완료한 면접 결과 보기' : '진행 중인 면접으로 돌아가기')
          : '면접 준비로 이동');
    link.href = canStart
      ? '/interview.html?analysisCaseId=' + encodeURIComponent(caseId)
      : linkedSessionId
        ? (completed
          ? '/interview-result.html?sessionId=' + encodeURIComponent(linkedSessionId)
          : '/interview.html?resumeSessionId=' + encodeURIComponent(linkedSessionId))
        : '/interview.html';
    card.appendChild(link);
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
    var side = node('aside', 'result-side');
    side.appendChild(renderInsights(result));
    side.appendChild(renderPlans(result));
    side.appendChild(renderCta(result, questions.length));
    layout.appendChild(main);
    layout.appendChild(side);
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
    if (root) renderUnavailable('유효하지 않은 분석 결과 주소입니다.', '/interview.html', '면접 준비로 이동');
    return;
  }

  api('/api/analysis/cases/' + caseId + '/status')
    .then(function (status) {
      if (status.analysisCaseStatus !== 'COMPLETED') {
        renderUnavailable('분석이 진행 중이거나 결과가 아직 준비되지 않았습니다.',
          '/api/analysis/' + encodeURIComponent(caseId), '분석 상태 확인');
        return null;
      }
      return api('/api/analysis/cases/' + caseId + '/result');
    })
    .then(function (result) { if (result) render(result); })
    .catch(function (error) {
      var message = error.status === 403
        ? '이 분석 결과를 볼 권한이 없습니다.'
        : '잠시 후 다시 시도해 주세요.';
      renderUnavailable(message, '/interview.html', '면접 준비로 이동');
    });
})();
