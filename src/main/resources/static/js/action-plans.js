(function () {
  'use strict';

  var GROUPS_PER_PAGE = 4;
  var state = {
    plans: [],
    filter: 'ALL',
    page: 1,
    savingIds: new Set(),
    focusedAnalysisCaseId: new URLSearchParams(window.location.search).get('analysisCaseId')
  };

  var list = document.getElementById('action-plan-list');
  var feedback = document.getElementById('action-plan-feedback');
  var pagination = document.getElementById('action-plan-load-more');
  var calendar = null;

  // 공통 API 응답을 검사하고 data 값만 반환한다.
  function api(path, options) {
    return fetch(window.JobPuzzleRoutes.path(path), Object.assign({
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json' }
    }, options || {})).then(function (response) {
      return response.json().catch(function () { return null; }).then(function (body) {
        if (!response.ok || !body || !body.success) {
          throw new Error(body && body.message ? body.message : '요청을 처리하지 못했습니다.');
        }
        return body.data;
      });
    });
  }

  // 안전하게 텍스트 노드를 가진 요소를 만든다.
  function textElement(tag, className, text) {
    var element = document.createElement(tag);
    if (className) element.className = className;
    element.textContent = text == null ? '' : text;
    return element;
  }

  // 서버 경력 수준을 화면용 한국어로 변환한다.
  function careerLabel(value) {
    return { NEW: '신입', EXPERIENCED: '경력', ANY: '무관' }[value] || '경력 미지정';
  }

  // 날짜·시간 문자열을 분석 일자 표기로 변환한다.
  function dateLabel(value) {
    if (!value) return '분석 일자 미상';
    var date = new Date(value);
    if (Number.isNaN(date.getTime())) return String(value).slice(0, 10);
    return date.toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' });
  }

  // 전체 진행률 요약을 현재 데이터와 동기화한다.
  function renderSummary() {
    var total = state.plans.length;
    var done = state.plans.filter(function (plan) { return plan.status === 'DONE'; }).length;
    var pending = total - done;
    var progress = total ? Math.round((done / total) * 100) : 0;
    document.getElementById('action-plan-total').textContent = total;
    document.getElementById('action-plan-done').textContent = done;
    document.getElementById('action-plan-pending').textContent = pending;
    document.getElementById('action-plan-progress-value').textContent = progress + '%';
    document.getElementById('action-plan-progress-bar').style.width = progress + '%';
  }

  // 선택한 완료 상태에 해당하는 과제만 반환한다.
  function filteredPlans() {
    if (state.filter === 'ALL') return state.plans;
    return state.plans.filter(function (plan) { return plan.status === state.filter; });
  }

  // 현재 필터의 과제를 공통 달력 표시 모델로 변환한다.
  function calendarItems() {
    return filteredPlans().map(function (plan) {
      var notice = window.ActionPlanDeadline.getNotice(plan.deadline, plan.status);
      return {
        id: String(plan.actionPlanId),
        date: plan.deadline,
        title: plan.suggestion || '보완 과제',
        description: plan.missingPoint || '',
        tone: plan.status === 'DONE' ? 'done' : (notice ? notice.tone : 'normal'),
        statusLabel: plan.status === 'DONE' ? '완료' : (notice ? notice.label : '진행 중')
      };
    });
  }

  // 과제를 분석 ID별로 묶고 선택해 들어온 분석을 가장 앞에 배치한다.
  function groupedPlans() {
    var groups = new Map();
    filteredPlans().forEach(function (plan) {
      var key = String(plan.analysisCaseId);
      if (!groups.has(key)) groups.set(key, { analysisCaseId: key, plans: [] });
      groups.get(key).plans.push(plan);
    });
    return Array.from(groups.values()).sort(function (a, b) {
      if (a.analysisCaseId === state.focusedAnalysisCaseId) return -1;
      if (b.analysisCaseId === state.focusedAnalysisCaseId) return 1;
      return new Date(b.plans[0].createdAt || 0) - new Date(a.plans[0].createdAt || 0);
    });
  }

  // 저장 중인 과제 컨트롤과 오류 문구를 한곳에서 갱신한다.
  function setSaving(actionPlanId, saving, message) {
    if (saving) state.savingIds.add(actionPlanId);
    else state.savingIds.delete(actionPlanId);
    var item = list.querySelector('[data-action-plan-id="' + actionPlanId + '"]');
    if (!item) return;
    item.querySelectorAll('button, input').forEach(function (control) {
      control.disabled = saving;
    });
    var error = item.querySelector('.action-plan-inline-error');
    if (error) error.textContent = message || '';
  }

  // 한 과제의 완료 처리와 일정 입력 UI를 만든다.
  function createPlanItem(plan) {
    var item = document.createElement('article');
    item.className = 'action-plan-item' + (plan.status === 'DONE' ? ' is-done' : '');
    item.dataset.actionPlanId = plan.actionPlanId;

    var completion = textElement('button', 'action-plan-completion', plan.status === 'DONE' ? '✓' : '');
    completion.type = 'button';
    completion.setAttribute('aria-label', plan.status === 'DONE' ? '완료 취소' : '과제 완료');
    completion.addEventListener('click', function () {
      updateCompletion(plan, plan.status !== 'DONE');
    });

    var copy = document.createElement('div');
    copy.className = 'action-plan-copy';
    copy.appendChild(textElement('h3', null, plan.suggestion || '보완 과제'));
    var meta = textElement('div', 'action-plan-meta', '');
    if (plan.matchLevel) {
      var matchClass = 'action-plan-match action-plan-match--' + String(plan.matchLevel).toLowerCase();
      meta.appendChild(textElement('span', matchClass, plan.matchLevel));
    }
    if (plan.requirement) {
      meta.appendChild(textElement('span', 'action-plan-requirement', plan.requirement));
    }
    copy.appendChild(meta);
    var detail = textElement('div', 'action-plan-detail', '');
    var missing = textElement('p', null, '');
    missing.appendChild(textElement('strong', null, '보완 포인트 · '));
    missing.appendChild(document.createTextNode(plan.missingPoint || '보완이 필요한 내용을 확인해 주세요.'));
    detail.appendChild(missing);
    copy.appendChild(detail);

    var deadlineBox = textElement('div', 'action-plan-deadline', '');
    var label = textElement('label', null, '마감일');
    label.htmlFor = 'action-plan-deadline-' + plan.actionPlanId;
    var input = document.createElement('input');
    input.type = 'date';
    input.id = label.htmlFor;
    input.value = plan.deadline || '';
    input.addEventListener('change', function () {
      updateDeadline(plan, input.value || null, input);
    });
    deadlineBox.appendChild(label);
    deadlineBox.appendChild(input);
    var notice = window.ActionPlanDeadline.getNotice(plan.deadline, plan.status);
    if (notice) {
      deadlineBox.classList.add('action-plan-deadline--' + notice.tone);
      deadlineBox.appendChild(textElement('span', 'action-plan-deadline-notice action-plan-deadline-notice--' + notice.tone, notice.label));
    }
    deadlineBox.appendChild(textElement('p', 'action-plan-inline-error', ''));

    item.appendChild(completion);
    item.appendChild(copy);
    item.appendChild(deadlineBox);
    return item;
  }

  // 분석 정보와 그 분석에서 생성된 과제들을 하나의 구획으로 만든다.
  function createAnalysisGroup(group) {
    var sample = group.plans[0];
    var section = document.createElement('section');
    section.className = 'action-plan-group' + (group.analysisCaseId === state.focusedAnalysisCaseId ? ' is-focused' : '');

    var header = document.createElement('header');
    header.className = 'action-plan-group__header';
    var titleBox = document.createElement('div');
    titleBox.appendChild(textElement('p', 'action-plan-group__eyebrow', '분석 #' + group.analysisCaseId));
    titleBox.appendChild(textElement('h3', null, [sample.mainCategory, sample.subCategory].filter(Boolean).join(' · ') || '직무 분석'));
    titleBox.appendChild(textElement('p', 'action-plan-group__meta', careerLabel(sample.careerLevel) + ' · ' + dateLabel(sample.createdAt)));
    header.appendChild(titleBox);
    var resultLink = textElement('a', 'action-plan-group__report', '분석 결과 보기');
    resultLink.href = window.JobPuzzleRoutes.path('/analysis-results/' + group.analysisCaseId);
    header.appendChild(resultLink);
    section.appendChild(header);

    var taskList = document.createElement('div');
    taskList.className = 'action-plan-group__tasks';
    group.plans.forEach(function (plan) { taskList.appendChild(createPlanItem(plan)); });
    section.appendChild(taskList);
    return section;
  }

  // 현재 페이지 번호를 앞뒤 이동과 숫자 버튼으로 표시한다.
  function renderPagination(pageCount) {
    pagination.replaceChildren();
    pagination.hidden = pageCount <= 1;
    if (pagination.hidden) return;

    function addButton(label, page, active, disabled, ariaLabel) {
      var button = textElement('button', active ? 'is-active' : '', label);
      button.type = 'button';
      button.disabled = disabled;
      if (ariaLabel) button.setAttribute('aria-label', ariaLabel);
      if (active) button.setAttribute('aria-current', 'page');
      button.addEventListener('click', function () {
        state.page = page;
        renderList();
        document.querySelector('.action-plan-board').scrollIntoView({ behavior: 'smooth', block: 'start' });
      });
      pagination.appendChild(button);
    }

    addButton('‹', Math.max(1, state.page - 1), false, state.page === 1, '이전 페이지');
    // 분석 묶음이 많아도 원하는 페이지로 바로 이동할 수 있도록 모든 번호를 표시한다.
    for (var page = 1; page <= pageCount; page++) {
      addButton(String(page), page, page === state.page, false, page + '페이지');
    }
    addButton('›', Math.min(pageCount, state.page + 1), false, state.page === pageCount, '다음 페이지');
  }

  // 현재 필터와 페이지에 맞는 분석 그룹 목록을 그린다.
  function renderList() {
    var groups = groupedPlans();
    if (calendar) calendar.setItems(calendarItems());
    var pageCount = Math.max(1, Math.ceil(groups.length / GROUPS_PER_PAGE));
    state.page = Math.min(state.page, pageCount);
    var visibleGroups = groups.slice((state.page - 1) * GROUPS_PER_PAGE, state.page * GROUPS_PER_PAGE);
    list.replaceChildren();
    feedback.hidden = groups.length > 0;
    if (!groups.length) {
      pagination.hidden = true;
      feedback.textContent = state.plans.length === 0
        ? '아직 생성된 액션플랜이 없습니다. 지원 분석을 완료하면 보완 과제가 이곳에 표시됩니다.'
        : '선택한 상태에 해당하는 과제가 없습니다.';
      return;
    }
    visibleGroups.forEach(function (group) { list.appendChild(createAnalysisGroup(group)); });
    renderPagination(pageCount);
  }

  // 변경된 한 과제만 상태에 반영한 뒤 요약과 목록을 동기화한다.
  function replacePlan(actionPlanId, changes) {
    state.savingIds.delete(actionPlanId);
    state.plans = state.plans.map(function (plan) {
      return String(plan.actionPlanId) === String(actionPlanId) ? Object.assign({}, plan, changes) : plan;
    });
    renderSummary();
    renderList();
  }

  // 사용자가 선택한 최종 완료 상태를 서버에 저장한다.
  function updateCompletion(plan, completed) {
    if (state.savingIds.has(plan.actionPlanId)) return;
    setSaving(plan.actionPlanId, true);
    api('/action-plan/' + encodeURIComponent(plan.actionPlanId) + '/completion', {
      method: 'PATCH',
      body: JSON.stringify({ completed: completed })
    }).then(function (updated) {
      replacePlan(plan.actionPlanId, { status: updated.status, completedAt: updated.completedAt });
    }).catch(function (error) {
      setSaving(plan.actionPlanId, false, error.message);
    });
  }

  // 사용자가 선택한 마감일을 저장하고 빈 값은 마감일 제거로 전달한다.
  function updateDeadline(plan, deadline, input) {
    if (state.savingIds.has(plan.actionPlanId)) return;
    setSaving(plan.actionPlanId, true);
    api('/action-plan/' + encodeURIComponent(plan.actionPlanId) + '/deadline', {
      method: 'PATCH',
      body: JSON.stringify({ deadline: deadline })
    }).then(function (updated) {
      replacePlan(plan.actionPlanId, { deadline: updated.deadline });
    }).catch(function (error) {
      input.value = plan.deadline || '';
      setSaving(plan.actionPlanId, false, error.message);
    });
  }

  // 필터 버튼 상태와 안내 문구를 갱신하고 첫 페이지를 표시한다.
  function selectFilter(filter) {
    state.filter = filter;
    state.page = 1;
    document.querySelectorAll('[data-action-plan-filter]').forEach(function (button) {
      button.classList.toggle('is-active', button.dataset.actionPlanFilter === filter);
    });
    var descriptions = {
      ALL: '분석 결과별로 묶은 전체 과제를 보고 있어요.',
      PENDING: '분석 결과별로 남은 과제만 보고 있어요.',
      DONE: '분석 결과별로 완료한 과제만 보고 있어요.'
    };
    document.getElementById('action-plan-filter-description').textContent = descriptions[filter];
    renderList();
  }

  // 달력에서 선택한 과제가 있는 목록 페이지로 전환해 해당 카드를 강조한다.
  function openPlanFromCalendar(actionPlanId) {
    var plan = state.plans.find(function (item) { return String(item.actionPlanId) === String(actionPlanId); });
    if (!plan) return;
    var groups = groupedPlans();
    var groupIndex = groups.findIndex(function (group) { return group.analysisCaseId === String(plan.analysisCaseId); });
    state.page = Math.max(1, Math.floor(groupIndex / GROUPS_PER_PAGE) + 1);
    document.querySelectorAll('[data-action-plan-view]').forEach(function (button) {
      button.classList.toggle('is-active', button.dataset.actionPlanView === 'list');
    });
    document.querySelectorAll('[data-action-plan-panel]').forEach(function (panel) {
      panel.hidden = panel.dataset.actionPlanPanel !== 'list';
    });
    renderList();
    var item = list.querySelector('[data-action-plan-id="' + actionPlanId + '"]');
    if (item) {
      item.classList.add('is-calendar-target');
      item.scrollIntoView({ behavior: 'smooth', block: 'center' });
      window.setTimeout(function () { item.classList.remove('is-calendar-target'); }, 1600);
    }
  }

  // 로그인 사용자의 전체 액션플랜을 최초 한 번 불러온다.
  function loadActionPlans() {
    api('/action-plan').then(function (plans) {
      state.plans = Array.isArray(plans) ? plans : [];
      renderSummary();
      renderList();
    }).catch(function (error) {
      feedback.hidden = false;
      feedback.textContent = error.message;
    });
  }

  // 필터 이벤트를 연결하고 액션플랜 조회를 시작한다.
  function init() {
    if (!list || !feedback || !pagination) return;
    var calendarRoot = document.getElementById('action-plan-calendar');
    if (calendarRoot) {
      calendar = new window.ActionPlanCalendar(calendarRoot, { onItemClick: openPlanFromCalendar });
    }
    document.querySelectorAll('[data-action-plan-filter]').forEach(function (button) {
      button.addEventListener('click', function () {
        selectFilter(button.dataset.actionPlanFilter);
      });
    });
    document.querySelectorAll('[data-action-plan-view]').forEach(function (button) {
      button.addEventListener('click', function () {
        document.querySelectorAll('[data-action-plan-view]').forEach(function (item) {
          item.classList.toggle('is-active', item === button);
        });
        document.querySelectorAll('[data-action-plan-panel]').forEach(function (panel) {
          panel.hidden = panel.dataset.actionPlanPanel !== button.dataset.actionPlanView;
        });
      });
    });
    loadActionPlans();
  }

  document.addEventListener('DOMContentLoaded', init);
})();
