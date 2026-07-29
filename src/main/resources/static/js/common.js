// common.js — shared helpers for app pages (dashboard, mydata, interview, interview-result, reports)
(function (global) {
  function esc(s) {
    var d = document.createElement('div');
    d.textContent = s || '';
    return d.innerHTML;
  }

  var TOAST_TYPES = ['success', 'error', 'warning', 'info'];
  var TOAST_DEFAULTS = {
    success: { title: '완료', duration: 3200 },
    error: { title: '오류', duration: 5000 },
    warning: { title: '확인 필요', duration: 4200 },
    info: { title: '안내', duration: 3500 }
  };

  function normalizeToastOptions(typeOrOptions, message, duration) {
    var options = typeof typeOrOptions === 'object' && typeOrOptions !== null
      ? Object.assign({}, typeOrOptions)
      : { type: typeOrOptions, message: message, duration: duration };
    var type = TOAST_TYPES.indexOf(options.type) >= 0 ? options.type : 'info';
    var defaults = TOAST_DEFAULTS[type];

    return {
      type: type,
      title: options.title === undefined ? defaults.title : String(options.title || ''),
      message: String(options.message || ''),
      duration: Number(options.duration) > 0 ? Number(options.duration) : defaults.duration
    };
  }

  function getToastRegion() {
    var region = document.getElementById('jp-toast-region');
    if (region) return region;

    region = document.createElement('div');
    region.id = 'jp-toast-region';
    region.className = 'jp-toast-region';
    region.setAttribute('aria-live', 'polite');
    region.setAttribute('aria-atomic', 'false');
    document.body.appendChild(region);
    return region;
  }

  function showToast(typeOrOptions, message, duration) {
    var options = normalizeToastOptions(typeOrOptions, message, duration);
    var toast = document.createElement('section');
    var icon = document.createElement('span');
    var content = document.createElement('div');
    var title = document.createElement('strong');
    var copy = document.createElement('p');
    var close = document.createElement('button');
    var timer;
    var removed = false;

    toast.className = 'jp-toast jp-toast--' + options.type;
    toast.setAttribute('role', options.type === 'error' ? 'alert' : 'status');
    toast.dataset.toastType = options.type;

    icon.className = 'jp-toast__icon';
    icon.setAttribute('aria-hidden', 'true');

    content.className = 'jp-toast__content';
    title.className = 'jp-toast__title';
    title.textContent = options.title;
    copy.className = 'jp-toast__message';
    copy.textContent = options.message;
    content.appendChild(title);
    if (options.message) content.appendChild(copy);

    close.className = 'jp-toast__close';
    close.type = 'button';
    close.setAttribute('aria-label', '알림 닫기');
    close.textContent = '×';

    toast.appendChild(icon);
    toast.appendChild(content);
    toast.appendChild(close);
    getToastRegion().appendChild(toast);

    function removeToast() {
      if (removed) return;
      removed = true;
      global.clearTimeout(timer);
      toast.classList.remove('is-visible');
      toast.classList.add('is-leaving');
      global.setTimeout(function () { toast.remove(); }, 220);
    }

    close.addEventListener('click', removeToast);
    global.requestAnimationFrame(function () {
      global.requestAnimationFrame(function () { toast.classList.add('is-visible'); });
    });
    timer = global.setTimeout(removeToast, options.duration);

    return { element: toast, close: removeToast };
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
  global.showToast = showToast;
  global.JobPuzzleNotice = {
    show: showToast,
    success: function (message, duration) { return showToast('success', message, duration); },
    error: function (message, duration) { return showToast('error', message, duration); },
    warning: function (message, duration) { return showToast('warning', message, duration); },
    info: function (message, duration) { return showToast('info', message, duration); }
  };
})(window);
