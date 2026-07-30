// login.js — /login 페이지: 아이디/비밀번호 로그인 폼 처리
(function () {
  'use strict';

  function showError(el, message) {
    el.textContent = message;
    el.classList.add('is-visible');
  }

  function hideError(el) {
    el.classList.remove('is-visible');
  }

  var idFailCount = 0;
  var pwFailCount = 0;
  var FAIL_HINT_THRESHOLD = 2;

  document.addEventListener('DOMContentLoaded', function () {
    var form = document.getElementById('login-form');
    var errorEl = document.getElementById('login-error');
    var findIdLink = document.getElementById('find-id-link');
    var passwdResetLink = document.getElementById('passwd-reset-link');

    form.addEventListener('submit', function (e) {
      e.preventDefault();
      hideError(errorEl);

      var loginId = document.getElementById('loginId').value.trim();
      var password = document.getElementById('password').value;
      var autoLogin = document.getElementById('autoLogin').checked;

      fetch(window.JobPuzzleRoutes.path('/user/login'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'same-origin',
        body: JSON.stringify({ loginId: loginId, password: password, autoLogin: autoLogin })
      })
        .then(function (res) {
          return res.json().then(function (body) { return { ok: res.ok, body: body }; });
        })
        .then(function (result) {
          if (result.ok && result.body.success) {
            // 로그인 성공 - 메인으로 이동
            window.location.href = window.JobPuzzleRoutes.path('/');
            return;
          }

          showError(errorEl, result.body.message || '로그인에 실패했습니다.');

          if (result.body.code === 'USER_011') {
            idFailCount += 1;
            pwFailCount = 0;
          } else if (result.body.code === 'USER_003') {
            pwFailCount += 1;
            idFailCount = 0;
          } else {
            idFailCount = 0;
            pwFailCount = 0;
          }

          findIdLink.style.display = idFailCount >= FAIL_HINT_THRESHOLD ? 'inline' : 'none';
          passwdResetLink.style.display = pwFailCount >= FAIL_HINT_THRESHOLD ? 'inline' : 'none';
        })
        .catch(function () {
          showError(errorEl, '일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
        });
    });
  });
})();
