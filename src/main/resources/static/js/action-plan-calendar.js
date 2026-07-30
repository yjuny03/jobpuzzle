(function () {
  'use strict';

  // 날짜를 시간대 변환 없는 YYYY-MM-DD 키로 만든다.
  function dateKey(year, month, day) {
    return [year, String(month + 1).padStart(2, '0'), String(day).padStart(2, '0')].join('-');
  }

  // 동적 문구를 안전하게 출력한다.
  function escapeHtml(value) {
    return String(value == null ? '' : value)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  // 액션플랜 날짜 표시용 월간 달력을 생성한다.
  function ActionPlanCalendar(root, options) {
    this.root = root;
    this.options = options || {};
    this.items = [];
    this.viewDate = new Date();
    this.viewDate.setDate(1);
    this.selectedDate = null;
    this.bindEvents();
  }

  // 달력 내부의 월 이동·선택·과제 클릭 이벤트를 한 번만 위임 연결한다.
  ActionPlanCalendar.prototype.bindEvents = function () {
    var self = this;
    this.root.addEventListener('click', function (event) {
      var move = event.target.closest('[data-calendar-move]');
      if (move) {
        self.viewDate.setMonth(self.viewDate.getMonth() + Number(move.dataset.calendarMove));
        self.selectedDate = null;
        self.render();
        return;
      }
      if (event.target.closest('[data-calendar-today]')) {
        self.viewDate = new Date();
        self.viewDate.setDate(1);
        self.selectedDate = dateKey(new Date().getFullYear(), new Date().getMonth(), new Date().getDate());
        self.render();
        return;
      }
      var day = event.target.closest('[data-calendar-date]');
      if (day) {
        self.selectedDate = day.dataset.calendarDate;
        self.render();
        return;
      }
      var item = event.target.closest('[data-calendar-item]');
      if (item && typeof self.options.onItemClick === 'function') {
        self.options.onItemClick(item.dataset.calendarItem);
      }
    });
    this.root.addEventListener('change', function (event) {
      if (!event.target.matches('[data-calendar-year], [data-calendar-month]')) return;
      var year = Number(self.root.querySelector('[data-calendar-year]').value);
      var month = Number(self.root.querySelector('[data-calendar-month]').value);
      self.viewDate = new Date(year, month, 1);
      self.selectedDate = null;
      self.render();
    });
  };

  // 새 과제 데이터로 달력과 선택 날짜 상세를 갱신한다.
  ActionPlanCalendar.prototype.setItems = function (items) {
    this.items = Array.isArray(items) ? items.filter(function (item) { return item.date; }) : [];
    this.render();
  };

  // 과제가 있는 연도를 포함하도록 연도 선택 범위를 계산한다.
  ActionPlanCalendar.prototype.yearOptions = function () {
    var current = new Date().getFullYear();
    var years = this.items.map(function (item) { return Number(String(item.date).slice(0, 4)); }).filter(Number.isFinite);
    var min = Math.min.apply(null, [current - 3].concat(years));
    var max = Math.max.apply(null, [current + 3].concat(years));
    var html = '';
    for (var year = min; year <= max; year++) {
      html += '<option value="' + year + '"' + (year === this.viewDate.getFullYear() ? ' selected' : '') + '>' + year + '년</option>';
    }
    return html;
  };

  // 선택한 날짜의 과제를 달력 아래 상세 목록으로 표시한다.
  ActionPlanCalendar.prototype.detailHtml = function (itemsByDate) {
    if (!this.selectedDate) {
      return '<div class="ap-calendar__empty">과제가 표시된 날짜를 선택하면 내용을 확인할 수 있어요.</div>';
    }
    var items = itemsByDate[this.selectedDate] || [];
    var heading = this.selectedDate.replace(/-/g, '.');
    if (!items.length) {
      return '<div class="ap-calendar__detail"><strong>' + heading + '</strong><p>이 날짜에 예정된 과제가 없습니다.</p></div>';
    }
    return '<div class="ap-calendar__detail"><strong>' + heading + ' · ' + items.length + '개 과제</strong><div class="ap-calendar__detail-list">' +
      items.map(function (item) {
        return '<button type="button" class="ap-calendar__detail-item" data-calendar-item="' + escapeHtml(item.id) + '">' +
          '<span class="ap-calendar__status ap-calendar__status--' + escapeHtml(item.tone || 'normal') + '">' + escapeHtml(item.statusLabel || '진행 중') + '</span>' +
          '<span><b>' + escapeHtml(item.title) + '</b><small>' + escapeHtml(item.description || '') + '</small></span>' +
        '</button>';
      }).join('') + '</div></div>';
  };

  // 현재 연도·월의 6주 달력과 과제 표시를 렌더링한다.
  ActionPlanCalendar.prototype.render = function () {
    var year = this.viewDate.getFullYear();
    var month = this.viewDate.getMonth();
    var today = new Date();
    var todayKey = dateKey(today.getFullYear(), today.getMonth(), today.getDate());
    var itemsByDate = {};
    this.items.forEach(function (item) {
      (itemsByDate[item.date] || (itemsByDate[item.date] = [])).push(item);
    });
    var monthOptions = '';
    for (var m = 0; m < 12; m++) {
      monthOptions += '<option value="' + m + '"' + (m === month ? ' selected' : '') + '>' + (m + 1) + '월</option>';
    }

    var first = new Date(year, month, 1);
    var gridStart = new Date(year, month, 1 - first.getDay());
    var cells = '';
    for (var index = 0; index < 42; index++) {
      var cellDate = new Date(gridStart.getFullYear(), gridStart.getMonth(), gridStart.getDate() + index);
      var key = dateKey(cellDate.getFullYear(), cellDate.getMonth(), cellDate.getDate());
      var tasks = itemsByDate[key] || [];
      // 해당 날짜의 모든 과제 상태를 점으로 표시하고 전체 건수도 함께 안내한다.
      var tones = tasks.map(function (item) {
        return '<i class="ap-calendar__dot ap-calendar__dot--' + escapeHtml(item.tone || 'normal') + '"></i>';
      }).join('');
      cells += '<button type="button" class="ap-calendar__day' +
        (cellDate.getMonth() !== month ? ' is-outside' : '') +
        (key === todayKey ? ' is-today' : '') +
        (key === this.selectedDate ? ' is-selected' : '') +
        (tasks.length ? ' has-items' : '') +
        '" data-calendar-date="' + key + '">' +
        '<span>' + cellDate.getDate() + '</span>' +
        (tasks.length ? '<span class="ap-calendar__markers">' + tones + '<b>' + tasks.length + '</b></span>' : '') +
      '</button>';
    }

    this.root.innerHTML =
      '<div class="ap-calendar__toolbar">' +
        '<div class="ap-calendar__selectors"><select data-calendar-year aria-label="연도 선택">' + this.yearOptions() + '</select>' +
        '<select data-calendar-month aria-label="월 선택">' + monthOptions + '</select></div>' +
        '<div class="ap-calendar__moves"><button type="button" data-calendar-today>오늘</button>' +
        '<button type="button" data-calendar-move="-1" aria-label="이전 달">‹</button>' +
        '<button type="button" data-calendar-move="1" aria-label="다음 달">›</button></div>' +
      '</div>' +
      '<div class="ap-calendar__weekdays">' + ['일', '월', '화', '수', '목', '금', '토'].map(function (day) { return '<span>' + day + '</span>'; }).join('') + '</div>' +
      '<div class="ap-calendar__grid">' + cells + '</div>' +
      this.detailHtml(itemsByDate);
  };

  window.ActionPlanCalendar = ActionPlanCalendar;
})();
