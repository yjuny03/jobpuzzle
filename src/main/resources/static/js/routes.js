// JobPuzzle 공통 URL 경계. 모든 화면·기능 요청은 애플리케이션 context path 아래에서 조합한다.
(function (window) {
  'use strict';

  var base = '/jobpuzzle';

  function path(value) {
    if (!value || value === '/') return base + '/';
    return base + (value.charAt(0) === '/' ? value : '/' + value);
  }

  window.JobPuzzleRoutes = Object.freeze({
    base: base,
    path: path
  });
})(window);
