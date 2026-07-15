// common.js — shared helpers for app pages (dashboard, mydata, jobanalysis, interview, interview-result, reports)
(function (global) {
  function esc(s) {
    var d = document.createElement('div');
    d.textContent = s || '';
    return d.innerHTML;
  }

  // Binds a tabbar__btn group: toggles is-active, shows the matching panel, and
  // optionally invokes onSelect(value) so callers can re-render their own content.
  function bindTabGroup(opts) {
    var btnSelector = opts.btnSelector;
    var datasetKey = opts.datasetKey;
    var panelSelector = opts.panelSelector;
    var panelDatasetKey = opts.panelDatasetKey;
    var onSelect = opts.onSelect;

    document.querySelectorAll(btnSelector).forEach(function (btn) {
      btn.addEventListener('click', function () {
        document.querySelectorAll(btnSelector).forEach(function (b) { b.classList.remove('is-active'); });
        btn.classList.add('is-active');
        var value = btn.dataset[datasetKey];
        if (panelSelector) {
          document.querySelectorAll(panelSelector).forEach(function (p) {
            p.hidden = p.dataset[panelDatasetKey] !== value;
          });
        }
        if (onSelect) onSelect(value);
      });
    });
  }

  global.esc = esc;
  global.bindTabGroup = bindTabGroup;
})(window);
