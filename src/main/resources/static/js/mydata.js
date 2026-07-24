// mydata.js — 지원 자료 페이지: 실제 백엔드 API(JSON-00) 연동. 등록/추출/페이지별 보기·수정/확정 로직은 document-flow.js 공용 모듈을 사용한다.
(function () {
  'use strict';

  var DF = window.DocumentFlow;
  var api = DF.api;
  var CATEGORY_LABEL = DF.CATEGORY_LABEL;
  var CATEGORIES = Object.keys(CATEGORY_LABEL).map(function (id) { return { id: id, label: CATEGORY_LABEL[id] }; });
  var DIRECT_INPUT_ALLOWED = DF.DIRECT_INPUT_ALLOWED;
  var versionLabel = DF.versionLabel;
  var statusLabel = DF.statusLabel;

  var state = {
    tab: 'docs',
    categoryFilter: 'all',
    docsPage: 0,
    docsPageData: null,
    selectedDocumentId: null,
    registerCategory: null,
    registerMethod: 'file',
    scaleAction: null,
    scaleScale: 'minor',
    deleteDocId: null
  };

  var extractPanel = DF.createExtractPanel(document.getElementById('extract-detail'), {
    onConfirmed: function () { loadDocuments(); },
    openScaleModal: openScale
  });

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
    switchToExtractTab();
    renderExtractNav();
    extractPanel.load(documentId).catch(function (e) { alert(e.message); });
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
  }

  // ---- register modal ----
  function openRegisterModal() {
    state.registerCategory = null;
    state.registerMethod = 'file';
    renderRegisterCategoryGrid();
    document.getElementById('register-name').value = '';
    document.getElementById('register-file').value = '';
    document.getElementById('register-file-list').textContent = '';
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
    document.getElementById('register-file').addEventListener('change', function () {
      document.getElementById('register-file-list').textContent = DF.describeSelectedFiles(this.files);
    });
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
        var files = document.getElementById('register-file').files;
        if (!files.length) { alert('파일을 선택해주세요.'); return; }
        var keepOriginal = document.getElementById('register-keep-original').checked;
        DF.registerDocument({ method: 'file', category: category, name: name, files: files, keepOriginal: keepOriginal })
          .then(function (doc) {
            document.getElementById('register-modal').hidden = true;
            state.docsPage = 0;
            loadDocuments();
            selectDocument(doc.documentId);
            DF.pollExtraction(doc.documentId, function () {
              loadDocuments();
              if (state.selectedDocumentId === doc.documentId) extractPanel.reload();
            });
          }).catch(function (e) { alert(e.message); });
      } else {
        var content = document.getElementById('register-content').value.trim();
        if (!content) { alert('내용을 입력해주세요.'); return; }
        DF.registerDocument({ method: 'text', category: category, name: name, content: content })
          .then(function (doc) {
            document.getElementById('register-modal').hidden = true;
            state.docsPage = 0;
            loadDocuments().then(function () { selectDocument(doc.documentId); });
          }).catch(function (e) { alert(e.message); });
      }
    });
  }

  // ---- scale modal ----
  function openScale(action) {
    api('/documents/' + state.selectedDocumentId + '/extractions').then(function (versions) {
      var latest = versions.filter(function (v) { return v.majorVersion != null; })[0] || null;
      var major = latest ? latest.majorVersion : 1;
      var minor = latest ? latest.minorVersion : 0;
      state.scaleAction = action;
      state.scaleScale = 'minor';
      document.getElementById('scale-current-version').textContent = 'v' + major + '.' + minor;
      document.getElementById('scale-minor-preview').textContent = major + '.' + (minor + 1);
      document.getElementById('scale-major-preview').textContent = (major + 1) + '.0';
      document.querySelectorAll('.scale-option').forEach(function (o) { o.classList.toggle('is-active', o.dataset.scale === 'minor'); });
      document.getElementById('scale-modal').hidden = false;
    });
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
        if (state.selectedDocumentId === id) { state.selectedDocumentId = null; extractPanel.clear(); }
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