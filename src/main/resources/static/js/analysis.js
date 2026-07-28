// analysis.js — 분석 진행 상태와 읽기 전용 결과 리포트
(function () {
    'use strict';

    var root = document.getElementById('analysis-root');
    var caseId = root && root.dataset.analysisCaseId;
    var state = {
        disposed: false,
        loading: false,
        running: false,
        pollTimer: null,
        retryTimer: null,
        statusSequence: 0,
        automaticRetries: 0,
        maximumAutomaticRetries: 2
    };
    var stages = [
        ['jobPostingAnalysisStatus', '채용공고 분석'],
        ['candidateMaterialAnalysisStatus', '지원자 자료 분석'],
        ['guideContextStatus', '직무 가이드 적용'],
        ['customizedAnalysisStatus', '맞춤 결과 생성']
    ];
    var stageText = { NOT_STARTED: '분석 대기', RUNNING: '분석 중', SUCCEEDED: '분석 완료', FAILED: '분석 실패' };

    function showAcceptedNotice() {
        var notice = sessionStorage.getItem('jobpuzzle_analysis_notice');
        if (!notice) return;
        sessionStorage.removeItem('jobpuzzle_analysis_notice');
        var toast = el('div', 'analysis-accepted-toast', notice + ' 이 화면에서 바로 분석을 시작할 수 있어요.');
        document.body.appendChild(toast);
        requestAnimationFrame(function () { toast.classList.add('is-visible'); });
        setTimeout(function () {
            toast.classList.remove('is-visible');
            setTimeout(function () { toast.remove(); }, 250);
        }, 3200);
    }

    function stageLabel(stageKey) {
        for (var i = 0; i < stages.length; i += 1) {
            if (stages[i][0] === stageKey) return stages[i][1];
        }
        return '분석 과정';
    }
    var levelMeta = {
        HIGH: { label: '충족', className: 'high' },
        MEDIUM: { label: '일부 충족', className: 'medium' },
        NONE: { label: '근거 부족', className: 'none' }
    };
    var typeText = {
        REQUIRED: '필수', PREFERRED: '우대',
        EXPERIENCE: '경험', SKILL: '기술', PROBLEM_SOLVING: '문제 해결', GENERAL: '일반'
    };
    var readinessText = {
        READY: '지원 준비가 잘 되어 있어요',
        PARTIAL: '부분적으로 준비되어 있어요',
        INSUFFICIENT: '근거를 조금 더 보완해 주세요'
    };
    var errorText = {
        ANALYSIS_002: '분석 작업을 찾을 수 없거나 접근 권한이 없습니다.',
        ANALYSIS_009: '분석을 시작할 준비가 아직 완료되지 않았습니다.',
        ANALYSIS_010: '분석 결과의 무결성을 확인하지 못했습니다.',
        ANALYSIS_011: '분석 자료의 vector 색인을 준비하지 못했습니다.',
        AI_001: '분석 결과를 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.',
        AI_002: '현재 분석을 준비할 수 없습니다. 관리자에게 문의해 주세요.'
    };

    function el(tag, className, text) {
        var node = document.createElement(tag);
        if (className) node.className = className;
        if (text != null) node.textContent = text;
        return node;
    }

    function clear() {
        if (!state.disposed) root.replaceChildren();
    }

    function api(path, options) {
        options = options || {};
        return fetch(path, {
            method: options.method || 'GET',
            credentials: 'same-origin',
            headers: options.headers || {}
        }).then(function (response) {
            return response.json().catch(function () { return {}; }).then(function (body) {
                if (!response.ok || !body.success) {
                    var error = new Error(body.message || '요청을 처리하지 못했습니다.');
                    error.code = body.code;
                    error.status = response.status;
                    throw error;
                }
                return body.data;
            });
        });
    }

    function analysisApi(suffix, options) {
        return api('/api/analysis/cases/' + caseId + suffix, options);
    }

    function friendlyError(error) {
        if (error && error.code && errorText[error.code]) return errorText[error.code];
        if (error && error.status === 401) return '로그인 후 이용해 주세요.';
        if (error && error.status === 403) return '이 분석 결과를 볼 권한이 없습니다.';
        return '요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.';
    }

    function section(title, description, count) {
        var card = el('section', 'report-card report-section');
        var head = el('div', 'report-section__head');
        var copy = el('div');
        copy.appendChild(el('h2', null, title));
        if (description) copy.appendChild(el('p', null, description));
        head.appendChild(copy);
        if (count != null) head.appendChild(el('span', 'report-count', String(count)));
        card.appendChild(head);
        return card;
    }

    function pill(level) {
        var meta = levelMeta[level] || levelMeta.NONE;
        var node = el('span', 'status-pill status-pill--' + meta.className, meta.label);
        node.appendChild(el('span', 'match-item__chevron', '⌄'));
        return node;
    }

    function renderProgress(status, runError) {
        clear();
        root.setAttribute('aria-busy', status.analysisCaseStatus === 'ANALYZING' ? 'true' : 'false');
        var card = el('section', 'report-card progress-card');
        var isFailed = status.analysisCaseStatus === 'FAILED';
        var isRunning = status.analysisCaseStatus === 'ANALYZING';
        var isReady = status.analysisCaseStatus === 'INPUT_CONFIRMED';
        card.appendChild(el('p', 'report-eyebrow', isFailed ? 'ANALYSIS NEEDS REVIEW' : isRunning ? 'ANALYSIS IN PROGRESS' : 'ANALYSIS READY'));
        card.appendChild(el('h2', null, isFailed ? '분석 결과를 확인해 주세요' : isRunning ? '지원 자료를 차근차근 읽고 있어요' : '분석을 시작할 준비가 되었어요'));
        card.appendChild(el('p', 'progress-card__description', isFailed
            ? '선택한 자료와 앞선 분석 결과는 보존되어 있습니다. 같은 자료로 다시 실행할 수 있어요.'
            : isRunning ? '페이지를 닫아도 서버의 분석 작업은 계속 진행됩니다.' : '선택한 공고와 지원 자료를 바탕으로 맞춤 분석을 시작합니다.'));
        var list = el('ol', 'analysis-stage-chain');
        stages.forEach(function (stage) {
            var value = status[stage[0]] || 'NOT_STARTED';
            var row = el('li', 'analysis-stage-node analysis-stage-node--' + value);
            row.appendChild(el('span', 'analysis-stage-node__mark', value === 'SUCCEEDED' ? '✓' : value === 'FAILED' ? '!' : value === 'RUNNING' ? '·' : ''));
            var copy = el('div', 'analysis-stage-node__copy');
            copy.appendChild(el('strong', null, stage[1]));
            copy.appendChild(el('span', null, stageText[value] || '상태 확인 중'));
            row.appendChild(copy);
            list.appendChild(row);
        });
        card.appendChild(list);
        var failure = runError || status.userMessage;
        if (failure && isFailed) {
            var message = typeof failure === 'string' ? failure : friendlyError(failure);
            var failureBox = el('div', 'report-error');
            failureBox.appendChild(el('strong', null, status.latestFailureStage
                ? stageLabel(status.latestFailureStage) + ' 단계에서 멈췄어요.'
                : '분석 결과 생성 중 멈췄어요.'));
            failureBox.appendChild(el('p', null, message));
            card.appendChild(failureBox);
        }
        if (isReady || isFailed) {
            var actions = el('div', 'report-actions');
            var button = el('button', 'btn btn--primary', status.analysisCaseStatus === 'FAILED' ? '분석 다시 시도' : '분석 시작');
            button.type = 'button';
            button.disabled = state.running;
            button.addEventListener('click', function () {
                if (isFailed && !window.confirm('선택한 자료와 앞선 분석 결과는 유지됩니다.\n\n분석을 다시 실행하면 AI·임베딩 요청이 다시 발생할 수 있습니다. 계속할까요?')) return;
                runAnalysis(isFailed);
            });
            actions.appendChild(button);
            card.appendChild(actions);
        }
        root.appendChild(card);
    }

    function countLevels(matches) {
        return (matches || []).reduce(function (counts, match) {
            counts[match.matchLevel] = (counts[match.matchLevel] || 0) + 1;
            return counts;
        }, { HIGH: 0, MEDIUM: 0, NONE: 0 });
    }

    function warningText(result) {
        var limits = result.readiness && result.readiness.limitations || [];
        return limits.find(function (item) { return /불일치|맞지 않|직무/.test(item); }) || '';
    }

    function renderHero(result) {
        var matches = result.requirementMatches || [];
        var counts = countLevels(matches);
        var total = matches.length || 1;
        var connected = counts.HIGH + counts.MEDIUM;
        var percent = Math.round(connected / total * 100);
        var ready = result.readiness || {};
        var card = el('section', 'report-card report-hero');
        var top = el('div', 'report-hero__top');
        var copy = el('div');
        copy.appendChild(el('p', 'report-eyebrow', 'YOUR JOB FIT'));
        copy.appendChild(el('h2', null, readinessText[ready.status] || '분석 결과가 준비됐어요'));
        copy.appendChild(el('p', 'report-hero__reason', ready.reason || '요구사항과 지원 자료의 연결 결과를 확인해 보세요.'));
        top.appendChild(copy);
        var ring = el('div', 'fit-ring');
        ring.style.setProperty('--fit-value', percent);
        var ringValue = el('div', 'fit-ring__value', percent + '%');
        ringValue.appendChild(el('small', null, '연결 근거 발견'));
        ring.appendChild(ringValue);
        top.appendChild(ring);
        card.appendChild(top);

        var distribution = el('div', 'fit-distribution');
        var labels = el('div', 'fit-distribution__labels');
        [['high', '충족 ' + counts.HIGH], ['medium', '일부 충족 ' + counts.MEDIUM], ['none', '근거 부족 ' + counts.NONE]].forEach(function (item) {
            var label = el('span');
            label.appendChild(el('i', 'fit-dot fit-dot--' + item[0]));
            label.appendChild(document.createTextNode(item[1]));
            labels.appendChild(label);
        });
        distribution.appendChild(labels);
        var bar = el('div', 'fit-bar');
        [['high', counts.HIGH], ['medium', counts.MEDIUM], ['none', counts.NONE]].forEach(function (item) {
            var segment = el('span', 'fit-bar__' + item[0]);
            segment.style.width = (item[1] / total * 100) + '%';
            bar.appendChild(segment);
        });
        distribution.appendChild(bar);
        card.appendChild(distribution);

        var warning = warningText(result);
        if (warning) {
            var box = el('div', 'report-warning');
            box.appendChild(el('span', 'report-warning__icon', '!'));
            var warningCopy = el('div');
            warningCopy.appendChild(el('strong', null, '분석 기준을 한 번 확인해 주세요'));
            warningCopy.appendChild(el('p', null, warning));
            box.appendChild(warningCopy);
            card.appendChild(box);
        }
        return card;
    }

    function sourceSummary(refs) {
        if (!Array.isArray(refs) || !refs.length) return '연결된 원문 위치가 없습니다.';
        return refs.map(function (ref) {
            var type = {
                RESUME: '이력서', COVER_LETTER: '자기소개서', PORTFOLIO: '포트폴리오',
                EXPERIENCE_NOTE: '경험정리', JOB_POSTING: '채용공고', COMPANY_INFO: '회사정보'
            }[ref.documentType] || ref.documentType || '지원 자료';
            return type + (ref.pageNumber ? ' · ' + ref.pageNumber + '페이지' : '');
        }).filter(function (value, index, all) { return all.indexOf(value) === index; }).join(', ');
    }

    function evidenceBox(label, value, extraClass) {
        var box = el('div', 'evidence-box' + (extraClass ? ' ' + extraClass : ''));
        box.appendChild(el('strong', null, label));
        box.appendChild(el('p', null, value || '확인된 내용이 없습니다.'));
        return box;
    }

    function renderMatches(result) {
        var matches = result.requirementMatches || [];
        var card = section('요구사항 연결 결과', '각 요구사항에 어떤 지원 자료가 연결됐고 무엇이 부족한지 확인할 수 있어요.', matches.length);
        var filters = el('div', 'report-filters');
        var filterData = [
            ['ALL', '전체 ' + matches.length],
            ['REQUIRED', '필수 ' + matches.filter(function (m) { return m.requirementType === 'REQUIRED'; }).length],
            ['PREFERRED', '우대 ' + matches.filter(function (m) { return m.requirementType === 'PREFERRED'; }).length],
            ['NONE', '근거 부족 ' + matches.filter(function (m) { return m.matchLevel === 'NONE'; }).length]
        ];
        var list = el('div', 'match-list');
        filterData.forEach(function (filter, index) {
            var button = el('button', 'report-filter' + (index === 0 ? ' is-active' : ''), filter[1]);
            button.type = 'button';
            button.addEventListener('click', function () {
                filters.querySelectorAll('.report-filter').forEach(function (node) { node.classList.remove('is-active'); });
                button.classList.add('is-active');
                list.querySelectorAll('.match-item').forEach(function (item) {
                    item.hidden = filter[0] !== 'ALL' && item.dataset.type !== filter[0] && item.dataset.level !== filter[0];
                });
            });
            filters.appendChild(button);
        });
        card.appendChild(filters);

        if (!matches.length) {
            card.appendChild(el('p', 'report-empty', '표시할 요구사항 연결 결과가 없습니다.'));
            return card;
        }
        var questions = result.questionSet && result.questionSet.questions || [];
        matches.forEach(function (match, index) {
            var item = el('article', 'match-item');
            item.dataset.type = match.requirementType || '';
            item.dataset.level = match.matchLevel || '';
            var button = el('button', 'match-item__button');
            button.type = 'button';
            button.setAttribute('aria-expanded', 'false');
            button.appendChild(el('span', 'match-item__index', String(index + 1).padStart(2, '0')));
            var title = el('div');
            title.appendChild(el('p', 'match-item__title', match.requirement || '요구사항'));
            var relatedQuestions = questions.filter(function (q) { return q.relatedRequirementId === match.requirementId; }).length;
            title.appendChild(el('p', 'match-item__meta', (typeText[match.requirementType] || match.requirementType || '요구사항') + ' · 연결 자료 ' + (Array.isArray(match.candidateSourceRefs) ? match.candidateSourceRefs.length : 0) + '개' + (relatedQuestions ? ' · 관련 질문 ' + relatedQuestions + '개' : '')));
            button.appendChild(title);
            button.appendChild(pill(match.matchLevel));
            item.appendChild(button);
            var detail = el('div', 'match-item__detail');
            detail.appendChild(evidenceBox('확인된 지원자 근거', match.candidateEvidence));
            detail.appendChild(evidenceBox('부족한 근거', match.missingPoint, 'evidence-box--missing'));
            detail.appendChild(evidenceBox('판단 이유', match.reason, 'evidence-box--wide'));
            detail.appendChild(evidenceBox('사용된 자료', sourceSummary(match.candidateSourceRefs), 'evidence-box--wide'));
            item.appendChild(detail);
            button.addEventListener('click', function () {
                var open = item.classList.toggle('is-open');
                button.setAttribute('aria-expanded', String(open));
            });
            list.appendChild(item);
        });
        card.appendChild(list);
        return card;
    }

    function focusText(value) {
        if (Array.isArray(value)) return value.join(' · ');
        if (value && typeof value === 'object') return Object.keys(value).map(function (key) { return value[key]; }).join(' · ');
        return value ? String(value) : '별도 평가 포인트 없음';
    }

    function renderQuestions(result) {
        var questions = result.questionSet && result.questionSet.questions || [];
        var card = section('면접에서 확인할 질문', '부족하거나 더 확인이 필요한 근거를 중심으로 만든 질문이에요.', questions.length);
        if (!questions.length) {
            card.appendChild(el('p', 'report-empty', '현재 자료에서는 생성된 질문이 없습니다.'));
            return card;
        }
        var types = ['ALL'].concat(questions.map(function (q) { return q.questionType; }).filter(function (value, index, all) { return all.indexOf(value) === index; }));
        var filters = el('div', 'report-filters');
        var list = el('div', 'question-list');
        types.forEach(function (type, index) {
            var count = type === 'ALL' ? questions.length : questions.filter(function (q) { return q.questionType === type; }).length;
            var button = el('button', 'report-filter' + (index === 0 ? ' is-active' : ''), (type === 'ALL' ? '전체' : typeText[type] || type) + ' ' + count);
            button.type = 'button';
            button.addEventListener('click', function () {
                filters.querySelectorAll('.report-filter').forEach(function (node) { node.classList.remove('is-active'); });
                button.classList.add('is-active');
                list.querySelectorAll('.question-item').forEach(function (item) { item.hidden = type !== 'ALL' && item.dataset.type !== type; });
            });
            filters.appendChild(button);
        });
        card.appendChild(filters);
        questions.slice().sort(function (a, b) { return a.displayOrder - b.displayOrder; }).forEach(function (question, index) {
            var item = el('article', 'question-item');
            item.dataset.type = question.questionType || '';
            item.appendChild(el('span', 'question-item__number', String(index + 1).padStart(2, '0')));
            item.appendChild(el('span', 'question-item__type', typeText[question.questionType] || question.questionType || '질문'));
            item.appendChild(el('h3', null, question.question));
            var details = el('details');
            details.open = true;
            details.appendChild(el('summary', null, '이 질문에서 준비할 핵심'));
            var detail = el('div', 'question-item__detail');
            var intent = el('p');
            intent.appendChild(el('strong', null, '왜 묻는 질문인가요?'));
            intent.appendChild(document.createTextNode(question.intent || '지원자의 경험을 구체적으로 확인합니다.'));
            detail.appendChild(intent);
            var focus = el('p');
            focus.appendChild(el('strong', null, '답변할 때 집중할 기준'));
            focus.appendChild(document.createTextNode(focusText(question.evaluationFocus)));
            detail.appendChild(focus);
            if (question.relatedRequirementId) {
                var related = el('p');
                related.appendChild(el('strong', null, '연결된 공고 기준'));
                related.appendChild(document.createTextNode(question.relatedRequirementId));
                detail.appendChild(related);
            }
            details.appendChild(detail);
            item.appendChild(details);
            list.appendChild(item);
        });
        card.appendChild(list);
        var actions = el('div', 'report-actions');
        var interviewLink = el('a', 'btn btn--primary', '질문을 선택하고 면접 시작');
        interviewLink.href = '/interview.html?analysisCaseId=' + encodeURIComponent(caseId);
        interviewLink.style.textDecoration = 'none';
        actions.appendChild(interviewLink);
        card.appendChild(actions);
        return card;
    }

    function renderPlans(result) {
        var plans = result.actionPlans || [];
        var visible = plans.slice(0, 4);
        var card = section('추천 보완 순서', '면접 전 먼저 준비하면 좋은 내용을 추렸어요.', plans.length);
        if (!visible.length) {
            card.appendChild(el('p', 'report-empty', '현재 등록된 보완 과제가 없습니다.'));
            return card;
        }
        var list = el('div', 'plan-list');
        visible.forEach(function (plan, index) {
            var item = el('article', 'plan-item');
            item.appendChild(el('p', 'plan-item__label', 'PRIORITY ' + String(index + 1).padStart(2, '0')));
            item.appendChild(el('h3', null, plan.missingPoint || '보완할 근거'));
            item.appendChild(el('p', null, plan.suggestion || '관련 경험을 구체적인 사례로 정리해 보세요.'));
            list.appendChild(item);
        });
        card.appendChild(list);
        return card;
    }

    function renderResult(result) {
        clear();
        root.setAttribute('aria-busy', 'false');
        root.appendChild(renderHero(result));
        root.appendChild(renderMatches(result));
        root.appendChild(renderQuestions(result));
        root.appendChild(renderPlans(result));
    }

    function schedulePoll() {
        clearTimeout(state.pollTimer);
        if (!state.disposed) state.pollTimer = setTimeout(loadStatus, 3500);
    }

    function showRetryProgress(status, delaySeconds) {
        clear();
        root.setAttribute('aria-busy', 'true');
        var card = el('section', 'report-card progress-card');
        card.appendChild(el('p', 'report-eyebrow', 'ANALYSIS RECOVERY'));
        card.appendChild(el('h2', null, '분석 연결을 다시 확인하고 있어요'));
        card.appendChild(el('p', 'progress-card__description',
            '일시적인 응답 지연을 감지해 ' + delaySeconds + '초 후 자동으로 이어서 진행합니다.'));
        var list = el('ol', 'analysis-stage-chain');
        stages.forEach(function (stage) {
            var value = status[stage[0]] === 'FAILED' ? 'RUNNING' : (status[stage[0]] || 'NOT_STARTED');
            var row = el('li', 'analysis-stage-node analysis-stage-node--' + value);
            row.appendChild(el('span', 'analysis-stage-node__mark', value === 'SUCCEEDED' ? '✓' : '·'));
            var copy = el('div', 'analysis-stage-node__copy');
            copy.appendChild(el('strong', null, stage[1]));
            copy.appendChild(el('span', null, value === 'SUCCEEDED' ? '분석 완료' : '다시 연결 중'));
            row.appendChild(copy);
            list.appendChild(row);
        });
        card.appendChild(list);
        root.appendChild(card);
    }

    function canRetryAutomatically(status) {
        var serverHasAttempts = status.attemptCount == null || status.maxAttempts == null
            || status.attemptCount < status.maxAttempts;
        return status.analysisCaseStatus === 'FAILED'
            && status.retryable === true
            && serverHasAttempts
            && state.automaticRetries < state.maximumAutomaticRetries;
    }

    function scheduleAutomaticRetry(status) {
        if (state.retryTimer || state.running || state.disposed) return;
        var delaySeconds = Number(status.retryAfterSeconds)
            || Math.min(3 * Math.pow(2, state.automaticRetries), 12);
        state.automaticRetries++;
        showRetryProgress(status, delaySeconds);
        state.retryTimer = setTimeout(function () {
            state.retryTimer = null;
            runAnalysis(true);
        }, delaySeconds * 1000);
    }

    function handleStatus(status) {
        if (status.analysisCaseStatus === 'COMPLETED') {
            clearTimeout(state.pollTimer);
            clearTimeout(state.retryTimer);
            window.location.replace('/analysis-results/' + encodeURIComponent(caseId));
            return;
        }
        if (canRetryAutomatically(status)) {
            scheduleAutomaticRetry(status);
            return;
        }
        renderProgress(status);
        if (status.analysisCaseStatus === 'ANALYZING') schedulePoll();
    }

    function loadStatus() {
        if (state.loading || state.disposed) return;
        var sequence = ++state.statusSequence;
        state.loading = true;
        analysisApi('/status').then(function (status) {
            state.loading = false;
            if (state.disposed || sequence !== state.statusSequence) return;
            handleStatus(status);
        }).catch(function (error) {
            state.loading = false;
            if (sequence !== state.statusSequence) return;
            clear();
            var card = el('section', 'report-card progress-card');
            card.appendChild(el('h2', null, '결과를 불러오지 못했어요'));
            card.appendChild(el('div', 'report-error', friendlyError(error)));
            root.appendChild(card);
        });
    }

    function runAnalysis(reuseExistingIndex) {
        if (state.running) return;
        state.running = true;
        var ensureIndexed = reuseExistingIndex
            ? Promise.resolve()
            : api('/api/analysis-cases/' + caseId + '/index', { method: 'POST' });
        ensureIndexed
            .then(function () {
                var runPromise = analysisApi('/run', { method: 'POST' });
                schedulePoll();
                return runPromise;
            })
            .then(function () {
                state.running = false;
                loadStatus();
            })
            .catch(function (error) {
                state.running = false;
                clearTimeout(state.pollTimer);
                // 이미 전송된 이전 status 응답이 실패 화면을 덮지 못하게 무효화한다.
                state.statusSequence++;
                analysisApi('/status').then(function (status) {
                    if (reuseExistingIndex === true && canRetryAutomatically(status)) {
                        scheduleAutomaticRetry(status);
                        return;
                    }
                    renderProgress(status, reuseExistingIndex === true ? null : error);
                }).catch(function () {
                    renderProgress(
                        { analysisCaseStatus: 'FAILED' },
                        reuseExistingIndex === true ? null : error
                    );
                });
            });
    }

    if (!root || !caseId || !/^\d+$/.test(caseId) || Number(caseId) <= 0) {
        if (root) root.appendChild(el('p', 'report-empty', '유효하지 않은 분석 작업 주소입니다.'));
        return;
    }
    window.addEventListener('pagehide', function () {
        state.disposed = true;
        clearTimeout(state.pollTimer);
        clearTimeout(state.retryTimer);
    }, { once: true });
    showAcceptedNotice();
    loadStatus();
})();
