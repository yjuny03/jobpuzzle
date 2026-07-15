(function () {
  "use strict";
  var loader = document.getElementById("jp-loader");
  if (!loader) return;
  if (loader.dataset.enabled !== "true") {
    loader.remove();
    return;
  }
  var key = "jobpuzzle_loader_flow_jigsaw_v10",
    seen = false;
  try {
    seen = sessionStorage.getItem(key) === "1";
  } catch (error) {}
  if (seen) {
    loader.className = "jp-loader jp-hidden";
    return;
  }
  var copy = document.getElementById("jp-loader-copy"),
    scene = document.getElementById("jp-puzzle-scene"),
    progress = document.getElementById("jp-loader-progress"),
    progressNumber = document.getElementById("jp-loader-progress-number"),
    serviceCopy = document.querySelector(".jp-service-copy"),
    stageTitle = document.getElementById("jp-stage-title"),
    stageDescription = document.getElementById("jp-stage-description"),
    preview = document.getElementById("jp-service-preview");
  var plan = [
    {
      piece: 1,
      start: 260,
      duration: 900,
      label: "자료 정리",
      preview: "materials",
      title: "자료를 정리합니다.",
      description: "지원서와 경력 자료에서 면접 근거가 될 경험을 확인합니다.",
      arcX: -90,
      arcY: -35,
    },
    {
      piece: 2,
      start: 1120,
      duration: 980,
      label: "공고 분석",
      preview: "analysis",
      title: "공고를 분석합니다.",
      description: "채용 공고의 핵심 역할과 요구 역량을 구조화합니다.",
      arcX: 45,
      arcY: -80,
    },
    {
      piece: 3,
      start: 2050,
      duration: 970,
      label: "경험 연결",
      preview: "matching",
      title: "경험을 연결합니다.",
      description:
        "지원자의 경험을 직무 요구사항과 연결해 답변의 근거를 만듭니다.",
      arcX: 100,
      arcY: -30,
    },
    {
      piece: 4,
      start: 2920,
      duration: 960,
      label: "질문 생성",
      preview: "questions",
      title: "질문을 설계합니다.",
      description: "연결된 경험과 공고를 바탕으로 맞춤 질문을 구성합니다.",
      arcX: -100,
      arcY: 45,
    },
    {
      piece: 5,
      start: 3820,
      duration: 980,
      label: "모의면접",
      preview: "practice",
      title: "답변을 점검합니다.",
      description: "실전 답변을 바탕으로 강점과 보완할 부분을 확인합니다.",
      arcX: -35,
      arcY: 90,
    },
    {
      piece: 6,
      start: 4740,
      duration: 1020,
      label: "결과 리포트",
      preview: "report",
      title: "다음 연습을 정리합니다.",
      description:
        "평가 결과와 약점 태그를 바탕으로 다음 연습 방향을 제안합니다.",
      arcX: 110,
      arcY: 55,
    },
  ];
  var introHold = 650,
    sequenceDuration = 6000,
    started = {},
    start = performance.now(),
    ended = false,
    typeTimer = 0,
    typeDelay = 0,
    previewTimer = 0,
    activeLabel = "",
    activePreview = "";
  function activate(label) {
    if (label === activeLabel) return;
    activeLabel = label;
    clearInterval(typeTimer);
    clearTimeout(typeDelay);
    function typeIn() {
      var index = 0;
      typeTimer = setInterval(function () {
        copy.textContent += label.charAt(index++);
        if (index >= label.length) clearInterval(typeTimer);
      }, 46);
    }
    if (!copy.textContent) {
      typeIn();
      return;
    }
    typeTimer = setInterval(function () {
      copy.textContent = copy.textContent.slice(0, -1);
      if (!copy.textContent) {
        clearInterval(typeTimer);
        typeDelay = setTimeout(typeIn, 72);
      }
    }, 28);
  }
  function setPreview(item) {
    if (!item || item.preview === activePreview) return;
    activePreview = item.preview;
    clearTimeout(previewTimer);
    if (serviceCopy) serviceCopy.classList.add("is-switching");
    if (preview) preview.classList.add("is-switching");
    previewTimer = setTimeout(function () {
      if (stageTitle) stageTitle.textContent = item.title;
      if (stageDescription) stageDescription.textContent = item.description;
      if (preview)
        preview.querySelectorAll("[data-preview]").forEach(function (card) {
          card.classList.toggle(
            "is-active",
            card.getAttribute("data-preview") === item.preview,
          );
        });
      requestAnimationFrame(function () {
        requestAnimationFrame(function () {
          if (serviceCopy) serviceCopy.classList.remove("is-switching");
          if (preview) preview.classList.remove("is-switching");
        });
      });
    }, 220);
  }
  function setInitialStage(item) {
    activeLabel = item.label;
    activePreview = item.preview;
    if (copy) copy.textContent = item.label;
    if (stageTitle) stageTitle.textContent = item.title;
    if (stageDescription) stageDescription.textContent = item.description;
    if (preview)
      preview.querySelectorAll("[data-preview]").forEach(function (card) {
        card.classList.toggle(
          "is-active",
          card.getAttribute("data-preview") === item.preview,
        );
      });
    if (progress) progress.style.transform = "scaleX(0)";
    if (progressNumber) progressNumber.textContent = "0%";
  }
  function initialTransform(piece) {
    return (
      "translate(" +
      piece.getAttribute("data-start-x") +
      "px," +
      piece.getAttribute("data-start-y") +
      "px) rotate(" +
      piece.getAttribute("data-start-r") +
      "deg) scale(.92)"
    );
  }
  function stagePieces() {
    document.querySelectorAll(".piece-flight").forEach(function (piece) {
      piece.style.transform = initialTransform(piece);
      piece.classList.add("is-staged");
    });
  }
  function fly(item) {
    var piece = document.querySelector('[data-piece="' + item.piece + '"]');
    if (!piece) return;
    activate(item.label);
    setPreview(item);
    piece.classList.add("is-flying");
    var sx = Number(piece.getAttribute("data-start-x")),
      sy = Number(piece.getAttribute("data-start-y")),
      sr = Number(piece.getAttribute("data-start-r"));
    var frames = [
      {
        transform:
          "translate(" +
          sx +
          "px," +
          sy +
          "px) rotate(" +
          sr +
          "deg) scale(.92)",
        opacity: 0.96,
      },
      {
        transform:
          "translate(" +
          (sx * 0.62 + item.arcX) +
          "px," +
          (sy * 0.62 + item.arcY) +
          "px) rotate(" +
          sr * 0.54 +
          "deg) scale(.98)",
        offset: 0.38,
      },
      {
        transform:
          "translate(" +
          (sx * 0.2 - item.arcX * 0.2) +
          "px," +
          (sy * 0.18 - item.arcY * 0.18) +
          "px) rotate(" +
          sr * 0.14 +
          "deg) scale(1.08)",
        offset: 0.79,
      },
      { transform: "translate(0,0) rotate(0deg) scale(.965)", offset: 0.92 },
      { transform: "translate(0,0) rotate(0deg) scale(1)", opacity: 1 },
    ];
    if (piece.animate) {
      var animation = piece.animate(frames, {
        duration: item.duration,
        easing: "cubic-bezier(.18,.82,.2,1)",
        fill: "forwards",
      });
      animation.onfinish = function () {
        piece.classList.remove("is-flying");
        piece.classList.add("is-joined");
      };
    } else {
      piece.style.transition =
        "transform " + item.duration + "ms cubic-bezier(.18,.82,.2,1)";
      requestAnimationFrame(function () {
        piece.style.transform = "translate(0,0) rotate(0) scale(1)";
      });
      setTimeout(function () {
        piece.classList.add("is-joined");
      }, item.duration);
    }
  }
  function reveal() {
    if (ended) return;
    ended = true;
    try {
      sessionStorage.setItem(key, "1");
    } catch (error) {}
    loader.classList.add("jp-opening");
    setTimeout(function () {
      loader.className = "jp-loader jp-hidden";
    }, 980);
  }
  function frame(now) {
    var elapsed = now - start;
    if (elapsed < introHold) {
      requestAnimationFrame(frame);
      return;
    }
    var sequenceElapsed = elapsed - introHold,
      ratio = Math.min(1, sequenceElapsed / sequenceDuration);
    if (progress) progress.style.transform = "scaleX(" + ratio + ")";
    if (progressNumber)
      progressNumber.textContent = Math.round(ratio * 100) + "%";
    plan.forEach(function (item) {
      if (sequenceElapsed >= item.start && !started[item.piece]) {
        started[item.piece] = true;
        fly(item);
      }
    });
    if (sequenceElapsed < sequenceDuration) requestAnimationFrame(frame);
    else {
      scene.classList.add("is-complete");
      setTimeout(reveal, 320);
    }
  }
  stagePieces();
  setInitialStage(plan[0]);
  requestAnimationFrame(frame);
  setTimeout(reveal, introHold + sequenceDuration + 3000);
})();
