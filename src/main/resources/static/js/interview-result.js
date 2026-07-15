// interview-result.js

(function () {
  'use strict';

  var QUESTIONS = [
    { summary: '자기소개와 지원 동기', original: '간단한 자기소개와 함께, 이 직무에 지원하게 된 계기를 말씀해주세요.', answer1: '3년차 백엔드 개발자로 트래픽이 많은 커머스 서비스에서 API 서버를 개발해왔습니다. LUME의 대규모 트래픽 처리 방식에 관심이 생겨 지원했습니다.', eval1: { good: ['경력과 지원 배경을 간결하게 정리했어요'], improve: ['회사·직무와 본인 경험을 연결하는 구체적인 이유가 조금 더 필요해요'] }, followUp: '말씀하신 커머스 서비스 경험에서, 본인이 직접 맡았던 역할은 구체적으로 무엇이었나요?', answer2: '팀 전체가 트래픽 대응을 담당했고, 저는 주로 캐싱 로직을 작업했습니다.', eval2: { good: ['담당 영역을 명확히 구분해서 답했어요'], improve: ['캐싱 로직에서 어떤 문제를 어떻게 해결했는지 구체적인 과정이 빠졌어요'] }, tags: ['성과 수치화 부족', '역할 구분 불명확'] },
    { summary: '강점과 약점', original: '본인의 강점과 약점을 각각 말씀해주세요.', answer1: '강점은 문제를 구조적으로 분석하는 능력이고, 약점은 완벽주의 성향으로 마감이 늦어질 때가 있습니다.', eval1: { good: ['강점과 약점을 균형있게 제시했어요'], improve: ['약점을 보완하기 위한 구체적인 노력이나 개선 과정이 없어요'] }, followUp: '완벽주의 성향으로 마감이 늦어졌던 경험이 있다면, 이후 어떻게 개선하셨나요?', answer2: '우선순위를 정해서 핵심 기능부터 마무리하는 방식으로 바꿨습니다.', eval2: { good: ['개선 방법을 구체적으로 제시했어요'], improve: ['실제 적용 후 어떤 변화가 있었는지 결과가 빠졌어요'] }, tags: ['역할 구분 불명확'] },
    { summary: '트래픽 처리 설계 경험', original: '대규모 트래픽을 처리하기 위해 설계한 경험을 설명해주세요.', answer1: '특가 이벤트 시 트래픽이 급증해 캐시 서버와 큐를 도입해 부하를 분산했습니다.', eval1: { good: ['문제 상황과 해결 방향을 명확히 설명했어요'], improve: ['캐시·큐 도입 후 수치로 확인된 개선 결과가 빠졌어요'] }, followUp: '캐시와 큐를 도입하는 과정에서 본인이 직접 판단하거나 결정한 부분은 무엇이었나요?', answer2: '캐시 만료 정책과 큐 재시도 정책을 제가 직접 설계했습니다.', eval2: { good: ['본인의 판단 근거를 구체적으로 밝혔어요'], improve: ['정책 설계 시 고려한 트레이드오프 설명이 있으면 더 좋아요'] }, tags: ['성과 수치화 부족'] },
    { summary: 'RESTful API 설계 원칙', original: 'RESTful API 설계 시 어떤 원칙을 가장 중요하게 생각하나요?', answer1: '자원 중심 URL 설계와 일관된 응답 구조를 가장 중요하게 생각합니다.', eval1: { good: ['핵심 원칙을 명확히 짚었어요'], improve: ['실제 프로젝트에 적용한 사례가 함께 있으면 더 설득력 있어요'] }, followUp: '실제로 이 원칙을 적용하다가 예외적으로 다르게 설계했던 경우가 있었나요?', answer2: '네, 성능 문제로 일부 API는 자원 중심 대신 목적 중심으로 설계했습니다.', eval2: { good: ['원칙과 예외를 함께 설명해 실무 감각을 보여줬어요'], improve: ['그 결정으로 인한 실제 성능 개선 수치가 있으면 좋아요'] }, tags: [] },
    { summary: '기술적으로 어려웠던 문제', original: '가장 어려웠던 기술적 문제와 해결 과정을 설명해주세요.', answer1: '야간에 반복적으로 발생하던 DB 커넥션 고갈 문제를 해결한 적이 있습니다.', eval1: { good: ['문제 상황을 명확하게 짚었어요'], improve: ['원인을 어떻게 추적했는지 구체적인 과정이 부족해요'] }, followUp: '그 문제의 근본 원인은 어떻게 찾아내셨나요?', answer2: '커넥션 풀 설정과 느린 쿼리 로그를 함께 분석해서 원인을 찾았습니다.', eval2: { good: ['원인 분석 과정을 구체적으로 설명했어요'], improve: ['최종적으로 문제가 얼마나 개선됐는지 수치가 빠졌어요'] }, tags: ['성과 수치화 부족'] },
    { summary: '협업 중 의견 충돌 경험', original: '협업 중 의견 충돌이 있었던 경험과 해결 방법을 말씀해주세요.', answer1: 'API 응답 구조를 두고 프론트엔드팀과 의견이 갈렸던 적이 있습니다.', eval1: { good: ['상황을 구체적으로 제시했어요'], improve: ['갈등을 해결한 방법과 결과가 빠졌어요'] }, followUp: '그 상황에서 최종적으로 어떻게 합의점을 찾으셨나요?', answer2: '각자 입장의 장단점을 정리해서 문서로 공유하고, 함께 결정했습니다.', eval2: { good: ['갈등 해결 과정을 구조적으로 설명했어요'], improve: ['합의 이후 협업 방식에 어떤 변화가 있었는지 결과가 있으면 좋아요'] }, tags: [] },
    { summary: '장애 대응 및 운영 경험', original: '서비스 운영·장애 대응 경험을 구체적으로 설명해주세요.', answer1: '결제 서버 장애 발생 시 온콜 대응을 맡아 원인을 파악하고 복구했습니다.', eval1: { good: ['실제 장애 대응 경험을 제시했어요'], improve: ['장애 감지부터 복구까지 걸린 시간이나 영향 범위가 빠졌어요'] }, followUp: '그 장애가 재발하지 않도록 이후에 어떤 조치를 하셨나요?', answer2: '알림 기준을 낮추고 회복 로직을 자동화했습니다.', eval2: { good: ['재발 방지 조치를 구체적으로 제시했어요'], improve: ['조치 이후 실제로 장애가 줄었는지 결과 확인이 없어요'] }, tags: ['성과 수치화 부족', '운영·장애대응 경험 부족'] },
    { summary: '데이터 시각화 도구 사용 경험', original: '데이터 시각화 도구를 사용해본 경험이 있나요?', answer1: '직접 사용한 경험은 많지 않지만, 대시보드 데이터를 참고한 적은 있습니다.', eval1: { good: ['솔직하게 현재 경험 수준을 답했어요'], improve: ['관련 경험이나 학습 계획을 제시하지 않았어요'] }, followUp: '관련 역량을 보완하기 위해 계획하고 있는 학습이 있나요?', answer2: '그라파나 같은 모니터링 도구를 사용해보려고 합니다.', eval2: { good: ['구체적인 학습 방향을 제시했어요'], improve: ['왜 그 도구를 선택했는지 이유가 있으면 더 좋아요'] }, tags: ['운영·장애대응 경험 부족'] },
    { summary: '팀 프로젝트 갈등 해결', original: '팀 프로젝트에서 갈등을 해결한 경험이 있나요?', answer1: '일정 우선순위를 두고 팀원과 갈등이 있었고, 대화를 통해 조율했습니다.', eval1: { good: ['갈등 상황을 솔직하게 공유했어요'], improve: ['구체적으로 어떤 기준으로 조율했는지 근거가 부족해요'] }, followUp: '우선순위를 조율할 때 어떤 기준을 사용하셨나요?', answer2: '고객 영향도가 큰 기능을 먼저 처리하는 기준으로 정했습니다.', eval2: { good: ['판단 기준을 명확히 제시했어요'], improve: ['그 기준을 적용한 결과가 실제로 어땠는지 빠졌어요'] }, tags: ['역할 구분 불명확'] },
    { summary: '5년 후 커리어 목표', original: '5년 후 본인의 커리어 목표는 무엇인가요?', answer1: '백엔드 아키텍처를 깊이 이해하는 시니어 개발자가 되고 싶습니다.', eval1: { good: ['목표를 명확하게 제시했어요'], improve: ['목표와 지원 직무·회사의 연결점이 약해요'] }, followUp: '그 목표를 위해 지금 준비하고 있는 것이 있다면 무엇인가요?', answer2: '대규모 트래픽 시스템 설계 스터디를 하고 있습니다.', eval2: { good: ['구체적인 준비 과정을 제시했어요'], improve: ['스터디를 통해 얻은 구체적인 성과가 있으면 더 좋아요'] }, tags: ['성과 수치화 부족'] }
  ];

  var EVAL_ITEMS = [
    { label: '질문 의도 이해', value: 84, explain: '질문에서 요구하는 핵심을 정확히 파악하고 답변 방향을 잘 잡았어요.' },
    { label: '경험 구체성', value: 63, explain: '상황은 잘 설명했지만 구체적인 수치나 결과가 자주 빠졌어요.' },
    { label: '본인 역할', value: 71, explain: '팀 성과와 본인 역할을 구분해 설명하려 했지만 조금 더 명확하게 표현하면 좋아요.' },
    { label: '문제 해결 과정', value: 78, explain: '문제 인식부터 해결까지 순서대로 설명하는 흐름이 자연스러웠어요.' },
    { label: '성과/결과 표현', value: 58, explain: '해결 이후 어떤 변화나 개선이 있었는지 결과 표현이 가장 부족했어요.' },
    { label: '공고 요구사항 연결성', value: 82, explain: '답변 내용이 공고에서 요구하는 역량과 자연스럽게 연결됐어요.' },
    { label: '직무 가이드 적합성', value: 69, explain: '백엔드 가이드에서 중요하게 보는 장애 대응·운영 관점 언급이 조금 부족했어요.' },
    { label: '답변 전달력', value: 75, explain: '구조는 안정적이었지만 문장이 길어지는 답변이 몇 차례 있었어요.' }
  ];
  var OVERALL_SCORE = Math.round(EVAL_ITEMS.reduce(function (sum, e) { return sum + e.value; }, 0) / EVAL_ITEMS.length);
  var PASS_THRESHOLD = 70;

  var WEAKNESS_TAGS = [
    { tag: '성과 수치화 부족', desc: '결과를 수치나 구체적인 변화로 표현하는 연습이 필요해요', lastAttempt: { itemScores: [72, 65, 80], finalScore: 74 } },
    { tag: '역할 구분 불명확', desc: '본인이 직접 수행한 역할을 더 명확하게 구분해서 답하면 좋아요', lastAttempt: { itemScores: [78, 82], finalScore: 80 } },
    { tag: '운영·장애대응 경험 부족', desc: '직무 가이드에서 중시하는 장애 대응·운영 관점을 더 반영해보세요', lastAttempt: { itemScores: [68, 71], finalScore: 69 } }
  ];
  function isWeaknessResolved(w) {
    return w.lastAttempt.finalScore >= PASS_THRESHOLD && w.lastAttempt.itemScores.every(function (v) { return v >= PASS_THRESHOLD; });
  }

  var RESUME_CARDS = [
    { title: '정량적 성과 표현 보강', what: '이력서 경력 사항의 프로젝트 설명 문장', why: '면접에서 결과를 수치로 표현하지 못해 설득력이 떨어졌어요. 트래픽 처리량, 응답 속도 개선 폭 등 구체적인 수치를 이력서에도 추가하면 서류 단계부터 강점을 어필할 수 있어요.' },
    { title: '본인 역할 구분 명시', what: '팀 프로젝트 경험 항목', why: '면접에서 팀 성과와 본인 역할을 명확히 구분하지 못했어요. 이력서에도 본인이 담당한 부분을 구체적으로 적으면 역량이 더 잘 드러나요.' },
    { title: '장애 대응·운영 경험 추가', what: '경력/프로젝트 경험 섹션', why: '직무 가이드에서 중요하게 보는 장애 대응·운영 경험 언급이 부족했어요. 관련 경험이 있다면 이력서에도 별도 항목으로 정리해보세요.' },
    { title: '공고 요구 역량 키워드 반영', what: '자기소개서 지원 동기 문단', why: '답변에서 공고 요구사항과의 연결이 약했던 부분이 있었어요. 이력서·자기소개서에 지원 공고의 핵심 키워드를 자연스럽게 녹여보세요.' },
    { title: '학습 계획 명시', what: '기술 스택/자기계발 항목', why: '데이터 시각화·모니터링 관련 경험이 부족하다고 답했어요. 학습 중이거나 계획 중인 도구가 있다면 이력서에도 "학습 중" 상태로 명시하면 성장 가능성을 보여줄 수 있어요.' }
  ];

  function sevLabel(v) { return v >= 80 ? '우수' : v >= 60 ? '보통' : '부족'; }
  function sevBg(v) { return v >= 80 ? '#E6F4EC' : v >= 60 ? '#EAF2FB' : '#FDF0E4'; }
  function sevColor(v) { return v >= 80 ? '#1E7A4C' : v >= 60 ? '#185FA5' : '#B5622E'; }

  var state = { tab: 'list', sel: null };

  function renderTabs() {
    bindTabGroup({
      btnSelector: '.tabbar__btn',
      datasetKey: 'tab',
      panelSelector: '.tab-panel',
      panelDatasetKey: 'panel'
    });
  }

  function renderQList() {
    document.getElementById('q-list').innerHTML = QUESTIONS.map(function (q, idx) {
      var selected = state.sel === idx;
      return '<div class="q-list-item' + (selected ? ' is-active' : '') + '" data-select-q="' + idx + '">' +
        '<span class="q-list-item__num">' + (idx + 1) + '</span><span class="q-list-item__summary">' + q.summary + '</span>' +
        '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#1E8E5A" stroke-width="3"><path d="M20 6L9 17l-5-5"/></svg>' +
      '</div>';
    }).join('');
    document.querySelectorAll('[data-select-q]').forEach(function (b) {
      b.addEventListener('click', function () { state.sel = parseInt(b.dataset.selectQ, 10); renderQList(); renderQDetail(); });
    });
  }

  function evalRows(items, cls, label) {
    return items.map(function (t) { return '<div class="eval-row"><span class="eval-chip eval-chip--' + cls + '">' + label + '</span><span class="eval-text">' + t + '</span></div>'; }).join('');
  }

  function renderQDetail() {
    var container = document.getElementById('q-detail');
    if (state.sel === null) {
      container.innerHTML = '<div style="display:flex; flex-direction:column; align-items:center; justify-content:center; height:100%; min-height:390px; text-align:center; color:#8A93A3;">' +
        '<p style="font-size:14px; font-weight:600; color:#5B6370; margin:0 0 4px;">질문을 선택해주세요</p>' +
        '<p style="font-size:12.5px; margin:0;">왼쪽 목록에서 질문을 클릭하면 대화 기록과 평가를 볼 수 있어요</p></div>';
      return;
    }
    var q = QUESTIONS[state.sel];
    var html =
      '<div class="thread-row"><div style="display:flex; flex-direction:column; align-items:center;"><div class="thread-avatar thread-avatar--q">Q</div><div class="thread-connector"></div></div>' +
        '<div style="flex:1; padding-bottom:4px;"><p class="thread-label">질문</p><p class="thread-text">' + q.original + '</p></div></div>' +
      '<div class="thread-row"><div style="display:flex; flex-direction:column; align-items:center;"><div class="thread-avatar thread-avatar--a">A</div><div class="thread-connector"></div></div>' +
        '<div style="flex:1; padding-bottom:4px;"><p class="thread-label">답변</p><p class="thread-text">' + q.answer1 + '</p>' +
        '<div class="eval-box">' + evalRows(q.eval1.good, 'good', '잘한 점') + evalRows(q.eval1.improve, 'improve', '개선할 점') + '</div></div></div>' +
      '<div class="thread-row"><div style="display:flex; flex-direction:column; align-items:center;"><div class="thread-avatar thread-avatar--q">↩</div><div class="thread-connector"></div></div>' +
        '<div style="flex:1; padding-bottom:4px;"><p class="thread-label">꼬리 질문</p><p class="thread-text">' + q.followUp + '</p></div></div>' +
      '<div class="thread-row"><div style="display:flex; flex-direction:column; align-items:center;"><div class="thread-avatar thread-avatar--a">A</div></div>' +
        '<div style="flex:1;"><p class="thread-label">답변</p><p class="thread-text">' + q.answer2 + '</p>' +
        '<div class="eval-box">' + evalRows(q.eval2.good, 'good', '잘한 점') + evalRows(q.eval2.improve, 'improve', '개선할 점') + '</div>' +
        (q.tags.length ? '<div class="flex-row gap-8" style="flex-wrap:wrap; margin-top:12px;"><span style="font-size:11px; color:#8A93A3;">관련 약점 태그</span>' + q.tags.map(function (t) { return '<span class="badge-pill" style="color:#B5622E; background:#FDF0E4;">#' + t + '</span>'; }).join('') + '</div>' : '') +
        '</div></div>';
    container.innerHTML = html;
  }

  function jumpToQuestion(idx) {
    document.querySelector('.tabbar__btn[data-tab="list"]').click();
    state.sel = idx;
    renderQList();
    renderQDetail();
    var el = document.querySelector('[data-select-q="' + idx + '"]');
    if (el) el.scrollIntoView({ block: 'nearest' });
  }

  function renderEvalList() {
    var sorted = EVAL_ITEMS.slice().sort(function (a, b) { return a.value - b.value; });
    document.getElementById('eval-list').innerHTML = sorted.map(function (e) {
      return '<div class="eval-item-card"><span class="eval-item-card__value">' + e.value + '</span><div style="flex:1;">' +
        '<div class="flex-row gap-8" style="margin-bottom:6px;"><span style="font-size:13.5px; font-weight:600;">' + e.label + '</span>' +
        '<span class="eval-item-card__sev" style="background:' + sevBg(e.value) + '; color:' + sevColor(e.value) + ';">' + sevLabel(e.value) + '</span></div>' +
        '<p style="font-size:12.5px; color:#5B6370; line-height:1.6; margin:0;">' + e.explain + '</p></div></div>';
    }).join('');
  }

  function renderResumeCards() {
    document.getElementById('resume-cards').innerHTML = RESUME_CARDS.map(function (rc, idx) {
      return '<div class="resume-card"><span class="resume-card__num">' + (idx + 1) + '</span><div style="flex:1;">' +
        '<p style="font-size:14px; font-weight:700; margin:0 0 8px;">' + rc.title + '</p>' +
        '<p style="font-size:12.5px; color:#5B6370; margin:0 0 6px;"><span style="font-weight:700; color:#14181F;">어떤 부분을 · </span>' + rc.what + '</p>' +
        '<p style="font-size:12.5px; color:#5B6370; margin:0;"><span style="font-weight:700; color:#14181F;">왜 · </span>' + rc.why + '</p></div></div>';
    }).join('');
  }

  function renderSidebar() {
    var deg = Math.round(OVERALL_SCORE / 100 * 360);
    document.getElementById('overall-donut').innerHTML =
      '<div style="width:88px; height:88px; border-radius:50%; background:conic-gradient(#185FA5 ' + deg + 'deg, #F0F2F5 0); display:flex; align-items:center; justify-content:center; margin:0 auto;">' +
      '<div style="width:70px; height:70px; border-radius:50%; background:#fff; display:flex; align-items:center; justify-content:center; font-size:20px; font-weight:700;">' + OVERALL_SCORE + '</div></div>';
    document.getElementById('overall-score-label').textContent = OVERALL_SCORE + '점 / 100점';

    document.getElementById('weakness-list').innerHTML = WEAKNESS_TAGS.map(function (w) {
      var related = [];
      QUESTIONS.forEach(function (q, idx) { if (q.tags.indexOf(w.tag) !== -1) related.push(idx); });
      var resolved = isWeaknessResolved(w);
      return '<div><div class="flex-row gap-8" style="margin-bottom:6px; flex-wrap:wrap;">' +
        '<span class="badge-pill" style="color:#B5622E; background:#FDF0E4;">#' + w.tag + '</span>' +
        '<span class="badge-pill" style="font-size:10px; color:' + (resolved ? '#1E7A4C' : '#8A93A3') + '; background:' + (resolved ? '#E6F4EC' : '#fff') + '; border:1px solid #EEDFC0;">' + (resolved ? '해결 완료' : '미해결') + '</span></div>' +
        '<p style="font-size:12px; color:#5B6370; line-height:1.5; margin:0 0 6px;">' + w.desc + '</p>' +
        '<div class="flex-row gap-8" style="flex-wrap:wrap;">' + related.map(function (idx) { return '<button class="weak-chip-jump" data-jump="' + idx + '">질문 ' + (idx + 1) + '</button>'; }).join('') + '</div></div>';
    }).join('');
    document.querySelectorAll('[data-jump]').forEach(function (b) { b.addEventListener('click', function () { jumpToQuestion(parseInt(b.dataset.jump, 10)); }); });
  }

  document.addEventListener('DOMContentLoaded', function () {
    renderTabs();
    renderQList();
    renderQDetail();
    renderEvalList();
    renderResumeCards();
    renderSidebar();
  });
})();
