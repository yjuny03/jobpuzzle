(function () {
  'use strict';

  var root = document.getElementById('analysis-root');
  var caseId = Number((location.pathname.match(/^\/analysis\/(\d+)$/) || [])[1]);
  var running = false;
  var stageTimer = null;
  var visualStage = 0;
  var stageDefinitions = [
    { key: 'jobPostingAnalysisStatus', title: '채용공고 이해', copy: '업무와 필수·우대 요건을 구조화합니다.' },
    { key: 'candidateMaterialAnalysisStatus', title: '지원자 경험 정리', copy: '이력과 프로젝트에서 답변 근거를 찾습니다.' },
    { key: 'guideContextStatus', title: '직무 기준 적용', copy: '선택한 직무와 경력에 맞는 기준을 적용합니다.' },
    { key: 'customizedAnalysisStatus', title: '맞춤 질문 설계', copy: '공고와 경험의 연결 지점으로 질문을 만듭니다.' }
  ];

  function escapeHtml(value) {
    return String(value == null ? '' : value)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;')
      .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }

  function api(path, options) {
    options = options || {};
    return fetch('/api/analysis/cases/' + caseId + path, {
      method: options.method || 'GET',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json' }
    }).then(function (response) {
      return response.json().catch(function () { return {}; }).then(function (body) {
        if (!response.ok || !body.success) {
          var error = new Error(body.message || '요청을 처리하지 못했습니다.');
          error.code = body.code;
          error.status = response.status;
          throw error;
        }
        return body.data;
      });
    });
  }

  function rememberAnalysis(status) {
    var key = 'jobpuzzle_analysis_cases';
    var items;
    try { items = JSON.parse(localStorage.getItem(key) || '[]'); } catch (ignore) { items = []; }
    var previous = items.filter(function (item) {
      return String(item.analysisCaseId) === String(caseId);
    })[0] || {};
    items = items.filter(function (item) { return String(item.analysisCaseId) !== String(caseId); });
    if (status !== 'COMPLETED') {
      previous.analysisCaseId = caseId;
      previous.status = status;
      previous.updatedAt = new Date().toISOString();
      items.unshift(previous);
    }
    localStorage.setItem(key, JSON.stringify(items.slice(0, 10)));
  }

  function statusClass(value, index, analysisStatus) {
    if (value === 'SUCCEEDED') return 'is-done';
    if (value === 'FAILED') return 'is-failed';
    if (value === 'RUNNING') return 'is-active';
    if (analysisStatus === 'ANALYZING' && index === visualStage) return 'is-active';
    return '';
  }

  function statusLabel(value, index, analysisStatus) {
    if (value === 'SUCCEEDED') return '완료';
    if (value === 'FAILED') return '확인 필요';
    if (value === 'RUNNING' || (analysisStatus === 'ANALYZING' && index === visualStage)) return '진행 중';
    return '대기';
  }

  function renderProgress(status) {
    var stages = stageDefinitions.map(function (stage, index) {
      var value = status[stage.key] || 'NOT_STARTED';
      var css = statusClass(value, index, status.analysisCaseStatus);
      return '<article class="analysis-progress-step ' + css + '">' +
        '<div class="analysis-progress-step__index">' + (css === 'is-done' ? '✓' : index + 1) + '</div>' +
        '<div><strong>' + stage.title + '</strong><p>' + stage.copy + '</p>' +
        '<span>' + statusLabel(value, index, status.analysisCaseStatus) + '</span></div></article>';
    }).join('');

    var failure = status.latestFailureMessage
      ? '<div class="analysis-alert"><strong>분석을 이어가지 못했습니다.</strong><p>' +
        escapeHtml(status.latestFailureMessage) + '</p></div>' : '';
    var action = '';
    if (status.analysisCaseStatus === 'INPUT_CONFIRMED' || status.analysisCaseStatus === 'FAILED') {
      action = '<button type="button" class="btn btn--primary analysis-run-button" id="analysis-run">' +
        (status.analysisCaseStatus === 'FAILED' ? '분석 다시 시작' : '맞춤 분석 시작') + '</button>';
    }

    root.innerHTML =
      '<section class="analysis-hero">' +
      '<div class="analysis-orbit" aria-hidden="true"><i></i><span>AI</span></div>' +
      '<div><span class="analysis-kicker">COMPANY FIT</span>' +
      '<h2>' + (status.analysisCaseStatus === 'ANALYZING' ? '자료를 면접 질문으로 연결하고 있어요' : '맞춤 면접을 준비할 자료가 확정됐어요') + '</h2>' +
      '<p>페이지를 나가도 분석은 유지됩니다. 면접 준비 화면에서 언제든 진행 상태를 확인할 수 있어요.</p></div>' +
      '</section><section class="analysis-progress-panel"><header><div><span>분석 진행률</span>' +
      '<strong>' + completedPercent(status) + '%</strong></div><div class="analysis-progress-bar"><i style="width:' +
      completedPercent(status) + '%"></i></div></header><div class="analysis-progress-grid">' + stages +
      '</div>' + failure + '<div class="analysis-progress-actions">' + action + '</div></section>';

    var button = document.getElementById('analysis-run');
    if (button) button.addEventListener('click', runAnalysis);
  }

  function completedPercent(status) {
    var done = stageDefinitions.filter(function (stage) { return status[stage.key] === 'SUCCEEDED'; }).length;
    if (status.analysisCaseStatus === 'COMPLETED') return 100;
    if (status.analysisCaseStatus === 'ANALYZING') return Math.max(12, Math.round((done + .45) / 4 * 100));
    return done * 25;
  }

  function compactList(items, selector, emptyText) {
    var values = (items || []).map(selector).filter(Boolean).slice(0, 4);
    if (!values.length) return '<p class="analysis-summary-empty">' + emptyText + '</p>';
    return '<ul>' + values.map(function (value) { return '<li>' + escapeHtml(value) + '</li>'; }).join('') + '</ul>';
  }

  function renderResult(result) {
    rememberAnalysis('COMPLETED');
    var matches = result.requirementMatches || [];
    var strengths = matches.filter(function (item) { return item.matchLevel === 'HIGH'; });
    var gaps = matches.filter(function (item) { return item.matchLevel !== 'HIGH'; });
    var questions = result.questionSet && result.questionSet.questions || [];
    var guide = result.guideContext || {};

    root.innerHTML =
      '<section class="analysis-complete-hero"><div><span class="analysis-complete-check">✓</span>' +
      '<span class="analysis-kicker">분석 완료</span><h2>면접에서 확인할 핵심이 정리됐어요</h2>' +
      '<p>' + escapeHtml(result.jobCategory.mainCategory + ' · ' + result.jobCategory.subCategory) +
      ' 기준으로 공고와 내 경험을 비교했습니다.</p></div>' +
      '<button type="button" class="btn btn--primary" id="open-company-questions">생성된 질문 ' +
      questions.length + '개 확인</button></section>' +
      '<section class="analysis-summary-grid">' +
      '<article class="analysis-summary-card is-primary"><span>연결된 강점</span><h3>답변에서 살릴 경험</h3>' +
      compactList(strengths, function (item) { return item.requirement; }, '명확하게 연결된 강점을 정리 중입니다.') + '</article>' +
      '<article class="analysis-summary-card is-warn"><span>보완할 지점</span><h3>면접에서 대비할 부분</h3>' +
      compactList(gaps, function (item) { return item.missingPoint || item.requirement; }, '추가로 보완할 항목이 없습니다.') + '</article>' +
      '<article class="analysis-summary-card"><span>적용 기준</span><h3>' + escapeHtml(guide.title || '직무 공통 면접 가이드') + '</h3>' +
      '<p>' + (guide.fallbackApplied ? '세부 가이드가 없어 상위 직무 공통 기준을 적용했습니다.' : '선택한 직무·경력에 정확히 맞는 기준을 적용했습니다.') +
      '</p><small>' + escapeHtml(guide.matchType || '') + '</small></article></section>' +
      '<section class="analysis-next-card"><div><span>다음 단계</span><h3>원하는 질문을 골라 바로 연습하세요</h3>' +
      '<p>질문 의도와 평가 관점은 세션을 시작할 때 함께 저장됩니다.</p></div>' +
      '<button type="button" class="btn btn--primary" id="open-company-questions-bottom">질문 선택하기</button></section>';

    function goToQuestions() {
      location.href = '/interview.html?analysisCaseId=' + encodeURIComponent(caseId);
    }
    document.getElementById('open-company-questions').addEventListener('click', goToQuestions);
    document.getElementById('open-company-questions-bottom').addEventListener('click', goToQuestions);
  }

  function startVisualProgress() {
    clearInterval(stageTimer);
    visualStage = 0;
    stageTimer = setInterval(function () {
      visualStage = Math.min(3, visualStage + 1);
    }, 4500);
  }

  function runAnalysis() {
    if (running) return;
    running = true;
    startVisualProgress();
    loadStatus();
    api('/run', { method: 'POST' }).then(function () {
      running = false;
      clearInterval(stageTimer);
      stageTimer = null;
      loadStatus();
    }).catch(function () {
      running = false;
      clearInterval(stageTimer);
      stageTimer = null;
      loadStatus();
    });
  }

  function loadStatus() {
    api('/status').then(function (status) {
      rememberAnalysis(status.analysisCaseStatus);
      if (status.analysisCaseStatus === 'COMPLETED') {
        clearInterval(stageTimer);
        stageTimer = null;
        return api('/result').then(renderResult);
      }
      renderProgress(status);
      if (status.analysisCaseStatus === 'ANALYZING') {
        if (!stageTimer) startVisualProgress();
        setTimeout(loadStatus, 2500);
      }
    }).catch(function (error) {
      root.innerHTML = '<div class="analysis-alert"><strong>분석 상태를 확인하지 못했습니다.</strong><p>' +
        escapeHtml(error.message) + '</p><a href="/interview.html">면접 준비로 돌아가기</a></div>';
    });
  }

  if (!caseId) {
    root.innerHTML = '<div class="analysis-alert"><strong>올바르지 않은 분석 주소입니다.</strong></div>';
  } else {
    loadStatus();
  }
})();
