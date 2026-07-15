// settings.js

(function () {
  'use strict';

  var MINOR_BY_MAJOR = {
    'IT·개발': ['백엔드 개발', '프론트엔드 개발', '데이터 분석'],
    '경영·사무': ['인사 채용', '마케팅'],
    '디자인': ['UX/UI 디자인', 'BX 디자인']
  };

  function renderMinorOptions(major) {
    var sel = document.getElementById('job-minor');
    sel.innerHTML = (MINOR_BY_MAJOR[major] || []).map(function (m) { return '<option>' + m + '</option>'; }).join('');
  }

  document.addEventListener('DOMContentLoaded', function () {
    var majorSel = document.getElementById('job-major');
    renderMinorOptions(majorSel.value);
    majorSel.addEventListener('change', function () { renderMinorOptions(majorSel.value); });
  });
})();
