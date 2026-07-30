// reports.js — 완료한 면접 목록과 실제 최종 리포트 진입 화면을 구성
(function () {
  'use strict';

  var state = {
    sessions: [],
    sessionsLoaded: false,
    selectedSessionId: null
  };

  // API 오류처럼 동적으로 받은 문구를 HTML에 넣기 전에 안전하게 이스케이프합니다.
  function esc(value) {
    var element = document.createElement('div');
    element.textContent = value == null ? '' : String(value);
    return element.innerHTML;
  }

  // 로그인 사용자의 완료된 면접 기록을 공통 API 응답 형식으로 조회합니다.
  function api(path) {
    return fetch(path, { credentials: 'same-origin' }).then(function (response) {
      return response.json().catch(function () { return {}; }).then(function (body) {
        if (!response.ok || !body.success) {
          throw new Error(body.message || '결과를 불러오지 못했습니다.');
        }
        return body.data;
      });
    });
  }

  // 내부 면접 모드를 사용자에게 익숙한 한글 명칭으로 변환합니다.
  function reportModeLabel(mode) {
    if (mode === 'BASIC') return '기본 모의면접';
    if (mode === 'COMPANY_FIT') return '회사 맞춤 면접';
    return '약점 보완 면접';
  }

  // 저장 시각을 리포트 목록에서 읽기 쉬운 국내 날짜 형식으로 표시합니다.
  function formatReportDate(value) {
    if (!value) return '완료 시각 정보 없음';
    return new Date(value).toLocaleString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  // 로딩·빈 데이터·오류 상태를 동일한 카드 구조로 표시합니다.
  function emptyMarkup(title, description, tone) {
    return '<div class="reports-empty' + (tone ? ' is-' + tone : '') + '">' +
      '<span class="reports-empty__puzzle" aria-hidden="true">✦</span>' +
      '<strong>' + esc(title) + '</strong><p>' + esc(description) + '</p></div>';
  }

  // 완료한 면접 목록과 현재 선택 상태를 렌더링합니다.
  function renderReportList() {
    var list = document.getElementById('report-list');
    var count = document.getElementById('report-count');
    count.textContent = state.sessions.length + '개';

    if (!state.sessions.length) {
      list.innerHTML = state.sessionsLoaded
        ? emptyMarkup('아직 완료한 면접이 없어요.', '면접을 완료하면 이곳에 기록이 차곡차곡 쌓입니다.')
        : emptyMarkup('면접 기록을 불러오고 있어요.', '잠시만 기다려 주세요.', 'loading');
      return;
    }

    list.innerHTML = state.sessions.map(function (session, index) {
      var selected = session.sessionId === state.selectedSessionId;
      return '<button type="button" class="report-list-item' + (selected ? ' is-active' : '') +
        '" data-select-report="' + session.sessionId + '" aria-pressed="' + selected + '">' +
        '<span class="report-list-item__number">' + String(index + 1).padStart(2, '0') + '</span>' +
        '<span class="report-list-item__copy"><strong class="report-list-item__title">' +
        reportModeLabel(session.mode) + '</strong><span class="report-list-item__date">' +
        formatReportDate(session.completedAt) + '</span></span>' +
        '<span class="report-list-item__score">질문 ' + session.questionCount + '개</span></button>';
    }).join('');

    document.querySelectorAll('[data-select-report]').forEach(function (button) {
      button.addEventListener('click', function () {
        state.selectedSessionId = Number(button.dataset.selectReport);
        renderReportList();
        renderReportDetail();
      });
    });
  }

  // 선택한 면접의 요약과 실제 최종 리포트 이동 버튼을 렌더링합니다.
  function renderReportDetail() {
    var detail = document.getElementById('report-detail');
    if (!state.sessions.length) {
      detail.innerHTML = emptyMarkup(
        '확인할 리포트가 없습니다.',
        '면접 답변을 완료하면 점수와 보완 방향을 자세히 확인할 수 있어요.'
      );
      return;
    }

    var selected = state.sessions.find(function (session) {
      return session.sessionId === state.selectedSessionId;
    }) || state.sessions[0];
    state.selectedSessionId = selected.sessionId;

    detail.innerHTML =
      '<div class="report-detail__top"><div><span class="report-detail__label">SELECTED REPORT</span>' +
      '<h3>' + reportModeLabel(selected.mode) + '</h3></div>' +
      '<span class="report-detail__question-count">질문 ' + selected.questionCount + '개</span></div>' +
      '<p class="report-detail__date">' + formatReportDate(selected.completedAt) + '</p>' +
      '<div class="report-detail__guide"><strong>이 리포트에서 확인할 수 있어요</strong>' +
      '<ul><li>면접 종합 점수와 평가 관점별 점수</li><li>답변에서 반복된 약점과 개선 방향</li>' +
      '<li>다음 연습 질문과 제출 자료 보완 제안</li></ul></div>' +
      '<p class="report-detail__notice">처음 여는 리포트는 저장된 답변을 종합하는 데 몇 초 정도 걸릴 수 있습니다.</p>' +
      '<a href="' + window.JobPuzzleRoutes.path('/interview-results?sessionId=' +
        encodeURIComponent(selected.sessionId)) + '" class="report-detail__button">최종 리포트 자세히 보기</a>';
  }

  // 완료 기록 조회 결과를 화면 상태에 반영합니다.
  function loadReports() {
    renderReportList();
    renderReportDetail();
    api(window.JobPuzzleRoutes.path('/interview-sessions/history')).then(function (sessions) {
      state.sessions = Array.isArray(sessions) ? sessions : [];
      state.sessionsLoaded = true;
      if (state.sessions.length) state.selectedSessionId = state.sessions[0].sessionId;
      renderReportList();
      renderReportDetail();
    }).catch(function (error) {
      state.sessionsLoaded = true;
      document.getElementById('report-count').textContent = '확인 필요';
      document.getElementById('report-list').innerHTML =
        emptyMarkup('면접 기록을 불러오지 못했습니다.', error.message, 'error');
      document.getElementById('report-detail').innerHTML =
        emptyMarkup('잠시 후 다시 확인해 주세요.', '문제가 계속되면 다시 로그인해 주세요.', 'error');
    });
  }

  document.addEventListener('DOMContentLoaded', loadReports);
})();
