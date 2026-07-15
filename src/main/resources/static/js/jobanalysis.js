// jobanalysis.js — 회사 자료 page logic

(function () {
  'use strict';

  var CATEGORIES = [
    { id: 'company', label: '회사 정보' },
    { id: 'job', label: '채용공고' }
  ];
  var CATEGORY_LABEL = { company: '회사 정보', job: '채용공고' };
  var INDUSTRY_OPTIONS = ['IT·소프트웨어', '커머스/유통', '금융', '제조', '미디어/콘텐츠', '바이오/헬스케어', '기타'];

  function initialDocs() {
    return [
      {
        id: 'company_lume', category: 'company', name: 'LUME 회사 정보', status: 'SUCCESS', currentVersion: '1.1', lastModified: '2026.07.06',
        versions: [{ version: '1.0', date: '2026.06.28', note: '최초 등록' }, { version: '1.1', date: '2026.07.06', note: '인재상 문구 보완' }],
        fields: { name: 'LUME', industry: 'IT·소프트웨어', serviceDesc: '커머스 플랫폼 LUME는 중소형 판매자를 위한 온라인 쇼핑몰 구축·운영 솔루션을 제공합니다.', values: '"기술로 판매자의 성장을 돕는다"는 미션 아래, 안정성과 실행 속도를 함께 추구합니다.', talentProfile: '문제를 스스로 정의하고 해결하는 주도성, 데이터 기반 의사결정, 협업 커뮤니케이션 역량을 중요하게 봅니다.', refUrl: 'https://lume.io' }
      },
      {
        id: 'job_lume', category: 'job', name: 'LUME 백엔드 개발자 공고', status: 'SUCCESS', currentVersion: '1.0', lastModified: '2026.07.07',
        versions: [{ version: '1.0', date: '2026.07.07', note: '최초 등록' }],
        fields: { companyName: 'LUME', title: '백엔드 개발자', mainDuties: '- 서버 API 설계 및 개발 (RESTful 기반)\n- 데이터베이스 설계 및 쿼리 최적화\n- 서비스 성능 개선 및 모니터링', requirements: '- Java 또는 Kotlin 기반 웹 개발 경험\n- Spring Boot, JPA 사용 경험\n- RDBMS 설계 및 SQL 작성 경험', preferred: '- 대규모 트래픽 처리 경험\n- AWS 등 클라우드 인프라 운영 경험', processInfo: '서류 → 코딩테스트 → 1차 실무면접 → 2차 임원면접 → 처우협의', url: 'https://recruit.lume.io/backend' }
      },
      {
        id: 'company_naver', category: 'company', name: '네이버클라우드 회사 정보', status: 'SUCCESS', currentVersion: '1.0', lastModified: '2026.06.20',
        versions: [{ version: '1.0', date: '2026.06.20', note: '최초 등록' }],
        fields: { name: '네이버클라우드', industry: 'IT·소프트웨어', serviceDesc: '클라우드 인프라(IaaS), 플랫폼(PaaS), AI/빅데이터 서비스를 제공하는 클라우드 사업자입니다.', values: '기술 내재화와 안정적인 서비스 운영을 최우선 가치로 삼습니다.', talentProfile: '대규모 시스템에 대한 깊은 이해와 안정성을 우선하는 엔지니어링 마인드를 중요하게 봅니다.', refUrl: 'https://www.navercloudcorp.com' }
      },
      {
        id: 'job_naver', category: 'job', name: '네이버클라우드 백엔드 공고', status: 'SUCCESS', currentVersion: '1.0', lastModified: '2026.06.20',
        versions: [{ version: '1.0', date: '2026.06.20', note: '최초 등록' }],
        fields: { companyName: '네이버클라우드', title: '백엔드 개발', mainDuties: '- 클라우드 플랫폼 백엔드 서비스 개발\n- 대규모 분산 시스템 설계', requirements: '- 대규모 트래픽 처리 경험\n- Java/Kotlin, Spring 기반 개발 경험', preferred: '- Kubernetes, MSA 경험', processInfo: '서류 → 코딩테스트 → 기술면접 → 임원면접', url: '' }
      },
      {
        id: 'job_data', category: 'job', name: '데이터플랫폼(주) 서버개발 공고', status: 'FAILED', currentVersion: '-', lastModified: '2026.06.18',
        versions: [{ version: '-', date: '2026.06.18', note: '최초 등록' }],
        fields: null
      }
    ];
  }

  function defaultFieldsFor(category) {
    return {
      company: { name: '', industry: '', serviceDesc: '', values: '', talentProfile: '', refUrl: '' },
      job: { companyName: '', title: '', mainDuties: '', requirements: '', preferred: '', processInfo: '', url: '' }
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
  var state = { tab: 'docs', categoryFilter: 'all', selectedId: 'company_lume', viewVersion: null, editMode: false, registerCategory: null, registerMethod: 'file', registerFileName: '', registerFields: null, scaleDocId: null, scaleScale: 'minor', scaleAction: null, deleteDocId: null };

  function companyNames() { return docs.filter(function (d) { return d.category === 'company'; }).map(function (d) { return d.fields && d.fields.name; }).filter(Boolean); }

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

  function renderDocsList() {
    var names = companyNames();
    var rows = filteredDocs().map(function (d) {
      var success = d.status === 'SUCCESS';
      var isJob = d.category === 'job';
      var linked = isJob && d.fields && names.indexOf(d.fields.companyName) !== -1;
      return '<div class="table-row" style="grid-template-columns:1.6fr 1fr 1fr 0.8fr 1fr 1.3fr;">' +
        '<div><span style="font-size:13.5px; font-weight:600; display:block;">' + esc(d.name) + '</span>' +
        (isJob ? '<span style="font-size:11px; font-weight:600; color:' + (linked ? '#1E8E5A' : '#B37A19') + ';">' + (linked ? '회사 정보 연결됨' : '회사 정보 미등록') + '</span>' : '') + '</div>' +
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
    document.getElementById('docs-rows').innerHTML = rows || '<div class="table-empty">이 구분에 등록된 자료가 없어요</div>';
    document.querySelectorAll('[data-view]').forEach(function (b) { b.addEventListener('click', function () { selectDoc(b.dataset.view, false); }); });
    document.querySelectorAll('[data-edit]').forEach(function (b) { b.addEventListener('click', function () { selectDoc(b.dataset.edit, true); }); });
    document.querySelectorAll('[data-del]').forEach(function (b) { b.addEventListener('click', function () { openDelete(b.dataset.del); }); });
  }

  function selectDoc(id, editMode) {
    state.selectedId = id; state.viewVersion = null; state.editMode = !!editMode; state.tab = 'extract';
    document.querySelectorAll('.tabbar__btn').forEach(function (b) { b.classList.toggle('is-active', b.dataset.tab === 'extract'); });
    document.querySelectorAll('.tab-panel').forEach(function (p) { p.hidden = p.dataset.panel !== 'extract'; });
    renderAll();
  }

  function renderExtractNav() {
    document.getElementById('extract-nav').innerHTML = filteredDocs().map(function (d) {
      var selected = d.id === state.selectedId;
      var success = d.status === 'SUCCESS';
      return '<div class="extract-nav-item' + (selected ? ' is-active' : '') + '" data-select-doc="' + d.id + '">' +
        '<p class="extract-nav-item__name">' + esc(d.name) + '</p>' +
        '<span class="extract-nav-item__meta" style="color:' + (success ? '#1E8E5A' : '#B5433D') + ';">' + (success ? '추출 완료' : '추출 실패') + ' · v' + d.currentVersion + '</span>' +
      '</div>';
    }).join('');
    document.querySelectorAll('[data-select-doc]').forEach(function (b) { b.addEventListener('click', function () { selectDoc(b.dataset.selectDoc, false); }); });
  }

  function fieldBlock(label, required, value, path, type, height, readOnly, placeholder) {
    var star = required ? '<span class="required-mark">필수</span>' : '<span class="optional-mark">선택</span>';
    var input = type === 'textarea'
      ? '<textarea class="textarea-input" style="height:' + (height || 70) + 'px;" data-path="' + path + '"' + (readOnly ? ' disabled' : '') + ' placeholder="' + esc(placeholder || '') + '">' + esc(value) + '</textarea>'
      : '<input class="text-input" data-path="' + path + '"' + (readOnly ? ' disabled' : '') + ' value="' + esc(value) + '" placeholder="' + esc(placeholder || '') + '">';
    return '<div class="extract-field-group"><p class="field-label--section">' + label + ' ' + star + '</p>' + input + '</div>';
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

    if (isOld) html += '<div class="old-version-banner"><span style="font-size:12px; color:#8A5A22; font-weight:600;">이전 버전(v' + viewVersion + ')을 보고 있어요 · 수정하려면 최신 버전으로 이동하세요</span><button class="btn-sm" style="margin-left:auto;" id="go-latest-btn">최신 버전으로</button></div>';
    if (versionEntry && versionEntry.note) html += '<p class="version-note">' + esc(versionEntry.note) + '</p>';

    if (!success) {
      html += '<div class="extract-fail"><p style="font-size:14px; font-weight:600; margin:0;">텍스트를 정확히 추출할 수 없습니다</p><p style="font-size:12.5px; color:#8A93A3; margin:0;">파일이 흐리거나 손상되었을 수 있어요.</p></div>';
    } else if (d.category === 'company') {
      var f = d.fields;
      html += fieldBlock('회사명', true, f.name, 'name', 'text', 0, readOnly, '예) LUME');
      var industryOpts = INDUSTRY_OPTIONS.map(function (i) { return '<option' + (i === f.industry ? ' selected' : '') + '>' + i + '</option>'; }).join('');
      html += '<div class="extract-field-group"><p class="field-label--section">산업군 <span class="required-mark">필수</span></p><select class="select-input" data-path="industry"' + (readOnly ? ' disabled' : '') + '>' + industryOpts + '</select></div>';
      html += fieldBlock('서비스/제품 설명', true, f.serviceDesc, 'serviceDesc', 'textarea', 90, readOnly);
      html += fieldBlock('회사 가치관', true, f.values, 'values', 'textarea', 76, readOnly);
      html += fieldBlock('인재상', true, f.talentProfile, 'talentProfile', 'textarea', 76, readOnly);
      html += fieldBlock('참고 URL', false, f.refUrl, 'refUrl', 'text', 0, readOnly);
    } else if (d.category === 'job') {
      var fj = d.fields;
      var linked = companyNames().indexOf(fj.companyName) !== -1;
      html += '<div class="link-status ' + (linked ? 'link-status--linked' : 'link-status--unlinked') + '">' + (linked ? "등록된 회사 정보 '" + esc(fj.companyName) + "'와 연결되어 있어요" : "'" + esc(fj.companyName) + "'은 아직 회사 정보가 등록되지 않았어요 · 공고 텍스트만으로 분석해요") + '</div>';
      html += '<div class="grid-2">' +
        '<div class="extract-field-group"><p class="field-label--section">회사명 <span class="required-mark">필수</span></p><input class="text-input" value="' + esc(fj.companyName) + '" data-path="companyName"' + (readOnly ? ' disabled' : '') + '></div>' +
        '<div class="extract-field-group"><p class="field-label--section">공고명 <span class="required-mark">필수</span></p><input class="text-input" value="' + esc(fj.title) + '" data-path="title"' + (readOnly ? ' disabled' : '') + '></div>' +
      '</div>';
      html += fieldBlock('주요 업무', true, fj.mainDuties, 'mainDuties', 'textarea', 88, readOnly);
      html += fieldBlock('자격 요건', true, fj.requirements, 'requirements', 'textarea', 88, readOnly);
      html += fieldBlock('우대 사항', false, fj.preferred, 'preferred', 'textarea', 76, readOnly);
      html += fieldBlock('전형 정보', false, fj.processInfo, 'processInfo', 'textarea', 76, readOnly);
      html += fieldBlock('공고 URL', false, fj.url, 'url', 'text', 0, readOnly);
    }

    if (success && !isOld && state.editMode) {
      html += '<div class="flex-row" style="justify-content:flex-end; margin-top:20px; border-top:1px solid #F0F2F5; padding-top:16px;"><button class="btn btn--primary" id="save-fields-btn">수정 내용 저장</button></div>';
    }

    document.getElementById('extract-detail').innerHTML = html;
    bindExtractDetailEvents(d, readOnly);
  }

  function bindExtractDetailEvents(d, readOnly) {
    var versionSelect = document.getElementById('version-select');
    if (versionSelect) versionSelect.addEventListener('change', function () { state.viewVersion = versionSelect.value; renderExtractDetail(); });
    var goLatest = document.getElementById('go-latest-btn');
    if (goLatest) goLatest.addEventListener('click', function () { state.viewVersion = null; renderExtractDetail(); });
    var enterEdit = document.getElementById('enter-edit-btn');
    if (enterEdit) enterEdit.addEventListener('click', function () { state.editMode = true; renderExtractDetail(); });

    if (!readOnly) {
      document.querySelectorAll('#extract-detail [data-path]').forEach(function (el) {
        el.addEventListener('input', function () { d.fields[el.dataset.path] = el.value; });
        el.addEventListener('change', function () { d.fields[el.dataset.path] = el.value; });
      });
    }
    var saveBtn = document.getElementById('save-fields-btn');
    if (saveBtn) saveBtn.addEventListener('click', function () {
      openScale(d.id, function (scale) {
        var next = bumpVersion(d.currentVersion, scale);
        d.versions.push({ version: next, date: todayLabel(), note: (scale === 'major' ? '큰 수정' : '간단한 수정') + ' (직접 편집)' });
        d.currentVersion = next; d.lastModified = todayLabel();
        renderAll();
      });
    });
  }

  // register
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
    var html = '';
    if (state.registerCategory === 'company') {
      html += '<input class="text-input" placeholder="회사명 *" data-rf="name" style="margin-bottom:10px;">' +
        '<select class="select-input" data-rf="industry" style="margin-bottom:10px;"><option value="">산업군을 선택하세요 *</option>' + INDUSTRY_OPTIONS.map(function (i) { return '<option>' + i + '</option>'; }).join('') + '</select>' +
        '<textarea class="textarea-input" placeholder="서비스/제품 설명 *" style="height:60px; margin-bottom:10px;" data-rf="serviceDesc"></textarea>' +
        '<textarea class="textarea-input" placeholder="회사 가치관 *" style="height:52px; margin-bottom:10px;" data-rf="values"></textarea>' +
        '<textarea class="textarea-input" placeholder="인재상 *" style="height:52px; margin-bottom:10px;" data-rf="talentProfile"></textarea>' +
        '<input class="text-input" placeholder="참고 URL (선택)" data-rf="refUrl">';
    } else if (state.registerCategory === 'job') {
      html += '<div class="grid-2" style="margin-bottom:10px;"><input class="text-input" placeholder="회사명 *" data-rf="companyName"><input class="text-input" placeholder="공고명 *" data-rf="title"></div>' +
        '<textarea class="textarea-input" placeholder="주요 업무 *" style="height:60px; margin-bottom:10px;" data-rf="mainDuties"></textarea>' +
        '<textarea class="textarea-input" placeholder="자격 요건 *" style="height:60px; margin-bottom:10px;" data-rf="requirements"></textarea>' +
        '<textarea class="textarea-input" placeholder="우대 사항 (선택)" style="height:52px; margin-bottom:10px;" data-rf="preferred"></textarea>' +
        '<textarea class="textarea-input" placeholder="전형 정보 (선택)" style="height:52px; margin-bottom:10px;" data-rf="processInfo"></textarea>' +
        '<input class="text-input" placeholder="공고 URL (선택)" data-rf="url">';
    }
    container.innerHTML = html;
    document.querySelectorAll('[data-rf]').forEach(function (el) {
      el.addEventListener('input', function () { state.registerFields[el.dataset.rf] = el.value; });
      el.addEventListener('change', function () { state.registerFields[el.dataset.rf] = el.value; });
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
    document.getElementById('register-file').addEventListener('change', function (e) { state.registerFileName = e.target.files[0] ? e.target.files[0].name : ''; });
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
    bindScaleModal();
    bindDeleteModal();
    renderAll();
  });
})();
