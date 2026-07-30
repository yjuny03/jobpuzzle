// admin-ai-logs.js — 관리자 AI 분석 오류 로그 조회 (/jobpuzzle/admin-api/ai-call-logs)
(function () {
  'use strict';

  function api(path) {
    return fetch(window.JobPuzzleRoutes.path(path.replace(/^\/admin/, '/admin-api')), { credentials: 'same-origin' }).then(function (res) {
      return res.json().then(function (body) {
        if (!res.ok || !body.success) {
          throw new Error((body && body.message) || '요청에 실패했습니다.');
        }
        return body.data;
      });
    });
  }

  var STATUS_BADGE = {
    PENDING: { bg: '#F6F8FB', color: '#5B6370', label: '대기' },
    RUNNING: { bg: '#EAF2FB', color: '#185FA5', label: '실행중' },
    SUCCEEDED: { bg: '#EAF7EF', color: '#1E8E5A', label: '성공' },
    FAILED: { bg: '#FBEEEC', color: '#B5433D', label: '실패' }
  };

  function pill(label, bg, color) {
    return '<span class="badge-pill" style="background:' + bg + '; color:' + color + ';">' + esc(label) + '</span>';
  }

  var state = { page: 0, status: '', pageData: null };

  function load() {
    var qs = 'page=' + state.page + '&size=10';
    if (state.status) qs += '&status=' + state.status;
    return api('/admin/ai-call-logs?' + qs).then(function (page) {
      state.pageData = page;
      renderRows();
      renderPagination();
    }).catch(function (e) { alert(e.message); });
  }

  function renderRows() {
    var rows = (state.pageData && state.pageData.content || []).map(function (r) {
      var status = STATUS_BADGE[r.status] || { bg: '#F6F8FB', color: '#5B6370', label: r.status };
      var errorText = r.errorType && r.errorType !== 'NONE'
        ? esc(r.errorType) + (r.errorMessage ? ' · ' + esc(r.errorMessage) : '')
        : '-';
      return '<div class="table-row" style="grid-template-columns:0.9fr 1.2fr 1fr 1fr 1.6fr 0.9fr 1.1fr;">' +
        '<span>' + pill(status.label, status.bg, status.color) + '</span>' +
        '<span style="font-size:12.5px; color:#5B6370;">' + esc(r.executionStage) + '</span>' +
        '<span style="font-size:12.5px; color:#5B6370;">' + esc(r.provider) + ' / ' + esc(r.model) + '</span>' +
        '<span style="font-size:12px; color:#8A93A3;">' + esc(r.inputReferenceType) + ' #' + esc(r.inputReferenceId) + '</span>' +
        '<span style="font-size:12px; color:' + (r.errorType && r.errorType !== 'NONE' ? '#B5433D' : '#8A93A3') + ';" title="' + errorText + '">' + errorText + '</span>' +
        '<span style="font-size:12.5px; color:#8A93A3;">' + r.retryCount + (r.reused ? ' · 재사용' : '') + '</span>' +
        '<span style="font-size:12.5px; color:#8A93A3;">' + (r.startedAt || '').replace('T', ' ').slice(0, 16) + '</span>' +
      '</div>';
    }).join('');
    document.getElementById('ai-log-rows').innerHTML = rows || '<div class="table-empty">조회된 로그가 없어요</div>';
  }

  function renderPagination() {
    var p = state.pageData;
    var container = document.getElementById('ai-log-pagination');
    if (!p || p.totalPages <= 1) { container.innerHTML = ''; return; }

    var current = p.page;
    var total = p.totalPages;
    var windowSize = 5;
    var windowStart = Math.max(0, Math.min(current - Math.floor(windowSize / 2), total - windowSize));
    var windowEnd = Math.min(total, windowStart + windowSize);

    function pageBtn(label, target, disabled) {
      return '<button class="btn-sm" data-page="' + target + '"' + (disabled ? ' disabled' : '') + '>' + label + '</button>';
    }

    var html = pageBtn('처음', 0, current === 0) + pageBtn('이전', current - 1, current === 0);
    for (var i = windowStart; i < windowEnd; i++) {
      html += '<button class="btn-sm' + (i === current ? ' btn-sm--primary-tint' : '') + '" data-page="' + i + '">' + (i + 1) + '</button>';
    }
    html += pageBtn('다음', current + 1, current === total - 1) + pageBtn('마지막', total - 1, current === total - 1);

    container.innerHTML = html;
    document.querySelectorAll('[data-page]').forEach(function (b) {
      b.addEventListener('click', function () {
        var target = parseInt(b.dataset.page, 10);
        if (target < 0 || target > total - 1) return;
        state.page = target;
        load();
      });
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    if (!document.getElementById('ai-log-rows')) return;
    document.getElementById('ai-log-status-filter').addEventListener('change', function (e) {
      state.status = e.target.value;
      state.page = 0;
      load();
    });
    load();
  });
})();
