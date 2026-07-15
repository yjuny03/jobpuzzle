// reports.js

(function () {
  'use strict';

  var PAST_REPORTS = [
    { id: 'r3', title: '백엔드 개발자 모의면접 #3', date: '2026.07.07', score: 82 },
    { id: 'r2', title: '백엔드 개발자 모의면접 #2', date: '2026.06.28', score: 74 },
    { id: 'r1', title: '백엔드 개발자 모의면접 #1', date: '2026.06.20', score: 68 }
  ];

  var CONNECTION_BY_ID = {
    r3: { level: 'HIGH', pct: 100, desc: '공고 요구사항과 이력서·포트폴리오 경험이 대부분 일치했어요. 자료 보완 없이 실제 요구사항 기반 질문으로 진행됐어요.', weakSpots: [],
      breakdown: [
        { label: '대규모 트래픽 처리 경험', level: 'HIGH', fit: '포트폴리오에 캐싱·큐 도입으로 응답속도와 동시접속 처리량을 개선한 수치가 구체적으로 정리되어 있어요.', gap: '' },
        { label: 'RESTful API 설계 경험', level: 'HIGH', fit: '포트폴리오에 API 엔드포인트 구조와 설계 원칙이 구체적인 프로젝트 사례로 남아있어요.', gap: '' },
        { label: '협업 커뮤니케이션 능력', level: 'HIGH', fit: '자기소개서와 경험정리 자료의 조율 과정이 구체적으로 서술되어 있어요.', gap: '' },
        { label: '장애 대응·운영 경험', level: 'HIGH', fit: '경험정리 자료에 장애 감지부터 복구, 재발 방지까지의 과정이 구체적으로 정리되어 있어요.', gap: '' },
        { label: '직무 관련 성과·문제해결 경험', level: 'HIGH', fit: '이력서·포트폴리오·경험정리 모두 성과 수치와 함께 정리되어 있어요.', gap: '' }
      ] },
    r2: { level: 'MEDIUM', pct: 75, desc: '핵심 요구사항은 충족했지만, 장애 대응·모니터링 경험 자료가 부족해 일부는 과제로 보완한 뒤 질문이 생성됐어요.', weakSpots: ['장애 대응·모니터링 경험을 뒷받침할 이력서·포트폴리오 자료가 부족해요', '클라우드 인프라 운영 경험에 대한 근거 자료가 일부 누락돼 있어요'],
      breakdown: [
        { label: '대규모 트래픽 처리 경험', level: 'MEDIUM', fit: '포트폴리오에 트래픽 처리 관련 프로젝트 사례가 있어요.', gap: '경험정리 자료에 트래픽 급증 대응 경험이 정리되어 있지 않아요.' },
        { label: 'RESTful API 설계 경험', level: 'HIGH', fit: '포트폴리오에 API 엔드포인트 구조와 설계 원칙이 구체적으로 남아있어요.', gap: '' },
        { label: '장애 대응·운영 경험', level: 'LOW', fit: '', gap: '경험정리 자료에 장애 감지·대응·재발방지 관련 경험이 담겨있지 않아요.' },
        { label: '직무 관련 성과·문제해결 경험', level: 'MEDIUM', fit: '이력서와 포트폴리오에 성과가 담긴 프로젝트 사례가 있어요.', gap: '경험정리 자료에 문제 해결 과정이 정리되어 있지 않아요.' }
      ] },
    r1: { level: 'LOW', pct: 50, desc: '자격 요건 중 클라우드 인프라 운영 경험 관련 자료가 부족해 여러 항목이 과제로 대체됐어요.', weakSpots: ['AWS 등 클라우드 인프라 운영 경험 관련 자료가 거의 없어요', '대규모 트래픽 처리 경험을 구체적으로 뒷받침할 자료가 부족해요', '협업 경험 자료가 본인 역할 중심으로 정리되어 있지 않아요'],
      breakdown: [
        { label: '대규모 트래픽 처리 경험', level: 'LOW', fit: '', gap: '포트폴리오와 경험정리 모두에 트래픽 처리량이나 성능 개선 수치가 없어요.' },
        { label: 'RESTful API 설계 경험', level: 'MEDIUM', fit: '포트폴리오에 관련 프로젝트 설명이 일부 있어요.', gap: '구체적인 엔드포인트 설계 기준은 드러나지 않아요.' },
        { label: '협업 커뮤니케이션 능력', level: 'LOW', fit: '', gap: '자기소개서와 경험정리 모두에 협업·갈등 해결 경험 서술이 없어요.' },
        { label: '장애 대응·운영 경험', level: 'NONE', fit: '', gap: '경험정리 자료에 장애 관련 내용이 전혀 담겨있지 않아요.' },
        { label: '직무 관련 성과·문제해결 경험', level: 'LOW', fit: '이력서에 프로젝트 경력이 간략히 적혀 있어요.', gap: '포트폴리오와 경험정리에 문제 해결 과정과 성과 수치가 구체적으로 담겨있지 않아요.' }
      ] }
  };
  var LEVEL_COLOR = { HIGH: '#1E7A4C', MEDIUM: '#185FA5', LOW: '#B5622E', INSUFFICIENT: '#B5433D', NONE: '#8A93A3' };

  var JOB_MAJOR_OPTIONS = ['IT·개발', '경영·사무', '디자인'];
  var JOB_MINOR_BY_MAJOR = {
    'IT·개발': ['백엔드 개발', '프론트엔드 개발', '데이터 분석'],
    '경영·사무': ['인사 채용', '마케팅'],
    '디자인': ['UX/UI 디자인', 'BX 디자인']
  };
  var JOB_CAREER_OPTIONS = ['신입', '경력 1~3년', '경력 3년 이상'];

  var JOB_REPORT_BY_KEY = {
    '백엔드 개발__신입': {
      sampleSize: 1284,
      topSkills: [{ label: 'Spring Boot', pct: 78 }, { label: 'MySQL/RDBMS 설계', pct: 71 }, { label: 'API 설계', pct: 68 }, { label: 'Redis/캐싱', pct: 54 }, { label: 'AWS/클라우드 인프라', pct: 47 }, { label: 'MSA/분산 시스템', pct: 33 }],
      commonWeaknesses: [{ tag: '성과 수치화 부족', pct: 62 }, { tag: '역할 구분 불명확', pct: 51 }, { tag: '문제 해결 과정 설명 부족', pct: 44 }, { tag: '직무 연결성 부족', pct: 37 }],
      frequentQuestions: [{ text: '트래픽이 급증했던 상황에서 어떻게 대응했나요?', freq: 412 }, { text: '본인이 직접 설계하거나 판단한 부분은 무엇인가요?', freq: 356 }, { text: 'DB 인덱스나 쿼리 최적화 경험이 있나요?', freq: 298 }, { text: '팀원과 의견이 갈렸을 때 어떻게 조율했나요?', freq: 241 }],
      toughQuestions: [{ text: '해당 기술을 선택한 이유와 트레이드오프를 설명해주세요.', strugglePct: 64 }, { text: '장애 발생 원인을 어떻게 추적하고 재발을 방지했나요?', strugglePct: 57 }, { text: '모니터링/로그 분석 도구를 사용해본 경험이 있나요?', strugglePct: 69 }]
    },
    '프론트엔드 개발__신입': {
      sampleSize: 842,
      topSkills: [{ label: 'React', pct: 82 }, { label: 'TypeScript', pct: 69 }, { label: '상태관리', pct: 58 }, { label: '성능 최적화', pct: 41 }, { label: 'CSS 아키텍처', pct: 35 }],
      commonWeaknesses: [{ tag: '성과 수치화 부족', pct: 55 }, { tag: '사용자 경험 근거 부족', pct: 48 }, { tag: '역할 구분 불명확', pct: 39 }],
      frequentQuestions: [{ text: '렌더링 성능 문제를 경험한 적이 있나요?', freq: 301 }, { text: '상태관리 라이브러리를 선택한 기준은 무엇인가요?', freq: 264 }],
      toughQuestions: [{ text: '적용한 최적화 기법의 원리를 설명해주세요.', strugglePct: 61 }]
    }
  };
  function jobReportFor(minor, career) { return JOB_REPORT_BY_KEY[minor + '__' + career] || JOB_REPORT_BY_KEY['백엔드 개발__신입']; }

  var REC_JOBS = [
    { company: 'LUME', title: '백엔드 개발자 (신입/무관)', location: '서울 · 정규직', match: 92 },
    { company: 'GreenTech', title: '백엔드 개발자 (신입)', location: '판교 · 정규직', match: 86 },
    { company: 'DevRoute', title: '백엔드 개발자 (신입/경력)', location: '서울 · 정규직', match: 78 }
  ];

  var state = { tab: 'report', selectedId: 'r3', reportStep: 'connection', guideMajor: 'IT·개발', guideMinor: '백엔드 개발', guideCareer: '신입' };

  function renderTabs() {
    bindTabGroup({
      btnSelector: '.tabbar__btn',
      datasetKey: 'tab',
      panelSelector: '.tab-panel',
      panelDatasetKey: 'panel',
      onSelect: function (value) { state.tab = value; renderAll(); }
    });
  }

  function donut(pct, size, innerLabelHtml, color) {
    color = color || '#185FA5';
    var deg = Math.round(pct / 100 * 360);
    return '<div class="donut" style="width:' + size + 'px; height:' + size + 'px; background:conic-gradient(' + color + ' ' + deg + 'deg, #E3E7ED 0);">' +
      '<div class="donut__inner" style="width:' + Math.round(size * 0.73) + 'px; height:' + Math.round(size * 0.73) + 'px;">' + innerLabelHtml + '</div></div>';
  }

  function renderReportList() {
    document.getElementById('report-list').innerHTML = PAST_REPORTS.map(function (r) {
      var selected = r.id === state.selectedId;
      return '<div class="report-list-item' + (selected ? ' is-active' : '') + '" data-select-report="' + r.id + '">' +
        '<p class="report-list-item__title">' + r.title + '</p><p class="report-list-item__date">' + r.date + '</p><p class="report-list-item__score">' + r.score + '점</p></div>';
    }).join('');
    document.querySelectorAll('[data-select-report]').forEach(function (b) {
      b.addEventListener('click', function () { state.selectedId = b.dataset.selectReport; state.reportStep = 'connection'; renderAll(); });
    });
  }

  function renderReportDetail() {
    var sel = PAST_REPORTS.find(function (r) { return r.id === state.selectedId; }) || PAST_REPORTS[0];
    var conn = CONNECTION_BY_ID[sel.id] || CONNECTION_BY_ID.r3;
    var html = '<p style="font-size:16px; font-weight:700; margin:0 0 4px;">' + sel.title + '</p><p style="font-size:12.5px; color:#8A93A3; margin:0 0 20px;">' + sel.date + ' · 총 질문 10개</p>';

    if (state.reportStep === 'connection') {
      html += '<p style="font-size:14px; font-weight:700; margin:0 0 4px;">공고 요구사항 · 자료 연결 분석 결과</p>' +
        '<p style="font-size:12.5px; color:#8A93A3; margin:0 0 20px;">면접 전 진행했던 자료 연결 분석 결과예요. 이 결과를 바탕으로 질문이 생성됐어요.</p>' +
        '<div class="flex-row gap-12" style="flex-wrap:wrap; margin-bottom:20px;">' +
          donut(conn.pct, 110, '<p style="font-size:17px; font-weight:700; margin:0;">' + conn.pct + '%</p><p style="font-size:11px; font-weight:700; margin:2px 0 0; color:#185FA5;">' + conn.level + '</p>') +
          '<p style="font-size:13px; color:#5B6370; margin:0; line-height:1.7; flex:1; min-width:220px;">' + conn.desc + '</p>' +
        '</div>';

      if (conn.weakSpots.length) {
        html += '<div class="weakspot-box"><p style="font-size:12.5px; font-weight:700; color:#8A5A22; margin:0 0 10px;">개선이 필요한 항목</p><div style="display:flex; flex-direction:column; gap:8px;">' +
          conn.weakSpots.map(function (w) { return '<p style="font-size:12.5px; color:#5B6370; margin:0; line-height:1.6;">• ' + w + '</p>'; }).join('') + '</div></div>';
      }

      html += '<div style="margin-bottom:24px;"><p style="font-size:13px; font-weight:700; margin:0 0 10px;">요구사항별 자료 적합도</p><div style="display:flex; flex-direction:column; gap:8px;">' +
        conn.breakdown.map(function (r) {
          return '<div class="req-row"><div class="flex-row" style="justify-content:space-between; gap:8px; flex-wrap:wrap; margin-bottom:6px;">' +
            '<p style="font-size:12.5px; font-weight:700; margin:0;">' + r.label + '</p><span style="font-size:11px; font-weight:700; color:' + LEVEL_COLOR[r.level] + ';">' + r.level + '</span></div>' +
            (r.fit ? '<p style="font-size:11.5px; color:#1E7A4C; margin:0 0 3px; line-height:1.6;">✓ ' + r.fit + '</p>' : '') +
            (r.gap ? '<p style="font-size:11.5px; color:#B5433D; margin:0; line-height:1.6;">✕ ' + r.gap + '</p>' : '') +
          '</div>';
        }).join('') + '</div></div>';

      html += '<div class="flex-row" style="justify-content:flex-end;"><button class="btn btn--primary" id="go-final-btn">면접 최종 평가 확인하기</button></div>';
    } else {
      var score = sel.score;
      html += '<button class="btn-sm" id="go-connection-btn" style="margin-bottom:20px;">← 연결 분석 결과 다시보기</button>' +
        '<div class="flex-row gap-12" style="margin-bottom:24px;">' +
          donut(score, 70, '<div style="font-size:17px; font-weight:700;">' + score + '</div>') +
          '<div><p style="font-size:14px; font-weight:600; margin:0;">최종 준비도 점수</p><p style="font-size:12px; color:#8A93A3; margin:4px 0 0;">항목별 점수와 종합 평가를 반영한 결과예요</p></div>' +
        '</div>' +
        '<div class="flex-row gap-12" style="flex-wrap:wrap;">' +
          '<a href="/interview-result.html" class="btn btn--primary" style="text-decoration:none;">질문별 답변 · 약점 태그 자세히 보기</a>' +
          '<a href="/interview.html" class="btn" style="background:#fff; border:1px solid #E3E7ED; text-decoration:none; color:#14181F;">약점 보완 모드로 연습 시작</a>' +
          '<button class="btn" style="background:#fff; border:1px solid #E3E7ED;">PDF 내보내기</button>' +
        '</div>';
    }

    document.getElementById('report-detail').innerHTML = html;
    var goFinal = document.getElementById('go-final-btn');
    if (goFinal) goFinal.addEventListener('click', function () { state.reportStep = 'final'; renderReportDetail(); });
    var goConn = document.getElementById('go-connection-btn');
    if (goConn) goConn.addEventListener('click', function () { state.reportStep = 'connection'; renderReportDetail(); });
  }

  function renderGuide() {
    var jr = jobReportFor(state.guideMinor, state.guideCareer);
    var html = '<div class="flex-row" style="justify-content:space-between; margin-bottom:6px; flex-wrap:wrap; gap:8px;">' +
      '<div><p style="font-size:12.5px; color:#8A93A3; margin:0 0 6px;">' + state.guideMajor + ' &gt; ' + state.guideMinor + ' &gt; ' + state.guideCareer + '</p><p style="font-size:18px; font-weight:700; margin:0;">' + state.guideMinor + ' 직무별 리포트</p></div>' +
      '<span class="badge-pill" style="color:#185FA5; background:#DCE7F3;">누적 ' + jr.sampleSize + '건 분석</span></div>';

    html += '<div class="job-filter-bar">' +
      '<div class="job-filter-field"><label>대분류</label><select id="guide-major">' + JOB_MAJOR_OPTIONS.map(function (m) { return '<option' + (m === state.guideMajor ? ' selected' : '') + '>' + m + '</option>'; }).join('') + '</select></div>' +
      '<div class="job-filter-field"><label>중분류</label><select id="guide-minor">' + (JOB_MINOR_BY_MAJOR[state.guideMajor] || []).map(function (m) { return '<option' + (m === state.guideMinor ? ' selected' : '') + '>' + m + '</option>'; }).join('') + '</select></div>' +
      '<div class="job-filter-field"><label>경력수준</label><select id="guide-career">' + JOB_CAREER_OPTIONS.map(function (c) { return '<option' + (c === state.guideCareer ? ' selected' : '') + '>' + c + '</option>'; }).join('') + '</select></div>' +
    '</div>';
    html += '<p style="font-size:12.5px; color:#8A93A3; margin:0 0 22px;">이 직무에 지원한 사용자들의 모의면접 데이터를 모아 분석한 리포트예요</p>';

    html += '<p style="font-size:13.5px; font-weight:700; margin:0 0 12px;">자주 요구되는 기술/역량</p><div class="flex-row gap-8" style="flex-wrap:wrap; margin-bottom:24px;">' +
      jr.topSkills.map(function (sk) { return '<span class="skill-chip">' + sk.label + ' <span style="color:#5B84AC; font-weight:500;">' + sk.pct + '%</span></span>'; }).join('') + '</div>';

    html += '<p style="font-size:13.5px; font-weight:700; margin:0 0 12px;">많이 확인된 약점 태그</p><div style="display:flex; flex-direction:column; gap:8px; margin-bottom:24px;">' +
      jr.commonWeaknesses.map(function (w) { return '<div class="weak-tag-row"><span style="font-size:13px; font-weight:700; color:#8A5A22;">#' + w.tag + '</span><span style="font-size:11.5px; color:#B08A4E;">지원자 ' + w.pct + '%에서 확인</span></div>'; }).join('') + '</div>';

    html += '<p style="font-size:13.5px; font-weight:700; margin:0 0 12px;">자주 출제된 질문</p><div style="display:flex; flex-direction:column; gap:8px; margin-bottom:24px;">' +
      jr.frequentQuestions.map(function (q) { return '<div class="freq-question"><p style="font-size:13.5px; font-weight:600; margin:0 0 4px;">' + q.text + '</p><p style="font-size:12px; color:#8A93A3; margin:0;">출제 빈도 ' + q.freq + '회</p></div>'; }).join('') + '</div>';

    html += '<p style="font-size:13.5px; font-weight:700; margin:0 0 12px;">답변에서 약점이 많이 발생하는 질문</p><div style="display:flex; flex-direction:column; gap:8px;">' +
      jr.toughQuestions.map(function (q) { return '<div class="tough-question"><p style="font-size:13.5px; font-weight:600; margin:0 0 4px;">' + q.text + '</p><p style="font-size:12px; color:#B5433D; margin:0;">지원자 ' + q.strugglePct + '%의 답변에서 약점 태그가 생성되었어요</p></div>'; }).join('') + '</div>';

    document.getElementById('guide-detail').innerHTML = html;

    document.getElementById('guide-major').addEventListener('change', function (e) {
      state.guideMajor = e.target.value;
      state.guideMinor = (JOB_MINOR_BY_MAJOR[state.guideMajor] || [])[0] || '';
      renderGuide();
    });
    document.getElementById('guide-minor').addEventListener('change', function (e) { state.guideMinor = e.target.value; renderGuide(); });
    document.getElementById('guide-career').addEventListener('change', function (e) { state.guideCareer = e.target.value; renderGuide(); });
  }

  function renderRecs() {
    document.getElementById('recs-list').innerHTML = REC_JOBS.map(function (rec) {
      return '<div class="rec-row"><div><p style="font-size:14.5px; font-weight:700; margin:0 0 4px;">' + rec.company + ' <span style="font-size:12px; font-weight:600; color:#1E8E5A;">매칭도 ' + rec.match + '%</span></p><p style="font-size:12.5px; color:#8A93A3; margin:0;">' + rec.title + ' · ' + rec.location + '</p></div><button class="btn btn--primary">분석 시작</button></div>';
    }).join('') + '<div style="background:#F3F8FD; border:1px solid #E3E7ED; border-radius:14px; padding:20px 24px; margin-top:8px;"><p style="font-size:13.5px; font-weight:700; margin:0 0 10px;">다음 연습 추천</p><p style="font-size:13px; color:#5B6370; margin:0;">API 설계 질문 연습 · DB 모델링 심화 연습 · 협업/커뮤니케이션 연습</p></div>';
  }

  function renderAll() {
    renderReportList();
    renderReportDetail();
    renderGuide();
    renderRecs();
  }

  document.addEventListener('DOMContentLoaded', function () {
    renderTabs();
    renderAll();
  });
})();
