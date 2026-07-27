// admin-guide-detail.js — 관리자 가이드 상세 페이지 (/admin/guides/{id})
(function () {
  'use strict';

  function api(path) {
    return fetch('/api' + path, { credentials: 'same-origin' }).then(function (res) {
      return res.json().then(function (body) {
        if (!res.ok || !body.success) {
          throw new Error((body && body.message) || '요청에 실패했습니다.');
        }
        return body.data;
      });
    });
  }

  var STATUS_BADGE = {
    DRAFT: { bg: '#F6F8FB', color: '#5B6370', label: '임시저장' },
    ACTIVE: { bg: '#EAF7EF', color: '#1E8E5A', label: '사용중' },
    INACTIVE: { bg: '#FBEEEC', color: '#B5433D', label: '비활성' }
  };
  var SCOPE_LABEL = {
    CATEGORY: '특정 직무분류',
    PARENT_CATEGORY: '상위 대분류',
    GLOBAL_COMMON: '전체 공통'
  };

  function pill(label, bg, color) {
    return '<span class="badge-pill" style="background:' + bg + '; color:' + color + ';">' + esc(label) + '</span>';
  }

  function detailField(label, value) {
    return '<div><p class="field-label" style="margin-bottom:4px;">' + esc(label) + '</p>' +
      '<p style="font-size:13.5px; margin:0; white-space:pre-line;">' + (value || '<span style="color:#8A93A3;">-</span>') + '</p></div>';
  }

  function detailListField(label, items) {
    var content = (items && items.length)
      ? '<ul style="margin:0; padding-left:18px;">' + items.map(function (i) { return '<li style="font-size:13.5px;">' + esc(i) + '</li>'; }).join('') + '</ul>'
      : '<p style="font-size:13.5px; margin:0; color:#8A93A3;">-</p>';
    return '<div><p class="field-label" style="margin-bottom:4px;">' + esc(label) + '</p>' + content + '</div>';
  }

  function guideIdFromUrl() {
    var parts = window.location.pathname.split('/').filter(Boolean);
    return parseInt(parts[parts.length - 1], 10);
  }

  function render(g) {
    var status = STATUS_BADGE[g.status] || { bg: '#F6F8FB', color: '#5B6370', label: g.status };
    var scope = SCOPE_LABEL[g.scopeType] || g.scopeType;
    var scopeDetail = g.scopeType === 'CATEGORY'
      ? (g.mainCategory ? ' (' + esc(g.mainCategory) + ' · ' + esc(g.subCategory) + ' · ' + esc(g.careerLevel || '') + ')' : '')
      : (g.scopeType === 'PARENT_CATEGORY' ? ' (' + esc(g.scopeMainCategory || '') + ')' : '');

    document.getElementById('guide-detail-title').innerHTML =
      esc(g.title) + ' ' + pill(status.label, status.bg, status.color);

    document.getElementById('guide-detail-body').innerHTML =
      detailField('가이드 코드 / 버전', esc(g.guideCode) + ' / ' + esc(g.version)) +
      detailField('적용 범위', esc(scope) + scopeDetail) +
      detailField('적용 범위 설명', esc(g.applicableScope)) +
      detailListField('평가 중점', g.evaluationFocus) +
      detailListField('근거 판단 기준', g.evidenceRules) +
      detailListField('질문 방향', g.questionDirection) +
      detailListField('피해야 할 질문', g.avoidQuestions) +
      detailField('자료 유형', esc(g.sourceType) + (g.hasFile ? ' (첨부파일 있음)' : '')) +
      detailField('등록자 / 등록일', esc(g.createdByLoginId) + ' / ' + (g.createdAt || '').slice(0, 10));
  }

  document.addEventListener('DOMContentLoaded', function () {
    if (!document.getElementById('guide-detail-body')) return;
    var guideId = guideIdFromUrl();
    if (!guideId) return;
    api('/admin/guides/' + guideId)
      .then(render)
      .catch(function (e) {
        document.getElementById('guide-detail-title').textContent = '가이드를 불러올 수 없어요';
        document.getElementById('guide-detail-body').innerHTML = '<p style="color:#B5433D;">' + esc(e.message) + '</p>';
      });
  });
})();