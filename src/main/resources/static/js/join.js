// join.js — /join 페이지: 회원가입 폼 + 아이디/이메일 실시간 중복확인
(function () {
  'use strict';

  function setHint(el, message, ok) {
    el.textContent = message;
    el.classList.remove('field-hint--ok', 'field-hint--error');
    el.classList.add(ok ? 'field-hint--ok' : 'field-hint--error');
  }

  function showError(el, message) {
    el.textContent = message;
    el.classList.add('is-visible');
  }

  function hideError(el) {
    el.classList.remove('is-visible');
  }

  var CAREER_LEVEL_LABEL = {
    NEW: '신입',
    EXPERIENCED: '경력',
    ANY: '경력무관'
  };

  document.addEventListener('DOMContentLoaded', function () {
    var form = document.getElementById('join-form');
    var errorEl = document.getElementById('join-error');

    var loginIdInput = document.getElementById('loginId');
    var loginIdHint = document.getElementById('loginId-hint');
    var emailInput = document.getElementById('email');
    var emailHint = document.getElementById('email-hint');
    var passwordInput = document.getElementById('password');
    var passwordCheckInput = document.getElementById('passwordCheck');
    var passwordCheckHint = document.getElementById('passwordCheck-hint');

    var mainCategorySelect = document.getElementById('mainCategory');
    var subCategorySelect = document.getElementById('subCategory');
    var careerLevelSelect = document.getElementById('careerLevel');
    var jobCategoryHint = document.getElementById('jobCategory-hint');

    var jobCategories = [];
    var selectedJobCategoryId = null;

    function uniqueInOrder(values) {
      var seen = {};
      var result = [];
      values.forEach(function (value) {
        if (!seen[value]) {
          seen[value] = true;
          result.push(value);
        }
      });
      return result;
    }

    function fillOptions(select, options, placeholder) {
      select.innerHTML = '';
      var placeholderOption = document.createElement('option');
      placeholderOption.value = '';
      placeholderOption.textContent = placeholder;
      select.appendChild(placeholderOption);
      options.forEach(function (option) {
        var el = document.createElement('option');
        el.value = option.value;
        el.textContent = option.label;
        select.appendChild(el);
      });
    }

    fetch('/api/job-category')
      .then(function (res) { return res.json(); })
      .then(function (body) {
        jobCategories = body.data || [];
        var mainCategories = uniqueInOrder(jobCategories.map(function (c) { return c.mainCategory; }));
        fillOptions(mainCategorySelect, mainCategories.map(function (m) {
          return { value: m, label: m };
        }), '선택해주세요');
      })
      .catch(function () {
        jobCategoryHint.textContent = '직무 목록을 불러오지 못했습니다. 새로고침 후 다시 시도해주세요.';
        jobCategoryHint.classList.add('field-hint--error');
      });

    mainCategorySelect.addEventListener('change', function () {
      selectedJobCategoryId = null;
      var mainCategory = mainCategorySelect.value;

      if (!mainCategory) {
        subCategorySelect.disabled = true;
        careerLevelSelect.disabled = true;
        fillOptions(subCategorySelect, [], '대분류를 먼저 선택해주세요');
        fillOptions(careerLevelSelect, [], '중분류를 먼저 선택해주세요');
        return;
      }

      var subCategories = uniqueInOrder(
        jobCategories.filter(function (c) { return c.mainCategory === mainCategory; })
          .map(function (c) { return c.subCategory; })
      );
      fillOptions(subCategorySelect, subCategories.map(function (s) {
        return { value: s, label: s };
      }), '선택해주세요');
      subCategorySelect.disabled = false;

      careerLevelSelect.disabled = true;
      fillOptions(careerLevelSelect, [], '중분류를 먼저 선택해주세요');
    });

    subCategorySelect.addEventListener('change', function () {
      selectedJobCategoryId = null;
      var mainCategory = mainCategorySelect.value;
      var subCategory = subCategorySelect.value;

      if (!subCategory) {
        careerLevelSelect.disabled = true;
        fillOptions(careerLevelSelect, [], '중분류를 먼저 선택해주세요');
        return;
      }

      var matches = jobCategories.filter(function (c) {
        return c.mainCategory === mainCategory && c.subCategory === subCategory;
      });
      fillOptions(careerLevelSelect, matches.map(function (c) {
        return { value: c.jobCategoryId, label: CAREER_LEVEL_LABEL[c.careerLevel] || c.careerLevel };
      }), '선택해주세요');
      careerLevelSelect.disabled = false;
    });

    careerLevelSelect.addEventListener('change', function () {
      selectedJobCategoryId = careerLevelSelect.value || null;
    });

    // 아이디를 바꾸면 중복확인 결과는 다시 확인해야 하므로 초기화
    var loginIdChecked = false;
    loginIdInput.addEventListener('input', function () {
      loginIdChecked = false;
      loginIdHint.textContent = '';
    });

    var emailChecked = false;
    emailInput.addEventListener('input', function () {
      emailChecked = false;
      emailHint.textContent = '';
    });

    document.getElementById('check-id-btn').addEventListener('click', function () {
      var loginId = loginIdInput.value.trim();
      if (!loginId) {
        setHint(loginIdHint, '아이디를 입력해주세요.', false);
        return;
      }
      fetch('/api/user/check-id?loginId=' + encodeURIComponent(loginId))
        .then(function (res) { return res.json(); })
        .then(function (body) {
          var duplicate = body.data;
          loginIdChecked = !duplicate;
          setHint(loginIdHint, duplicate ? '이미 사용 중인 아이디입니다.' : '사용 가능한 아이디입니다.', !duplicate);
        })
        .catch(function () {
          setHint(loginIdHint, '중복확인 중 오류가 발생했습니다.', false);
        });
    });

    document.getElementById('check-email-btn').addEventListener('click', function () {
      var email = emailInput.value.trim();
      if (!email) {
        setHint(emailHint, '이메일을 입력해주세요.', false);
        return;
      }
      fetch('/api/user/check-email?email=' + encodeURIComponent(email))
        .then(function (res) { return res.json(); })
        .then(function (body) {
          var duplicate = body.data;
          emailChecked = !duplicate;
          setHint(emailHint, duplicate ? '이미 사용 중인 이메일입니다.' : '사용 가능한 이메일입니다.', !duplicate);
        })
        .catch(function () {
          setHint(emailHint, '중복확인 중 오류가 발생했습니다.', false);
        });
    });

    form.addEventListener('submit', function (e) {
      e.preventDefault();
      hideError(errorEl);

      if (passwordInput.value !== passwordCheckInput.value) {
        setHint(passwordCheckHint, '비밀번호가 일치하지 않습니다.', false);
        return;
      }
      passwordCheckHint.textContent = '';

      if (!loginIdChecked) {
        showError(errorEl, '아이디 중복확인을 먼저 진행해주세요.');
        return;
      }
      if (!emailChecked) {
        showError(errorEl, '이메일 중복확인을 먼저 진행해주세요.');
        return;
      }
      if (!selectedJobCategoryId) {
        showError(errorEl, '관심 직무를 선택해주세요.');
        return;
      }

      var payload = {
        loginId: loginIdInput.value.trim(),
        password: passwordInput.value,
        email: emailInput.value.trim(),
        name: document.getElementById('name').value.trim(),
        defaultJobCategoryId: Number(selectedJobCategoryId)
      };

      fetch('/api/user/join', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'same-origin',
        body: JSON.stringify(payload)
      })
        .then(function (res) {
          return res.json().then(function (body) { return { ok: res.ok, body: body }; });
        })
        .then(function (result) {
          if (result.ok && result.body.success) {
            // 가입 성공 - 로그인 페이지로 이동
            window.location.href = '/login';
          } else {
            showError(errorEl, result.body.message || '회원가입에 실패했습니다.');
          }
        })
        .catch(function () {
          showError(errorEl, '일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
        });
    });
  });
})();
