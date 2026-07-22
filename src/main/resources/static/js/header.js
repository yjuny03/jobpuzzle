// header.js — interactions for the shared Thymeleaf application header.
(function () {
  'use strict';

  function bindAppUserMenu() {
    var chip = document.getElementById('app-user-chip');
    var menu = document.getElementById('app-user-menu');
    if (!chip || !menu) return;

    chip.addEventListener('click', function (event) {
      event.stopPropagation();
      menu.hidden = !menu.hidden;
    });

    document.addEventListener('click', function () {
      menu.hidden = true;
    });
  }

  // 헤더의 이름/아바타를 회원 정보로 채움 - 최초 로딩 시와 설정 화면에서 정보 수정 후(app-user-updated) 둘 다 호출됨
  function renderUserDisplay(user) {
    var nameEl = document.getElementById('app-user-name');
    var avatarEl = document.getElementById('app-user-avatar');
    if (!nameEl || !avatarEl) return;

    nameEl.textContent = (user.name || user.loginId) + '님';
    avatarEl.textContent = (user.name || user.loginId).charAt(0);
  }

  // 로그인된 실제 회원 정보를 불러와서 헤더의 이름/아바타를 채움
  function loadCurrentUser() {
    fetch('/api/user/me', { credentials: 'same-origin' })
      .then(function (res) { return res.json().then(function (body) { return { ok: res.ok, body: body }; }); })
      .then(function (result) {
        if (result.ok && result.body.success) {
          var user = result.body.data;
          // 사용자 정보를 app-user-loaded 로 저장해서 다른 곳에서도 가져다 쓸 수 있게 만듦
          document.dispatchEvent(new CustomEvent('app-user-loaded', {detail: user}));
          renderUserDisplay(user);
        } else {
          // 로그인 안 된 상태로 이 화면에 들어온 경우 - 로그인 페이지로 이동
          window.location.href = '/login';
        }
      })
      .catch(function () {
        // 네트워크 오류 등 - 최소한 화면은 그대로 두고 넘어감
      });
  }

  // 로그아웃 클릭 시 실제 로그아웃 API 호출 후 이동 (그냥 링크로 두면 세션이 안 지워짐)
  function bindLogout() {
    var logoutLink = document.getElementById('app-logout-link');
    if (!logoutLink) return;

    logoutLink.addEventListener('click', function (event) {
      event.preventDefault();
      fetch('/api/user/logout', { method: 'POST', credentials: 'same-origin' })
        .finally(function () {
          window.location.href = '/index.html';
        });
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    bindAppUserMenu();
    bindLogout();
    loadCurrentUser();
    document.addEventListener('app-user-updated', function (event) {
      renderUserDisplay(event.detail);
    });
  });
})();
