// dashboard-puzzle.js — dashboard autoplay and shared lightweight puzzle-piece canvases
(function () {
  'use strict';

  var heroCanvas = document.getElementById('jobPuzzleCanvas');
  var cardCanvases = [];
  var canvasObserver = null;

  function refreshCanvases() {
    cardCanvases = Array.prototype.slice.call(
      document.querySelectorAll(
      '.stat-puzzle-canvas[data-puzzle-piece], .evaluation-puzzle-canvas[data-puzzle-piece]'
      + ', .mode-puzzle-canvas[data-puzzle-piece], .analysis-puzzle-canvas[data-puzzle-complete]'
      )
    );
  }

  refreshCanvases();
  if (window.MutationObserver && document.body) {
    canvasObserver = new MutationObserver(refreshCanvases);
    canvasObserver.observe(document.body, { childList: true, subtree: true });
  }
  var reduceMotion = window.matchMedia
    && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  var disposed = false;
  var autoplayTimer = null;

  // The landing WebGL scene keeps its original click behavior. On this page only,
  // synthetic clicks advance the same assembly stages at a calm fixed interval.
  function startAutoplay() {
    if (!heroCanvas || reduceMotion) return;
    autoplayTimer = window.setInterval(function () {
      if (!document.hidden && !disposed) {
        heroCanvas.dispatchEvent(new MouseEvent('click', {
          bubbles: true,
          cancelable: true,
          view: window
        }));
      }
    }, 1100);
  }

  function resizeCanvas(canvas) {
    var ratio = Math.min(window.devicePixelRatio || 1, 2);
    var width = Math.max(1, Math.round(canvas.clientWidth * ratio));
    var height = Math.max(1, Math.round(canvas.clientHeight * ratio));
    if (canvas.width !== width || canvas.height !== height) {
      canvas.width = width;
      canvas.height = height;
    }
    return ratio;
  }

  function puzzlePath(ctx, size, variant) {
    var left = -size;
    var right = size;
    var top = -size * 0.78;
    var bottom = size * 0.78;
    var tab = size * 0.32;
    var neck = size * 0.28;
    var topTab = variant % 2 === 0 ? -1 : 1;
    var sideTab = variant < 2 ? 1 : -1;

    ctx.beginPath();
    ctx.moveTo(left, top);
    ctx.lineTo(-neck, top);
    ctx.bezierCurveTo(
      -neck,
      top + topTab * tab,
      neck,
      top + topTab * tab,
      neck,
      top
    );
    ctx.lineTo(right, top);
    ctx.lineTo(right, -neck);
    ctx.bezierCurveTo(
      right + sideTab * tab,
      -neck,
      right + sideTab * tab,
      neck,
      right,
      neck
    );
    ctx.lineTo(right, bottom);
    ctx.lineTo(neck, bottom);
    ctx.bezierCurveTo(neck, bottom - tab, -neck, bottom - tab, -neck, bottom);
    ctx.lineTo(left, bottom);
    ctx.lineTo(left, neck);
    ctx.bezierCurveTo(left + tab, neck, left + tab, -neck, left, -neck);
    ctx.closePath();
  }

  function drawPiece(canvas, now) {
    var ctx = canvas.getContext('2d');
    if (!ctx) return;
    var ratio = resizeCanvas(canvas);
    var width = canvas.width;
    var height = canvas.height;
    var index = Number(canvas.dataset.puzzlePiece) || 0;
    var phase = index * 1.37;
    var motion = reduceMotion ? 0 : 1;
    var floatY = Math.sin(now * 0.00105 + phase) * 5 * ratio * motion;
    var rotate = Math.sin(now * 0.00072 + phase) * 0.075 * motion;
    var scale = 1 + Math.sin(now * 0.00086 + phase) * 0.025 * motion;
    var size = Math.min(width, height) * 0.25;
    var colors = [
      ['#4B83A8', '#285A7A'],
      ['#58A5AA', '#3D8F92'],
      ['#6D9AB5', '#3F6F8C'],
      ['#70A9A7', '#438783']
    ][index % 4];

    ctx.clearRect(0, 0, width, height);
    ctx.save();
    ctx.translate(width * 0.54, height * 0.5 + floatY);
    ctx.rotate(rotate);
    ctx.scale(scale, scale);
    ctx.shadowColor = 'rgba(38, 59, 75, 0.24)';
    ctx.shadowBlur = 9 * ratio;
    ctx.shadowOffsetY = 5 * ratio;

    puzzlePath(ctx, size, index);
    var gradient = ctx.createLinearGradient(-size, -size, size, size);
    gradient.addColorStop(0, colors[0]);
    gradient.addColorStop(1, colors[1]);
    ctx.fillStyle = gradient;
    ctx.fill();

    ctx.shadowColor = 'transparent';
    ctx.lineWidth = 2 * ratio;
    ctx.strokeStyle = '#263B4B';
    ctx.stroke();

    puzzlePath(ctx, size * 0.88, index);
    ctx.lineWidth = 1 * ratio;
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.28)';
    ctx.stroke();
    ctx.restore();
  }

  function drawAssembly(canvas, now) {
    var ctx = canvas.getContext('2d');
    if (!ctx) return;
    var ratio = resizeCanvas(canvas);
    var width = canvas.width;
    var height = canvas.height;
    var completed = Math.max(0, Math.min(4, Number(canvas.dataset.puzzleComplete) || 0));
    var centerX = width * 0.5;
    var centerY = height * 0.51;
    var size = Math.min(width, height) * 0.16;
    var spreadX = size * 1.02;
    var spreadY = size * 0.8;
    var reduce = reduceMotion ? 0 : 1;
    var pulse = (Math.sin(now * 0.0022) + 1) * 0.5 * reduce;
    var colors = [
      ['#5B91B3', '#326B90'],
      ['#65AAA9', '#3C8787'],
      ['#718EB5', '#4C6894'],
      ['#77AAA3', '#46847E']
    ];
    var targets = [
      [-spreadX, -spreadY],
      [spreadX, -spreadY],
      [-spreadX, spreadY],
      [spreadX, spreadY]
    ];
    var origins = [
      [-width * 0.34, -height * 0.26],
      [width * 0.34, -height * 0.28],
      [-width * 0.36, height * 0.28],
      [width * 0.35, height * 0.3]
    ];

    ctx.clearRect(0, 0, width, height);
    if (completed > 0) {
      var glowRadius = size * (2.5 + completed * 0.2 + pulse * 0.25);
      var glow = ctx.createRadialGradient(centerX, centerY, size * 0.35, centerX, centerY, glowRadius);
      glow.addColorStop(0, 'rgba(112, 210, 196, ' + (0.24 + completed * 0.045) + ')');
      glow.addColorStop(0.55, 'rgba(89, 158, 205, ' + (0.12 + pulse * 0.05) + ')');
      glow.addColorStop(1, 'rgba(89, 158, 205, 0)');
      ctx.fillStyle = glow;
      ctx.fillRect(0, 0, width, height);
    }

    for (var index = 0; index < 4; index += 1) {
      var isComplete = index < completed;
      var isCurrent = index === completed && completed < 4;
      var gather = isComplete ? 1 : isCurrent ? (0.28 + pulse * 0.58) : 0;
      var x = centerX + origins[index][0] * (1 - gather) + targets[index][0] * gather;
      var y = centerY + origins[index][1] * (1 - gather) + targets[index][1] * gather;
      var floatY = isComplete ? 0 : Math.sin(now * 0.0013 + index * 1.4) * 5 * ratio * reduce;
      var rotation = isComplete ? 0 : Math.sin(now * 0.0009 + index) * 0.13 * reduce;

      ctx.save();
      ctx.globalAlpha = isComplete ? 1 : isCurrent ? 0.92 : 0.38;
      ctx.translate(x, y + floatY);
      ctx.rotate(rotation);
      ctx.shadowColor = isComplete
        ? 'rgba(72, 183, 167, ' + (0.46 + pulse * 0.18) + ')'
        : 'rgba(38, 59, 75, 0.2)';
      ctx.shadowBlur = (isComplete ? 18 + pulse * 8 : 8) * ratio;
      ctx.shadowOffsetY = isComplete ? 0 : 4 * ratio;
      puzzlePath(ctx, size, index);
      var gradient = ctx.createLinearGradient(-size, -size, size, size);
      gradient.addColorStop(0, colors[index][0]);
      gradient.addColorStop(1, colors[index][1]);
      ctx.fillStyle = gradient;
      ctx.fill();
      ctx.shadowColor = 'transparent';
      ctx.lineWidth = 2 * ratio;
      ctx.strokeStyle = '#263B4B';
      ctx.stroke();
      puzzlePath(ctx, size * 0.88, index);
      ctx.lineWidth = 1 * ratio;
      ctx.strokeStyle = 'rgba(255,255,255,.32)';
      ctx.stroke();
      ctx.restore();
    }
  }

  function render(now) {
    if (disposed) return;
    if (!document.hidden) {
      cardCanvases.forEach(function (canvas) {
        if (canvas.hasAttribute('data-puzzle-complete')) drawAssembly(canvas, now);
        else drawPiece(canvas, now);
      });
    }
    window.requestAnimationFrame(render);
  }

  window.addEventListener('pagehide', function () {
    disposed = true;
    window.clearInterval(autoplayTimer);
    if (canvasObserver) canvasObserver.disconnect();
  }, { once: true });

  startAutoplay();
  window.requestAnimationFrame(render);
})();
