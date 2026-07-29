// document-flow.js — 자료 등록·추출·페이지별 보기/수정/확정 공용 로직 (my-data.html, interview.html 공용)
(function (global) {
  'use strict';

  var API_BASE = '/api';

  var CATEGORY_LABEL = {
    JOB_POSTING: '채용공고', COMPANY_INFO: '회사정보', RESUME: '이력서',
    COVER_LETTER: '자기소개서', PORTFOLIO: '포트폴리오', EXPERIENCE_NOTE: '경험 자료'
  };
  var DIRECT_INPUT_ALLOWED = ['JOB_POSTING', 'COMPANY_INFO', 'EXPERIENCE_NOTE'];

  function notify(type, message, duration) {
    if (typeof global.showToast === 'function') {
      return global.showToast(type, message, duration);
    }
    global.alert(message);
    return null;
  }

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

  function joinPages(pages, withMarkers) {
    if (!withMarkers) return pages[0] || '';
    return pages.map(function (text, idx) { return '[' + (idx + 1) + '페이지]\n' + text; }).join('\n\n');
  }

  // 선택된 파일 목록을 안내 텍스트로 요약 (이미지 여러 장 선택 시 파일명 나열)
  function describeSelectedFiles(files) {
    if (!files || files.length === 0) return '';
    if (files.length === 1) return '선택됨: ' + files[0].name;
    return files.length + '개 파일 선택됨: ' + Array.prototype.map.call(files, function (f) { return f.name; }).join(', ');
  }

  // 파일 업로드(등록 + 자동 추출 트리거) 또는 직접 입력으로 자료 등록
  function registerDocument(opts) {
    var category = opts.category;
    var name = opts.name;
    if (opts.method === 'file') {
      var fd = new FormData();
      for (var i = 0; i < opts.files.length; i++) fd.append('files', opts.files[i]);
      fd.append('documentType', category);
      fd.append('displayName', name);
      fd.append('keepOriginal', !!opts.keepOriginal);
      return api('/documents', { method: 'POST', formData: fd }).then(function (doc) {
        return api('/documents/' + doc.documentId + '/extractions', { method: 'POST' }).then(function () {
          return doc;
        });
      });
    }
    if (DIRECT_INPUT_ALLOWED.indexOf(category) === -1) {
      return Promise.reject(new Error('이 자료 유형은 직접 입력을 지원하지 않아요.'));
    }
    return api('/documents/text', { method: 'POST', json: { documentType: category, content: opts.content, displayName: name } })
      .then(function (extraction) { return { documentId: extraction.documentId }; });
  }

  // 업로드 직후 비동기 추출이 끝날 때까지 짧게 폴링
  function pollExtraction(documentId, onReady, attempt) {
    attempt = attempt || 0;
    if (attempt > 20) return;
    api('/documents/' + documentId + '/extractions').then(function (versions) {
      if (versions.length > 0) {
        onReady(versions);
      } else {
        setTimeout(function () { pollExtraction(documentId, onReady, attempt + 1); }, 1000);
      }
    });
  }

  // 페이지 이동 슬롯 (처음/이전/직접입력/다음/마지막). pageIndex를 인자로 받는 순수 함수라 여러 인스턴스에서 공유 가능
  function renderPageNav(pageCount, pageIndex, label) {
    if (pageCount <= 1) return '<div class="extract-page-nav"></div>';
    var isFirst = pageIndex === 0;
    var isLast = pageIndex === pageCount - 1;
    return '<div class="extract-page-nav">' +
      '<button class="btn-sm" data-panel-action="first"' + (isFirst ? ' disabled' : '') + '>처음</button>' +
      '<button class="btn-sm" data-panel-action="prev"' + (isFirst ? ' disabled' : '') + '>이전</button>' +
      '<span style="font-size:12.5px; color:#8A93A3; display:inline-flex; align-items:center; gap:5px;">' +
        '<input type="number" class="text-input" data-panel-jump min="1" max="' + pageCount + '" value="' + (pageIndex + 1) + '" style="width:44px; padding:4px 6px; font-size:12.5px; text-align:center;">' +
        ' / ' + pageCount + label +
      '</span>' +
      '<button class="btn-sm" data-panel-action="next"' + (isLast ? ' disabled' : '') + '>다음</button>' +
      '<button class="btn-sm" data-panel-action="last"' + (isLast ? ' disabled' : '') + '>마지막</button>' +
      '</div>';
  }

  // 문서 하나를 대상으로 버전 선택 + 페이지별 보기/수정 + 확정을 담당하는 독립 위젯을 만든다.
  // 같은 화면에 여러 개를 동시에 띄워도 서로 상태가 섞이지 않도록 container 안에서만 querySelector로 동작한다.
  // options.onConfirmed(extraction) - 확정 성공 시 호출
  // options.onRendered(container) - 매 렌더링 후 호출 (레이아웃 후처리용)
  // options.openScaleModal(onPick) - 이미 확정 이력 있는 자료를 수정 저장할 때 자잘한/큰 수정 선택 모달. 안 넘기면 자잘한 수정으로 처리
  function createExtractPanel(container, options) {
    options = options || {};
    var onConfirmed = options.onConfirmed || function () {};
    var onRendered = options.onRendered || function () {};

    var state = {
      documentId: null, documentMeta: null, versions: [], selectedExtractionId: null,
      editMode: false, pageIndex: 0, editingPages: null, editingHasMarkers: false
    };

    function load(documentId) {
      state.documentId = documentId;
      state.editMode = false; state.editingPages = null; state.pageIndex = 0;
      return Promise.all([
        api('/documents/' + documentId).then(function (detail) { state.documentMeta = detail.document; }),
        loadVersions()
      ]).then(render);
    }

    function reload() {
      return loadVersions().then(render);
    }

    function loadVersions() {
      return api('/documents/' + state.documentId + '/extractions').then(function (versions) {
        state.versions = versions;
        state.selectedExtractionId = versions.length ? versions[0].extractionId : null;
      });
    }

    function clear() {
      state.documentId = null;
      container.innerHTML = '';
    }

    function render() {
      if (!state.documentId || !state.documentMeta) { container.innerHTML = '<p class="text-faint">불러올 자료가 없어요</p>'; return; }

      var doc = state.documentMeta;
      var versions = state.versions;
      var extraction = versions.filter(function (v) { return v.extractionId === state.selectedExtractionId; })[0];
      if (!extraction) { container.innerHTML = '<p class="text-faint">아직 추출된 내용이 없어요. 잠시 후 다시 확인해주세요.</p>'; onRendered(container); return; }

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
          '<select class="select-input" style="width:auto; padding:6px 10px; font-size:12.5px;" data-panel-el="version-select">' +
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
          (isLatest ? '<button class="btn btn--primary" data-panel-action="retry">다시 추출 시도</button>' : '') +
          '</div>';
      } else if (state.editMode) {
        var editPages = state.editingPages;
        if (state.pageIndex >= editPages.length) state.pageIndex = 0;
        html += '<div class="extract-body"><div class="extract-body__content">' +
          '<textarea class="textarea-input page-viewer" data-panel-el="content-editor" style="font-family:inherit;">' + esc(editPages[state.pageIndex]) + '</textarea>' +
          renderPageNav(editPages.length, state.pageIndex, '페이지 수정 중') +
          '</div><div class="extract-body__actions">' +
          '<button class="btn btn--ghost" style="border:1px solid #E3E7ED;" data-panel-action="edit-cancel">취소</button>' +
          '<button class="btn btn--primary" data-panel-action="edit-save">저장</button>' +
          '</div></div>';
      } else {
        var pages = splitPages(extraction.content);
        if (state.pageIndex >= pages.length) state.pageIndex = 0;
        html += '<div class="extract-body"><div class="extract-body__content">' +
          '<div class="page-viewer">' + esc(pages[state.pageIndex]).replace(/\n/g, '<br>') + '</div>' +
          renderPageNav(pages.length, state.pageIndex, '페이지') +
          '</div><div class="extract-body__actions">' +
          (canEdit ? '<button class="btn-sm btn-sm--primary-tint" data-panel-action="enter-edit">수정</button>' : '') +
          (canConfirm ? '<button class="btn btn--primary" data-panel-action="confirm">확정하기</button>' : '') +
          '</div></div>';
      }

      container.innerHTML = html;
      bindEvents(extraction, hasAnyVersion);
      onRendered(container);
    }

    function bindEvents(extraction, hasAnyVersion) {
      var versionSelect = container.querySelector('[data-panel-el="version-select"]');
      if (versionSelect) versionSelect.addEventListener('change', function () {
        state.selectedExtractionId = parseInt(versionSelect.value, 10);
        state.editMode = false; state.editingPages = null; state.pageIndex = 0;
        render();
      });

      var pageCount = state.editMode ? state.editingPages.length : splitPages(extraction.content).length;

      bindPanelAction('first', function () { captureCurrentEditingPage(); state.pageIndex = 0; render(); });
      bindPanelAction('prev', function () { captureCurrentEditingPage(); state.pageIndex--; render(); });
      bindPanelAction('next', function () { captureCurrentEditingPage(); state.pageIndex++; render(); });
      bindPanelAction('last', function () { captureCurrentEditingPage(); state.pageIndex = pageCount - 1; render(); });

      var jumpInput = container.querySelector('[data-panel-jump]');
      if (jumpInput) jumpInput.addEventListener('keydown', function (e) {
        if (e.key !== 'Enter') return;
        var target = parseInt(jumpInput.value, 10);
        if (!target || target < 1 || target > pageCount) { notify('warning', '1~' + pageCount + ' 사이의 페이지 번호를 입력해주세요.'); return; }
        captureCurrentEditingPage();
        state.pageIndex = target - 1;
        render();
      });

      bindPanelAction('retry', function () {
        var targetDocId = state.documentId;
        api('/documents/' + targetDocId + '/extractions', { method: 'POST' })
          .then(function () {
            notify('info', '자료 추출을 다시 시작했습니다.');
            pollExtraction(targetDocId, function () {
              if (state.documentId === targetDocId) reload();
            });
          })
          .catch(function (e) { notify('error', e.message); });
      });

      bindPanelAction('enter-edit', function () {
        state.editingPages = splitPages(extraction.content);
        state.editingHasMarkers = hasPageMarkers(extraction.content);
        state.editMode = true;
        render();
      });
      bindPanelAction('edit-cancel', function () {
        state.editMode = false; state.editingPages = null; state.pageIndex = 0;
        render();
      });
      bindPanelAction('edit-save', function () {
        captureCurrentEditingPage();
        var content = joinPages(state.editingPages, state.editingHasMarkers).trim();
        if (!content) { notify('warning', '내용을 입력해주세요.'); return; }
        if (hasAnyVersion && options.openScaleModal) {
          options.openScaleModal(function (changeType) { submitEdit(extraction.extractionId, content, changeType); });
        } else if (hasAnyVersion) {
          submitEdit(extraction.extractionId, content, 'MINOR');
        } else {
          submitEdit(extraction.extractionId, content, null);
        }
      });

      bindPanelAction('confirm', function () {
        api('/document-extractions/' + extraction.extractionId + '/confirm', { method: 'POST' })
          .then(function (confirmed) {
            return reload().then(function () {
              onConfirmed(confirmed);
              notify('success', '추출 결과를 확정했습니다.');
            });
          })
          .catch(function (e) { notify('error', e.message); });
      });

      function bindPanelAction(name, fn) {
        var el = container.querySelector('[data-panel-action="' + name + '"]');
        if (el) el.addEventListener('click', fn);
      }
    }

    function captureCurrentEditingPage() {
      var editor = container.querySelector('[data-panel-el="content-editor"]');
      if (editor && state.editingPages) state.editingPages[state.pageIndex] = editor.value;
    }

    function submitEdit(baseExtractionId, content, changeType) {
      var payload = { content: content };
      if (changeType) payload.changeType = changeType;
      return api('/document-extractions/' + baseExtractionId + '/versions', { method: 'POST', json: payload })
        .then(function () {
          state.editMode = false; state.editingPages = null; state.pageIndex = 0;
          return reload().then(function () {
            notify('success', '추출 내용을 저장했습니다.');
          });
        }).catch(function (e) { notify('error', e.message); });
    }

    return { load: load, reload: reload, clear: clear };
  }

  global.DocumentFlow = {
    api: api,
    CATEGORY_LABEL: CATEGORY_LABEL,
    DIRECT_INPUT_ALLOWED: DIRECT_INPUT_ALLOWED,
    versionLabel: versionLabel,
    statusLabel: statusLabel,
    splitPages: splitPages,
    hasPageMarkers: hasPageMarkers,
    joinPages: joinPages,
    describeSelectedFiles: describeSelectedFiles,
    registerDocument: registerDocument,
    pollExtraction: pollExtraction,
    createExtractPanel: createExtractPanel
  };
})(window);
