// interview.js — 면접 준비: 질문 생성 flow (dummy data, simplified but functional)

(function () {
  'use strict';

  var MODES = [
    { id: 'basic', icon: 'basic', label: '기본 질문 모드', desc: '직무 공통 질문으로 빠르게 연습해요' },
    { id: 'weakness', icon: 'tag', label: '약점 보완 모드', desc: '반복되는 약점 태그를 골라 집중 연습해요' },
    { id: 'custom', icon: 'sparkle', label: '맞춤 면접 질문', desc: '내 자료와 공고를 분석해 맞춤 질문을 만들어요' }
  ];

  var WEAKNESS_TAGS = [
    { tag: '성과 수치화 부족', desc: '결과를 수치나 구체적인 변화로 표현하는 연습이 필요해요', resolved: false },
    { tag: '운영·장애대응 경험 부족', desc: '직무 가이드에서 중시하는 장애 대응·운영 관점을 더 반영해보세요', resolved: false },
    { tag: '역할 구분 불명확', desc: '본인이 직접 수행한 역할을 더 명확하게 구분해서 답하면 좋아요', resolved: true }
  ];

  var MAJOR_OPTIONS = ['IT·개발', '경영·사무', '디자인'];
  var MINOR_BY_MAJOR = { 'IT·개발': ['백엔드 개발', '프론트엔드 개발', '데이터 분석'], '경영·사무': ['인사 채용', '마케팅'], '디자인': ['UX/UI 디자인', 'BX 디자인'] };
  var LEVEL_OPTIONS = ['신입', '경력 1~3년', '경력 3년 이상'];

  var MATERIAL_CATEGORIES_COMPANY = [
    { key: 'jobPosting', label: '채용공고', docs: [{ id: 'jd1', title: 'LUME 백엔드 개발자 공고', date: '2026.07.07' }] },
    { key: 'companyInfo', label: '회사 정보', docs: [{ id: 'ci1', title: 'LUME 회사 정보', date: '2026.07.06' }] }
  ];
  var MATERIAL_CATEGORIES_CANDIDATE = [
    { key: 'resume', label: '이력서', docs: [{ id: 'r1', title: '홍길동_이력서', date: '2026.07.07' }] },
    { key: 'coverLetter', label: '자기소개서', docs: [{ id: 'c1', title: '홍길동_자기소개서', date: '2026.07.07' }] },
    { key: 'portfolio', label: '포트폴리오', docs: [{ id: 'p1', title: '백엔드_프로젝트_포트폴리오', date: '2026.07.05' }] },
    { key: 'experience', label: '경험정리', docs: [{ id: 'e1', title: '경험_정리_STAR노트', date: '2026.06.28' }] }
  ];

  var COMPANY_SUGGESTIONS = ['LUME', 'LUME커머스', '네이버클라우드'];

  var COMPANY_EXTRACT = {
    jobTitle: '백엔드 개발자',
    mainDuties: '- 서버 API 설계 및 개발 (RESTful 기반)\n- 데이터베이스 설계 및 쿼리 최적화\n- 서비스 성능 개선 및 모니터링',
    requirements: '- Java 또는 Kotlin 기반 웹 개발 경험\n- Spring Boot, JPA 사용 경험\n- RDBMS 설계 및 SQL 작성 경험',
    preferred: '- 대규모 트래픽 처리 경험\n- AWS 등 클라우드 인프라 운영 경험',
    processInfo: '서류 → 코딩테스트 → 1차 실무면접 → 2차 임원면접 → 처우협의',
    url: 'https://recruit.lume.io/backend',
    company: { name: 'LUME', industry: 'IT·소프트웨어', serviceDesc: '커머스 플랫폼 LUME는 중소형 판매자를 위한 온라인 쇼핑몰 구축·운영 솔루션을 제공합니다.', values: '"기술로 판매자의 성장을 돕는다"는 미션 아래, 안정성과 실행 속도를 함께 추구합니다.', talentProfile: '문제를 스스로 정의하고 해결하는 주도성, 데이터 기반 의사결정, 협업 커뮤니케이션 역량을 중요하게 봅니다.', refUrl: 'https://lume.io' }
  };

  var RESUME_CARD = { name: '홍길동', desiredJob: '백엔드 개발자', education: 'OO대학교 컴퓨터공학과, 2018.03 ~ 2022.02, 학사', skills: 'Java, Kotlin, Spring Boot, JPA, MySQL, Redis, Docker, AWS', experience: '주문 API 응답속도 40% 개선, 동시접속 처리량 3배 확대 (2024.01~06) / 사내 관리자 대시보드 구축 (2023.09~12)', certs: '정보처리기사, AWS SAA, 교내 해커톤 우수상' };
  var COVER_CARD = { desiredJob: '백엔드 개발자', motivation: 'LUME의 대용량 트래픽 처리 기술력과 커머스 서비스 비전에 공감해 지원했습니다.', coreExperience: '온라인 쇼핑몰 백엔드 개발 프로젝트에서 주문·결제 API를 설계하고 캐싱 전략을 도입해 응답속도를 40% 개선했습니다.', collabExperience: '프론트엔드 팀과 API 스펙에 대한 의견 차이가 있었을 때 회의를 통해 조율했습니다.', jobConnection: '대규모 트래픽 처리와 REST API 설계 경험이 LUME 백엔드 개발자 직무와 연결됩니다.', futureGoal: '입사 후 서비스 장애 대응 프로세스를 함께 고도화하고 싶습니다.' };
  var PORTFOLIO_CARDS = [
    { projectName: '스마트몰 주문/결제 API 설계', period: '2024.01 ~ 2024.06', role: '백엔드 설계 및 구현 (본인 담당)', tech: 'Spring Boot, Redis, MySQL, AWS', coreContent: '주문·결제·재고 도메인의 REST API를 설계했습니다.', problemSolving: '피크 시간대 응답 지연 문제를 프로파일링으로 파악하고 캐싱으로 해결했습니다.', outcome: 'API 응답속도 40% 개선, 동시접속 처리량 3배 확대' },
    { projectName: '사내 관리자 대시보드', period: '2023.09 ~ 2023.12', role: '풀스택 개발', tech: 'Node.js, MongoDB, React', coreContent: '수기 정산·재고 관리 업무를 대시보드로 전환했습니다.', problemSolving: '현업 인터뷰로 우선순위 기능을 선정하고 반복 개선했습니다.', outcome: '처리 시간 20% 단축' }
  ];
  var EXPERIENCE_CARD = { situation: '프론트엔드 팀과 응답 데이터 구조에 대한 의견이 갈렸습니다.', taskGoal: '일정 지연 없이 양 팀이 동의할 수 있는 스펙을 확정해야 했습니다.', action: '실사용 시나리오를 정리해 회의를 열고 우선순위 기준을 세워 조율했습니다.', result: '하루 만에 합의에 도달했고 문서화 프로세스를 제안해 반영했습니다.', tech: 'Notion, Figma', learned: '갈등은 사실과 시나리오로 풀어야 빠르게 합의된다는 것을 배웠습니다.', interviewPoint: '데이터 기반 조율과 재발 방지 제안을 강조하고 싶습니다.' };

  var CONNECTION_SCENARIOS = {
    full: {
      level: 'HIGH', pct: 96,
      explanation: '공고 요구사항과 등록하신 자료가 대부분 일치해요. 과제 없이 바로 맞춤 질문으로 진행돼요.',
      weakSpots: [],
      breakdown: [
        { label: '대규모 트래픽 처리 경험', level: 'HIGH', basis: '자격 요건에 "RDBMS 설계 및 SQL 작성 경험", 우대사항에 "대규모 트래픽 처리 경험"이 명시되어 있어요.', fit: '포트폴리오에 캐싱·큐 도입으로 응답속도와 처리량을 개선한 수치가 구체적으로 정리되어 있어요.', gap: '' },
        { label: 'RESTful API 설계 경험', level: 'HIGH', basis: '주요 업무에 "서버 API 설계 및 개발 (RESTful 기반)"이 명시되어 있어요.', fit: '포트폴리오에 API 엔드포인트 구조와 설계 원칙이 프로젝트 사례로 남아있어요.', gap: '' },
        { label: '협업 커뮤니케이션 능력', level: 'HIGH', basis: '인재상에 "협업 커뮤니케이션 역량"이 명시되어 있어요.', fit: '자기소개서와 경험정리 자료에 조율 과정이 구체적으로 서술되어 있어요.', gap: '' }
      ],
      assignments: [], questions: true
    },
    partial: {
      level: 'MEDIUM', pct: 74,
      explanation: '핵심 요구사항은 충족했지만 일부 경험 자료가 부족해요. 부족한 부분은 과제로 보완한 뒤, 나머지는 질문으로 진행돼요.',
      weakSpots: ['장애 대응·모니터링 경험을 뒷받침할 자료가 부족해요', '클라우드 인프라 운영 경험 근거가 일부 누락돼 있어요'],
      breakdown: [
        { label: '대규모 트래픽 처리 경험', level: 'MEDIUM', basis: '우대사항에 "대규모 트래픽 처리 경험"이 명시되어 있어요.', fit: '포트폴리오에 관련 프로젝트 사례가 있어요.', gap: '경험정리 자료에 트래픽 급증 대응 경험이 정리되어 있지 않아요.' },
        { label: '장애 대응·운영 경험', level: 'LOW', basis: '주요 업무에 "서비스 성능 개선 및 모니터링"이 명시되어 있어요.', fit: '', gap: '경험정리 자료에 장애 감지·대응·재발방지 관련 경험이 담겨있지 않아요.' },
        { label: 'RESTful API 설계 경험', level: 'HIGH', basis: '주요 업무에 "서버 API 설계 및 개발"이 명시되어 있어요.', fit: '포트폴리오에 구체적으로 남아있어요.', gap: '' }
      ],
      assignments: [{ title: '장애 대응 경험 정리 과제', desc: '서비스 운영 중 발생했던 장애 상황을 감지 → 대응 → 재발 방지 순서로 정리해보세요.', reason: '장애 대응·운영 경험 근거 자료 부족' }],
      questions: true
    },
    insufficient: {
      level: 'INSUFFICIENT', pct: 30,
      explanation: '', weakSpots: [], breakdown: [
        { label: '대규모 트래픽 처리 경험', level: 'NONE', basis: '우대사항에 "대규모 트래픽 처리 경험"이 명시되어 있어요.', fit: '', gap: '포트폴리오와 경험정리 모두에 관련 내용이 없어요.' },
        { label: '협업 커뮤니케이션 능력', level: 'INSUFFICIENT', basis: '인재상에 "협업 커뮤니케이션 역량"이 명시되어 있어요.', fit: '', gap: '자기소개서·경험정리 모두에 협업 경험 서술이 없어요.' }
      ],
      assignments: [
        { title: '장애 대응 경험 정리 과제', desc: '서비스 운영 중 장애 대응 경험을 정리해보세요.', reason: '장애 대응 관련 근거 자료 없음' },
        { title: '협업 커뮤니케이션 사례 작성', desc: '팀 프로젝트에서 의견 차이를 조율했던 경험을 정리해보세요.', reason: '협업 경험 근거 자료 없음' }
      ],
      questions: false
    }
  };

  var state = {
    step: 'mode', mode: null,
    selectedWeaknessTag: null,
    companyName: '', companySearchInput: '',
    desiredJobMajor: 'IT·개발', desiredJobMinor: '백엔드 개발', desiredJobLevel: '신입',
    selectedDocs: {}, // key -> Set of ids
    materialPanelOpen: {},
    companyExtract: JSON.parse(JSON.stringify(COMPANY_EXTRACT)),
    reviewTab: 'company', reviewSub: 'resume',
    connectionScenario: 'full',
    questions: [],
    selectedQIdx: null,
    hintOpen: false,
    draftAnswer: '',
    textEntryKey: null, textEntryDraft: null
  };

  function icon(name) {
    var icons = {
      basic: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6"/></svg>',
      tag: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/><circle cx="7" cy="7" r="1"/></svg>',
      sparkle: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2l1.9 5.8L20 10l-6.1 2.2L12 18l-1.9-5.8L4 10l6.1-2.2z"/></svg>',
      back: '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>'
    };
    return icons[name] || '';
  }

  function render() {
    var root = document.getElementById('step-root');
    if (state.step === 'mode') root.innerHTML = renderModeStep();
    else if (state.step === 'weaknessPick') root.innerHTML = renderWeaknessPick();
    else if (state.step === 'materialSelect') root.innerHTML = renderMaterialSelect();
    else if (state.step === 'materialReview') root.innerHTML = renderMaterialReview();
    else if (state.step === 'connectionResult') root.innerHTML = renderConnectionResult();
    else if (state.step === 'list') root.innerHTML = renderQuestionList();
    bindStepEvents();
  }

  // ---- step: mode select ----
  function renderModeStep() {
    return '<div class="card card--pad-lg">' +
      '<p style="font-size:14px; font-weight:700; margin:0 0 4px;">질문 모드를 선택하세요</p>' +
      '<p style="font-size:12.5px; color:#8A93A3; margin:0 0 20px;">선택한 모드에 맞춰 질문을 생성해드려요 · LUME 백엔드 개발자</p>' +
      '<div class="mode-grid">' + MODES.map(function (m) {
        return '<div class="mode-card" data-mode="' + m.id + '"><div class="mode-card__icon">' + icon(m.icon) + '</div><p class="mode-card__title">' + m.label + '</p><p class="mode-card__desc">' + m.desc + '</p></div>';
      }).join('') + '</div></div>';
  }

  // ---- step: weakness pick ----
  function renderWeaknessPick() {
    return '<div class="card card--pad-lg">' +
      backBtn('backToMode', '모드 다시 선택') +
      '<p style="font-size:14px; font-weight:700; margin:0 0 4px;">보완할 약점 태그를 선택하세요</p>' +
      '<p style="font-size:12.5px; color:#8A93A3; margin:0 0 20px;">태그를 선택하면 그 약점에 맞춘 질문을 생성해요 · 해결된 약점은 표시되지 않아요</p>' +
      '<div style="display:flex; flex-direction:column; gap:10px;">' +
      WEAKNESS_TAGS.filter(function (w) { return !w.resolved; }).map(function (w) {
        return '<div class="weak-pick-row" data-weak-tag="' + esc(w.tag) + '"><div><span style="font-size:13.5px; font-weight:700;">#' + w.tag + '</span><p style="font-size:12.5px; color:#5B6370; margin:6px 0 0;">' + w.desc + '</p></div>' +
          '<span class="badge-pill" style="background:#FDF0E4; color:#B5622E; white-space:nowrap;">미해결</span></div>';
      }).join('') + '</div></div>';
  }

  function backBtn(action, label) {
    return '<div style="margin-bottom:16px;"><button class="btn-sm" data-action="' + action + '">' + icon('back') + ' ' + label + '</button></div>';
  }

  // ---- step: material select ----
  function docListHtml(cat) {
    var selected = state.selectedDocs[cat.key] || {};
    var html = '';
    if (cat.docs.length) {
      html += cat.docs.map(function (doc) {
        var checked = !!selected[doc.id];
        return '<div class="material-doc-row' + (checked ? ' is-checked' : '') + '" data-toggle-doc="' + cat.key + '|' + doc.id + '"><div class="material-checkbox"></div><div><p style="font-size:12.5px; font-weight:600; margin:0;">' + doc.title + '</p><p style="font-size:11px; color:#8A93A3; margin:2px 0 0;">' + doc.date + ' 등록</p></div></div>';
      }).join('');
    } else {
      html += '<p style="font-size:11.5px; color:#8A93A3; margin:0 0 10px;">등록된 자료가 없어요</p>';
    }
    var panelOpen = !!state.materialPanelOpen[cat.key];
    if (panelOpen) {
      html += '<div style="display:flex; gap:6px; flex-wrap:wrap;">' +
        '<button class="material-register-btn material-register-btn--primary" data-reg-text="' + cat.key + '">텍스트로 입력</button>' +
        '<button class="material-register-btn" data-reg-file="' + cat.key + '">이미지 업로드</button>' +
        '<button class="material-register-btn" data-reg-file="' + cat.key + '">PDF 업로드</button>' +
        '<button class="material-register-btn" style="background:transparent; border:none; color:#8A93A3;" data-close-panel="' + cat.key + '">취소</button></div>';
    } else {
      html += '<button class="material-register-btn" data-open-panel="' + cat.key + '">+ 자료 등록하기</button>';
    }
    return html;
  }

  function renderMaterialSelect() {
    var majorOpts = MAJOR_OPTIONS.map(function (m) { return '<option' + (m === state.desiredJobMajor ? ' selected' : '') + '>' + m + '</option>'; }).join('');
    var minorOpts = (MINOR_BY_MAJOR[state.desiredJobMajor] || []).map(function (m) { return '<option' + (m === state.desiredJobMinor ? ' selected' : '') + '>' + m + '</option>'; }).join('');
    var levelOpts = LEVEL_OPTIONS.map(function (l) { return '<option' + (l === state.desiredJobLevel ? ' selected' : '') + '>' + l + '</option>'; }).join('');

    var suggestions = state.companySearchInput
      ? COMPANY_SUGGESTIONS.filter(function (s) { return s.toLowerCase().indexOf(state.companySearchInput.toLowerCase()) !== -1; })
      : [];

    return '<div class="card card--pad-lg">' +
      backBtn('backToMode', '모드 다시 선택') +
      '<p style="font-size:14px; font-weight:700; margin:0 0 4px;">면접에 사용할 자료를 선택하세요</p>' +
      '<p style="font-size:12.5px; color:#8A93A3; margin:0 0 20px;">이미 등록된 자료 중 여러 개를 선택하거나, 없으면 새로 등록해주세요 · 텍스트, 이미지, PDF로 등록할 수 있어요</p>' +
      '<div class="material-columns">' +
        '<div><p style="font-size:12.5px; font-weight:700; color:#5B6370; margin:0 0 12px;">회사 공고 / 회사 정보</p>' +
          '<div class="material-category">' +
            '<p style="font-size:12.5px; font-weight:700; margin:0 0 8px;">회사명 <span style="color:#D64545;">*</span> <span style="font-weight:400; color:#8A93A3;">필수 입력</span></p>' +
            '<input id="company-name-input" class="text-input" placeholder="회사명을 검색하거나 입력하세요" value="' + esc(state.companyName || state.companySearchInput) + '" style="margin-bottom:6px;">' +
            (suggestions.length ? '<div class="flex-row gap-8" style="flex-wrap:wrap;">' + suggestions.map(function (s) { return '<button class="chip" data-pick-company="' + s + '">' + s + '</button>'; }).join('') + '</div>' :
              (state.companySearchInput && !state.companyName ? '<p style="font-size:11px; color:#8A93A3; margin:2px 0 0;">검색 결과가 없어요. 입력한 이름 그대로 등록돼요.</p>' : '')) +
          '</div>' +
          MATERIAL_CATEGORIES_COMPANY.map(function (cat) { return '<div class="material-category"><p style="font-size:12.5px; font-weight:700; margin:0 0 8px;">' + cat.label + '</p>' + docListHtml(cat) + '</div>'; }).join('') +
        '</div>' +
        '<div>' +
          '<div style="background:#F3F8FD; border:1px solid #DCE7F3; border-radius:12px; padding:14px; margin-bottom:14px;">' +
            '<p style="font-size:11px; color:#8A93A3; margin:0 0 8px;">희망 직무</p>' +
            '<div class="flex-row gap-8" style="flex-wrap:wrap;">' +
              '<select id="major-select" class="select-input" style="width:auto; padding:7px 8px; font-size:12px;">' + majorOpts + '</select>' +
              '<select id="minor-select" class="select-input" style="width:auto; padding:7px 8px; font-size:12px;">' + minorOpts + '</select>' +
              '<select id="level-select" class="select-input" style="width:auto; padding:7px 8px; font-size:12px;">' + levelOpts + '</select>' +
            '</div></div>' +
          '<p style="font-size:12.5px; font-weight:700; color:#5B6370; margin:0 0 12px;">이력서 / 자기소개서 / 포트폴리오 / 경험정리</p>' +
          MATERIAL_CATEGORIES_CANDIDATE.map(function (cat) { return '<div class="material-category"><p style="font-size:12.5px; font-weight:700; margin:0 0 8px;">' + cat.label + '</p>' + docListHtml(cat) + '</div>'; }).join('') +
        '</div>' +
      '</div>' +
      '<div class="flex-row" style="justify-content:flex-end; margin-top:22px;"><button class="btn btn--primary" data-action="goMaterialReview">다음: 자료 확인하기</button></div>' +
    '</div>';
  }

  // ---- step: material review ----
  function reviewFieldRow(label, key, value, height) {
    return '<div style="margin-bottom:16px;"><p style="font-size:12.5px; font-weight:700; margin:0 0 8px;">' + label + '</p>' +
      '<textarea class="textarea-input" style="height:' + (height || 76) + 'px;" data-company-field="' + key + '">' + esc(value) + '</textarea></div>';
  }

  function renderMaterialReview() {
    var ce = state.companyExtract;
    var companyTabHtml = state.companyName
      ? '<p style="font-size:12.5px; font-weight:700; color:#5B6370; margin:0 0 10px;">채용공고</p>' +
        '<div style="background:#F6F8FB; border-radius:12px; padding:16px 18px; margin-bottom:16px;"><p style="font-size:14.5px; font-weight:700; margin:0 0 2px;">' + esc(state.companyName) + '</p><p style="font-size:12.5px; color:#8A93A3; margin:0;">' + esc(ce.jobTitle) + '</p></div>' +
        reviewFieldRow('주요업무', 'mainDuties', ce.mainDuties, 96) +
        reviewFieldRow('자격요건', 'requirements', ce.requirements, 96) +
        reviewFieldRow('우대사항', 'preferred', ce.preferred, 96) +
        reviewFieldRow('전형정보', 'processInfo', ce.processInfo, 56) +
        '<div style="margin-bottom:20px;"><p style="font-size:12.5px; font-weight:700; margin:0 0 8px;">공고 URL</p><input class="text-input" value="' + esc(ce.url) + '" data-company-field="url"></div>' +
        '<p style="font-size:12.5px; font-weight:700; color:#5B6370; margin:0 0 10px;">회사 정보</p>' +
        '<div class="grid-2" style="margin-bottom:16px;"><div><p style="font-size:12.5px; font-weight:700; margin:0 0 8px;">회사명</p><input class="text-input" value="' + esc(ce.company.name) + '" data-company-nested="name"></div><div><p style="font-size:12.5px; font-weight:700; margin:0 0 8px;">산업군</p><input class="text-input" value="' + esc(ce.company.industry) + '" data-company-nested="industry"></div></div>' +
        reviewFieldRowNested('서비스/제품 설명', 'serviceDesc', ce.company.serviceDesc, 72) +
        reviewFieldRowNested('회사 가치관', 'values', ce.company.values, 56) +
        reviewFieldRowNested('인재상', 'talentProfile', ce.company.talentProfile, 56) +
        '<div><p style="font-size:12.5px; font-weight:700; margin:0 0 8px;">참고 URL</p><input class="text-input" value="' + esc(ce.company.refUrl) + '" data-company-nested="refUrl"></div>'
      : '<p style="font-size:12.5px; color:#8A93A3; margin:0;">회사명이 입력되지 않았어요. 이전 단계에서 회사명을 입력해주세요.</p>';

    var subTabs = ['resume', 'coverLetter', 'portfolio', 'experience'];
    var subLabels = { resume: '이력서', coverLetter: '자기소개서', portfolio: '포트폴리오', experience: '경험정리' };
    var candidateTabHtml = '<div style="display:flex; align-items:center; gap:8px; background:#F3F8FD; border:1px solid #DCE7F3; border-radius:10px; padding:10px 14px; margin-bottom:16px;"><span style="font-size:12px; color:#8A93A3;">희망 직무</span><span style="font-size:12.5px; font-weight:700;">' + state.desiredJobMajor + ' &gt; ' + state.desiredJobMinor + ' · ' + state.desiredJobLevel + '</span></div>';
    candidateTabHtml += '<div class="review-sub-tabbar">' + subTabs.map(function (s) { return '<button class="' + (state.reviewSub === s ? 'is-active' : '') + '" data-review-sub="' + s + '">' + subLabels[s] + '</button>'; }).join('') + '</div>';

    if (state.reviewSub === 'resume') {
      var r = RESUME_CARD;
      candidateTabHtml += '<div class="review-card">' + miniField('이름', r.name) + miniField('희망직무', r.desiredJob) + miniField('학력', r.education) + miniField('기술', r.skills) + miniField('프로젝트/경험', r.experience) + miniField('자격/수상', r.certs) + '</div>';
    } else if (state.reviewSub === 'coverLetter') {
      var c = COVER_CARD;
      candidateTabHtml += '<div class="review-card">' + miniField('지원직무', c.desiredJob) + miniField('지원동기', c.motivation) + miniField('핵심경험', c.coreExperience) + miniField('협업/갈등해결 경험', c.collabExperience) + miniField('직무 연결성', c.jobConnection) + miniField('입사 후 목표', c.futureGoal) + '</div>';
    } else if (state.reviewSub === 'portfolio') {
      candidateTabHtml += PORTFOLIO_CARDS.map(function (p) {
        return '<div class="review-card"><p style="font-size:13px; font-weight:700; margin:0 0 4px;">' + p.projectName + '</p><p style="font-size:11.5px; color:#8A93A3; margin:0 0 10px;">' + p.period + '</p>' +
          miniField('내 역할', p.role) + miniField('사용 기술', p.tech) + miniField('핵심 내용', p.coreContent) + miniField('문제 해결 과정', p.problemSolving) + miniField('성과/지표', p.outcome) + '</div>';
      }).join('');
    } else if (state.reviewSub === 'experience') {
      var e = EXPERIENCE_CARD;
      candidateTabHtml += '<div class="review-card">' + miniField('상황', e.situation) + miniField('과제/목표', e.taskGoal) + miniField('행동', e.action) + miniField('결과', e.result) + miniField('사용 기술', e.tech) + miniField('배운 점', e.learned) + miniField('면접에서 강조할 포인트', e.interviewPoint) + '</div>';
    }

    return '<div class="card card--pad-lg">' +
      backBtn('backToMaterialSelect', '자료 다시 선택') +
      '<p style="font-size:14px; font-weight:700; margin:0 0 4px;">추출된 자료를 확인하고 필요하면 수정하세요</p>' +
      '<p style="font-size:12.5px; color:#8A93A3; margin:0 0 18px;">확정을 누르면 이 내용을 기준으로 자료 충분도와 연결 분석을 진행해요</p>' +
      '<div class="review-sub-tabbar"><button class="' + (state.reviewTab === 'company' ? 'is-active' : '') + '" data-review-tab="company">회사 분석</button><button class="' + (state.reviewTab === 'candidate' ? 'is-active' : '') + '" data-review-tab="candidate">지원자 분석</button></div>' +
      (state.reviewTab === 'company' ? companyTabHtml : candidateTabHtml) +
      '<div class="flex-row" style="justify-content:flex-end; margin-top:22px;"><button class="btn btn--primary" data-action="confirmMaterials">확정하고 분석 시작</button></div>' +
    '</div>';
  }
  function reviewFieldRowNested(label, key, value, height) {
    return '<div style="margin-bottom:16px;"><p style="font-size:12.5px; font-weight:700; margin:0 0 8px;">' + label + '</p><textarea class="textarea-input" style="height:' + height + 'px;" data-company-nested="' + key + '">' + esc(value) + '</textarea></div>';
  }
  function miniField(label, value) {
    return '<div style="margin-bottom:10px;"><p class="review-mini-label">' + label + '</p><textarea class="textarea-input" style="height:44px; font-size:12.5px;">' + esc(value) + '</textarea></div>';
  }

  // ---- step: connection result ----
  var LEVEL_COLOR = { HIGH: '#1E7A4C', MEDIUM: '#185FA5', LOW: '#B5622E', INSUFFICIENT: '#B5433D', NONE: '#8A93A3' };

  function renderConnectionResult() {
    var sc = CONNECTION_SCENARIOS[state.connectionScenario];
    var deg = Math.round(sc.pct / 100 * 360);
    var html = '<div class="card card--pad-lg">' +
      '<p style="font-size:14px; font-weight:700; margin:0 0 4px;">공고 요구사항 · 자료 연결 분석 결과</p>' +
      '<p style="font-size:12.5px; color:#8A93A3; margin:0 0 22px;">자료가 요구사항을 완벽히 충족하면 HIGH, 조금 부족하면 MEDIUM, 많이 부족하면 LOW·INSUFFICIENT·NONE 순으로 낮아져요.</p>' +
      '<div class="flex-row gap-12" style="flex-wrap:wrap; margin-bottom:20px;">' +
        '<div class="donut-lg" style="width:150px; height:150px; background:conic-gradient(' + LEVEL_COLOR[sc.level] + ' ' + deg + 'deg, #E3E7ED 0);"><div class="donut-lg__inner" style="width:104px; height:104px;"><p style="font-size:22px; font-weight:700; margin:0;">' + sc.pct + '%</p><p style="font-size:12px; font-weight:700; margin:2px 0 0; color:' + LEVEL_COLOR[sc.level] + ';">' + sc.level + '</p></div></div>' +
        '<div style="flex:1; min-width:240px;"><p style="font-size:13.5px; font-weight:700; margin:0 0 8px;">' + (sc.explanation || (sc.level === 'INSUFFICIENT' ? '자료가 요구사항을 충족하기엔 많이 부족해요. 과제를 먼저 완료해주세요.' : '')) + '</p></div>' +
      '</div>';

    if (sc.weakSpots.length) {
      html += '<div class="weakspot-box"><p style="font-size:12.5px; font-weight:700; color:#8A5A22; margin:0 0 10px;">어느 부분이 부족한가요</p><div style="display:flex; flex-direction:column; gap:8px;">' +
        sc.weakSpots.map(function (w) { return '<p style="font-size:12px; color:#5B6370; margin:0; line-height:1.6;">• ' + w + '</p>'; }).join('') + '</div></div>';
    }

    html += '<div style="margin-bottom:20px;"><p style="font-size:13px; font-weight:700; margin:0 0 10px;">요구사항별 자료 적합도</p><div style="display:flex; flex-direction:column; gap:8px;">' +
      sc.breakdown.map(function (r) {
        return '<div class="req-row"><div class="flex-row" style="justify-content:space-between; gap:8px; flex-wrap:wrap; margin-bottom:6px;"><p style="font-size:12.5px; font-weight:700; margin:0;">' + r.label + '</p><span style="font-size:11px; font-weight:700; color:' + LEVEL_COLOR[r.level] + ';">' + r.level + '</span></div>' +
          '<p style="font-size:11.5px; color:#8A93A3; margin:0 0 6px; line-height:1.6;">이 요구사항이 나온 근거: ' + r.basis + '</p>' +
          (r.fit ? '<p style="font-size:11.5px; color:#1E7A4C; margin:0 0 3px; line-height:1.6;">✓ ' + r.fit + '</p>' : '') +
          (r.gap ? '<p style="font-size:11.5px; color:#B5433D; margin:0; line-height:1.6;">✕ ' + r.gap + '</p>' : '') +
        '</div>';
      }).join('') + '</div></div>';

    if (sc.assignments.length) {
      html += '<div style="margin-bottom:20px;"><p style="font-size:13px; font-weight:700; margin:0 0 10px;">생성된 과제</p><div style="display:flex; flex-direction:column; gap:8px;">' +
        sc.assignments.map(function (a) { return '<div class="req-row"><p style="font-size:12.5px; font-weight:700; margin:0 0 4px;">' + a.title + '</p><p style="font-size:12px; color:#5B6370; margin:0 0 6px; line-height:1.6;">' + a.desc + '</p><p style="font-size:11px; color:#8A93A3; margin:0;">근거: ' + a.reason + '</p></div>'; }).join('') + '</div></div>';
    }

    if (sc.questions) {
      html += '<div class="flex-row" style="justify-content:space-between; gap:12px; background:#F3F8FD; border:1px solid #DCE7F3; border-radius:10px; padding:14px 16px; flex-wrap:wrap;">' +
        '<span style="font-size:12.5px; color:#185FA5; font-weight:600;">' + (sc.assignments.length ? '부족한 항목은 과제로, 나머지는 질문으로 만들었어요.' : '모든 요구사항이 충분히 확인되어 질문으로 바로 진행해요.') + '</span>' +
        '<button class="btn btn--primary" data-action="proceedToQuestions">질문 생성하기</button></div>';
    } else {
      html += '<div style="text-align:center; padding:16px 0 4px;">' +
        '<p style="font-size:13px; color:#5B6370; margin:0 0 18px;">자료가 너무 부족해 질문을 만들 수 없어요. 과제를 먼저 완료하거나 기본 질문 모드로 진행해주세요.</p>' +
        '<div class="flex-row gap-10" style="justify-content:center;"><button class="btn" style="background:#fff; border:1px solid #E3E7ED;" data-action="goBasicMode">기본 질문 모드로 전환</button><a href="/dashboard.html" class="btn btn--primary" style="text-decoration:none;">과제 목록 확인하기</a></div></div>';
    }

    html += '</div>';
    return html;
  }

  // ---- step: question list + chat ----
  var MODE_HINTS = {
    basic: '질문의 의도를 파악하고 상황-행동-결과 순서로 답해보세요.',
    weakness: '이전에 부족했던 부분을 의식하며 구체적인 수치와 본인의 역할을 강조해보세요.',
    custom: '공고 요구사항과 본인 경험을 연결지어 답해보세요.'
  };

  function ensureQuestions() {
    if (state.questions.length) return;
    var base = [
      '자기소개와 함께 이 직무에 지원하게 된 계기를 말씀해주세요.',
      '대규모 트래픽을 처리하기 위해 설계한 경험을 설명해주세요.',
      'RESTful API 설계 시 어떤 원칙을 가장 중요하게 생각하나요?',
      '가장 어려웠던 기술적 문제와 해결 과정을 설명해주세요.',
      '협업 중 의견 충돌이 있었던 경험과 해결 방법을 말씀해주세요.',
      '서비스 운영·장애 대응 경험을 구체적으로 설명해주세요.',
      '본인의 강점과 약점을 각각 말씀해주세요.',
      '팀 프로젝트에서 갈등을 해결한 경험이 있나요?',
      '데이터 시각화 도구를 사용해본 경험이 있나요?',
      '5년 후 본인의 커리어 목표는 무엇인가요?'
    ];
    state.questions = base.map(function (text, idx) {
      return { n: idx + 1, text: text, answered: false, followUp: '방금 답변에서 본인이 직접 판단하거나 결정한 부분은 구체적으로 무엇이었나요?', messages: [{ from: 'bot', text: text }] };
    });
  }

  function renderQuestionList() {
    ensureQuestions();
    var answeredCount = state.questions.filter(function (q) { return q.answered; }).length;
    var total = state.questions.length;
    var finishDisabled = answeredCount < total;
    var modeLabelMap = { basic: '기본 질문 모드', weakness: '약점 보완 · #' + (state.selectedWeaknessTag || ''), custom: '맞춤 면접 질문' };
    var backLabel = state.mode === 'custom' ? '연결 분석 결과로' : '모드 다시 선택';

    var html = '<div class="card card--pad-lg">' +
      '<div class="flex-row" style="justify-content:space-between; margin-bottom:16px; flex-wrap:wrap; gap:12px;">' +
        '<div class="flex-row gap-10">' +
          '<button class="btn-sm" data-action="backFromList">' + icon('back') + ' ' + backLabel + '</button>' +
          '<span class="badge-pill" style="color:#185FA5; background:#EAF2FB;">' + modeLabelMap[state.mode] + '</span>' +
        '</div>' +
        '<div class="flex-row gap-12">' +
          '<span style="font-size:12.5px; color:#5B6370; font-weight:600; white-space:nowrap;">' + answeredCount + '/' + total + ' 답변 완료</span>' +
          '<button class="btn" style="background:' + (finishDisabled ? '#B9C3D6' : '#185FA5') + '; color:#fff;" ' + (finishDisabled ? 'disabled' : '') + ' data-action="finishAll">최종 결과 확인하기</button>' +
        '</div>' +
      '</div>' +
      '<div class="q-gen-layout">' +
        '<div class="q-gen-list">' + state.questions.map(function (q, idx) {
          var active = state.selectedQIdx === idx;
          return '<div class="q-gen-item' + (active ? ' is-active' : '') + '" data-select-q="' + idx + '"><span class="q-gen-item__num">' + q.n + '</span><span class="q-gen-item__text">' + q.text + '</span>' +
            (q.answered ? '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#1E8E5A" stroke-width="3"><path d="M20 6L9 17l-5-5"/></svg>' : '') + '</div>';
        }).join('') + '</div>' +
        '<div class="chat-panel" id="chat-panel"></div>' +
      '</div>' +
    '</div>';
    return html;
  }

  function renderChatPanel() {
    var panel = document.getElementById('chat-panel');
    if (!panel) return;
    if (state.selectedQIdx === null) {
      panel.innerHTML = '<div style="display:flex; flex-direction:column; align-items:center; justify-content:center; height:100%; text-align:center; color:#8A93A3;">' +
        '<p style="font-size:14px; font-weight:600; color:#5B6370; margin:0 0 4px;">질문을 선택해주세요</p><p style="font-size:12.5px; margin:0;">왼쪽 목록에서 질문을 클릭하면 답변을 시작할 수 있어요</p></div>';
      return;
    }
    var q = state.questions[state.selectedQIdx];
    var html = '<div class="flex-row gap-10" style="margin-bottom:14px;"><button class="btn-sm" id="toggle-hint-btn">💡 힌트 보기</button></div>';
    if (state.hintOpen) html += '<div class="hint-box">' + (MODE_HINTS[state.mode] || '') + '</div>';
    html += '<div class="chat-thread" id="chat-thread">' + q.messages.map(function (m) {
      return '<div class="chat-bubble-row chat-bubble-row--' + (m.from === 'user' ? 'user' : 'bot') + '"><div class="chat-bubble chat-bubble--' + (m.from === 'user' ? 'user' : 'bot') + '">' + esc(m.text) + '</div></div>';
    }).join('') + '</div>';
    if (q.answered) {
      html += '<div class="chat-done-banner">✓ 이 질문에 답변을 완료했어요</div>';
    } else {
      html += '<div class="chat-input-area"><textarea id="draft-answer" placeholder="답변을 입력하세요">' + esc(state.draftAnswer) + '</textarea><div class="flex-row" style="justify-content:flex-end;"><button class="btn btn--primary" id="send-answer-btn">전송</button></div></div>';
    }
    panel.innerHTML = html;
    var hintBtn = document.getElementById('toggle-hint-btn');
    if (hintBtn) hintBtn.addEventListener('click', function () { state.hintOpen = !state.hintOpen; renderChatPanel(); });
    var draft = document.getElementById('draft-answer');
    if (draft) draft.addEventListener('input', function () { state.draftAnswer = draft.value; });
    var sendBtn = document.getElementById('send-answer-btn');
    if (sendBtn) sendBtn.addEventListener('click', sendAnswer);
  }

  function sendAnswer() {
    var q = state.questions[state.selectedQIdx];
    if (!state.draftAnswer.trim()) return;
    if (q.messages.length === 1) {
      q.messages.push({ from: 'user', text: state.draftAnswer });
      q.messages.push({ from: 'bot', text: q.followUp });
      state.draftAnswer = '';
      renderChatPanel();
    } else {
      q.messages.push({ from: 'user', text: state.draftAnswer });
      q.answered = true;
      state.draftAnswer = '';
      renderQuestionListInPlace();
      renderChatPanel();
    }
  }

  function renderQuestionListInPlace() {
    var root = document.getElementById('step-root');
    root.innerHTML = renderQuestionList();
    bindStepEvents();
    renderChatPanel();
  }

  // ---- events ----
  function bindStepEvents() {
    document.querySelectorAll('[data-mode]').forEach(function (el) {
      el.addEventListener('click', function () {
        state.mode = el.dataset.mode;
        if (state.mode === 'basic') { state.step = 'list'; state.questions = []; state.selectedQIdx = null; }
        else if (state.mode === 'weakness') state.step = 'weaknessPick';
        else if (state.mode === 'custom') state.step = 'materialSelect';
        render();
        if (state.step === 'list') renderChatPanel();
      });
    });

    document.querySelectorAll('[data-weak-tag]').forEach(function (el) {
      el.addEventListener('click', function () {
        state.selectedWeaknessTag = el.dataset.weakTag;
        state.step = 'list'; state.questions = []; state.selectedQIdx = null;
        render();
        renderChatPanel();
      });
    });

    bindAction('backToMode', function () { state.step = 'mode'; render(); });
    bindAction('backToMaterialSelect', function () { state.step = 'materialSelect'; render(); });
    bindAction('goMaterialReview', function () {
      if (!state.companyName && state.companySearchInput) state.companyName = state.companySearchInput;
      state.step = 'materialReview'; render();
    });
    bindAction('confirmMaterials', function () {
      // decide scenario from doc selection breadth (dummy heuristic)
      var totalSelected = 0;
      Object.keys(state.selectedDocs).forEach(function (k) { totalSelected += Object.keys(state.selectedDocs[k] || {}).length; });
      if (!state.companyName) { state.connectionScenario = 'insufficient'; }
      else if (totalSelected >= 3) state.connectionScenario = 'full';
      else if (totalSelected >= 1) state.connectionScenario = 'partial';
      else state.connectionScenario = 'insufficient';
      state.step = 'connectionResult';
      render();
    });
    bindAction('proceedToQuestions', function () { state.step = 'list'; state.questions = []; state.selectedQIdx = null; render(); renderChatPanel(); });
    bindAction('goBasicMode', function () { state.mode = 'basic'; state.step = 'list'; state.questions = []; state.selectedQIdx = null; render(); renderChatPanel(); });
    bindAction('backFromList', function () {
      if (state.mode === 'custom') { state.step = 'connectionResult'; }
      else { state.step = 'mode'; }
      render();
    });
    bindAction('finishAll', function () {
      try { localStorage.setItem('jobpuzzle_latest_session', JSON.stringify({ title: 'LUME 백엔드 개발자 모의면접', date: new Date().toISOString().slice(0, 10).replace(/-/g, '.'), score: 78 })); } catch (e) {}
      window.location.href = '/interview-result.html';
    });

    document.querySelectorAll('[data-select-q]').forEach(function (el) {
      el.addEventListener('click', function () { state.selectedQIdx = parseInt(el.dataset.selectQ, 10); state.hintOpen = false; renderQuestionListInPlace(); });
    });

    // material select interactions
    var companyInput = document.getElementById('company-name-input');
    if (companyInput) {
      companyInput.addEventListener('input', function () { state.companySearchInput = companyInput.value; state.companyName = ''; render(); });
    }
    document.querySelectorAll('[data-pick-company]').forEach(function (el) {
      el.addEventListener('click', function () { state.companyName = el.dataset.pickCompany; state.companySearchInput = el.dataset.pickCompany; render(); });
    });
    var majorSel = document.getElementById('major-select');
    if (majorSel) majorSel.addEventListener('change', function () { state.desiredJobMajor = majorSel.value; state.desiredJobMinor = (MINOR_BY_MAJOR[majorSel.value] || [])[0] || ''; render(); });
    var minorSel = document.getElementById('minor-select');
    if (minorSel) minorSel.addEventListener('change', function () { state.desiredJobMinor = minorSel.value; });
    var levelSel = document.getElementById('level-select');
    if (levelSel) levelSel.addEventListener('change', function () { state.desiredJobLevel = levelSel.value; });

    document.querySelectorAll('[data-toggle-doc]').forEach(function (el) {
      el.addEventListener('click', function () {
        var parts = el.dataset.toggleDoc.split('|');
        var key = parts[0], id = parts[1];
        state.selectedDocs[key] = state.selectedDocs[key] || {};
        if (state.selectedDocs[key][id]) delete state.selectedDocs[key][id]; else state.selectedDocs[key][id] = true;
        render();
      });
    });
    document.querySelectorAll('[data-open-panel]').forEach(function (el) { el.addEventListener('click', function () { state.materialPanelOpen[el.dataset.openPanel] = true; render(); }); });
    document.querySelectorAll('[data-close-panel]').forEach(function (el) { el.addEventListener('click', function () { state.materialPanelOpen[el.dataset.closePanel] = false; render(); }); });
    document.querySelectorAll('[data-reg-file]').forEach(function (el) { el.addEventListener('click', function () { alert('데모: 파일 업로드는 실제 서비스에서 지원돼요.'); }); });
    document.querySelectorAll('[data-reg-text]').forEach(function (el) { el.addEventListener('click', function () { openTextEntry(el.dataset.regText); }); });

    // material review interactions
    document.querySelectorAll('[data-review-tab]').forEach(function (el) { el.addEventListener('click', function () { state.reviewTab = el.dataset.reviewTab; render(); }); });
    document.querySelectorAll('[data-review-sub]').forEach(function (el) { el.addEventListener('click', function () { state.reviewSub = el.dataset.reviewSub; render(); }); });
    document.querySelectorAll('[data-company-field]').forEach(function (el) { el.addEventListener('input', function () { state.companyExtract[el.dataset.companyField] = el.value; }); });
    document.querySelectorAll('[data-company-nested]').forEach(function (el) { el.addEventListener('input', function () { state.companyExtract.company[el.dataset.companyNested] = el.value; }); });

    if (document.getElementById('chat-panel')) renderChatPanel();
  }

  function bindAction(name, fn) {
    document.querySelectorAll('[data-action="' + name + '"]').forEach(function (el) { el.addEventListener('click', fn); });
  }

  // ---- text entry modal (register material as text, mirrors mydata style) ----
  var MATERIAL_LABELS = { resume: '이력서', coverLetter: '자기소개서', portfolio: '포트폴리오', experience: '경험정리', jobPosting: '채용공고', companyInfo: '회사 정보' };
  function textEntryDefaults(key) {
    return {
      resume: { name: '', desiredJob: '', education: '', skills: '', experience: '', certs: '' },
      coverLetter: { desiredJob: '', motivation: '', coreExperience: '', collabExperience: '', jobConnection: '', futureGoal: '' },
      portfolio: { projectName: '', period: '', role: '', tech: '', coreContent: '', problemSolving: '', outcome: '' },
      experience: { situation: '', taskGoal: '', action: '', result: '', tech: '', learned: '', interviewPoint: '' },
      jobPosting: { title: '', mainDuties: '', requirements: '', preferred: '', processInfo: '', url: '' },
      companyInfo: { name: '', industry: '', serviceDesc: '', values: '', talentProfile: '', refUrl: '' }
    }[key];
  }
  function openTextEntry(key) {
    state.textEntryKey = key;
    state.textEntryDraft = textEntryDefaults(key);
    document.getElementById('text-entry-title').textContent = MATERIAL_LABELS[key] + ' 직접 입력';
    renderTextEntryFields();
    document.getElementById('text-entry-modal').hidden = false;
  }
  function renderTextEntryFields() {
    var key = state.textEntryKey;
    var d = state.textEntryDraft;
    var html = '';
    var fieldDefs = {
      resume: [['name', '이름', false], ['desiredJob', '희망직무', false], ['education', '학력', true], ['skills', '기술 스택', true], ['experience', '프로젝트/경력', true], ['certs', '자격/수상', true]],
      coverLetter: [['desiredJob', '지원직무', false], ['motivation', '지원동기', true], ['coreExperience', '핵심경험', true], ['collabExperience', '협업/갈등해결 경험', true], ['jobConnection', '직무 연결성', true], ['futureGoal', '입사 후 목표', true]],
      portfolio: [['projectName', '프로젝트명', false], ['period', '기간', false], ['role', '내 역할', true], ['tech', '사용 기술', true], ['coreContent', '핵심 내용', true], ['problemSolving', '문제 해결 과정', true], ['outcome', '성과/지표', true]],
      experience: [['situation', '상황', true], ['taskGoal', '과제/목표', true], ['action', '행동', true], ['result', '결과', true], ['tech', '사용 기술', false], ['learned', '배운 점', false], ['interviewPoint', '면접에서 강조할 포인트', false]],
      jobPosting: [['title', '공고명', false], ['mainDuties', '주요업무', true], ['requirements', '자격요건', true], ['preferred', '우대사항', true], ['processInfo', '전형정보', false], ['url', '공고 URL', false]],
      companyInfo: [['name', '회사명', false], ['industry', '산업군', false], ['serviceDesc', '서비스/제품 설명', true], ['values', '회사 가치관', true], ['talentProfile', '인재상', true], ['refUrl', '참고 URL', false]]
    };
    fieldDefs[key].forEach(function (f) {
      var fieldKey = f[0], label = f[1], isTextarea = f[2];
      html += '<div style="margin-bottom:10px;">' +
        (isTextarea ? '<textarea class="textarea-input" style="height:50px;" placeholder="' + label + '" data-te-field="' + fieldKey + '"></textarea>' : '<input class="text-input" placeholder="' + label + '" data-te-field="' + fieldKey + '">') +
      '</div>';
    });
    document.getElementById('text-entry-fields').innerHTML = html;
    document.querySelectorAll('[data-te-field]').forEach(function (el) {
      el.addEventListener('input', function () { state.textEntryDraft[el.dataset.teField] = el.value; });
    });
  }
  function bindTextEntryModal() {
    document.getElementById('text-entry-close').addEventListener('click', function () { document.getElementById('text-entry-modal').hidden = true; });
    document.getElementById('text-entry-cancel').addEventListener('click', function () { document.getElementById('text-entry-modal').hidden = true; });
    document.getElementById('text-entry-submit').addEventListener('click', function () {
      var key = state.textEntryKey;
      var id = 'new' + Date.now();
      var catList = MATERIAL_CATEGORIES_COMPANY.concat(MATERIAL_CATEGORIES_CANDIDATE);
      var cat = catList.filter(function (c) { return c.key === key; })[0];
      if (cat) {
        var title = state.textEntryDraft.name || state.textEntryDraft.title || state.textEntryDraft.projectName || ('직접 입력한 ' + MATERIAL_LABELS[key]);
        cat.docs.push({ id: id, title: title, date: '2026.07.13' });
        state.selectedDocs[key] = state.selectedDocs[key] || {};
        state.selectedDocs[key][id] = true;
      }
      document.getElementById('text-entry-modal').hidden = true;
      state.materialPanelOpen[key] = false;
      render();
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    bindTextEntryModal();
    render();
  });
})();
