(function () {
  'use strict';

  var sessionId = new URLSearchParams(window.location.search).get('sessionId');
  var sessionInfo = null;
  var remainingQuestions = [];
  var sessionQuestions = [];
  var pendingSelectedQuestions = [];
  var sessionScoreByQuestion = {};
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

  // 면접 결과 화면의 안내와 오류를 공통 토스트 디자인으로 표시합니다.
  function notify(type, message, duration) {
    if (typeof window.showToast === 'function') {
      return window.showToast(type, message, duration);
    }
    return null;
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

  function weaknessLabel(tag) {
    var original = String(tag || '');
    if (/[가-힣]/.test(original)) return original;
    var value = original.toLowerCase();
    var compact = value.replace(/[_\-\s]/g, '');
    if (compact.indexOf('lackofspecificdetail') >= 0) return '구체성 부족';
    if (compact.indexOf('lackoftechnicaldepth') >= 0) return '기술적 깊이 부족';
    if (compact.indexOf('insufficientcloudexperience') >= 0) return '클라우드 경험 부족';
    if (compact.indexOf('vaguerole') >= 0) return '역할 설명 부족';
    if (compact.indexOf('limitedownership') >= 0) return '주도성 부족';
    if (value.indexOf('requirementconnection') >= 0) return '공고 요구사항 연결 부족';
    if (value.indexOf('specificity') >= 0) return '답변의 구체성 부족';
    if (value.indexOf('ownrole') >= 0) return '본인 역할 설명 부족';
    if (value.indexOf('problemsolving') >= 0) return '문제 해결 과정 부족';
    if (value.indexOf('resultexpression') >= 0) return '성과·결과 표현 부족';
    if (value.indexOf('guidealignment') >= 0) return '직무 기준 연결 부족';
    if (value.indexOf('deliveryclarity') >= 0) return '답변 전달력 부족';
    if (value.indexOf('intentmatch') >= 0) return '질문 의도 파악 부족';
    return '세부 진단 보완 필요';
  }

  var QUESTION_TYPE_LABELS = {
    GENERAL: '일반', COMPANY_FIT: '회사 맞춤', EXPERIENCE: '경험', PROBLEM_SOLVING: '문제 해결', SKILL: '기술',
    SELF_INTRO: '자기소개', MOTIVATION: '지원 동기', STRENGTH_WEAKNESS: '강점·약점',
    FAILURE_CONFLICT: '실패·갈등 경험', JOB_GENERAL: '직무 일반', WEAKNESS_FOLLOWUP: '약점 보완 후속'
  };
  var IMPROVEMENT_TARGET_LABELS = {
    resume: '이력서', coverLetter: '자기소개서', portfolio: '포트폴리오', experienceNote: '경험정리'
  };

  function jumpToQuestionIndex(index) {
    var listTabButton = document.querySelector('.tabbar__btn[data-tab="list"]');
    if (listTabButton) listTabButton.click();

    var listButton = document.querySelector('.q-list-item[data-question-index="' + index + '"]');
    if (listButton) {
      listButton.click();
      listButton.scrollIntoView({ block: 'nearest' });
    }
  }

  // 이 약점 태그가 달린 질문들의 인덱스를 전부 찾는다. sessionQuestions가 아직 안 채워졌으면 빈 배열.
  function questionIndicesForTag(tag) {
    var indices = [];
    sessionQuestions.forEach(function (question, index) {
      var item = sessionScoreByQuestion[question.sessionQuestionId];
      if (item && item.weaknessTags && item.weaknessTags.indexOf(tag) !== -1) {
        indices.push(index);
      }
    });
    return indices;
  }

  // 최종 리포트와 질문별 결과(/score) 로딩 순서가 보장되지 않으므로, 둘 중 하나가 끝날 때마다 다시 채운다.
  function renderWeaknessQuestionLinks() {
    document.querySelectorAll('.weakness-tag-row__questions[data-tag]').forEach(function (container) {
      var indices = questionIndicesForTag(container.dataset.tag);
      container.innerHTML = indices.map(function (index) {
        return '<button type="button" class="weakness-question-chip" data-question-index="' + index + '">' +
          (index + 1) + '</button>';
      }).join('');
    });
    document.querySelectorAll('.weakness-question-chip[data-question-index]').forEach(function (button) {
      button.addEventListener('click', function () {
        jumpToQuestionIndex(Number(button.dataset.questionIndex));
      });
    });
  }

  // JSON-07 overallAssessment는 "[잘한 점]/[부족한 점] (세션 종합 점수: N점)/[총평]" 형식의 단일 문자열이다.
  // 마커를 못 찾으면(v1.5 이전 리포트 등) 통짜 텍스트 블록 하나로 대체 표시한다.
  var ASSESSMENT_SECTIONS = ['[잘한 점]', '[부족한 점]', '[총평]'];

  function formatParagraph(text) {
    return esc(text).replace(/\n+/g, '<br>');
  }

  function formatAssessmentBody(text) {
    var lines = text.split('\n').map(function (line) { return line.trim(); }).filter(Boolean);
    var isBulletList = lines.length > 0 && lines.every(function (line) { return line.indexOf('-') === 0; });
    if (!isBulletList) return '<p>' + formatParagraph(text) + '</p>';
    return '<ul class="final-assessment-bullets">' + lines.map(function (line) {
      return '<li>' + esc(line.replace(/^-\s*/, '')) + '</li>';
    }).join('') + '</ul>';
  }

  function splitAssessmentSections(text) {
    var positions = ASSESSMENT_SECTIONS.map(function (marker) { return text.indexOf(marker); });
    if (positions.some(function (position) { return position === -1; })) return null;
    var sections = {};
    for (var i = 0; i < ASSESSMENT_SECTIONS.length; i++) {
      var start = positions[i] + ASSESSMENT_SECTIONS[i].length;
      var end = i + 1 < ASSESSMENT_SECTIONS.length ? positions[i + 1] : text.length;
      sections[ASSESSMENT_SECTIONS[i]] = text.slice(start, end).trim();
    }
    return sections;
  }

  // "부족한 점" 섹션 첫 줄에 v1.8 이전 리포트는 "(세션 종합 점수: N점)"이 남아있을 수 있다.
  // 종합 점수는 이제 그래프로 따로 보여주므로 본문에서는 그냥 잘라내고 버린다.
  function stripLeadingParenthetical(text) {
    return text.replace(/^\([^)]*\)\s*/, '');
  }

  function renderOverallAssessment(rawText) {
    if (!rawText) return '';
    var sections = splitAssessmentSections(rawText);
    if (!sections) {
      return '<div class="final-assessment"><div class="final-assessment-block">' +
        formatAssessmentBody(rawText) + '</div></div>';
    }
    var blocks = [
      { title: '잘한 점', body: sections['[잘한 점]'] },
      { title: '부족한 점', body: stripLeadingParenthetical(sections['[부족한 점]']) },
      { title: '총평', body: sections['[총평]'] }
    ];
    return '<div class="final-assessment">' + blocks.map(function (block) {
      return '<div class="final-assessment-block">' +
        '<div class="final-assessment-block__head"><strong>' + esc(block.title) + '</strong></div>' +
        formatAssessmentBody(block.body) + '</div>';
    }).join('') + '</div>';
  }

  // 관점별 점수를 상단 요약 영역에 시각화한다. 3개 이상이면 레이더, 1~2개면 큰 도넛, 0개면 빈 상태 문구.
  function dimensionEntries(categoryScores) {
    return Object.keys(CATEGORY_LABELS)
      .filter(function (key) { return categoryScores && categoryScores[key] != null; })
      .map(function (key) { return { key: key, value: categoryScores[key] }; });
  }

  // AI 호출 없이 지금 있는 점수만으로 계산하는 한 줄 요약
  function dimensionSummaryLine(entries) {
    if (!entries.length) return '';
    if (entries.length === 1) {
      var only = entries[0];
      return '이번 세션은 ' + esc(CATEGORY_LABELS[only.key] || only.key) + ' 관점만 평가됐어요 (' + only.value + '점).';
    }
    var sorted = entries.slice().sort(function (a, b) { return b.value - a.value; });
    var best = sorted[0];
    var worst = sorted[sorted.length - 1];
    if (best.value === worst.value) {
      return '관점별 점수가 ' + best.value + '점으로 고르게 나왔어요.';
    }
    return '가장 강한 관점은 ' + esc(CATEGORY_LABELS[best.key] || best.key) + '(' + best.value + '점), 가장 약한 관점은 ' +
      esc(CATEGORY_LABELS[worst.key] || worst.key) + '(' + worst.value + '점)이에요.';
  }

  // 모든 점수 그래프가 같은 네 단계 색상 기준을 사용하도록 점수 구간을 반환합니다.
  function graphScoreTone(value) {
    if (value >= 70) return 'good';
    if (value >= 50) return 'blue';
    if (value >= 25) return 'yellow';
    return 'danger';
  }

  // 원형 그래프와 레이더 차트가 공유하는 점수 구간 안내를 표시합니다.
  function graphScoreLegend() {
    return '<div class="score-tone-legend" aria-label="점수 색상 기준">' +
      '<span class="is-good">70점 이상</span><span class="is-blue">50점 이상</span>' +
      '<span class="is-yellow">25점 이상</span><span class="is-danger">25점 미만</span></div>';
  }

  function radarChart(entries) {
    var size = 620;
    var center = size / 2;
    var maxRadius = center - 140;
    var count = entries.length;
    var averageScore = Math.round(entries.reduce(function (sum, item) {
      return sum + item.value;
    }, 0) / count);
    var chartTone = graphScoreTone(averageScore);
    // 관점이 적을수록 글자가 잘릴 걱정이 없으니 더 크게, 많을수록 겹치지 않게 작게.
    var labelFontSize = count <= 4 ? 18 : count <= 6 ? 14 : 11;
    var scoreFontSize = count <= 4 ? 16 : count <= 6 ? 12 : 10;
    var points = entries.map(function (item, index) {
      var angle = (Math.PI * 2 * index / count) - Math.PI / 2;
      return {
        angle: angle,
        cos: Math.cos(angle),
        sin: Math.sin(angle),
        item: item
      };
    });
    var axisPoint = function (p, ratio) {
      return { x: center + maxRadius * ratio * p.cos, y: center + maxRadius * ratio * p.sin };
    };
    var toStr = function (pt) { return pt.x.toFixed(1) + ',' + pt.y.toFixed(1); };

    var rings = [0.33, 0.66, 1].map(function (ratio) {
      return '<polygon points="' + points.map(function (p) { return toStr(axisPoint(p, ratio)); }).join(' ') +
        '" class="radar-ring" />';
    }).join('');

    var axes = points.map(function (p) {
      var end = axisPoint(p, 1);
      return '<line x1="' + center + '" y1="' + center + '" x2="' + end.x.toFixed(1) +
        '" y2="' + end.y.toFixed(1) + '" class="radar-axis" />';
    }).join('');

    var scorePolygon = points.map(function (p) {
      return toStr(axisPoint(p, p.item.value / 100));
    }).join(' ');

    // 각 축의 점수선은 해당 관점의 점수 구간 색상을 그대로 사용합니다.
    var scoreLines = points.map(function (p) {
      var pt = axisPoint(p, p.item.value / 100);
      return '<line x1="' + center + '" y1="' + center + '" x2="' + pt.x.toFixed(1) +
        '" y2="' + pt.y.toFixed(1) + '" class="radar-score-line score-tone--' +
        graphScoreTone(p.item.value) + '" />';
    }).join('');

    var dots = points.map(function (p) {
      var pt = axisPoint(p, p.item.value / 100);
      return '<circle cx="' + pt.x.toFixed(1) + '" cy="' + pt.y.toFixed(1) +
        '" r="5.5" class="radar-dot score-tone--' + graphScoreTone(p.item.value) + '" />';
    }).join('');

    var labels = points.map(function (p) {
      var labelPt = axisPoint(p, 1.1);
      var anchor = p.cos > 0.3 ? 'start' : p.cos < -0.3 ? 'end' : 'middle';
      return '<text x="' + labelPt.x.toFixed(1) + '" y="' + labelPt.y.toFixed(1) +
        '" text-anchor="' + anchor + '" class="radar-label" style="font-size:' + labelFontSize + 'px">' +
        esc(CATEGORY_LABELS[p.item.key] || p.item.key) +
        '</text><text x="' + labelPt.x.toFixed(1) + '" y="' + (labelPt.y + labelFontSize + 4).toFixed(1) +
        '" text-anchor="' + anchor + '" class="radar-label-score score-tone--' +
        graphScoreTone(p.item.value) + '" style="font-size:' + scoreFontSize + 'px">' +
        p.item.value + '점</text>';
    }).join('');

    return '<svg viewBox="0 0 ' + size + ' ' + size + '" class="radar-chart score-tone--' + chartTone +
      '" role="img" aria-label="관점별 점수 레이더 차트">' +
      rings + axes + '<polygon points="' + scorePolygon + '" class="radar-score-area" />' +
      scoreLines + dots + labels + '</svg>';
  }

  function dimensionDonuts(entries) {
    var single = entries.length === 1;
    return '<div class="dimension-donut-list ' + (single ? 'is-single' : 'is-pair') + '">' +
      entries.map(function (item) {
      return '<div class="dimension-donut">' +
        scoreRingMarkup(item.value, single
          ? { size: 240, stroke: 16, uid: 'dim-' + item.key }
          : { size: 130, stroke: 10, uid: 'dim-' + item.key, variant: 'sm' }) +
        '<p>' + esc(CATEGORY_LABELS[item.key] || item.key) + '</p></div>';
    }).join('') + '</div>';
  }

  // 종합 점수 및 관점별 점수를 강조하는 그래프 — 얇은 링 + 그라데이션 + 은은한 그림자로 표현한다.
  function scoreRingMarkup(value, options) {
    options = options || {};
    var size = options.size || 240;
    var stroke = options.stroke || 16;
    var uid = options.uid || 'main';
    var gradientId = 'score-ring-gradient-' + uid;
    var shadowId = 'score-ring-shadow-' + uid;
    var ringClass = 'score-ring' + (options.variant ? ' score-ring--' + options.variant : '');
    var center = size / 2;
    var radius = center - stroke / 2 - 4;
    var circumference = 2 * Math.PI * radius;
    var ratio = value == null ? 0 : Math.max(0, Math.min(100, value)) / 100;
    var dashoffset = circumference * (1 - ratio);
    var tone = graphScoreTone(value == null ? 0 : value);
    var colors = {
      good: ['#83dc91', '#31945b', '#31945b'],
      blue: ['#65b9ff', '#086bd6', '#086bd6'],
      yellow: ['#f5dc72', '#c59618', '#c59618'],
      danger: ['#ff8b82', '#c94450', '#c94450']
    }[tone];
    return '<svg viewBox="0 0 ' + size + ' ' + size + '" class="' + ringClass + ' score-tone--' + tone +
      '" role="img" aria-label="' +
      (value == null ? '평가 전' : value + '점') + '">' +
      '<defs>' +
      '<linearGradient id="' + gradientId + '" x1="0%" y1="0%" x2="100%" y2="100%">' +
      '<stop offset="0%" stop-color="' + colors[0] + '" /><stop offset="100%" stop-color="' + colors[1] + '" />' +
      '</linearGradient>' +
      '<filter id="' + shadowId + '" x="-30%" y="-30%" width="160%" height="160%">' +
      '<feDropShadow dx="0" dy="3" stdDeviation="5" flood-color="' + colors[2] + '" flood-opacity="0.32" />' +
      '</filter>' +
      '</defs>' +
      '<circle cx="' + center + '" cy="' + center + '" r="' + radius +
      '" class="score-ring-outline" stroke-width="' + (stroke + 8) + '" fill="none" />' +
      '<circle cx="' + center + '" cy="' + center + '" r="' + radius + '" class="score-ring-track" stroke-width="' + stroke + '" fill="none" />' +
      '<circle cx="' + center + '" cy="' + center + '" r="' + radius + '" class="score-ring-progress" stroke-width="' + stroke +
      '" fill="none" stroke-dasharray="' + circumference.toFixed(1) + '" stroke-dashoffset="' + dashoffset.toFixed(1) +
      '" transform="rotate(-90 ' + center + ' ' + center + ')" stroke="url(#' + gradientId + ')" filter="url(#' + shadowId + ')" />' +
      '<text x="' + center + '" y="' + (center + size * 0.02).toFixed(1) + '" text-anchor="middle" class="score-ring-value">' +
      (value == null ? '-' : value) + '</text>' +
      '<text x="' + center + '" y="' + (center + size * 0.14).toFixed(1) + '" text-anchor="middle" class="score-ring-label">/ 100점</text>' +
      '</svg>';
  }

  function dimensionReasonList(entries, reasons) {
    return '<div class="dimension-reason-list">' + entries.map(function (item) {
      var reason = reasons && reasons[item.key];
      return '<div class="dimension-reason"><div class="dimension-reason__head">' +
        '<strong>' + esc(CATEGORY_LABELS[item.key] || item.key) + '</strong>' +
        '<em class="score-tone-badge score-tone--' + graphScoreTone(item.value) + '">' +
        item.value + '점</em></div>' +
        '<p>' + esc(reason || '아직 근거 설명이 없어요.') + '</p></div>';
    }).join('') + '</div>';
  }

  function renderDimensionVisual(overallScore, categoryScores, categoryScoreReasons, overallAssessment) {
    var container = document.getElementById('dimension-visual');
    if (!container) return;
    var entries = dimensionEntries(categoryScores);
    var assessment = renderOverallAssessment(overallAssessment) ||
      '<p class="result-empty-copy">종합 평가 내용이 없습니다.</p>';
    var overallBlock = '<div class="dimension-visual__overall">' +
      '<span class="dimension-visual__type">면접 종합 점수</span>' +
      '<div class="dimension-visual__graphic-wrap">' + scoreRingMarkup(overallScore) + '</div>' +
      '<div class="dimension-visual__overall-detail">' + assessment + '</div></div>';

    if (!entries.length) {
      container.innerHTML = '<div class="dimension-visual__graphics">' + overallBlock + '</div>' +
        '<p class="result-empty-copy">평가에 성공한 관점이 없습니다.</p>';
      return;
    }
    var typeLabel = entries.length >= 3 ? '레이더 차트' : '관점별 점수';
    var chart = entries.length >= 3 ? radarChart(entries) : dimensionDonuts(entries);
    var chartBlock = '<div class="dimension-visual__chart">' +
      '<span class="dimension-visual__type">' + esc(typeLabel) + '</span>' +
      '<div class="dimension-visual__graphic-wrap">' + chart + '</div>' +
      '<div class="dimension-visual__chart-detail">' +
      '<p class="dimension-visual__summary">' + dimensionSummaryLine(entries) + '</p>' +
      graphScoreLegend() +
      '<div class="dimension-visual__desc">' +
      dimensionReasonList(entries, categoryScoreReasons) + '</div></div></div>';
    container.innerHTML =
      '<div class="dimension-visual__graphics">' + overallBlock + chartBlock + '</div>';
  }

  function renderFinalReport(report) {
    var panel = document.getElementById('final-report-panel');
    panel.setAttribute('aria-busy', 'false');
    var weaknessTags = report.weaknessTagSummary || [];
    var recommendations = report.nextPracticeRecommendation || [];
    var learningDirection = report.learningDirection || [];
    var suggestion = report.improvementSuggestion || {};

    var html = '<h2>최종 리포트</h2>' +
      '<p class="result-empty-copy">' + esc(report.totalQuestionCount) + '개 질문, 답변 ' +
      esc(report.submittedQuestionCount) + '개를 모두 종합해 정리한 결과예요.</p>';

    html += '<div id="dimension-visual" class="dimension-visual"></div>';

    html += '<div class="result-section-title"><span>답변 과정에서 발견 약점 포인트</span><small>번호를 누르면 해당 질문으로 이동해요</small></div>';
    html += weaknessTags.length
      ? '<div class="weakness-tag-list">' + weaknessTags.map(function (item) {
          return '<div class="weakness-tag-row">' +
            '<div class="weakness-tag-row__head"><strong>' + esc(weaknessLabel(item.tag)) + '</strong><em>' + item.count + '회</em></div>' +
            (item.reason ? '<p class="weakness-tag-row__reason">' + esc(item.reason) + '</p>' : '') +
            '<div class="weakness-tag-row__questions-line">' +
            '<span class="weakness-tag-row__questions-label">약점이 나온 질문</span>' +
            '<div class="weakness-tag-row__questions" data-tag="' + esc(item.tag) + '"></div>' +
            '</div></div>';
        }).join('') + '</div>'
      : '<p class="result-empty-copy">확인된 약점 태그가 없습니다.</p>';

    html += '<div class="result-section-title" style="margin-top:22px;"><span>다음에 연습하면 좋은 것</span></div>';
    html += recommendations.length
      ? '<div class="final-recommend-list">' + recommendations.map(function (item) {
          return '<article class="final-recommend-card">' +
            '<span class="eval-scope">' + esc(QUESTION_TYPE_LABELS[item.questionType] || item.questionType) + ' 질문</span>' +
            '<p>' + esc(item.reason) + '</p></article>';
        }).join('') + '</div>'
      : '<p class="result-empty-copy">추천 항목이 없습니다.</p>';

    var suggestionKeys = Object.keys(IMPROVEMENT_TARGET_LABELS).filter(function (key) {
      return suggestion[key] && suggestion[key].length;
    });
    if (suggestionKeys.length) {
      html += '<div class="result-section-title" style="margin-top:22px;"><span>서류 보완 제안</span></div>';
      html += '<div class="final-suggestion-list">' + suggestionKeys.map(function (key) {
        return '<div class="final-suggestion-group"><div class="final-suggestion-group__head">' +
          '<span class="final-suggestion-icon" aria-hidden="true">▤</span>' +
          '<strong>' + esc(IMPROVEMENT_TARGET_LABELS[key]) + '</strong></div>' +
          '<ul>' + suggestion[key].map(function (text) { return '<li>' + esc(text) + '</li>'; }).join('') + '</ul></div>';
      }).join('') + '</div>';
    }

    if (learningDirection.length) {
      html += '<div class="result-section-title" style="margin-top:22px;"><span>학습 방향</span></div>' +
        '<ul class="final-learning-list">' + learningDirection.map(function (text) {
          return '<li>' + esc(text) + '</li>';
        }).join('') + '</ul>';
    }

    if (report.analysisCaseId) {
      html += '<div style="margin-top:24px;"><a class="btn btn--secondary" href="' +
        window.JobPuzzleRoutes.path('/analysis/' + encodeURIComponent(report.analysisCaseId)) +
        '">공고 요구사항 연결 분석 결과 보기</a></div>';
    }

    panel.innerHTML = html;
    renderDimensionVisual(
      report.overallScore,
      report.categoryScores,
      report.categoryScoreReasons,
      report.overallAssessment
    );
    renderWeaknessQuestionLinks();
  }

  // 최종 리포트 AI 응답을 기다리는 동안 기존 분석 퍼즐과 대기 진행 바를 표시합니다.
  function renderFinalReportLoading() {
    var panel = document.getElementById('final-report-panel');
    var stages = [
      { progress: 18, copy: '저장된 면접 답변을 모으고 있어요.' },
      { progress: 42, copy: '답변별 평가 근거를 정리하고 있어요.' },
      { progress: 68, copy: '강점과 보완 방향을 종합하고 있어요.' },
      { progress: 88, copy: '최종 리포트를 마무리하고 있어요.' }
    ];
    var stageIndex = 0;

    panel.setAttribute('aria-busy', 'true');
    panel.innerHTML =
      '<div class="final-report-loading" role="status" aria-live="polite">' +
      '<div class="final-report-loading__puzzle">' +
      '<canvas data-job-puzzle-scene data-analysis-puzzle data-analysis-stage="1" aria-hidden="true"></canvas>' +
      '</div><div class="final-report-loading__content">' +
      '<span class="final-report-loading__badge">AI 리포트 생성 중</span>' +
      '<h2>면접 답변을 종합하고 있어요</h2>' +
      '<p id="final-report-loading-copy">' + stages[0].copy + '</p>' +
      '<div class="final-report-loading__progress" aria-hidden="true"><i style="width:' +
      stages[0].progress + '%"></i></div>' +
      '<small>응답이 도착하면 최종 리포트가 자동으로 표시됩니다.</small>' +
      '</div></div>';

    var canvas = panel.querySelector('[data-analysis-puzzle]');
    var fill = panel.querySelector('.final-report-loading__progress i');
    var copy = document.getElementById('final-report-loading-copy');

    // 서버가 세부 진행률을 제공하지 않으므로 완료 전에는 100%로 표시하지 않습니다.
    return window.setInterval(function () {
      stageIndex = Math.min(stageIndex + 1, stages.length - 1);
      canvas.dataset.analysisStage = String(stageIndex + 1);
      fill.style.width = stages[stageIndex].progress + '%';
      copy.textContent = stages[stageIndex].copy;
    }, 1800);
  }

  // 질문별 결과의 모든 점수 배지가 최종 리포트 그래프와 같은 구간을 사용하도록 합니다.
  function scoreTone(score) {
    return 'question-score-badge ' +
      (score == null ? 'is-unrated' : 'score-tone--' + graphScoreTone(score));
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

  function unevaluatedLabel(question, questionScore) {
    if (questionScore && questionScore.finalScore != null) {
      return questionScore.finalScore + '점';
    }
    var hasUserAnswer = (question.conversation || []).some(function (message) {
      return message.sender === 'USER' &&
        (message.messageType === 'ORIGINAL_ANSWER' ||
          message.messageType === 'FOLLOW_UP_ANSWER' ||
          message.messageType === 'REJECTED_ANSWER' ||
          message.messageType === 'EVALUATION_FAILED_ANSWER');
    });
    return hasUserAnswer ? '미평가' : '미응답';
  }

  function renderQuestionDetail(question, questionScore, score) {
    var detail = document.getElementById('q-detail');
    var displayOrder = Number(question.displayOrder) > 0
      ? question.displayOrder
      : (questions.indexOf(question) + 1);
    var finalScore = questionScore && questionScore.finalScore != null
      ? questionScore.finalScore
      : null;
    var dimensionScores = questionScore ? questionScore.dimensionScores : {};
    var dimensionCounts = questionScore ? questionScore.dimensionEvaluationCounts : {};
    var dimensionCount = Object.keys(dimensionScores || {}).length;
    var answerEvaluationCount = Object.keys(dimensionCounts || {}).reduce(function (max, key) {
      return Math.max(max, dimensionCounts[key] || 0);
    }, 0);
    detail.innerHTML =
      '<div class="question-detail-head"><span class="question-number-badge">질문 ' +
      displayOrder + '</span>' +
      '<strong class="' + scoreTone(finalScore) + '">' +
      unevaluatedLabel(question, questionScore) +
      '</strong></div>' +
      '<h2>' + esc(question.questionText) + '</h2>' +
      '<p class="question-intent">' + esc(question.intent || '질문 의도가 기록되지 않았습니다.') + '</p>' +
      conversationTimeline(question) +
      '<div class="question-evaluation-row">' +
      '<div class="question-score-formula"><div><span>① 답변 분석</span><strong>' +
      answerEvaluationCount + '회</strong></div><i>→</i><div><span>② 핵심 평가 기준</span><strong>' +
      dimensionCount + '개</strong></div><i>→</i><div><span>③ 질문 종합 점수</span><strong class="' +
      scoreTone(finalScore) + '">' +
      unevaluatedLabel(question, questionScore) +
      '</strong></div></div>' +
      '<div class="dimension-list">' +
      dimensionItems(dimensionScores, dimensionCounts) + '</div></div>' +
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

    var scoreByQuestion = {};
    (score.questionScores || []).forEach(function (item) {
      scoreByQuestion[item.sessionQuestionId] = item;
    });
    sessionQuestions = questions;
    sessionScoreByQuestion = scoreByQuestion;
    var list = document.getElementById('q-list');
    list.innerHTML = questions.map(function (question, index) {
      var item = scoreByQuestion[question.sessionQuestionId];
      return '<button type="button" class="q-list-item' + (index === 0 ? ' is-active' : '') +
        '" data-question-index="' + index + '"><span class="q-list-item__num">' +
        (index + 1) + '</span><span class="q-list-item__summary">' +
        esc(question.questionText) + '</span><strong class="' +
        scoreTone(item && item.finalScore != null ? item.finalScore : null) + '">' +
        unevaluatedLabel(question, item) + '</strong></button>';
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
    renderWeaknessQuestionLinks();
    renderInterimActions();
  }

  function focusLabels(items) {
    return (items || []).map(function (item) {
      return CATEGORY_LABELS[item] || weaknessLabel(item);
    }).join(' · ');
  }

  function closeRemainingModal() {
    var modal = document.getElementById('remaining-question-modal');
    if (modal) modal.remove();
    document.body.classList.remove('has-result-modal');
  }

  function openRemainingModal() {
    closeRemainingModal();
    var modal = document.createElement('div');
    modal.id = 'remaining-question-modal';
    modal.className = 'remaining-question-modal question-selection-modal';
    modal.innerHTML =
      '<div class="remaining-question-backdrop question-selection-backdrop" data-close-remaining></div>' +
      '<section class="remaining-question-dialog question-selection-dialog" role="dialog" aria-modal="true">' +
      '<header><div><span class="question-selection-mode">이어 연습하기</span><h2>남은 질문을 추가해 보세요</h2>' +
      '<p>추가한 질문의 평가도 지금까지의 점수와 함께 집계됩니다.</p></div>' +
      '<button type="button" class="question-selection-close" data-close-remaining aria-label="닫기">×</button></header>' +
      '<div class="remaining-question-list q-gen-list">' + remainingQuestions.map(function (question, index) {
        return '<label class="q-gen-item q-gen-item--detailed"><input type="checkbox" value="' + question.questionId + '" checked>' +
          '<span class="q-gen-item__num">' + (index + 1) + '</span>' +
          '<span class="q-gen-item__content"><strong class="q-gen-item__text">' + esc(question.questionText) + '</strong>' +
          '<button type="button" class="question-hint-toggle" aria-expanded="false"><span>질문 힌트 보기</span><span class="question-hint-chevron" aria-hidden="true"></span></button>' +
          '<span class="question-hint-panel" hidden><small><b>이 질문의 의도</b>' + esc(question.intent || '답변의 핵심 근거를 확인합니다.') + '</small>' +
          '<small><b>집중 평가 기준</b>' + esc(focusLabels(question.evaluationFocus)) + '</small></span>' +
          '</span></label>';
      }).join('') + '</div>' +
      '<footer><span>하나 이상 선택해 주세요.</span>' +
      '<button id="add-remaining-questions" class="btn btn--primary">선택한 질문 이어서 연습</button></footer>' +
      '</section>';
    document.body.appendChild(modal);
    document.body.classList.add('has-result-modal');
    modal.querySelectorAll('[data-close-remaining]').forEach(function (button) {
      button.addEventListener('click', closeRemainingModal);
    });
    modal.querySelectorAll('.question-hint-toggle').forEach(function (button) {
      button.addEventListener('click', function (event) {
        event.preventDefault();
        event.stopPropagation();
        var panel = button.parentElement.querySelector('.question-hint-panel');
        var expanded = button.getAttribute('aria-expanded') === 'true';
        button.setAttribute('aria-expanded', String(!expanded));
        button.querySelector('span').textContent = expanded ? '질문 힌트 보기' : '질문 힌트 숨기기';
        panel.hidden = expanded;
      });
    });
    document.getElementById('add-remaining-questions').addEventListener('click', function () {
      var ids = Array.prototype.slice.call(
        modal.querySelectorAll('input[type="checkbox"]:checked')
      ).map(function (input) { return Number(input.value); });
      if (!ids.length) {
        notify('warning', '이어갈 질문을 하나 이상 선택해 주세요.');
        return;
      }
      api(window.JobPuzzleRoutes.path('/interview-sessions/' + sessionId + '/questions'), {
        method: 'POST',
        json: { questionIds: ids }
      }).then(function () {
        window.location.href = window.JobPuzzleRoutes.path('/interview?resumeSessionId=' + encodeURIComponent(sessionId));
      }).catch(function (error) { notify('error', error.message); });
    });
  }

  function finalizeSession() {
    if (!window.confirm(
      '이 면접을 최종 확정할까요?\n\n확정하면 선택하지 않은 질문은 더 이상 추가할 수 없고, 리포트 생성 대상으로 전달됩니다.'
    )) return;
    api(window.JobPuzzleRoutes.path('/interview-sessions/' + sessionId + '/complete'), { method: 'POST' })
      .then(function () {
        window.location.replace(window.JobPuzzleRoutes.path('/interview-results?sessionId=' + encodeURIComponent(sessionId)));
      })
      .catch(function (error) { notify('error', error.message); });
  }

  function renderInterimActions() {
    var area = document.getElementById('interim-actions');
    if (!area || !sessionInfo || sessionInfo.status === 'COMPLETED') {
      if (area) area.hidden = true;
      return;
    }
    area.hidden = false;
    area.innerHTML =
      '<div><span>중간 결과</span><h2>지금까지의 답변을 확인하고 다음 단계를 선택하세요</h2>' +
      '<p>아직 리포트가 확정되지 않았습니다. 선택해 둔 질문을 이어서 답하거나 남은 질문을 추가할 수 있습니다.</p></div>' +
      '<div class="interim-actions__buttons">' +
      (pendingSelectedQuestions.length
        ? '<button id="resume-selected-questions" class="btn btn--outline">선택한 질문 ' +
          pendingSelectedQuestions.length + '개 이어서 답변</button>'
        : '') +
      (remainingQuestions.length
        ? '<button id="open-remaining-questions" class="btn btn--outline">남은 질문 ' +
          remainingQuestions.length + '개 추가</button>'
        : (pendingSelectedQuestions.length
          ? ''
          : '<span class="interim-actions__done">준비된 질문을 모두 답변했습니다</span>')) +
      '<button id="finalize-session" class="btn btn--primary">면접 끝내고 리포트 확정</button></div>';
    var resumeButton = document.getElementById('resume-selected-questions');
    if (resumeButton) {
      resumeButton.addEventListener('click', function () {
        window.location.href = window.JobPuzzleRoutes.path(
          '/interview?resumeSessionId=' + encodeURIComponent(sessionId)
        );
      });
    }
    var addButton = document.getElementById('open-remaining-questions');
    if (addButton) addButton.addEventListener('click', openRemainingModal);
    document.getElementById('finalize-session').addEventListener('click', finalizeSession);
  }

  function formatDate(value) {
    if (!value) return '';
    return new Date(value).toLocaleString('ko-KR', {
      year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit'
    });
  }

  // 완료된 면접(리포트)을 보고 있을 때는 헤더에서 "면접 준비" 대신 "리포트"를 활성 표시한다.
  function highlightReportsNav() {
    var interviewLink = document.querySelector('.site-header__nav a[href="' + window.JobPuzzleRoutes.path('/interview') + '"]');
    var reportsLink = document.querySelector('.site-header__nav a[href="' + window.JobPuzzleRoutes.path('/reports') + '"]');
    if (interviewLink) interviewLink.classList.remove('is-active');
    if (reportsLink) reportsLink.classList.add('is-active');
  }

  function renderHistory(items) {
    document.getElementById('result-title').textContent = '완료한 면접';
    document.getElementById('result-meta').textContent =
      items.length ? '완료한 면접을 선택해 질문별 점수와 진행 결과를 확인하세요.' :
        '아직 완료한 면접이 없습니다.';
    document.querySelector('.result-layout').classList.add('result-layout--history');
    document.querySelector('.result-tabs').hidden = true;
    document.getElementById('q-detail').hidden = true;
    highlightReportsNav();
    var list = document.getElementById('q-list');
    list.className = 'history-list';
    list.innerHTML = items.length ? items.map(function (item) {
      return '<a class="history-card" href="' +
        window.JobPuzzleRoutes.path('/interview-results?sessionId=' + encodeURIComponent(item.sessionId)) +
        '"><div><span>' + esc(modeLabel(item.mode)) +
        '</span><h2>' + item.questionCount + '개 질문 면접</h2><p>' +
        esc(formatDate(item.completedAt)) +
        (item.targetWeaknessTag ? ' · #' + esc(weaknessLabel(item.targetWeaknessTag)) : '') +
        '</p></div><strong>결과 보기</strong></a>';
    }).join('') :
      '<div class="history-empty"><h2>첫 면접을 시작해 보세요</h2>' +
      '<p>답변을 하나 이상 제출하고 정상 완료하면 이곳에 결과가 저장됩니다.</p>' +
      '<a class="btn btn--primary" href="' + window.JobPuzzleRoutes.path('/interview') + '">면접 시작</a></div>';
  }

  document.addEventListener('DOMContentLoaded', function () {
    if (!sessionId) {
      api(window.JobPuzzleRoutes.path('/interview-sessions/history')).then(renderHistory).catch(function (error) {
        document.getElementById('result-meta').textContent = error.message;
      });
      return;
    }
    Promise.all([
      api(window.JobPuzzleRoutes.path('/interview-sessions/' + sessionId)),
      api(window.JobPuzzleRoutes.path('/interview-sessions/' + sessionId + '/score')),
      api(window.JobPuzzleRoutes.path('/interview-sessions/' + sessionId + '/questions')),
      api(window.JobPuzzleRoutes.path('/interview-sessions/' + sessionId + '/questions/remaining'))
    ]).then(function (values) {
      sessionInfo = values[0];
      remainingQuestions = values[3] || [];
      pendingSelectedQuestions = (values[2] || []).filter(function (question) {
        return question.status === 'PENDING' || question.status === 'IN_PROGRESS';
      });
      renderSession(values[1], values[2]);
      if (sessionInfo.status !== 'COMPLETED') {
        var finalTabButton = document.querySelector('.tabbar__btn[data-tab="final"]');
        var finalPanel = document.querySelector('.tab-panel[data-panel="final"]');
        if (finalTabButton) finalTabButton.remove();
        if (finalPanel) finalPanel.remove();
        var listTabButton = document.querySelector('.tabbar__btn[data-tab="list"]');
        if (listTabButton) listTabButton.click();
      } else {
        highlightReportsNav();
        var finalReportLoadingTimer = renderFinalReportLoading();
        api(window.JobPuzzleRoutes.path('/final-report/sessions/' + sessionId))
          .then(function (report) {
            window.clearInterval(finalReportLoadingTimer);
            renderFinalReport(report);
          })
          .catch(function (error) {
            var panel = document.getElementById('final-report-panel');
            window.clearInterval(finalReportLoadingTimer);
            panel.setAttribute('aria-busy', 'false');
            panel.innerHTML =
              '<p class="result-empty-copy">' + esc(error.message) + '</p>';
            notify('error', error.message);
          });
      }
    }).catch(function (error) {
      document.getElementById('result-meta').textContent = error.message;
    });
  });
})();
