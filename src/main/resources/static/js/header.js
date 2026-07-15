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

  document.addEventListener('DOMContentLoaded', bindAppUserMenu);
})();
