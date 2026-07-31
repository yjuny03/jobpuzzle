(function () {
  'use strict';

  // YYYY-MM-DD 문자열을 시간대 변환 없이 브라우저의 로컬 날짜 자정으로 해석한다.
  function parseLocalDate(value) {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value || '')) return null;
    var parts = value.split('-').map(Number);
    return new Date(parts[0], parts[1] - 1, parts[2]);
  }

  // 완료되지 않은 과제의 마감일까지 남은 날짜로 고정 경고 문구를 계산한다.
  function getNotice(deadline, status, today) {
    if (!deadline || status === 'DONE') return null;
    var due = parseLocalDate(deadline);
    if (!due) return null;
    var base = today ? new Date(today.getFullYear(), today.getMonth(), today.getDate()) : new Date();
    base.setHours(0, 0, 0, 0);
    var days = Math.round((due.getTime() - base.getTime()) / 86400000);
    if (days < 0) return { tone: 'danger', label: '기한 종료', days: days };
    if (days === 0) return { tone: 'danger', label: '오늘 마감', days: 0 };
    if (days <= 3) return { tone: 'warning', label: '마감 임박 · D-' + days, days: days };
    return null;
  }

  window.ActionPlanDeadline = Object.freeze({
    getNotice: getNotice
  });
})();
