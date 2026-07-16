// landing.js — dummy-data driven interactions for the JobPuzzle landing page
// No frameworks, no build step. Runs directly in the browser.

(function () {
  'use strict';

  var STATE_CONFIG = {
    guest: {
      isLoggedIn: false,
      ctaLabel: '로그인하기',
      ctaHint: '로그인이 필요해요 · 클릭하면 로그인 페이지로 이동해요',
      reportLocked: true,
      lockTitle: '로그인하고 리포트 확인하기',
      lockDesc: '로그인하면 직무별 요구역량 리포트를 볼 수 있어요',
      lockBtnLabel: '로그인'
    },
    noData: {
      isLoggedIn: true,
      ctaLabel: '분석할 자료 등록하러 가기',
      ctaHint: '채용공고와 이력서를 등록하면 분석이 시작돼요',
      reportLocked: true,
      lockTitle: '자료를 등록하면 리포트가 열려요',
      lockDesc: '채용공고와 이력서를 등록하면 나만의 리포트를 확인할 수 있어요',
      lockBtnLabel: '자료 등록하기'
    },
    ready: {
      isLoggedIn: true,
      ctaLabel: '면접 준비 이어가기',
      ctaHint: '최근 등록한 백엔드 신입 공고 기준으로 이어갈 수 있어요',
      reportLocked: false
    }
  };

  var DUMMY_USER_NAME = '김지원';

  var els = {};

  function cacheEls() {
    els.previewBar = document.querySelector('.preview-bar');
    els.previewBtns = document.querySelectorAll('.preview-bar__btn');
    els.authGuest = document.getElementById('auth-guest');
    els.authUser = document.getElementById('auth-user');
    els.userName = document.getElementById('user-name');
    els.userAvatar = document.getElementById('user-avatar');
    els.userChip = document.getElementById('user-chip');
    els.userMenu = document.getElementById('user-menu');
    els.heroCtaLabel = document.getElementById('hero-cta-label');
    els.heroCtaHint = document.getElementById('hero-cta-hint');
    els.reportLock = document.getElementById('report-lock');
    els.lockTitle = document.getElementById('lock-title');
    els.lockDesc = document.getElementById('lock-desc');
    els.lockBtn = document.getElementById('lock-btn');
    els.reportUnlocked = document.getElementById('report-unlocked');
    els.reportUsername = document.getElementById('report-username');
  }

  function configurePreviewToolbar() {
    var params = new URLSearchParams(window.location.search);
    var previewEnabled = params.get('preview') === 'true';
    if (els.previewBar) els.previewBar.hidden = !previewEnabled;
  }

  function applyMode(mode, userName) {
    var config = STATE_CONFIG[mode] || STATE_CONFIG.guest;
    var name = userName || DUMMY_USER_NAME;

    // preview bar active state
    els.previewBtns.forEach(function (btn) {
      btn.classList.toggle('is-active', btn.dataset.mode === mode);
    });

    // header auth area
    if (config.isLoggedIn) {
      els.authGuest.hidden = true;
      els.authUser.hidden = false;
      els.userName.textContent = name + '님';
      els.userAvatar.textContent = name.charAt(0);
    } else {
      els.authGuest.hidden = false;
      els.authUser.hidden = true;
      els.userMenu.hidden = true;
    }

    // hero CTA
    els.heroCtaLabel.textContent = config.ctaLabel;
    els.heroCtaHint.textContent = config.ctaHint;

    // job report lock/unlock
    if (config.reportLocked) {
      els.reportLock.hidden = false;
      els.lockTitle.textContent = config.lockTitle;
      els.lockDesc.textContent = config.lockDesc;
      els.lockBtn.textContent = config.lockBtnLabel;
      els.reportUnlocked.hidden = true;
    } else {
      els.reportLock.hidden = true;
      els.reportUnlocked.hidden = false;
      els.reportUsername.textContent = name;
    }
  }

  function bindEvents() {
    els.previewBtns.forEach(function (btn) {
      btn.addEventListener('click', function () {
        applyMode(btn.dataset.mode);
      });
    });

    els.userChip.addEventListener('click', function (e) {
      e.stopPropagation();
      els.userMenu.hidden = !els.userMenu.hidden;
    });
    document.addEventListener('click', function () {
      els.userMenu.hidden = true;
    });
  }

  // 로그아웃 클릭 시 실제 로그아웃 API 호출 후 새로고침 (더미 상태 말고 실제 세션을 지워야 함)
  function bindLogout() {
    var logoutLink = document.getElementById('logout-link');
    if (!logoutLink) return;

    logoutLink.addEventListener('click', function (event) {
      event.preventDefault();
      fetch('/api/user/logout', { method: 'POST', credentials: 'same-origin' })
        .finally(function () {
          window.location.href = '/index.html';
        });
    });
  }

  // 실제 로그인 여부를 서버에 물어봐서 헤더/CTA 상태를 결정
  // (?preview=true로 들어온 경우엔 기존처럼 더미 상태 미리보기 툴바를 그대로 사용)
  function applyRealAuthState() {
    fetch('/api/user/me', { credentials: 'same-origin' })
      .then(function (res) { return res.json().then(function (body) { return { ok: res.ok, body: body }; }); })
      .then(function (result) {
        if (result.ok && result.body.success) {
          var user = result.body.data;
          // 로그인은 했지만 아직 등록한 자료가 있는지는 알 수 없어서, 안전하게 "자료 없음" 상태로 보여줌
          applyMode('noData', user.name || user.loginId);
        } else {
          applyMode('guest');
        }
      })
      .catch(function () {
        applyMode('guest');
      });
  }

  document.addEventListener('DOMContentLoaded', function () {
    cacheEls();
    configurePreviewToolbar();
    bindEvents();
    bindLogout();

    var params = new URLSearchParams(window.location.search);
    if (params.get('preview') === 'true') {
      applyMode('ready'); // 미리보기 모드는 기존 더미 상태로 시작, 툴바로 자유롭게 전환
    } else {
      applyRealAuthState();
    }
  });
})();
