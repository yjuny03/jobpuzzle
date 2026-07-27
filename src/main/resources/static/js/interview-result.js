(function () {
  'use strict';

  document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('.tabbar__btn[data-tab]').forEach(function (button) {
      button.addEventListener('click', function () {
        var tab = button.dataset.tab;
        document.querySelectorAll('.tabbar__btn[data-tab]').forEach(function (item) {
          item.classList.toggle('is-active', item === button);
        });
        document.querySelectorAll('.tab-panel[data-panel]').forEach(function (panel) {
          panel.hidden = panel.dataset.panel !== tab;
        });
      });
    });
  });
})();
