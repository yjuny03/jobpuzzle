// dashboard.js — dummy-data driven interactions for the dashboard page

(function () {
  'use strict';

  var TODAY = '2026-07-13';

  var ASSIGNMENTS = [
    { id: 'a1', title: '장애 대응 경험 정리 과제', due: '2026-07-05', done: false, relatedTag: '운영·장애대응 경험 부족', desc: '서비스 운영 중 발생했던 장애 상황을 감지 → 대응 → 재발 방지 순서로 정리해보세요. 장애 발생 시점, 원인 파악 과정, 복구 조치, 이후 개선 사항을 구체적으로 작성하면 좋아요.' },
    { id: 'a2', title: '협업 커뮤니케이션 사례 작성', due: '2026-07-10', done: false, relatedTag: null, desc: '팀 프로젝트에서 의견 차이를 조율했던 경험을 상황 → 대화 방식 → 결과 순서로 정리해보세요. 갈등을 해결한 기준과 이후 협업 방식의 변화까지 담으면 좋아요.' },
    { id: 'a3', title: '성과 수치화 연습 과제', due: '2026-07-20', done: false, relatedTag: '성과 수치화 부족', desc: '최근 프로젝트에서 얻은 성과를 트래픽 처리량, 응답 속도, 오류율 등 구체적인 수치로 표현하는 연습을 해보세요.' },
    { id: 'a4', title: '데이터 시각화 학습 정리', due: null, done: false, relatedTag: '운영·장애대응 경험 부족', desc: '그라파나 같은 모니터링·시각화 도구를 학습하고, 어떤 지표를 시각화했는지, 왜 그 지표를 선택했는지 정리해보세요.' },
    { id: 'a5', title: '포트폴리오 보완 과제', due: null, done: false, relatedTag: null, desc: '담당 기능, 사용 기술, 문제 해결 과정을 포트폴리오에 구체적으로 추가해보세요.' }
  ];

  var els = {};
  var state = { editingDueId: null, detailId: null };

  function assignmentStatus(a) {
    if (a.done) return 'done';
    if (!a.due) return 'none';
    return a.due < TODAY ? 'overdue' : 'upcoming';
  }
  function statusLabel(status) {
    return { done: '완료됨', overdue: '마감 초과', upcoming: '진행 중', none: '마감일 미지정' }[status];
  }
  function statusClasses(status) {
    return {
      done: { bg: '#E6F4EC', color: '#1E7A4C' },
      overdue: { bg: '#FBEEEC', color: '#B5433D' },
      upcoming: { bg: '#EAF2FB', color: '#185FA5' },
      none: { bg: '#F0F2F5', color: '#8A93A3' }
    }[status];
  }
  function dueLabel(a) {
    return a.due ? '마감일 ' + a.due.slice(5).replace('-', '.') : '마감일 미지정';
  }

  function renderTabs() {
    bindTabGroup({
      btnSelector: '.tabbar__btn[data-tab]',
      datasetKey: 'tab',
      panelSelector: '.tab-panel',
      panelDatasetKey: 'panel'
    });

    bindTabGroup({
      btnSelector: '.tabbar__btn[data-taskview]',
      datasetKey: 'taskview',
      panelSelector: '.task-view',
      panelDatasetKey: 'taskviewPanel'
    });
  }

  function renderTodos() {
    var todos = [];
    todos.push({ icon: 'play', title: '진행 중인 면접 마무리하기', desc: '약점 보완 모의면접 · #성과 수치화 부족', cta: '이어서 진행하기', href: window.JobPuzzleRoutes.path('/interview') });
    todos.push({ icon: 'tag', title: '약점 태그 해결하기', desc: '#성과 수치화 부족 · #운영·장애대응 경험 부족 연습이 필요해요', cta: '약점 보완 연습하기', href: window.JobPuzzleRoutes.path('/interview') });

    var incomplete = ASSIGNMENTS.filter(function (a) { return !a.done; }).length;
    if (incomplete > 0) {
      todos.push({ icon: 'list', title: '미완료 과제 처리하기', desc: incomplete + '건의 과제가 남아있어요', cta: '과제 목록 보기', action: function () {
        document.querySelector('.tabbar__btn[data-tab="tasks"]').click();
      } });
    }

    var list = document.getElementById('todo-list');
    if (todos.length === 0) {
      list.innerHTML = '<p class="todo-empty">지금 처리할 일이 없어요. 잘하고 있어요!</p>';
      return;
    }
    var iconSvg = {
      play: '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="5 3 19 12 5 21 5 3"/></svg>',
      tag: '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/><circle cx="7" cy="7" r="1"/></svg>',
      list: '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>'
    };
    list.innerHTML = todos.map(function (t, idx) {
      return '<div class="todo-row">' +
        '<div class="todo-row__left">' +
          '<div class="todo-row__icon">' + iconSvg[t.icon] + '</div>' +
          '<div><p class="todo-row__title">' + t.title + '</p><p class="todo-row__desc">' + t.desc + '</p></div>' +
        '</div>' +
        (t.href ? '<a href="' + t.href + '" class="todo-row__cta" style="text-decoration:none; display:inline-block;">' + t.cta + '</a>' : '<button class="todo-row__cta" data-todo-idx="' + idx + '">' + t.cta + '</button>') +
      '</div>';
    }).join('');
    list.querySelectorAll('button[data-todo-idx]').forEach(function (btn) {
      var t = todos[parseInt(btn.dataset.todoIdx, 10)];
      btn.addEventListener('click', t.action);
    });
  }

  function taskRowActionsHtml(a, compact) {
    var status = assignmentStatus(a);
    var isEditing = state.editingDueId === a.id;
    var html = '';
    if (isEditing) {
      html += '<input type="date" class="task-date-input" value="' + (a.due || '') + '" data-due-input="' + a.id + '">';
      html += '<button class="btn-save-due" data-save-due="' + a.id + '">저장</button>';
    } else {
      if (status === 'none') html += '<button class="btn-sm" data-start-edit="' + a.id + '">마감일 지정</button>';
      if (status === 'overdue') html += '<button class="btn-sm" data-start-edit="' + a.id + '">마감일 재지정</button>';
      if (status === 'overdue' || status === 'upcoming') html += '<button class="btn-complete" data-complete="' + a.id + '">완료</button>';
    }
    return html;
  }

  function renderTasks() {
    var list = document.getElementById('task-list');
    list.innerHTML = ASSIGNMENTS.map(function (a) {
      var status = assignmentStatus(a);
      var sc = statusClasses(status);
      return '<div class="task-row">' +
        '<div><p class="task-row__title' + (status === 'done' ? ' task-row__title--done' : '') + '">' + a.title + '</p><p class="task-row__due">' + dueLabel(a) + '</p></div>' +
        '<div class="task-row__actions">' +
          '<span class="badge-pill" style="background:' + sc.bg + '; color:' + sc.color + ';">' + statusLabel(status) + '</span>' +
          '<button class="btn-sm" data-open-detail="' + a.id + '">상세보기</button>' +
          taskRowActionsHtml(a) +
        '</div>' +
      '</div>';
    }).join('');
    bindTaskRowEvents(list);

    renderCalendar();
    renderStats();
  }

  function bindTaskRowEvents(scope) {
    scope.querySelectorAll('[data-start-edit]').forEach(function (btn) {
      btn.addEventListener('click', function () { state.editingDueId = btn.dataset.startEdit; renderTasks(); });
    });
    scope.querySelectorAll('[data-save-due]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var id = btn.dataset.saveDue;
        var input = scope.querySelector('[data-due-input="' + id + '"]');
        var a = ASSIGNMENTS.find(function (x) { return x.id === id; });
        if (a && input.value) a.due = input.value;
        state.editingDueId = null;
        renderTasks();
      });
    });
    scope.querySelectorAll('[data-complete]').forEach(function (btn) {
      btn.addEventListener('click', function () { completeAssignment(btn.dataset.complete); });
    });
    scope.querySelectorAll('[data-open-detail]').forEach(function (btn) {
      btn.addEventListener('click', function () { openDetail(btn.dataset.openDetail); });
    });
  }

  function completeAssignment(id) {
    var a = ASSIGNMENTS.find(function (x) { return x.id === id; });
    if (!a) return;
    a.done = true;
    closeDetail();
    renderTasks();
    renderTodos();
    document.getElementById('complete-title').textContent = a.title + ' 완료!';
    document.getElementById('complete-modal').hidden = false;
  }

  function openDetail(id) {
    var a = ASSIGNMENTS.find(function (x) { return x.id === id; });
    if (!a) return;
    state.detailId = id;
    var status = assignmentStatus(a);
    var sc = statusClasses(status);
    document.getElementById('detail-title').textContent = a.title;
    document.getElementById('detail-title').style.textDecoration = status === 'done' ? 'line-through' : 'none';
    var statusEl = document.getElementById('detail-status');
    statusEl.textContent = statusLabel(status);
    statusEl.style.background = sc.bg;
    statusEl.style.color = sc.color;
    document.getElementById('detail-due').textContent = dueLabel(a);
    var tagEl = document.getElementById('detail-tag');
    if (a.relatedTag) { tagEl.textContent = '#' + a.relatedTag; tagEl.style.display = 'inline-block'; } else { tagEl.style.display = 'none'; }
    document.getElementById('detail-desc').textContent = a.desc;
    var actions = document.getElementById('detail-actions');
    actions.innerHTML = taskRowActionsHtml(a);
    bindTaskRowEvents(actions);
    document.getElementById('detail-modal').hidden = false;
  }
  function closeDetail() {
    document.getElementById('detail-modal').hidden = true;
    state.detailId = null;
  }

  function renderCalendar() {
    var grid = document.getElementById('calendar-grid');
    var weekdays = ['일', '월', '화', '수', '목', '금', '토'];
    var html = weekdays.map(function (w) { return '<div class="cal-weekday">' + w + '</div>'; }).join('');

    var firstWeekday = 4; // July 2026: 1st is Thursday
    var daysInMonth = 31;
    var dueMap = {};
    ASSIGNMENTS.forEach(function (a) { if (a.due && !a.done) dueMap[a.due] = a; });

    for (var i = 0; i < firstWeekday; i++) html += '<div class="cal-cell cal-cell--empty"></div>';
    for (var day = 1; day <= daysInMonth; day++) {
      var dateStr = '2026-07-' + (day < 10 ? '0' + day : '' + day);
      var task = dueMap[dateStr];
      var isToday = dateStr === TODAY;
      var status = task ? assignmentStatus(task) : null;
      html += '<div class="cal-cell' + (task ? ' cal-cell--clickable' : '') + '"' + (task ? ' data-cal-task="' + task.id + '"' : '') + '>' +
        '<span class="cal-cell__day' + (isToday ? ' cal-cell__day--today' : '') + '">' + day + '</span>' +
        (task ? '<div class="cal-cell__task cal-cell__task--' + (status === 'overdue' ? 'overdue' : 'upcoming') + '">' + task.title + '</div>' : '') +
      '</div>';
    }
    grid.innerHTML = html;
    grid.querySelectorAll('[data-cal-task]').forEach(function (cell) {
      cell.addEventListener('click', function () { openDetail(cell.dataset.calTask); });
    });
  }

  function renderStats() {
    var overdue = ASSIGNMENTS.filter(function (a) { return assignmentStatus(a) === 'overdue'; }).length;
    var noDue = ASSIGNMENTS.filter(function (a) { return assignmentStatus(a) === 'none'; }).length;
    var cards = document.querySelectorAll('.stat-card .stat-card__value');
    // index 2 = overdue, index 3 = no due (stat-grid order: session, weakness, overdue, noDue)
    if (cards[2]) cards[2].innerHTML = overdue + '<span class="stat-card__unit">건</span>';
    if (cards[3]) cards[3].innerHTML = noDue + '<span class="stat-card__unit">건</span>';
  }

  document.addEventListener('DOMContentLoaded', function () {
    renderTabs();
    renderTodos();
    renderTasks();

    document.getElementById('detail-close-btn').addEventListener('click', closeDetail);
    document.getElementById('detail-modal').addEventListener('click', function (e) {
      if (e.target === e.currentTarget) closeDetail();
    });
    document.getElementById('complete-close-btn').addEventListener('click', function () {
      document.getElementById('complete-modal').hidden = true;
    });
    document.getElementById('complete-modal').addEventListener('click', function (e) {
      if (e.target === e.currentTarget) document.getElementById('complete-modal').hidden = true;
    });
  });
})();
