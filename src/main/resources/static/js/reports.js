// reports.js

(function () {
  'use strict';

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

  function reportModeLabel(mode) {
    if (mode === 'BASIC') return '기본 모의면접';
    if (mode === 'COMPANY_FIT') return '회사 맞춤 면접';
    return '약점 보완 면접';
  }

  function formatReportDate(value) {
    if (!value) return '';
    return new Date(value).toLocaleString('ko-KR', {
      year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit'
    });
  }

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

  var state = { tab: 'report', sessions: [], sessionsLoaded: false, selectedSessionId: null, guideMajor: 'IT·개발', guideMinor: '백엔드 개발', guideCareer: '신입' };

  function renderTabs() {
    bindTabGroup({
      btnSelector: '.tabbar__btn',
      datasetKey: 'tab',
      panelSelector: '.tab-panel',
      panelDatasetKey: 'panel',
      onSelect: function (value) { state.tab = value; renderAll(); }
    });
  }

  function renderReportList() {
    var list = document.getElementById('report-list');
    if (!state.sessions.length) {
      list.innerHTML = state.sessionsLoaded
        ? '<p style="color:#8A93A3; font-size:12.5px;">완료한 면접이 없습니다.</p>'
        : '<p style="color:#8A93A3; font-size:12.5px;">불러오는 중입니다...</p>';
      return;
    }
    list.innerHTML = state.sessions.map(function (s) {
      var selected = s.sessionId === state.selectedSessionId;
      return '<div class="report-list-item' + (selected ? ' is-active' : '') + '" data-select-report="' + s.sessionId + '">' +
        '<p class="report-list-item__title">' + reportModeLabel(s.mode) + '</p>' +
        '<p class="report-list-item__date">' + formatReportDate(s.completedAt) + '</p>' +
        '<p class="report-list-item__score">질문 ' + s.questionCount + '개</p></div>';
    }).join('');
    document.querySelectorAll('[data-select-report]').forEach(function (b) {
      b.addEventListener('click', function () {
        state.selectedSessionId = Number(b.dataset.selectReport);
        renderAll();
      });
    });
  }

  function renderReportDetail() {
    var detail = document.getElementById('report-detail');
    if (!state.sessions.length) {
      detail.innerHTML = '<p style="color:#8A93A3; font-size:13px;">완료한 면접이 없습니다. 면접을 완료하면 여기서 최종 리포트를 다시 볼 수 있어요.</p>';
      return;
    }
    var sel = state.sessions.find(function (s) { return s.sessionId === state.selectedSessionId; }) || state.sessions[0];
    state.selectedSessionId = sel.sessionId;

    var html = '<p style="font-size:16px; font-weight:700; margin:0 0 4px;">' + reportModeLabel(sel.mode) + '</p>' +
      '<p style="font-size:12.5px; color:#8A93A3; margin:0 0 20px;">' + formatReportDate(sel.completedAt) +
      ' · 질문 ' + sel.questionCount + '개' + '</p>' +
      '<p style="font-size:13px; color:#5B6370; margin:0 0 20px; line-height:1.7;">약점 태그, 다음 연습 추천, 서류 보완 제안 같은 최종 리포트 내용은 아래 화면에서 자세히 볼 수 있어요. ' +
      '(처음 여는 리포트는 그 자리에서 만들어지느라 몇 초 걸릴 수 있어요.)</p>' +
      '<a href="/interview-result.html?sessionId=' + encodeURIComponent(sel.sessionId) +
      '" class="btn btn--primary" style="text-decoration:none;">최종 리포트 자세히 보기</a>';

    detail.innerHTML = html;
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
    api('/api/interview-sessions/history').then(function (sessions) {
      state.sessions = sessions;
      state.sessionsLoaded = true;
      if (sessions.length) state.selectedSessionId = sessions[0].sessionId;
      renderReportList();
      renderReportDetail();
    }).catch(function (error) {
      state.sessionsLoaded = true;
      document.getElementById('report-list').innerHTML =
        '<p style="color:#B5433D; font-size:12.5px;">' + error.message + '</p>';
    });
  });
})();
