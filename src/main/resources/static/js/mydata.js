// mydata.js — 지원 자료 페이지: 실제 백엔드 API(JSON-00) 연동
(function () {
  'use strict';

  var API_BASE = '/api';

  var CATEGORY_LABEL = {
    JOB_POSTING: '채용공고', COMPANY_INFO: '회사정보', RESUME: '이력서',
    COVER_LETTER: '자기소개서', PORTFOLIO: '포트폴리오', EXPERIENCE_NOTE: '경험 자료'
  };
  var CATEGORIES = Object.keys(CATEGORY_LABEL).map(function (id) { return { id: id, label: CATEGORY_LABEL[id] }; });
  var DIRECT_INPUT_ALLOWED = ['JOB_POSTING', 'COMPANY_INFO', 'EXPERIENCE_NOTE'];

  var state = {
    tab: 'docs',
    categoryFilter: 'all',
    docsPage: 0,
    docsPageData: null,
    selectedDocumentId: null,
    documentMeta: null,
    versions: [],
    selectedExtractionId: null,
    editMode: false,
    pageIndex: 0,
    editingPages: null,
    editingHasMarkers: false,
    registerCategory: null,
    registerMethod: 'file',
    scaleAction: null,
    scaleScale: 'minor',
    deleteDocId: null
  };

  // ---- fetch helper: ApiResponse<T> 래퍼를 벗겨서 data만 반환, 실패하면 message로 reject ----
  function api(path, opts) {
    opts = opts || {};
    var headers = opts.headers || {};
    var fetchOpts = { method: opts.method || 'GET', credentials: 'same-origin', headers: headers };
    if (opts.json !== undefined) {
      headers['Content-Type'] = 'application/json; charset=UTF-8';
      fetchOpts.body = JSON.stringify(opts.json);
    } else if (opts.formData) {
      fetchOpts.body = opts.formData;
    }
    return fetch(API_BASE + path, fetchOpts).then(function (res) {
      if (res.status === 204) {
        if (!res.ok) throw new Error('요청에 실패했습니다.');
        return null;
      }
      return res.json().then(function (body) {
        if (!res.ok || !body.success) {
          throw new Error((body && body.message) || '요청에 실패했습니다.');
        }
        return body.data;
      });
    });
  }

  function versionLabel(major, minor) {
    return major == null ? '미확정' : ('v' + major + '.' + minor);
  }

  function statusLabel(extractionStatus, versionStatus) {
    if (extractionStatus === 'FAILED') return { text: '추출 실패', color: '#B5433D' };
    if (versionStatus === 'CONFIRMED') return { text: '확정됨', color: '#1E8E5A' };
    if (versionStatus === 'SUPERSEDED') return { text: '이전 버전', color: '#8A93A3' };
    if (extractionStatus === 'PARTIAL') return { text: '일부만 추출됨', color: '#8A5A22' };
    return { text: '검토 필요', color: '#8A5A22' };
  }

  // [N페이지] 마커를 기준으로 원문을 페이지 단위로 쪼갠다 (백엔드 DocumentExtractionAsyncRunner 참고)
  function splitPages(content) {
    if (!content) return [''];
    var regex = /\[(\d+)페이지\]\n/g;
    var matches = [];
    var m;
    while ((m = regex.exec(content)) !== null) matches.push(m);
    if (matches.length === 0) return [content];
    var pages = [];
    for (var i = 0; i < matches.length; i++) {
      var start = matches[i].index + matches[i][0].length;
      var end = (i + 1 < matches.length) ? matches[i + 1].index : content.length;
      pages.push(content.slice(start, end).trim());
    }
    return pages;
  }

  function hasPageMarkers(content) {
    return /^\[\d+페이지\]\n/.test(content || '');
  }

  // splitPages로 나눈 페이지들을 저장용 content 문자열 하나로 다시 합치기 (원래 페이지 마커가 있던 문서만 마커를 다시 붙임)
  function joinPages(pages, withMarkers) {
    if (!withMarkers) return pages[0] || '';
    return pages.map(function (text, idx) { return '[' + (idx + 1) + '페이지]\n' + text; }).join('\n\n');
  }

  // ---- tabs & chips ----
  function renderTabs() {
    bindTabGroup({
      btnSelector: '.tabbar__btn[data-tab]',
      datasetKey: 'tab',
      panelSelector: '.tab-panel',
      panelDatasetKey: 'panel',
      onSelect: function (value) {
        state.tab = value;
        if (value === 'extract') renderExtractNav();
      }
    });
  }

  function switchToExtractTab() {
    document.querySelectorAll('.tabbar__btn[data-tab]').forEach(function (b) { b.classList.toggle('is-active', b.dataset.tab === 'extract'); });
    document.querySelectorAll('.tab-panel').forEach(function (p) { p.hidden = p.dataset.panel !== 'extract'; });
    state.tab = 'extract';
  }

  function renderChips() {
    var chips = [{ id: 'all', label: '전체' }].concat(CATEGORIES);
    document.getElementById('category-chips').innerHTML = chips.map(function (c) {
      return '<button class="chip' + (state.categoryFilter === c.id ? ' is-active' : '') + '" data-cat="' + c.id + '">' + c.label + '</button>';
    }).join('');
    document.querySelectorAll('.chip').forEach(function (btn) {
      btn.addEventListener('click', function () {
        state.categoryFilter = btn.dataset.cat;
        state.docsPage = 0;
        renderChips();
        loadDocuments();
      });
    });
  }

  // ---- docs list ----
  function loadDocuments() {
    var qs = 'page=' + state.docsPage + '&size=10';
    if (state.categoryFilter !== 'all') qs += '&documentType=' + state.categoryFilter;
    return api('/documents?' + qs).then(function (page) {
      state.docsPageData = page;
      renderDocsList();
      renderDocsPagination();
      if (state.tab === 'extract') renderExtractNav();
    }).catch(function (e) { alert(e.message); });
  }

  function renderDocsList() {
    var docs = (state.docsPageData && state.docsPageData.content) || [];
    var rows = docs.map(function (d) {
      var status = statusLabel(null, d.latestVersionStatus);
      return '<div class="table-row" style="grid-template-columns:1.8fr 1fr 1fr 0.8fr 1fr 1.3fr;">' +
        '<span style="font-size:13.5px; font-weight:600;">' + esc(d.displayName) + '</span>' +
        '<span class="badge">' + CATEGORY_LABEL[d.documentType] + '</span>' +
        '<span style="font-size:12.5px; font-weight:600; color:' + status.color + ';">' + status.text + '</span>' +
        '<span style="font-size:12.5px; color:#5B6370;">' + versionLabel(d.latestMajorVersion, d.latestMinorVersion) + '</span>' +
        '<span style="font-size:12.5px; color:#8A93A3;">' + (d.updatedAt || '').slice(0, 10) + '</span>' +
        '<span style="display:flex; gap:6px;">' +
          '<button class="btn-sm" data-view="' + d.documentId + '">보기</button>' +
          '<button class="btn-sm btn-sm--danger" data-del="' + d.documentId + '" data-name="' + esc(d.displayName) + '">삭제</button>' +
        '</span>' +
      '</div>';
    }).join('');
    document.getElementById('docs-rows').innerHTML = rows || '<div class="table-empty">이 카테고리에 등록된 자료가 없어요</div>';

    document.querySelectorAll('[data-view]').forEach(function (b) { b.addEventListener('click', function () { selectDocument(parseInt(b.dataset.view, 10)); }); });
    document.querySelectorAll('[data-del]').forEach(function (b) { b.addEventListener('click', function () { openDelete(parseInt(b.dataset.del, 10), b.dataset.name); }); });
  }

  function renderDocsPagination() {
    var p = state.docsPageData;
    var container = document.getElementById('docs-pagination');
    if (!p || p.totalPages <= 1) { container.innerHTML = ''; return; }

    var current = p.page;
    var total = p.totalPages;
    var windowSize = 5;
    var windowStart = Math.max(0, Math.min(current - Math.floor(windowSize / 2), total - windowSize));
    var windowEnd = Math.min(total, windowStart + windowSize);

    function pageBtn(label, target, disabled) {
      return '<button class="btn-sm" data-page="' + target + '"' + (disabled ? ' disabled' : '') + '>' + label + '</button>';
    }

    var html = pageBtn('처음', 0, current === 0);
    html += pageBtn('이전', current - 1, current === 0);
    for (var i = windowStart; i < windowEnd; i++) {
      html += '<button class="btn-sm' + (i === current ? ' btn-sm--primary-tint' : '') + '" data-page="' + i + '">' + (i + 1) + '</button>';
    }
    html += pageBtn('다음', current + 1, current === total - 1);
    html += pageBtn('마지막', total - 1, current === total - 1);
    html += '<span class="page-jump-group">' +
      '<input type="number" min="1" max="' + total + '" id="page-jump-input" class="text-input" ' +
      'style="width:56px; padding:6px 8px; font-size:12px;" placeholder="' + (current + 1) + '">' +
      '<button class="btn-sm" id="page-jump-btn" type="button">이동</button>' +
      '</span>';

    container.innerHTML = html;
    document.querySelectorAll('[data-page]').forEach(function (b) {
      b.addEventListener('click', function () {
        var target = parseInt(b.dataset.page, 10);
        if (target < 0 || target > total - 1) return;
        state.docsPage = target;
        loadDocuments();
      });
    });

    function jumpToPage() {
      var input = document.getElementById('page-jump-input');
      var target = parseInt(input.value, 10);
      if (!target || target < 1 || target > total) { alert('1~' + total + ' 사이의 페이지 번호를 입력해주세요.'); return; }
      state.docsPage = target - 1;
      loadDocuments();
    }
    document.getElementById('page-jump-btn').addEventListener('click', jumpToPage);
    document.getElementById('page-jump-input').addEventListener('keydown', function (e) {
      if (e.key === 'Enter') jumpToPage();
    });
  }

  // ---- extract nav + detail ----
  function selectDocument(documentId) {
    state.selectedDocumentId = documentId;
    state.editMode = false;
    state.editingPages = null;
    state.pageIndex = 0;
    switchToExtractTab();
    Promise.all([
      api('/documents/' + documentId).then(function (detail) { state.documentMeta = detail.document; }),
      loadVersions(documentId)
    ]).then(function () {
      renderExtractNav();
      renderExtractDetail();
    }).catch(function (e) { alert(e.message); });
  }

  function loadVersions(documentId) {
    return api('/documents/' + documentId + '/extractions').then(function (versions) {
      state.versions = versions;
      state.selectedExtractionId = versions.length ? versions[0].extractionId : null;
      renderExtractDetail();
    });
  }

  function renderExtractNav() {
    var docs = (state.docsPageData && state.docsPageData.content) || [];
    var html = docs.map(function (d) {
      var selected = d.documentId === state.selectedDocumentId;
      var status = statusLabel(null, d.latestVersionStatus);
      return '<div class="extract-nav-item' + (selected ? ' is-active' : '') + '" data-select-doc="' + d.documentId + '">' +
        '<p class="extract-nav-item__name">' + esc(d.displayName) + '</p>' +
        '<span class="extract-nav-item__meta" style="color:' + status.color + ';">' + status.text + ' · ' + versionLabel(d.latestMajorVersion, d.latestMinorVersion) + '</span>' +
      '</div>';
    }).join('');
    document.getElementById('extract-nav').innerHTML = html;
    document.querySelectorAll('[data-select-doc]').forEach(function (b) { b.addEventListener('click', function () { selectDocument(parseInt(b.dataset.selectDoc, 10)); }); });
    syncDetailContentHeight();
  }

  function latestVersionedExtraction() {
    for (var i = 0; i < state.versions.length; i++) {
      if (state.versions[i].majorVersion != null) return state.versions[i];
    }
    return null;
  }

  // 페이지가 1장뿐이라 이전/다음이 필요 없을 때도 같은 높이를 차지하는 빈 슬롯을 반환 (본문 높이를 항상 동일하게 유지하기 위함)
  function renderPageNav(pageCount, label) {
    if (pageCount <= 1) return '<div class="extract-page-nav"></div>';
    var isFirst = state.pageIndex === 0;
    var isLast = state.pageIndex === pageCount - 1;
    return '<div class="extract-page-nav">' +
      '<button class="btn-sm" id="page-first-btn"' + (isFirst ? ' disabled' : '') + '>처음</button>' +
      '<button class="btn-sm" id="page-prev-btn"' + (isFirst ? ' disabled' : '') + '>이전</button>' +
      '<span style="font-size:12.5px; color:#8A93A3; display:inline-flex; align-items:center; gap:5px;">' +
        '<input type="number" class="text-input" id="page-jump-input" min="1" max="' + pageCount + '" value="' + (state.pageIndex + 1) + '" style="width:44px; padding:4px 6px; font-size:12.5px; text-align:center;">' +
        ' / ' + pageCount + label +
      '</span>' +
      '<button class="btn-sm" id="page-next-btn"' + (isLast ? ' disabled' : '') + '>다음</button>' +
      '<button class="btn-sm" id="page-last-btn"' + (isLast ? ' disabled' : '') + '>마지막</button>' +
      '</div>';
  }

  function renderExtractDetail() {
    var container = document.getElementById('extract-detail');
    if (!state.selectedDocumentId || !state.documentMeta) { container.innerHTML = '<p class="text-faint">왼쪽에서 자료를 선택해주세요</p>'; return; }

    var doc = state.documentMeta;
    var versions = state.versions;
    var extraction = versions.filter(function (v) { return v.extractionId === state.selectedExtractionId; })[0];
    if (!extraction) { container.innerHTML = '<p class="text-faint">아직 추출된 내용이 없어요. 잠시 후 다시 확인해주세요.</p>'; return; }

    var isFailed = extraction.extractionStatus === 'FAILED';
    var isLatest = versions.length > 0 && versions[0].extractionId === extraction.extractionId;
    var hasAnyVersion = versions.some(function (v) { return v.majorVersion != null; });
    var canConfirm = extraction.versionStatus === 'DRAFT' && isLatest;
    var canEdit = !isFailed;
    var status = statusLabel(extraction.extractionStatus, extraction.versionStatus);

    var html = '<div class="flex-row" style="justify-content:space-between; align-items:flex-start; gap:10px; flex-wrap:wrap; margin-bottom:8px;">' +
      '<div><p style="font-size:16px; font-weight:700; margin:0 0 4px;">' + esc(doc.displayName) + '</p>' +
      '<p style="font-size:12px; color:#8A93A3; margin:0;">' + CATEGORY_LABEL[doc.documentType] + ' · ' + doc.sourceType + '</p></div>' +
      '<div class="flex-row gap-8" style="flex-wrap:wrap;">' +
        '<span class="badge-pill" style="background:#F5F1FF; color:' + status.color + ';">' + status.text + '</span>' +
        '<select class="select-input" style="width:auto; padding:6px 10px; font-size:12.5px;" id="version-select">' +
          versions.map(function (v) {
            var s = statusLabel(v.extractionStatus, v.versionStatus);
            return '<option value="' + v.extractionId + '"' + (v.extractionId === extraction.extractionId ? ' selected' : '') + '>' +
              versionLabel(v.majorVersion, v.minorVersion) + ' · ' + s.text + '</option>';
          }).join('') +
        '</select>' +
      '</div></div>';

    if (isFailed) {
      html += '<div class="extract-fail"><p style="font-size:14px; font-weight:600; margin:0;">텍스트를 추출할 수 없습니다</p>' +
        '<p style="font-size:12.5px; color:#8A93A3; margin:0;">' + esc(extraction.failureReason || '파일을 다시 확인해주세요.') + '</p>' +
        (isLatest ? '<button class="btn btn--primary" id="retry-extract-btn">다시 추출 시도</button>' : '') +
        '</div>';
    } else if (state.editMode) {
      var editPages = state.editingPages;
      if (state.pageIndex >= editPages.length) state.pageIndex = 0;
      html += '<div class="extract-body"><div class="extract-body__content">' +
        '<textarea class="textarea-input page-viewer" id="content-editor" style="font-family:inherit;">' + esc(editPages[state.pageIndex]) + '</textarea>' +
        renderPageNav(editPages.length, '페이지 수정 중') +
        '</div><div class="extract-body__actions">' +
        '<button class="btn btn--ghost" style="border:1px solid #E3E7ED;" id="edit-cancel-btn">취소</button>' +
        '<button class="btn btn--primary" id="edit-save-btn">저장</button>' +
        '</div></div>';
    } else {
      var pages = splitPages(extraction.content);
      if (state.pageIndex >= pages.length) state.pageIndex = 0;
      html += '<div class="extract-body"><div class="extract-body__content">' +
        '<div class="page-viewer">' + esc(pages[state.pageIndex]).replace(/\n/g, '<br>') + '</div>' +
        renderPageNav(pages.length, '페이지') +
        '</div><div class="extract-body__actions">' +
        (canEdit ? '<button class="btn-sm btn-sm--primary-tint" id="enter-edit-btn">수정</button>' : '') +
        (canConfirm ? '<button class="btn btn--primary" id="confirm-btn">확정하기</button>' : '') +
        '</div></div>';
    }

    container.innerHTML = html;
    bindExtractDetailEvents(extraction, hasAnyVersion);
    syncDetailContentHeight();
  }

  // 왼쪽 자료 목록 카드의 실제 높이만큼 오른쪽 본문 박스 높이를 맞춘다
  function syncDetailContentHeight() {
    var nav = document.getElementById('extract-nav');
    var content = document.querySelector('#extract-detail .page-viewer');
    if (!nav || !content) return;
    content.style.height = Math.max(nav.offsetHeight, 200) + 'px';
  }

  function bindExtractDetailEvents(extraction, hasAnyVersion) {
    var versionSelect = document.getElementById('version-select');
    if (versionSelect) versionSelect.addEventListener('change', function () {
      state.selectedExtractionId = parseInt(versionSelect.value, 10);
      state.editMode = false;
      state.editingPages = null;
      state.pageIndex = 0;
      renderExtractDetail();
    });

    var pageCount = state.editMode ? state.editingPages.length : splitPages(extraction.content).length;

    var firstBtn = document.getElementById('page-first-btn');
    if (firstBtn) firstBtn.addEventListener('click', function () {
      captureCurrentEditingPage();
      state.pageIndex = 0;
      renderExtractDetail();
    });
    var prevBtn = document.getElementById('page-prev-btn');
    if (prevBtn) prevBtn.addEventListener('click', function () {
      captureCurrentEditingPage();
      state.pageIndex--;
      renderExtractDetail();
    });
    var nextBtn = document.getElementById('page-next-btn');
    if (nextBtn) nextBtn.addEventListener('click', function () {
      captureCurrentEditingPage();
      state.pageIndex++;
      renderExtractDetail();
    });
    var lastBtn = document.getElementById('page-last-btn');
    if (lastBtn) lastBtn.addEventListener('click', function () {
      captureCurrentEditingPage();
      state.pageIndex = pageCount - 1;
      renderExtractDetail();
    });
    var jumpInput = document.getElementById('page-jump-input');
    if (jumpInput) jumpInput.addEventListener('keydown', function (e) {
      if (e.key !== 'Enter') return;
      var target = parseInt(jumpInput.value, 10);
      if (!target || target < 1 || target > pageCount) { alert('1~' + pageCount + ' 사이의 페이지 번호를 입력해주세요.'); return; }
      captureCurrentEditingPage();
      state.pageIndex = target - 1;
      renderExtractDetail();
    });

    var retryBtn = document.getElementById('retry-extract-btn');
    if (retryBtn) retryBtn.addEventListener('click', function () {
      api('/documents/' + state.selectedDocumentId + '/extractions', { method: 'POST' })
        .then(function () { pollExtraction(state.selectedDocumentId); })
        .catch(function (e) { alert(e.message); });
    });

    var enterEdit = document.getElementById('enter-edit-btn');
    if (enterEdit) enterEdit.addEventListener('click', function () {
      state.editingPages = splitPages(extraction.content);
      state.editingHasMarkers = hasPageMarkers(extraction.content);
      state.editMode = true;
      renderExtractDetail();
    });
    var editCancel = document.getElementById('edit-cancel-btn');
    if (editCancel) editCancel.addEventListener('click', function () {
      state.editMode = false;
      state.editingPages = null;
      state.pageIndex = 0;
      renderExtractDetail();
    });
    var editSave = document.getElementById('edit-save-btn');
    if (editSave) editSave.addEventListener('click', function () {
      captureCurrentEditingPage();
      var content = joinPages(state.editingPages, state.editingHasMarkers).trim();
      if (!content) { alert('내용을 입력해주세요.'); return; }
      if (hasAnyVersion) {
        openScale(function (changeType) { submitEdit(extraction.extractionId, content, changeType); });
      } else {
        submitEdit(extraction.extractionId, content, null);
      }
    });

    var confirmBtn = document.getElementById('confirm-btn');
    if (confirmBtn) confirmBtn.addEventListener('click', function () {
      api('/document-extractions/' + extraction.extractionId + '/confirm', { method: 'POST' })
        .then(function () { loadDocuments(); loadVersions(state.selectedDocumentId); })
        .catch(function (e) { alert(e.message); });
    });
  }

  // 수정 중인 페이지 textarea의 현재 내용을 state.editingPages에 반영 (페이지 이동/저장 직전에 호출)
  function captureCurrentEditingPage() {
    var editor = document.getElementById('content-editor');
    if (editor && state.editingPages) state.editingPages[state.pageIndex] = editor.value;
  }

  function submitEdit(baseExtractionId, content, changeType) {
    var payload = { content: content };
    if (changeType) payload.changeType = changeType;
    api('/document-extractions/' + baseExtractionId + '/versions', { method: 'POST', json: payload })
      .then(function () {
        state.editMode = false;
        state.editingPages = null;
        state.pageIndex = 0;
        loadDocuments();
        loadVersions(state.selectedDocumentId);
      }).catch(function (e) { alert(e.message); });
  }

  // 업로드 직후 비동기 추출이 끝날 때까지 짧게 폴링
  function pollExtraction(documentId, attempt) {
    attempt = attempt || 0;
    if (attempt > 20) return;
    api('/documents/' + documentId + '/extractions').then(function (versions) {
      if (versions.length > 0) {
        loadDocuments();
        if (state.selectedDocumentId === documentId) loadVersions(documentId);
      } else {
        setTimeout(function () { pollExtraction(documentId, attempt + 1); }, 1000);
      }
    });
  }

  // ---- register modal ----
  function openRegisterModal() {
    state.registerCategory = null;
    state.registerMethod = 'file';
    renderRegisterCategoryGrid();
    document.getElementById('register-name').value = '';
    document.getElementById('register-file').value = '';
    document.getElementById('register-content').value = '';
    document.getElementById('register-keep-original').checked = false;
    document.querySelectorAll('#register-modal [data-method]').forEach(function (b) { b.classList.toggle('is-active', b.dataset.method === 'file'); });
    document.querySelector('[data-method-panel="file"]').hidden = false;
    document.querySelector('[data-method-panel="text"]').hidden = true;
    updateRegisterMethodVisibility();
    document.getElementById('register-modal').hidden = false;
  }

  function renderRegisterCategoryGrid() {
    document.getElementById('register-category-grid').innerHTML = CATEGORIES.map(function (c) {
      return '<button class="reg-cat-btn' + (state.registerCategory === c.id ? ' is-active' : '') + '" data-reg-cat="' + c.id + '">' + c.label + '</button>';
    }).join('');
    document.querySelectorAll('[data-reg-cat]').forEach(function (b) {
      b.addEventListener('click', function () {
        state.registerCategory = b.dataset.regCat;
        renderRegisterCategoryGrid();
        updateRegisterMethodVisibility();
      });
    });
  }

  // 직접 입력을 지원하지 않는 자료 유형이면 "직접 입력" 탭 자체를 숨긴다
  function updateRegisterMethodVisibility() {
    var textTabBtn = document.querySelector('#register-modal [data-method="text"]');
    var allowed = !!state.registerCategory && DIRECT_INPUT_ALLOWED.indexOf(state.registerCategory) !== -1;
    textTabBtn.hidden = !allowed;
    if (!allowed && state.registerMethod === 'text') {
      state.registerMethod = 'file';
      document.querySelectorAll('#register-modal [data-method]').forEach(function (x) { x.classList.toggle('is-active', x.dataset.method === 'file'); });
      document.querySelector('[data-method-panel="file"]').hidden = false;
      document.querySelector('[data-method-panel="text"]').hidden = true;
    }
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
      });
    });

    document.getElementById('register-submit').addEventListener('click', function () {
      var category = state.registerCategory;
      var name = document.getElementById('register-name').value.trim();
      if (!category || !name) { alert('자료 종류와 자료명을 입력해주세요.'); return; }

      if (state.registerMethod === 'file') {
        var file = document.getElementById('register-file').files[0];
        if (!file) { alert('파일을 선택해주세요.'); return; }
        var keepOriginal = document.getElementById('register-keep-original').checked;
        var fd = new FormData();
        fd.append('file', file);
        fd.append('documentType', category);
        fd.append('displayName', name);
        fd.append('keepOriginal', keepOriginal);
        api('/documents', { method: 'POST', formData: fd }).then(function (doc) {
          document.getElementById('register-modal').hidden = true;
          state.docsPage = 0;
          loadDocuments();
          return api('/documents/' + doc.documentId + '/extractions', { method: 'POST' }).then(function () {
            selectDocument(doc.documentId);
            pollExtraction(doc.documentId);
          });
        }).catch(function (e) { alert(e.message); });
      } else {
        if (DIRECT_INPUT_ALLOWED.indexOf(category) === -1) { alert('이 자료 유형은 직접 입력을 지원하지 않아요.'); return; }
        var content = document.getElementById('register-content').value.trim();
        if (!content) { alert('내용을 입력해주세요.'); return; }
        api('/documents/text', { method: 'POST', json: { documentType: category, content: content, displayName: name } })
          .then(function (extraction) {
            document.getElementById('register-modal').hidden = true;
            state.docsPage = 0;
            loadDocuments().then(function () { selectDocument(extraction.documentId); });
          }).catch(function (e) { alert(e.message); });
      }
    });
  }

  // ---- scale modal ----
  function openScale(action) {
    var latest = latestVersionedExtraction();
    var major = latest ? latest.majorVersion : 1;
    var minor = latest ? latest.minorVersion : 0;
    state.scaleAction = action;
    state.scaleScale = 'minor';
    document.getElementById('scale-current-version').textContent = 'v' + major + '.' + minor;
    document.getElementById('scale-minor-preview').textContent = major + '.' + (minor + 1);
    document.getElementById('scale-major-preview').textContent = (major + 1) + '.0';
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
      if (state.scaleAction) state.scaleAction(state.scaleScale.toUpperCase());
    });
  }

  // ---- delete modal ----
  function openDelete(docId, name) {
    state.deleteDocId = docId;
    document.getElementById('delete-doc-name').textContent = name || '';
    document.getElementById('delete-modal').hidden = false;
  }
  function bindDeleteModal() {
    document.getElementById('delete-cancel').addEventListener('click', function () { document.getElementById('delete-modal').hidden = true; });
    document.getElementById('delete-confirm').addEventListener('click', function () {
      var id = state.deleteDocId;
      api('/documents/' + id, { method: 'DELETE' }).then(function () {
        document.getElementById('delete-modal').hidden = true;
        if (state.selectedDocumentId === id) { state.selectedDocumentId = null; state.documentMeta = null; state.versions = []; }
        loadDocuments();
      }).catch(function (e) { alert(e.message); document.getElementById('delete-modal').hidden = true; });
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    renderTabs();
    renderChips();
    bindRegisterModal();
    bindScaleModal();
    bindDeleteModal();
    loadDocuments();
    document.getElementById('extract-detail').innerHTML = '<p class="text-faint">왼쪽 자료 목록에서 확인할 자료를 선택해주세요</p>';
  });
})();