// dashboard.js — 대시보드의 세션·약점 예시와 실제 액션플랜을 표시

(function () {
  'use strict';

  var now = new Date();
  var TODAY = [now.getFullYear(), String(now.getMonth() + 1).padStart(2, '0'), String(now.getDate()).padStart(2, '0')].join('-');
  var ASSIGNMENTS = [];
  var dashboardCalendar = null;

  var els = {};
  var state = { editingDueId: null, detailId: null };

  // 공통 API 응답을 검사하고 실제 data만 반환한다.
  function api(path, options) {
    return fetch(window.JobPuzzleRoutes.path(path), Object.assign({ credentials: 'same-origin' }, options || {}))
      .then(function (response) {
        return response.json().then(function (body) {
          if (!response.ok || !body.success) throw new Error(body.message || '요청에 실패했습니다.');
          return body.data;
        });
      });
  }

  // 서버 액션플랜 응답을 기존 대시보드 과제 표시 모델로 변환한다.
  function toAssignment(plan) {
    return {
      id: String(plan.actionPlanId),
      title: plan.suggestion || '보완 과제',
      due: plan.deadline,
      done: plan.status === 'DONE',
      relatedTag: plan.requirement || plan.relatedRequirementId,
      desc: plan.missingPoint || '보완이 필요한 내용을 확인해 주세요.'
    };
  }

  // AI 문장을 innerHTML에 넣기 전에 특수문자를 이스케이프한다.
  function escapeHtml(value) {
    return String(value == null ? '' : value)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  // 완료 여부와 마감일까지 남은 날짜로 대시보드 상태를 계산한다.
  function assignmentStatus(a) {
    if (a.done) return 'done';
    if (!a.due) return 'none';
    var notice = window.ActionPlanDeadline.getNotice(a.due, 'PENDING');
    if (notice && notice.label === '기한 종료') return 'overdue';
    if (notice && notice.tone === 'danger') return 'today';
    if (notice && notice.tone === 'warning') return 'soon';
    return 'upcoming';
  }
  function statusLabel(status) {
    return { done: '완료됨', overdue: '기한 종료', today: '오늘 마감', soon: '마감 임박', upcoming: '진행 중', none: '마감일 미지정' }[status];
  }

  // 임박한 과제는 남은 날짜를 바로 알 수 있도록 D-day 문구를 우선 표시한다.
  function assignmentStatusLabel(a, status) {
    var notice = window.ActionPlanDeadline.getNotice(a.due, a.done ? 'DONE' : 'PENDING');
    return notice ? notice.label : statusLabel(status);
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
      if (status !== 'none' && status !== 'done') html += '<button class="btn-complete" data-complete="' + a.id + '">완료</button>';
    }
    return html;
  }

  function renderTasks() {
    var list = document.getElementById('task-list');
    if (ASSIGNMENTS.length === 0) {
      list.innerHTML = '<p class="todo-empty">등록된 액션플랜이 없습니다.</p>';
      renderCalendar();
      renderStats();
      return;
    }
    list.innerHTML = ASSIGNMENTS.slice(0, 8).map(function (a) {
      var status = assignmentStatus(a);
      return '<div class="task-row task-row--' + status + '">' +
        '<div><p class="task-row__title' + (status === 'done' ? ' task-row__title--done' : '') + '">' + escapeHtml(a.title) + '</p><p class="task-row__due">' + escapeHtml(dueLabel(a)) + '</p></div>' +
        '<div class="task-row__actions">' +
          '<span class="badge-pill task-status task-status--' + status + '">' + assignmentStatusLabel(a, status) + '</span>' +
          '<button class="btn-sm" data-open-detail="' + a.id + '">상세보기</button>' +
          taskRowActionsHtml(a) +
        '</div>' +
      '</div>';
    }).join('') + (ASSIGNMENTS.length > 8
      ? '<a class="task-list__all" href="' + window.JobPuzzleRoutes.path('/action-plans') + '">전체 액션플랜 보기</a>'
      : '');
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
        if (!a) return;
        api('/action-plan/' + encodeURIComponent(id) + '/deadline', {
          method: 'PATCH', headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ deadline: input.value || null })
        }).then(function (updated) {
          a.due = updated.deadline;
          state.editingDueId = null;
          renderTasks();
        });
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
    api('/action-plan/' + encodeURIComponent(id) + '/completion', {
      method: 'PATCH', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ completed: true })
    }).then(function () {
      a.done = true;
      closeDetail();
      renderTasks();
      renderTodos();
      document.getElementById('complete-title').textContent = a.title + ' 완료!';
      document.getElementById('complete-modal').hidden = false;
    });
  }

  function openDetail(id) {
    var a = ASSIGNMENTS.find(function (x) { return x.id === id; });
    if (!a) return;
    state.detailId = id;
    var status = assignmentStatus(a);
    document.getElementById('detail-title').textContent = a.title;
    document.getElementById('detail-title').style.textDecoration = status === 'done' ? 'line-through' : 'none';
    var statusEl = document.getElementById('detail-status');
    statusEl.textContent = assignmentStatusLabel(a, status);
    statusEl.className = 'badge-pill task-status dashboard-task-modal__status task-status--' + status;
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

  // 실제 월간 달력에 액션플랜 날짜 표시와 선택 상세를 동기화한다.
  function renderCalendar() {
    if (!dashboardCalendar) return;
    dashboardCalendar.setItems(ASSIGNMENTS.map(function (a) {
      var status = assignmentStatus(a);
      var notice = window.ActionPlanDeadline.getNotice(a.due, a.done ? 'DONE' : 'PENDING');
      return {
        id: a.id,
        date: a.due,
        title: a.title,
        description: a.desc,
        tone: a.done ? 'done' : (notice ? notice.tone : 'normal'),
        statusLabel: a.done ? '완료' : (notice ? notice.label : statusLabel(status))
      };
    }));
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
    var calendarRoot = document.getElementById('dashboard-action-plan-calendar');
    if (calendarRoot) {
      dashboardCalendar = new window.ActionPlanCalendar(calendarRoot, {
        onItemClick: function (id) { openDetail(id); }
      });
    }
    api('/action-plan').then(function (plans) {
      ASSIGNMENTS = (plans || []).map(toAssignment);
      renderTodos();
      renderTasks();
    }).catch(function () {
      document.getElementById('task-list').innerHTML = '<p class="todo-empty">액션플랜을 불러오지 못했습니다.</p>';
    });

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
