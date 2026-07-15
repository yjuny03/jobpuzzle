// mydata.js — 지원 자료 page logic (dummy data, full CRUD-ish interactivity)

(function () {
  'use strict';

  var CATEGORIES = [
    { id: 'resume', label: '이력서' },
    { id: 'cover', label: '자기소개서' },
    { id: 'portfolio', label: '포트폴리오' },
    { id: 'exp', label: '경험 자료' }
  ];
  var CATEGORY_LABEL = { resume: '이력서', cover: '자기소개서', portfolio: '포트폴리오', exp: '경험 자료' };

  function initialDocs() {
    return [
      {
        id: 'resume1', category: 'resume', name: '홍길동_이력서', status: 'SUCCESS', currentVersion: '2.0', lastModified: '2026.07.07',
        versions: [
          { version: '1.0', date: '2026.06.01', note: '최초 등록' },
          { version: '1.1', date: '2026.06.15', note: '경력 기간 오타 수정' },
          { version: '2.0', date: '2026.07.07', note: '스마트몰 프로젝트 경험 추가' }
        ],
        fields: {
          basic: { name: '홍길동', desiredJob: '백엔드 개발자', contact: '010-1234-5678' },
          education: [{ school: 'OO대학교', major: '컴퓨터공학과', period: '2018.03 ~ 2022.02', degree: '학사' }],
          skills: 'Java, Kotlin, Spring Boot, JPA, MySQL, Redis, Docker, AWS',
          experience: [
            { period: '2024.01 ~ 2024.06', role: '백엔드 개발 (본인 담당)', tech: 'Spring Boot, MySQL, Redis', achievement: '주문 API 응답속도 40% 개선, 동시접속 처리량 3배 확대' },
            { period: '2023.09 ~ 2023.12', role: '인턴 개발자', tech: 'Node.js, MongoDB', achievement: '사내 관리자 대시보드 구축, 업무 처리 시간 20% 단축' }
          ],
          certs: '정보처리기사, AWS Solutions Architect Associate, 교내 해커톤 우수상'
        }
      },
      {
        id: 'cover1', category: 'cover', name: '홍길동_자기소개서', status: 'SUCCESS', currentVersion: '1.0', lastModified: '2026.07.07',
        versions: [{ version: '1.0', date: '2026.07.07', note: '최초 등록' }],
        fields: {
          companyName: 'LUME', desiredJob: '백엔드 개발자',
          motivation: 'LUME의 대용량 트래픽 처리 기술력과 커머스 서비스 비전에 공감해 지원했습니다.',
          coreExperience: '온라인 쇼핑몰 백엔드 개발 프로젝트에서 주문·결제 API를 설계하고, 캐싱 전략을 도입해 응답속도를 40% 개선했습니다.',
          collabExperience: '프론트엔드 팀과 API 스펙에 대한 의견 차이가 있었을 때, 회의를 통해 우선순위를 조율했습니다.',
          jobConnection: '대규모 트래픽 처리와 REST API 설계 경험이 LUME 백엔드 개발자 직무 역량과 연결된다고 생각합니다.',
          futureGoal: '입사 후 서비스 장애 대응 프로세스를 함께 고도화하고 싶습니다.'
        }
      },
      {
        id: 'portfolio1', category: 'portfolio', name: '백엔드_프로젝트_포트폴리오', status: 'SUCCESS', currentVersion: '1.2', lastModified: '2026.07.05',
        versions: [
          { version: '1.0', date: '2026.06.02', note: '최초 등록' },
          { version: '1.1', date: '2026.06.20', note: '산출물 문구 수정' },
          { version: '1.2', date: '2026.07.05', note: '산출물 수치 보완' }
        ],
        fields: {
          title: '스마트몰 백엔드 & 사내 도구 포트폴리오',
          projects: [
            { name: '스마트몰 주문/결제 API 설계', period: '2024.01 ~ 2024.06', role: '백엔드 설계 및 구현 (본인 담당)', tech: 'Spring Boot, Redis, MySQL, AWS', coreContent: '주문·결제·재고 도메인의 REST API를 설계했습니다.', problemSolving: '피크 시간대 응답 지연 문제를 프로파일링으로 파악하고 캐싱으로 해결했습니다.', outcome: 'API 응답속도 40% 개선, 동시접속 처리량 3배 확대', link: '' },
            { name: '사내 관리자 대시보드', period: '2023.09 ~ 2023.12', role: '풀스택 개발', tech: 'Node.js, MongoDB, React', coreContent: '수기 정산·재고 관리 업무를 대시보드로 전환했습니다.', problemSolving: '현업 인터뷰로 우선순위 기능을 선정하고 반복 개선했습니다.', outcome: '처리 시간 20% 단축', link: '' }
          ]
        }
      },
      {
        id: 'exp1', category: 'exp', name: '경험_정리_STAR노트', status: 'SUCCESS', currentVersion: '1.0', lastModified: '2026.06.28',
        versions: [{ version: '1.0', date: '2026.06.28', note: '최초 등록' }],
        fields: {
          entries: [
            { situation: '프론트엔드 팀과 응답 데이터 구조에 대한 의견이 갈렸습니다.', taskGoal: '일정 지연 없이 양 팀이 동의할 수 있는 스펙을 확정해야 했습니다.', action: '실사용 시나리오를 정리해 회의를 열고 우선순위 기준을 세워 조율했습니다.', result: '하루 만에 합의에 도달했고 문서화 프로세스를 제안해 반영했습니다.', tech: 'Notion, Figma', learned: '갈등은 사실과 시나리오로 풀어야 빠르게 합의된다는 것을 배웠습니다.', interviewPoint: '데이터 기반 조율과 재발 방지 제안을 강조하고 싶습니다.' }
          ]
        }
      },
      {
        id: 'portfolio2', category: 'portfolio', name: '알고리즘_스터디_정리', status: 'FAILED', currentVersion: '-', lastModified: '2026.06.20',
        versions: [{ version: '-', date: '2026.06.20', note: '최초 등록' }],
        fields: null
      }
    ];
  }

  function defaultFieldsFor(category) {
    return {
      resume: { basic: { name: '', desiredJob: '', contact: '' }, education: [{ school: '', major: '', period: '', degree: '' }], skills: '', experience: [{ period: '', role: '', tech: '', achievement: '' }], certs: '' },
      cover: { companyName: '', desiredJob: '', motivation: '', coreExperience: '', collabExperience: '', jobConnection: '', futureGoal: '' },
      portfolio: { title: '', projects: [{ name: '', period: '', role: '', tech: '', coreContent: '', problemSolving: '', outcome: '', link: '' }] },
      exp: { entries: [{ situation: '', taskGoal: '', action: '', result: '', tech: '', learned: '', interviewPoint: '' }] }
    }[category];
  }

  function bumpVersion(current, scale) {
    var parts = (current || '1.0').split('.');
    var maj = parseInt(parts[0], 10) || 1;
    var min = parseInt(parts[1], 10) || 0;
    return scale === 'major' ? (maj + 1) + '.0' : maj + '.' + (min + 1);
  }
  function todayLabel() {
    var d = new Date();
    return d.getFullYear() + '.' + String(d.getMonth() + 1).padStart(2, '0') + '.' + String(d.getDate()).padStart(2, '0');
  }
  var docs = initialDocs();
  var state = { tab: 'docs', categoryFilter: 'all', selectedId: 'resume1', viewVersion: null, editMode: false, registerCategory: null, registerMethod: 'file', registerFileName: '', registerFields: null, reuploadDocId: null, reuploadFileName: '', scaleDocId: null, scaleScale: 'minor', scaleAction: null, deleteDocId: null };

  // ---- tabs & chips ----
  function renderTabs() {
    bindTabGroup({
      btnSelector: '.tabbar__btn',
      datasetKey: 'tab',
      panelSelector: '.tab-panel',
      panelDatasetKey: 'panel',
      onSelect: function (value) { state.tab = value; renderAll(); }
    });
  }

  function renderChips() {
    var chips = [{ id: 'all', label: '전체' }].concat(CATEGORIES);
    document.getElementById('category-chips').innerHTML = chips.map(function (c) {
      return '<button class="chip' + (state.categoryFilter === c.id ? ' is-active' : '') + '" data-cat="' + c.id + '">' + c.label + '</button>';
    }).join('');
    document.querySelectorAll('.chip').forEach(function (btn) {
      btn.addEventListener('click', function () { state.categoryFilter = btn.dataset.cat; renderAll(); });
    });
  }

  function filteredDocs() {
    return docs.filter(function (d) { return state.categoryFilter === 'all' || d.category === state.categoryFilter; });
  }

  // ---- docs list ----
  function renderDocsList() {
    var rows = filteredDocs().map(function (d) {
      var success = d.status === 'SUCCESS';
      return '<div class="table-row" style="grid-template-columns:1.8fr 1fr 1fr 0.8fr 1fr 1.3fr;">' +
        '<span style="font-size:13.5px; font-weight:600;">' + esc(d.name) + '</span>' +
        '<span class="badge">' + CATEGORY_LABEL[d.category] + '</span>' +
        '<span style="font-size:12.5px; font-weight:600; color:' + (success ? '#1E8E5A' : '#B5433D') + ';">' + (success ? '추출 완료' : '추출 실패') + '</span>' +
        '<span style="font-size:12.5px; color:#5B6370;">v' + d.currentVersion + '</span>' +
        '<span style="font-size:12.5px; color:#8A93A3;">' + d.lastModified + '</span>' +
        '<span style="display:flex; gap:6px;">' +
          '<button class="btn-sm" data-view="' + d.id + '">보기</button>' +
          '<button class="btn-sm btn-sm--primary-tint" data-edit="' + d.id + '">수정</button>' +
          '<button class="btn-sm btn-sm--danger" data-del="' + d.id + '">삭제</button>' +
        '</span>' +
      '</div>';
    }).join('');
    document.getElementById('docs-rows').innerHTML = rows || '<div class="table-empty">이 카테고리에 등록된 자료가 없어요</div>';

    document.querySelectorAll('[data-view]').forEach(function (b) { b.addEventListener('click', function () { selectDoc(b.dataset.view, false); }); });
    document.querySelectorAll('[data-edit]').forEach(function (b) { b.addEventListener('click', function () { selectDoc(b.dataset.edit, true); }); });
    document.querySelectorAll('[data-del]').forEach(function (b) { b.addEventListener('click', function () { openDelete(b.dataset.del); }); });
  }

  function selectDoc(id, editMode) {
    state.selectedId = id;
    state.viewVersion = null;
    state.editMode = !!editMode;
    state.tab = 'extract';
    document.querySelectorAll('.tabbar__btn').forEach(function (b) { b.classList.toggle('is-active', b.dataset.tab === 'extract'); });
    document.querySelectorAll('.tab-panel').forEach(function (p) { p.hidden = p.dataset.panel !== 'extract'; });
    renderAll();
  }

  // ---- extract nav + detail ----
  function renderExtractNav() {
    var html = filteredDocs().map(function (d) {
      var selected = d.id === state.selectedId;
      var success = d.status === 'SUCCESS';
      return '<div class="extract-nav-item' + (selected ? ' is-active' : '') + '" data-select-doc="' + d.id + '">' +
        '<p class="extract-nav-item__name">' + esc(d.name) + '</p>' +
        '<span class="extract-nav-item__meta" style="color:' + (success ? '#1E8E5A' : '#B5433D') + ';">' + (success ? '추출 완료' : '추출 실패') + ' · v' + d.currentVersion + '</span>' +
      '</div>';
    }).join('');
    document.getElementById('extract-nav').innerHTML = html;
    document.querySelectorAll('[data-select-doc]').forEach(function (b) { b.addEventListener('click', function () { selectDoc(b.dataset.selectDoc, false); }); });
  }

  function fieldInputHtml(opts) {
    // opts: {label, required, value, path, type: text|textarea, placeholder, height, readOnly}
    var star = opts.required ? '<span class="required-mark">필수</span>' : '<span class="optional-mark">선택</span>';
    var attrs = 'data-path="' + opts.path + '"' + (opts.readOnly ? ' disabled' : '');
    var input = opts.type === 'textarea'
      ? '<textarea class="textarea-input" style="height:' + (opts.height || 70) + 'px;" placeholder="' + esc(opts.placeholder || '') + '" ' + attrs + '>' + esc(opts.value) + '</textarea>'
      : '<input class="text-input" placeholder="' + esc(opts.placeholder || '') + '" value="' + esc(opts.value) + '" ' + attrs + '>';
    return '<div class="extract-field-group"><p class="field-label--section">' + opts.label + ' ' + star + '</p>' + input + '</div>';
  }

  function renderExtractDetail() {
    var d = docs.find(function (x) { return x.id === state.selectedId; }) || docs[0];
    if (!d) { document.getElementById('extract-detail').innerHTML = ''; return; }
    var success = d.status === 'SUCCESS';
    var viewVersion = state.viewVersion || d.currentVersion;
    var isOld = viewVersion !== d.currentVersion;
    var versionEntry = d.versions.find(function (v) { return v.version === viewVersion; });
    var readOnly = isOld || !success || !state.editMode;
    var canEnterEdit = success && !isOld && !state.editMode;

    var html = '<div class="flex-row" style="justify-content:space-between; align-items:flex-start; gap:10px; flex-wrap:wrap; margin-bottom:8px;">' +
      '<div><p style="font-size:16px; font-weight:700; margin:0 0 4px;">' + esc(d.name) + '</p><p style="font-size:12px; color:#8A93A3; margin:0;">최종 수정일 ' + d.lastModified + '</p></div>' +
      '<div class="flex-row gap-8" style="flex-wrap:wrap;">' +
        '<span class="badge-pill" style="background:' + (success ? '#E6F4EC' : '#FBEEEC') + '; color:' + (success ? '#1E8E5A' : '#B5433D') + ';">' + (success ? '추출 완료' : '추출 실패') + '</span>' +
        '<select class="select-input" style="width:auto; padding:6px 10px; font-size:12.5px;" id="version-select">' +
          d.versions.map(function (v) { return '<option value="' + v.version + '"' + (v.version === viewVersion ? ' selected' : '') + '>v' + v.version + ' · ' + v.date + '</option>'; }).join('') +
        '</select>' +
        (canEnterEdit ? '<button class="btn-sm btn-sm--primary-tint" id="enter-edit-btn">수정</button>' : '') +
      '</div></div>';

    if (isOld) {
      html += '<div class="old-version-banner"><span style="font-size:12px; color:#8A5A22; font-weight:600;">이전 버전(v' + viewVersion + ')을 보고 있어요 · 수정하려면 최신 버전으로 이동하세요</span>' +
        '<button class="btn-sm" style="margin-left:auto;" id="go-latest-btn">최신 버전으로</button></div>';
    }
    if (versionEntry && versionEntry.note) html += '<p class="version-note">' + esc(versionEntry.note) + '</p>';

    if (!success) {
      html += '<div class="extract-fail"><p style="font-size:14px; font-weight:600; margin:0;">텍스트를 정확히 추출할 수 없습니다</p>' +
        '<p style="font-size:12.5px; color:#8A93A3; margin:0;">파일이 흐리거나 손상되었을 수 있어요. 다시 업로드해주세요.</p>' +
        '<button class="btn btn--primary" id="reupload-req-btn">재업로드 요청</button></div>';
    } else {
      html += renderCategoryFields(d, readOnly);
    }

    if (success && !isOld && state.editMode) {
      html += '<div class="flex-row" style="justify-content:flex-end; margin-top:20px; border-top:1px solid #F0F2F5; padding-top:16px;">' +
        '<button class="btn btn--primary" id="save-fields-btn">수정 내용 저장</button></div>';
    }

    document.getElementById('extract-detail').innerHTML = html;
    bindExtractDetailEvents(d, readOnly);
  }

  function renderCategoryFields(d, readOnly) {
    var f = d.fields;
    if (d.category === 'resume') {
      var html = '<div class="extract-field-group"><p class="field-label--section">기본 정보 <span class="required-mark">필수</span></p><div class="grid-3">' +
        '<input class="text-input" placeholder="이름" value="' + esc(f.basic.name) + '" data-path="basic.name"' + (readOnly ? ' disabled' : '') + '>' +
        '<input class="text-input" placeholder="희망 직무" value="' + esc(f.basic.desiredJob) + '" data-path="basic.desiredJob"' + (readOnly ? ' disabled' : '') + '>' +
        '<input class="text-input" placeholder="연락처 (선택)" value="' + esc(f.basic.contact) + '" data-path="basic.contact"' + (readOnly ? ' disabled' : '') + '>' +
      '</div></div>';
      html += '<div class="extract-field-group"><div class="flex-row" style="justify-content:space-between; margin-bottom:10px;"><p class="field-label--section" style="margin:0;">학력 <span class="required-mark">필수</span></p>' + (readOnly ? '' : '<button class="icon-btn-add" data-add-edu>+ 학력 추가</button>') + '</div>';
      f.education.forEach(function (edu, idx) {
        html += '<div class="grid-3" style="grid-template-columns:1.3fr 1.3fr 1fr 0.8fr 32px; margin-bottom:8px;">' +
          '<input class="text-input" placeholder="학교명" value="' + esc(edu.school) + '" data-path="education.' + idx + '.school"' + (readOnly ? ' disabled' : '') + '>' +
          '<input class="text-input" placeholder="전공" value="' + esc(edu.major) + '" data-path="education.' + idx + '.major"' + (readOnly ? ' disabled' : '') + '>' +
          '<input class="text-input" placeholder="재학 기간" value="' + esc(edu.period) + '" data-path="education.' + idx + '.period"' + (readOnly ? ' disabled' : '') + '>' +
          '<input class="text-input" placeholder="학위" value="' + esc(edu.degree) + '" data-path="education.' + idx + '.degree"' + (readOnly ? ' disabled' : '') + '>' +
          (readOnly ? '' : '<button class="icon-btn-remove" data-remove-edu="' + idx + '">&times;</button>') +
        '</div>';
      });
      html += '</div>';
      html += fieldInputHtml({ label: '기술 스택', required: true, value: f.skills, path: 'skills', placeholder: '예) Java, Spring Boot, MySQL', readOnly: readOnly });
      html += '<div class="extract-field-group"><div class="flex-row" style="justify-content:space-between; margin-bottom:10px;"><p class="field-label--section" style="margin:0;">프로젝트/경험 <span class="required-mark">필수</span></p>' + (readOnly ? '' : '<button class="icon-btn-add" data-add-exp>+ 프로젝트/경험 추가</button>') + '</div>';
      f.experience.forEach(function (exp, idx) {
        html += '<div class="extract-project-card"><div class="grid-3" style="grid-template-columns:1fr 1fr 1fr 1.4fr 32px;">' +
          '<input class="text-input" placeholder="기간" value="' + esc(exp.period) + '" data-path="experience.' + idx + '.period"' + (readOnly ? ' disabled' : '') + '>' +
          '<input class="text-input" placeholder="역할" value="' + esc(exp.role) + '" data-path="experience.' + idx + '.role"' + (readOnly ? ' disabled' : '') + '>' +
          '<input class="text-input" placeholder="사용 기술" value="' + esc(exp.tech) + '" data-path="experience.' + idx + '.tech"' + (readOnly ? ' disabled' : '') + '>' +
          '<input class="text-input" placeholder="주요 성과" value="' + esc(exp.achievement) + '" data-path="experience.' + idx + '.achievement"' + (readOnly ? ' disabled' : '') + '>' +
          (readOnly ? '' : '<button class="icon-btn-remove" data-remove-exp="' + idx + '">&times;</button>') +
        '</div></div>';
      });
      html += '</div>';
      html += fieldInputHtml({ label: '자격/수상', required: false, value: f.certs, path: 'certs', placeholder: '예) 정보처리기사, AWS SAA', readOnly: readOnly });
      return html;
    }
    if (d.category === 'cover') {
      var h = '<div class="grid-2"><div class="extract-field-group"><p class="field-label--section">지원 회사 <span class="optional-mark">선택</span></p><input class="text-input" value="' + esc(f.companyName) + '" data-path="companyName"' + (readOnly ? ' disabled' : '') + '></div>' +
        '<div class="extract-field-group"><p class="field-label--section">지원 직무 <span class="required-mark">필수</span></p><input class="text-input" value="' + esc(f.desiredJob) + '" data-path="desiredJob"' + (readOnly ? ' disabled' : '') + '></div></div>';
      h += fieldInputHtml({ label: '지원 동기', required: true, value: f.motivation, path: 'motivation', height: 76, readOnly: readOnly });
      h += fieldInputHtml({ label: '핵심 경험', required: true, value: f.coreExperience, path: 'coreExperience', height: 88, readOnly: readOnly });
      h += fieldInputHtml({ label: '협업/갈등 해결 경험', required: false, value: f.collabExperience, path: 'collabExperience', height: 76, readOnly: readOnly });
      h += fieldInputHtml({ label: '직무 연결성', required: true, value: f.jobConnection, path: 'jobConnection', height: 76, readOnly: readOnly });
      h += fieldInputHtml({ label: '입사 후 목표', required: false, value: f.futureGoal, path: 'futureGoal', height: 76, readOnly: readOnly });
      return h;
    }
    if (d.category === 'portfolio') {
      var hp = fieldInputHtml({ label: '포트폴리오 제목', required: true, value: f.title, path: 'title', readOnly: readOnly });
      hp += '<div class="extract-field-group"><div class="flex-row" style="justify-content:space-between; margin-bottom:10px;"><p class="field-label--section" style="margin:0;">프로젝트 목록</p>' + (readOnly ? '' : '<button class="icon-btn-add" data-add-project>+ 프로젝트 추가</button>') + '</div>';
      f.projects.forEach(function (proj, idx) {
        hp += '<div class="extract-project-card">' +
          '<div class="flex-row" style="justify-content:space-between; margin-bottom:10px;"><span class="extract-project-card__badge">프로젝트 ' + (idx + 1) + '</span>' + (readOnly ? '' : '<button class="icon-btn-remove" data-remove-project="' + idx + '">&times;</button>') + '</div>' +
          '<div class="grid-2" style="grid-template-columns:1.4fr 1fr; margin-bottom:8px;">' +
            '<input class="text-input" placeholder="프로젝트명" value="' + esc(proj.name) + '" data-path="projects.' + idx + '.name"' + (readOnly ? ' disabled' : '') + '>' +
            '<input class="text-input" placeholder="기간" value="' + esc(proj.period) + '" data-path="projects.' + idx + '.period"' + (readOnly ? ' disabled' : '') + '>' +
          '</div>' +
          '<div class="grid-2" style="margin-bottom:8px;">' +
            '<input class="text-input" placeholder="내 역할" value="' + esc(proj.role) + '" data-path="projects.' + idx + '.role"' + (readOnly ? ' disabled' : '') + '>' +
            '<input class="text-input" placeholder="사용 기술" value="' + esc(proj.tech) + '" data-path="projects.' + idx + '.tech"' + (readOnly ? ' disabled' : '') + '>' +
          '</div>' +
          '<p style="font-size:11px; font-weight:700; color:#8A93A3; margin:0 0 5px;">핵심 기능</p>' +
          '<textarea class="textarea-input" style="height:60px; margin-bottom:8px;" data-path="projects.' + idx + '.coreContent"' + (readOnly ? ' disabled' : '') + '>' + esc(proj.coreContent) + '</textarea>' +
          '<p style="font-size:11px; font-weight:700; color:#8A93A3; margin:0 0 5px;">문제 해결 과정</p>' +
          '<textarea class="textarea-input" style="height:70px; margin-bottom:8px;" data-path="projects.' + idx + '.problemSolving"' + (readOnly ? ' disabled' : '') + '>' + esc(proj.problemSolving) + '</textarea>' +
          '<p style="font-size:11px; font-weight:700; color:#8A93A3; margin:0 0 5px;">성과/지표</p>' +
          '<textarea class="textarea-input" style="height:56px; margin-bottom:8px;" data-path="projects.' + idx + '.outcome"' + (readOnly ? ' disabled' : '') + '>' + esc(proj.outcome) + '</textarea>' +
          '<p style="font-size:11px; font-weight:700; color:#8A93A3; margin:0 0 5px;">링크 (선택)</p>' +
          '<input class="text-input" value="' + esc(proj.link) + '" data-path="projects.' + idx + '.link"' + (readOnly ? ' disabled' : '') + '>' +
        '</div>';
      });
      hp += '</div>';
      return hp;
    }
    if (d.category === 'exp') {
      var he = '<div class="extract-field-group"><div class="flex-row" style="justify-content:space-between; margin-bottom:10px;"><p class="field-label--section" style="margin:0;">STAR 경험 소재</p>' + (readOnly ? '' : '<button class="icon-btn-add" data-add-entry>+ 경험 항목 추가</button>') + '</div>';
      f.entries.forEach(function (en, idx) {
        he += '<div class="extract-entry-card">' +
          '<div class="flex-row" style="justify-content:space-between; margin-bottom:10px;"><span class="extract-entry-card__badge">경험 ' + (idx + 1) + '</span>' + (readOnly ? '' : '<button class="icon-btn-remove" data-remove-entry="' + idx + '">&times;</button>') + '</div>' +
          '<div class="grid-2">' +
            fieldMini('상황(S)', en.situation, 'entries.' + idx + '.situation', readOnly) +
            fieldMini('과제/목표(T)', en.taskGoal, 'entries.' + idx + '.taskGoal', readOnly) +
            fieldMini('행동(A)', en.action, 'entries.' + idx + '.action', readOnly) +
            fieldMini('결과(R)', en.result, 'entries.' + idx + '.result', readOnly) +
          '</div>' +
          '<div class="grid-3" style="margin-top:8px;">' +
            '<input class="text-input" placeholder="사용 기술" value="' + esc(en.tech) + '" data-path="entries.' + idx + '.tech"' + (readOnly ? ' disabled' : '') + '>' +
            '<input class="text-input" placeholder="배운 점" value="' + esc(en.learned) + '" data-path="entries.' + idx + '.learned"' + (readOnly ? ' disabled' : '') + '>' +
            '<input class="text-input" placeholder="강조할 포인트" value="' + esc(en.interviewPoint) + '" data-path="entries.' + idx + '.interviewPoint"' + (readOnly ? ' disabled' : '') + '>' +
          '</div>' +
        '</div>';
      });
      he += '</div>';
      return he;
    }
    return '';
  }
  function fieldMini(label, value, path, readOnly) {
    return '<div><p style="font-size:11px; font-weight:700; color:#8A93A3; margin:0 0 5px;">' + label + '</p><textarea class="textarea-input" style="height:56px;" data-path="' + path + '"' + (readOnly ? ' disabled' : '') + '>' + esc(value) + '</textarea></div>';
  }

  function setDeep(obj, path, value) {
    var parts = path.split('.');
    var cur = obj;
    for (var i = 0; i < parts.length - 1; i++) {
      var key = parts[i];
      cur = cur[isNaN(key) ? key : parseInt(key, 10)];
    }
    var last = parts[parts.length - 1];
    cur[isNaN(last) ? last : parseInt(last, 10)] = value;
  }

  function bindExtractDetailEvents(d, readOnly) {
    var versionSelect = document.getElementById('version-select');
    if (versionSelect) versionSelect.addEventListener('change', function () { state.viewVersion = versionSelect.value; renderExtractDetail(); });
    var goLatest = document.getElementById('go-latest-btn');
    if (goLatest) goLatest.addEventListener('click', function () { state.viewVersion = null; renderExtractDetail(); });
    var enterEdit = document.getElementById('enter-edit-btn');
    if (enterEdit) enterEdit.addEventListener('click', function () { state.editMode = true; renderExtractDetail(); });
    var reuploadReq = document.getElementById('reupload-req-btn');
    if (reuploadReq) reuploadReq.addEventListener('click', function () { openReupload(d.id); });

    if (!readOnly) {
      document.querySelectorAll('#extract-detail [data-path]').forEach(function (el) {
        el.addEventListener('input', function () { setDeep(d.fields, el.dataset.path, el.value); });
      });
      var addEdu = document.querySelector('[data-add-edu]');
      if (addEdu) addEdu.addEventListener('click', function () { d.fields.education.push({ school: '', major: '', period: '', degree: '' }); renderExtractDetail(); });
      document.querySelectorAll('[data-remove-edu]').forEach(function (b) { b.addEventListener('click', function () { d.fields.education.splice(parseInt(b.dataset.removeEdu, 10), 1); renderExtractDetail(); }); });
      var addExp = document.querySelector('[data-add-exp]');
      if (addExp) addExp.addEventListener('click', function () { d.fields.experience.push({ period: '', role: '', tech: '', achievement: '' }); renderExtractDetail(); });
      document.querySelectorAll('[data-remove-exp]').forEach(function (b) { b.addEventListener('click', function () { d.fields.experience.splice(parseInt(b.dataset.removeExp, 10), 1); renderExtractDetail(); }); });
      var addProject = document.querySelector('[data-add-project]');
      if (addProject) addProject.addEventListener('click', function () { d.fields.projects.push({ name: '', period: '', role: '', tech: '', coreContent: '', problemSolving: '', outcome: '', link: '' }); renderExtractDetail(); });
      document.querySelectorAll('[data-remove-project]').forEach(function (b) { b.addEventListener('click', function () { d.fields.projects.splice(parseInt(b.dataset.removeProject, 10), 1); renderExtractDetail(); }); });
      var addEntry = document.querySelector('[data-add-entry]');
      if (addEntry) addEntry.addEventListener('click', function () { d.fields.entries.push({ situation: '', taskGoal: '', action: '', result: '', tech: '', learned: '', interviewPoint: '' }); renderExtractDetail(); });
      document.querySelectorAll('[data-remove-entry]').forEach(function (b) { b.addEventListener('click', function () { d.fields.entries.splice(parseInt(b.dataset.removeEntry, 10), 1); renderExtractDetail(); }); });
    }

    var saveBtn = document.getElementById('save-fields-btn');
    if (saveBtn) saveBtn.addEventListener('click', function () {
      openScale(d.id, function (scale) {
        var next = bumpVersion(d.currentVersion, scale);
        d.versions.push({ version: next, date: todayLabel(), note: (scale === 'major' ? '큰 수정' : '간단한 수정') + ' (직접 편집)' });
        d.currentVersion = next;
        d.lastModified = todayLabel();
        renderAll();
      });
    });
  }

  // ---- register modal ----
  function openRegisterModal() {
    state.registerCategory = null; state.registerMethod = 'file'; state.registerFileName = ''; state.registerFields = null;
    renderRegisterCategoryGrid();
    document.getElementById('register-name').value = '';
    document.getElementById('register-file').value = '';
    document.querySelectorAll('#register-modal [data-method]').forEach(function (b) { b.classList.toggle('is-active', b.dataset.method === 'file'); });
    document.querySelector('[data-method-panel="file"]').hidden = false;
    document.querySelector('[data-method-panel="text"]').hidden = true;
    document.getElementById('register-modal').hidden = false;
  }
  function renderRegisterCategoryGrid() {
    document.getElementById('register-category-grid').innerHTML = CATEGORIES.map(function (c) {
      return '<button class="reg-cat-btn' + (state.registerCategory === c.id ? ' is-active' : '') + '" data-reg-cat="' + c.id + '">' + c.label + '</button>';
    }).join('');
    document.querySelectorAll('[data-reg-cat]').forEach(function (b) {
      b.addEventListener('click', function () {
        state.registerCategory = b.dataset.regCat;
        state.registerFields = defaultFieldsFor(state.registerCategory);
        renderRegisterCategoryGrid();
        renderRegisterTextFields();
      });
    });
  }
  function renderRegisterTextFields() {
    var container = document.getElementById('register-text-fields');
    if (!state.registerCategory) { container.innerHTML = '<p class="text-faint" style="font-size:12.5px;">먼저 자료 종류를 선택해주세요</p>'; return; }
    var f = state.registerFields;
    var html = '';
    if (state.registerCategory === 'resume') {
      html += '<div class="grid-3" style="margin-bottom:10px;">' +
        '<input class="text-input" placeholder="이름" data-rf="basic.name">' +
        '<input class="text-input" placeholder="희망 직무" data-rf="basic.desiredJob">' +
        '<input class="text-input" placeholder="연락처 (선택)" data-rf="basic.contact"></div>' +
        '<input class="text-input" placeholder="학교명/전공/기간/학위 (예: OO대학교, 컴퓨터공학과, 2018~2022, 학사)" data-rf="education.0.school" style="margin-bottom:10px;">' +
        '<input class="text-input" placeholder="기술 스택 (예: Java, Spring Boot, MySQL)" data-rf="skills" style="margin-bottom:10px;">' +
        '<textarea class="textarea-input" placeholder="프로젝트/경험" style="height:60px; margin-bottom:10px;" data-rf="experience.0.achievement"></textarea>' +
        '<input class="text-input" placeholder="자격/수상 (선택)" data-rf="certs">';
    } else if (state.registerCategory === 'cover') {
      html += '<div class="grid-2" style="margin-bottom:10px;">' +
        '<input class="text-input" placeholder="지원 회사 (선택)" data-rf="companyName">' +
        '<input class="text-input" placeholder="지원 직무 *" data-rf="desiredJob"></div>' +
        '<textarea class="textarea-input" placeholder="지원 동기 *" style="height:56px; margin-bottom:10px;" data-rf="motivation"></textarea>' +
        '<textarea class="textarea-input" placeholder="핵심 경험 *" style="height:56px; margin-bottom:10px;" data-rf="coreExperience"></textarea>' +
        '<textarea class="textarea-input" placeholder="협업/갈등 해결 경험 (선택)" style="height:56px; margin-bottom:10px;" data-rf="collabExperience"></textarea>' +
        '<textarea class="textarea-input" placeholder="직무 연결성 *" style="height:56px; margin-bottom:10px;" data-rf="jobConnection"></textarea>' +
        '<textarea class="textarea-input" placeholder="입사 후 목표 (선택)" style="height:56px;" data-rf="futureGoal"></textarea>';
    } else if (state.registerCategory === 'portfolio') {
      html += '<input class="text-input" placeholder="포트폴리오 제목 *" data-rf="title" style="margin-bottom:10px;">' +
        '<input class="text-input" placeholder="프로젝트명 *" data-rf="projects.0.name" style="margin-bottom:10px;">' +
        '<input class="text-input" placeholder="내 역할 * / 사용 기술 *" data-rf="projects.0.role" style="margin-bottom:10px;">' +
        '<textarea class="textarea-input" placeholder="핵심 기능 * / 문제 해결 과정 * / 성과·지표 *" style="height:70px;" data-rf="projects.0.coreContent"></textarea>';
    } else if (state.registerCategory === 'exp') {
      html += '<textarea class="textarea-input" placeholder="상황(S) *" style="height:50px; margin-bottom:8px;" data-rf="entries.0.situation"></textarea>' +
        '<textarea class="textarea-input" placeholder="과제/목표(T) *" style="height:50px; margin-bottom:8px;" data-rf="entries.0.taskGoal"></textarea>' +
        '<textarea class="textarea-input" placeholder="행동(A) *" style="height:50px; margin-bottom:8px;" data-rf="entries.0.action"></textarea>' +
        '<textarea class="textarea-input" placeholder="결과(R) *" style="height:50px;" data-rf="entries.0.result"></textarea>';
    }
    container.innerHTML = html;
    document.querySelectorAll('[data-rf]').forEach(function (el) {
      el.addEventListener('input', function () { setDeep(state.registerFields, el.dataset.rf, el.value); });
    });
  }

  function bindRegisterModal() {
    document.getElementById('open-register').addEventListener('click', openRegisterModal);
    document.getElementById('register-close').addEventListener('click', function () { document.getElementById('register-modal').hidden = true; });
    document.getElementById('register-cancel').addEventListener('click', function () { document.getElementById('register-modal').hidden = true; });
    document.querySelectorAll('#register-modal [data-method]').forEach(function (b) {
      b.addEventListener('click', function () {
        state.registerMethod = b.dataset.method;
        document.querySelectorAll('#register-modal [data-method]').forEach(function (x) { x.classList.toggle('is-active', x === b); });
        document.querySelector('[data-method-panel="file"]').hidden = state.registerMethod !== 'file';
        document.querySelector('[data-method-panel="text"]').hidden = state.registerMethod !== 'text';
        if (state.registerMethod === 'text') renderRegisterTextFields();
      });
    });
    document.getElementById('register-file').addEventListener('change', function (e) {
      state.registerFileName = e.target.files[0] ? e.target.files[0].name : '';
    });
    document.getElementById('register-submit').addEventListener('click', function () {
      var name = document.getElementById('register-name').value.trim();
      if (!state.registerCategory || !name) { alert('자료 종류와 자료명을 입력해주세요.'); return; }
      if (state.registerMethod === 'file' && !state.registerFileName) { alert('파일을 선택해주세요.'); return; }
      var id = state.registerCategory + '_' + Date.now();
      var fields = state.registerMethod === 'text' ? state.registerFields : defaultFieldsFor(state.registerCategory);
      docs.push({ id: id, category: state.registerCategory, name: name, status: 'SUCCESS', currentVersion: '1.0', lastModified: todayLabel(), versions: [{ version: '1.0', date: todayLabel(), note: '최초 등록' }], fields: fields });
      document.getElementById('register-modal').hidden = true;
      selectDoc(id, false);
    });
  }

  // ---- reupload modal ----
  function openReupload(docId) {
    state.reuploadDocId = docId; state.reuploadFileName = '';
    document.getElementById('reupload-doc-name').textContent = (docs.find(function (d) { return d.id === docId; }) || {}).name || '';
    document.getElementById('reupload-file').value = '';
    document.getElementById('reupload-modal').hidden = false;
  }
  function bindReuploadModal() {
    document.getElementById('reupload-close').addEventListener('click', function () { document.getElementById('reupload-modal').hidden = true; });
    document.getElementById('reupload-cancel').addEventListener('click', function () { document.getElementById('reupload-modal').hidden = true; });
    document.getElementById('reupload-file').addEventListener('change', function (e) { state.reuploadFileName = e.target.files[0] ? e.target.files[0].name : ''; });
    document.getElementById('reupload-submit').addEventListener('click', function () {
      if (!state.reuploadFileName) { alert('파일을 선택해주세요.'); return; }
      document.getElementById('reupload-modal').hidden = true;
      var docId = state.reuploadDocId;
      openScale(docId, function (scale) {
        var d = docs.find(function (x) { return x.id === docId; });
        var next = bumpVersion(d.currentVersion === '-' ? '0.9' : d.currentVersion, scale);
        d.status = 'SUCCESS';
        d.versions.push({ version: next, date: todayLabel(), note: (scale === 'major' ? '큰 수정' : '간단한 수정') + ' (재업로드)' });
        d.currentVersion = next;
        d.lastModified = todayLabel();
        renderAll();
      });
    });
  }

  // ---- scale modal ----
  function openScale(docId, action) {
    var d = docs.find(function (x) { return x.id === docId; });
    state.scaleDocId = docId; state.scaleScale = 'minor'; state.scaleAction = action;
    document.getElementById('scale-current-version').textContent = 'v' + (d ? d.currentVersion : '1.0');
    document.getElementById('scale-minor-preview').textContent = bumpVersion(d ? d.currentVersion : '1.0', 'minor');
    document.getElementById('scale-major-preview').textContent = bumpVersion(d ? d.currentVersion : '1.0', 'major');
    document.querySelectorAll('.scale-option').forEach(function (o) { o.classList.toggle('is-active', o.dataset.scale === 'minor'); });
    document.getElementById('scale-modal').hidden = false;
  }
  function bindScaleModal() {
    document.querySelectorAll('.scale-option').forEach(function (o) {
      o.addEventListener('click', function () {
        state.scaleScale = o.dataset.scale;
        document.querySelectorAll('.scale-option').forEach(function (x) { x.classList.toggle('is-active', x === o); });
      });
    });
    document.getElementById('scale-cancel').addEventListener('click', function () { document.getElementById('scale-modal').hidden = true; });
    document.getElementById('scale-confirm').addEventListener('click', function () {
      document.getElementById('scale-modal').hidden = true;
      if (state.scaleAction) state.scaleAction(state.scaleScale);
    });
  }

  // ---- delete modal ----
  function openDelete(docId) {
    state.deleteDocId = docId;
    document.getElementById('delete-doc-name').textContent = (docs.find(function (d) { return d.id === docId; }) || {}).name || '';
    document.getElementById('delete-modal').hidden = false;
  }
  function bindDeleteModal() {
    document.getElementById('delete-cancel').addEventListener('click', function () { document.getElementById('delete-modal').hidden = true; });
    document.getElementById('delete-confirm').addEventListener('click', function () {
      var id = state.deleteDocId;
      docs = docs.filter(function (d) { return d.id !== id; });
      if (state.selectedId === id) state.selectedId = docs[0] ? docs[0].id : null;
      document.getElementById('delete-modal').hidden = true;
      renderAll();
    });
  }

  function renderAll() {
    renderChips();
    renderDocsList();
    renderExtractNav();
    renderExtractDetail();
  }

  document.addEventListener('DOMContentLoaded', function () {
    renderTabs();
    bindRegisterModal();
    bindReuploadModal();
    bindScaleModal();
    bindDeleteModal();
    renderAll();
  });
})();
