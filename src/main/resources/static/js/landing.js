//  landing.js 역할: 랜딩페이지의 미리보기 상태, 진입 모션, 스크롤 장면, 카드 상호작용을 담당합니다.


//  즉시 실행 함수(IIFE): 전역 변수 오염을 막고 DOMContentLoaded 시점에 기능을 초기화합니다.
(function () {
    'use strict';

    //  01. 화면 상태 설정값: 비로그인(guest), 로그인·자료없음(noData), 로그인·자료있음(ready)
    var STATE_CONFIG = {
        guest: {
            isLoggedIn: false,
            ctaLabel: '로그인하기',
            ctaHint: '로그인이 필요해요 · 클릭하면 로그인 페이지로 이동해요',
            ctaHref: '/login',
            reportLocked: true,
            lockTitle: '로그인하고 리포트 확인하기',
            lockDesc: '로그인하면 직무별 요구역량 리포트를 볼 수 있어요',
            lockBtnLabel: '로그인'
        },
        noData: {
            isLoggedIn: true,
            ctaLabel: '분석할 자료 등록하러 가기',
            ctaHint: '채용공고와 이력서를 등록하면 분석이 시작돼요',
            ctaHref: '/my-data.html',
            reportLocked: true,
            lockTitle: '자료를 등록하면 리포트가 열려요',
            lockDesc: '채용공고와 이력서를 등록하면 나만의 리포트를 확인할 수 있어요',
            lockBtnLabel: '자료 등록하기'
        },
        ready: {
            isLoggedIn: true,
            ctaLabel: '면접 준비 이어가기',
            ctaHint: '최근 등록한 백엔드 신입 공고 기준으로 이어갈 수 있어요',
            ctaHref: '/interview.html',
            reportLocked: false
        }
    };

    //  데모 화면에 표시할 가상 사용자 이름
    var DUMMY_USER_NAME = '김지원';

    //  자주 사용하는 DOM 요소를 한 번만 찾아 보관하는 객체
    var els = {};

    //  02. DOM 요소 캐싱: 이후 함수들이 반복 querySelector를 하지 않도록 참조를 저장
    function cacheEls() {
        els.previewBar = document.querySelector('.preview-bar');
        els.previewBtns = document.querySelectorAll('.preview-bar__btn');
        els.authGuest = document.getElementById('auth-guest');
        els.authUser = document.getElementById('auth-user');
        els.userName = document.getElementById('user-name');
        els.userAvatar = document.getElementById('user-avatar');
        els.userChip = document.getElementById('user-chip');
        els.userMenu = document.getElementById('user-menu');
        els.adminLink = document.getElementById('admin-link');
        els.heroCtaLabel = document.getElementById('hero-cta-label');
        els.heroCta = document.getElementById('hero-cta');
        els.heroCtaHint = document.getElementById('hero-cta-hint');
        els.reportLock = document.getElementById('report-lock');
        els.lockTitle = document.getElementById('lock-title');
        els.lockDesc = document.getElementById('lock-desc');
        els.lockBtn = document.getElementById('lock-btn');
        els.reportUnlocked = document.getElementById('report-unlocked');
        els.reportUsername = document.getElementById('report-username');
    }

    //  03. 개발용 미리보기 바 표시 여부: URL 쿼리 ?preview=true를 확인
    function configurePreviewToolbar() {
        var params = new URLSearchParams(window.location.search);
        var previewEnabled = params.get('preview') === 'true';
        if (els.previewBar) els.previewBar.hidden = !previewEnabled;
    }

    //  04. 현재 미리보기 모드를 화면에 반영: 헤더 로그인 상태, HERO CTA, 리포트 잠금 상태를 함께 변경
    function applyMode(mode, userName, role) {
        var config = STATE_CONFIG[mode] || STATE_CONFIG.guest;
        var name = userName || DUMMY_USER_NAME;

        //  선택된 미리보기 버튼에 활성 클래스 적용
        // preview bar active state
        els.previewBtns.forEach(function (btn) {
            btn.classList.toggle('is-active', btn.dataset.mode === mode);
        });

        //  로그인 여부에 따라 공통 헤더의 게스트/사용자 영역 전환
        // header auth area
        if (config.isLoggedIn) {
            if (els.authGuest) els.authGuest.hidden = true;
            if (els.authUser) els.authUser.hidden = false;
            if (els.userName) els.userName.textContent = name + '님';
            if (els.userAvatar) els.userAvatar.textContent = name.charAt(0);
            if (els.adminLink) els.adminLink.hidden = role !== 'ADMIN';
        } else {
            if (els.authGuest) els.authGuest.hidden = false;
            if (els.authUser) els.authUser.hidden = true;
            if (els.userMenu) els.userMenu.hidden = true;
        }

        //  HERO 메인 버튼 문구·안내·이동 경로 갱신
        // hero CTA
        if (els.heroCtaLabel) els.heroCtaLabel.textContent = config.ctaLabel;
        if (els.heroCtaHint) els.heroCtaHint.textContent = config.ctaHint;
        if (els.heroCta) els.heroCta.setAttribute('href', config.ctaHref);

        //  직무 리포트 카드의 잠금/해제 상태와 안내 문구 갱신
        // job report lock/unlock
        if (config.reportLocked) {
            if (els.reportLock) els.reportLock.hidden = false;
            if (els.lockTitle) els.lockTitle.textContent = config.lockTitle;
            if (els.lockDesc) els.lockDesc.textContent = config.lockDesc;
            if (els.lockBtn) els.lockBtn.textContent = config.lockBtnLabel;
            if (els.lockBtn) els.lockBtn.dataset.href = config.ctaHref;
            if (els.reportUnlocked) els.reportUnlocked.hidden = true;
        } else {
            if (els.reportLock) els.reportLock.hidden = true;
            if (els.reportUnlocked) els.reportUnlocked.hidden = false;
            if (els.reportUsername) els.reportUsername.textContent = name;
        }
    }

    //  05. 기본 클릭 이벤트 연결: 미리보기 모드 버튼이 applyMode()를 호출
    function bindEvents() {
        els.previewBtns.forEach(function (btn) {
            btn.addEventListener('click', function () {
                applyMode(btn.dataset.mode);
            });
        });

        if (els.lockBtn) {
            els.lockBtn.addEventListener('click', function () {
                window.location.href = els.lockBtn.dataset.href || '/login';
            });
        }

        // Profile menus are owned by the shared header.js. Keeping a second
        // listener here toggled the same menu twice and made it appear broken.
    }

    // The preview toolbar stays dummy-data driven, but the real landing page
    // always derives its header and CTA state from the server-side session.
    function applyRealAuthState() {
        fetch('/api/user/me', { credentials: 'same-origin' })
            .then(function (response) { return response.json().then(function (body) { return { ok: response.ok, body: body }; }); })
            .then(function (result) {
                if (result.ok && result.body.success) {
                    var user = result.body.data;
                    applyMode('noData', user.name || user.loginId, user.role);
                    return;
                }
                applyMode('guest');
            })
            .catch(function () { applyMode('guest'); });
    }

    //  06. 로더 종료 후 HERO 진입 모션 시작: 중복 실행 방지와 구형 로더용 fallback 포함
    function runHomeEntrance(callback) {
        var finished = false;
        function enter() {
            if (finished) return;
            finished = true;
            document.body.classList.add('is-home-ready');
            window.setTimeout(callback, 110);
        }

        if (window.__jobPuzzleLoaderFinished) {
            enter();
            return;
        }

        document.addEventListener('jobpuzzle:loaderclosed', enter, { once: true });
        // Defensive fallback for an old loader fragment that does not emit the event.
        window.setTimeout(enter, 11200);
    }

    //  07. HERO 우측 결과 카드의 마우스 위치 기반 3D 기울기 효과
    function bindHeroCardTilt() {
        var cardWrap = document.querySelector('.hero__card-wrap');
        var reduceMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
        if (!cardWrap || reduceMotion || window.matchMedia('(pointer: coarse)').matches) return;

        cardWrap.addEventListener('pointermove', function (event) {
            var rect = cardWrap.getBoundingClientRect();
            var x = (event.clientX - rect.left) / rect.width - 0.5;
            var y = (event.clientY - rect.top) / rect.height - 0.5;
            cardWrap.style.transform = 'perspective(900px) rotateX(' + (-y * 3) + 'deg) rotateY(' + (x * 3) + 'deg)';
        });
        cardWrap.addEventListener('pointerleave', function () {
            cardWrap.style.transform = '';
        });
    }

    //  08. HERO 스크롤 분위기 효과: 스크롤 비율을 CSS 변수로 전달해 배경/워드마크 이동
    function initHeroScrollAtmosphere() {
        var hero = document.querySelector('.hero');
        var reduceMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
        if (!hero || reduceMotion) return;
        var ticking = false;
        //  현재 스크롤 비율 계산 후 --hero-scroll-y, --hero-scroll-mark-y 갱신
        function update() {
            var ratio = Math.max(0, Math.min(1, window.scrollY / Math.max(hero.offsetHeight, 1)));
            hero.style.setProperty('--hero-scroll-y', (ratio * 72).toFixed(1) + 'px');
            hero.style.setProperty('--hero-scroll-mark-y', (ratio * -38).toFixed(1) + 'px');
            ticking = false;
        }
        window.addEventListener('scroll', function () {
            if (ticking) return;
            ticking = true;
            window.requestAnimationFrame(update);
        }, { passive: true });
        update();
    }

    //  09. 서비스 흐름(story-stage) 활성화: 화면 중앙에 들어온 단계를 현재 단계로 표시
    function initStoryStageFocus() {
        var stages = Array.prototype.slice.call(document.querySelectorAll('.story-stage'));
        if (!stages.length || !('IntersectionObserver' in window)) return;
        var rail = document.querySelector('.service-story__rail');
        var active = document.querySelector('.story-active');
        var activeIndex = document.getElementById('story-active-index');
        var activeTitle = document.getElementById('story-active-title');
        var switchTimer;

        //  지정한 story-stage를 활성화하고 좌측 번호/제목 및 세로 진행선을 갱신
        function setCurrent(stage) {
            var index = stages.indexOf(stage);
            stages.forEach(function (item) { item.classList.toggle('is-current', item === stage); });
            if (rail) rail.style.setProperty('--story-progress', ((index / Math.max(stages.length - 1, 1)) * 100) + '%');
            if (!active || !activeIndex || !activeTitle) return;
            active.classList.add('is-switching');
            window.clearTimeout(switchTimer);
            switchTimer = window.setTimeout(function () {
                activeIndex.textContent = '0' + (index + 1) + ' / 0' + stages.length;
                activeTitle.textContent = stage.getAttribute('data-story-title') || stage.querySelector('h3').textContent;
                active.classList.remove('is-switching');
            }, 150);
        }

        //  IntersectionObserver가 중앙 감지 구간에 들어온 단계만 setCurrent()로 전달
        var stageObserver = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (!entry.isIntersecting) return;
                setCurrent(entry.target);
            });
        }, { rootMargin: '-38% 0px -38% 0px', threshold: 0.05 });

        stages.forEach(function (stage) { stageObserver.observe(stage); });
        setCurrent(stages[0]);
    }

    //  10. HERO 스크롤 안내 버튼: 대상 섹션까지 easing을 적용해 부드럽게 이동
    function bindHeroScrollCue() {
        var cue = document.querySelector('.hero__scroll-cue');
        if (!cue) return;
        cue.addEventListener('click', function (event) {
            var target = document.querySelector(cue.getAttribute('href'));
            if (!target) return;
            event.preventDefault();
            var reduceMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
            var hero = cue.closest('.hero');
            var startY = window.scrollY;
            var targetY = target.getBoundingClientRect().top + window.scrollY;
            var distance = targetY - startY;
            var duration = reduceMotion ? 0 : 900;
            var startedAt = 0;
            if (hero) hero.classList.add('is-scroll-leaving');
            //  requestAnimationFrame 기반 커스텀 스크롤 애니메이션 한 프레임 처리
            function step(now) {
                if (!startedAt) startedAt = now;
                var progress = duration ? Math.min(1, (now - startedAt) / duration) : 1;
                var eased = 1 - Math.pow(1 - progress, 4);
                window.scrollTo(0, startY + distance * eased);
                if (progress < 1) window.requestAnimationFrame(step);
                else if (hero) window.setTimeout(function () { hero.classList.remove('is-scroll-leaving'); }, 160);
            }
            window.requestAnimationFrame(step);
        });
    }

    //  12. 리포트/이용 단계 섹션의 최초 노출 애니메이션 실행
    function initSectionScenes() {
        var report = document.querySelector('[data-report-scene]');
        var steps = document.querySelector('[data-steps-scene]');
        var targets = [report, steps].filter(Boolean);
        if (!targets.length) return;
        var reduceMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

        //  감지된 섹션 종류에 맞는 완료 클래스 추가
        function reveal(target) {
            if (target === report) target.classList.add('is-report-visible');
            if (target === steps) target.classList.add('is-steps-visible');
        }

        if (reduceMotion) {
            targets.forEach(reveal);
            return;
        }

        if (window.gsap && window.ScrollTrigger) {
            gsap.registerPlugin(ScrollTrigger);
            targets.forEach(function (target) {
                ScrollTrigger.create({
                    trigger: target,
                    start: 'top 78%',
                    once: true,
                    onEnter: function () { reveal(target); }
                });
            });
            return;
        }

        if (!('IntersectionObserver' in window)) {
            targets.forEach(reveal);
            return;
        }

        //  GSAP을 불러오지 못한 경우에만 기본 Observer로 노출
        var observer = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (!entry.isIntersecting) return;
                reveal(entry.target);
                observer.unobserve(entry.target);
            });
        }, { threshold: .22, rootMargin: '0px 0px -8% 0px' });
        targets.forEach(function (target) { observer.observe(target); });
    }

    //  13. WHY 카드 무대: ScrollTrigger가 고정·스크럽·트랙 이동을 하나의 시간축으로 제어
    function initWhyMotionStage() {
        var section = document.querySelector('[data-why-motion]');
        if (!section) return;
        var cards = Array.prototype.slice.call(section.querySelectorAll('[data-why-motion-card]'));
        var prev = section.querySelector('[data-why-motion-prev]');
        var next = section.querySelector('[data-why-motion-next]');
        var current = section.querySelector('[data-why-motion-current]');
        var progressBar = section.querySelector('[data-why-motion-bar]');
        var shell = section.querySelector('.why-motion-stage__shell');
        var viewport = section.querySelector('.why-motion-viewport');
        var track = section.querySelector('.why-motion-track');
        var coarsePointer = window.matchMedia && window.matchMedia('(hover: none)').matches;
        var reduceMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
        var index = 0;
        var pointerStartX = null;
        var desktopTrigger = null;

        function clearFlips() {
            cards.forEach(function (item) {
                var card = item.querySelector('.why-motion-card');
                if (card) card.classList.remove('is-flipped');
            });
        }

        function renderIndex(nextIndex) {
            index = Math.max(0, Math.min(cards.length - 1, nextIndex));
            if (current) current.textContent = String(index + 1).padStart(2, '0');
            if (prev) prev.disabled = index === 0;
            if (next) next.disabled = index === cards.length - 1;
            cards.forEach(function (item, itemIndex) {
                var card = item.querySelector('.why-motion-card');
                item.setAttribute('aria-hidden', itemIndex === index ? 'false' : 'true');
                if (card) card.tabIndex = itemIndex === index ? 0 : -1;
            });
        }

        function mobileTargetX() {
            if (!track || !viewport || !cards[index]) return;
            var centeredOffset = Math.max(0, (viewport.clientWidth - cards[index].offsetWidth) / 2);
            return -(cards[index].offsetLeft - centeredOffset);
        }

        function renderMobileTrack(immediate) {
            if (!window.gsap || !track) return;
            gsap.to(track, {
                x: mobileTargetX(),
                yPercent: -50,
                duration: immediate ? 0 : 0.9,
                ease: 'power3.out',
                overwrite: true
            });
            if (progressBar) {
                gsap.to(progressBar, {
                    scaleX: (index + 1) / cards.length,
                    duration: immediate ? 0 : 0.65,
                    ease: 'power3.out',
                    overwrite: true
                });
            }
        }

        function scrollToIndex(targetIndex) {
            var bounded = Math.max(0, Math.min(cards.length - 1, targetIndex));
            if (bounded === index) return false;
            clearFlips();
            if (!desktopTrigger) {
                renderIndex(bounded);
                renderMobileTrack(false);
                return true;
            }
            var progress = bounded / Math.max(cards.length - 1, 1);
            window.scrollTo({
                top: desktopTrigger.start + (desktopTrigger.end - desktopTrigger.start) * progress,
                behavior: 'smooth'
            });
            return true;
        }

        function step(direction) {
            return scrollToIndex(index + direction);
        }

        if (prev) prev.addEventListener('click', function () { step(-1); });
        if (next) next.addEventListener('click', function () { step(1); });

        section.addEventListener('keydown', function (event) {
            if (event.key === 'ArrowLeft') { event.preventDefault(); step(-1); }
            if (event.key === 'ArrowRight') { event.preventDefault(); step(1); }
            if ((event.key === 'Enter' || event.key === ' ') && event.target.classList.contains('why-motion-card')) {
                event.preventDefault();
                event.target.classList.toggle('is-flipped');
            }
        });

        section.addEventListener('pointerdown', function (event) {
            if (event.pointerType === 'mouse') return;
            pointerStartX = event.clientX;
        });
        section.addEventListener('pointerup', function (event) {
            if (pointerStartX === null) return;
            var distance = event.clientX - pointerStartX;
            pointerStartX = null;
            if (Math.abs(distance) > 44) step(distance < 0 ? 1 : -1);
        });
        section.addEventListener('pointercancel', function () { pointerStartX = null; });

        cards.forEach(function (item) {
            var card = item.querySelector('.why-motion-card');
            if (!card) return;
            card.addEventListener('click', function () {
                if (!coarsePointer) return;
                var nextState = !card.classList.contains('is-flipped');
                clearFlips();
                card.classList.toggle('is-flipped', nextState);
            });
        });

        renderIndex(0);

        if (!window.gsap || !window.ScrollTrigger || !track || !shell || !cards.length) {
            renderMobileTrack(true);
            return;
        }

        gsap.registerPlugin(ScrollTrigger);
        if (reduceMotion) {
            gsap.set(track, { x: 0, yPercent: -50 });
            gsap.set(progressBar, { scaleX: 1 / cards.length });
            gsap.set('.why-motion-card__inner', { transitionDuration: 0 });
            return;
        }
        var media = gsap.matchMedia();

        media.add('(min-width: 761px)', function () {
            var maximumTravel = function () {
                return Math.max(0, cards[cards.length - 1].offsetLeft);
            };
            gsap.set(track, { x: 0, yPercent: -50 });
            gsap.set(progressBar, { scaleX: 1 / cards.length });

            var trackTween = gsap.to(track, {
                x: function () { return -maximumTravel(); },
                yPercent: -50,
                ease: 'none',
                paused: true
            });

            desktopTrigger = ScrollTrigger.create({
                trigger: section,
                start: 'top top',
                end: function () { return '+=' + Math.max(window.innerHeight * 3.2, maximumTravel() * 2.15); },
                pin: shell,
                scrub: 0.85,
                animation: trackTween,
                anticipatePin: 1,
                invalidateOnRefresh: true,
                onUpdate: function (self) {
                    var nextIndex = Math.round(self.progress * (cards.length - 1));
                    if (nextIndex !== index) {
                        clearFlips();
                        renderIndex(nextIndex);
                    }
                    if (progressBar) {
                        gsap.set(progressBar, {
                            scaleX: (1 + self.progress * (cards.length - 1)) / cards.length
                        });
                    }
                }
            });

            return function () {
                desktopTrigger = null;
                trackTween.kill();
            };
        });

        media.add('(max-width: 760px)', function () {
            desktopTrigger = null;
            renderMobileTrack(true);
            var refreshMobile = function () { renderMobileTrack(true); };
            window.addEventListener('resize', refreshMobile);
            return function () { window.removeEventListener('resize', refreshMobile); };
        });

    }

    //  13. 공통 data-reveal 요소 처리: HERO 진입 요소와 일반 스크롤 요소를 분리
    function initMotion() {
        var revealTargets = Array.prototype.slice.call(document.querySelectorAll('[data-reveal]'));
        var heroTargets = revealTargets.filter(function (element) { return element.closest('[data-hero-reveal]') || element.classList.contains('hero__card-wrap'); });
        var scrollTargets = revealTargets.filter(function (element) { return heroTargets.indexOf(element) === -1; });
        var reduceMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

        //  모션 감소 설정 또는 Observer 미지원 환경에서는 애니메이션 없이 즉시 표시
        if (reduceMotion || !('IntersectionObserver' in window)) {
            revealTargets.forEach(function (element) { element.classList.add('is-visible'); });
            document.body.classList.add('is-home-ready');
            var reducedHeroCard = document.querySelector('.hero__card-wrap');
            if (reducedHeroCard) reducedHeroCard.classList.add('is-hero-ready');
            return;
        }

        //  로더 종료 후 HERO 요소와 결과 카드를 순차적으로 표시
        runHomeEntrance(function () {
            heroTargets.forEach(function (element) { element.classList.add('is-visible'); });
            var heroCard = document.querySelector('.hero__card-wrap');
            if (heroCard) heroCard.classList.add('is-hero-ready');
        });

        if (window.gsap && window.ScrollTrigger && scrollTargets.length) {
            gsap.registerPlugin(ScrollTrigger);
            scrollTargets.forEach(function (element) {
                gsap.fromTo(element,
                    { autoAlpha: 0, y: 48 },
                    {
                        autoAlpha: 1,
                        y: 0,
                        duration: 1.05,
                        ease: 'power3.out',
                        clearProps: 'transform,opacity,visibility',
                        scrollTrigger: {
                            trigger: element,
                            start: 'top 84%',
                            once: true
                        },
                        onComplete: function () { element.classList.add('is-visible'); }
                    }
                );
            });
            return;
        }

        //  GSAP을 불러오지 못한 경우에만 기본 Observer로 노출
        var observer = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (!entry.isIntersecting) return;
                entry.target.classList.add('is-visible');
                observer.unobserve(entry.target);
            });
        }, { rootMargin: '0px 0px -8% 0px', threshold: 0.12 });

        scrollTargets.forEach(function (element) { observer.observe(element); });
    }

    //  14. 초기화 진입점: DOM 생성 완료 후 아래 순서대로 모든 기능을 연결
    document.addEventListener('DOMContentLoaded', function () {
        cacheEls();
        configurePreviewToolbar();
        bindEvents();
        var params = new URLSearchParams(window.location.search);
        if (params.get('preview') === 'true') {
            try { applyMode('ready'); } catch (error) { console.warn('[JobPuzzle] preview state skipped:', error); }
        } else {
            applyRealAuthState();
        }
        initMotion();
        bindHeroCardTilt();
        initHeroScrollAtmosphere();
        initStoryStageFocus();
        bindHeroScrollCue();
        initWhyMotionStage();
        initSectionScenes();
    });
})();
