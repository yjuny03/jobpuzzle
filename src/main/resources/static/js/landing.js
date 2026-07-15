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

  function applyMode(mode) {
    var config = STATE_CONFIG[mode] || STATE_CONFIG.guest;

    // preview bar active state
    els.previewBtns.forEach(function (btn) {
      btn.classList.toggle('is-active', btn.dataset.mode === mode);
    });

    // header auth area
    if (config.isLoggedIn) {
      els.authGuest.hidden = true;
      els.authUser.hidden = false;
      els.userName.textContent = DUMMY_USER_NAME + '님';
      els.userAvatar.textContent = DUMMY_USER_NAME.charAt(0);
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
      els.reportUsername.textContent = DUMMY_USER_NAME;
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

  document.addEventListener('DOMContentLoaded', function () {
    cacheEls();
    configurePreviewToolbar();
    bindEvents();
    applyMode('ready'); // default dummy state: logged in with data, matches previous default
  });
})();
