import "./styles.css";
import { BOOKS } from "./curriculum-data.js";

const STORAGE_KEY = "XIGUA_ENGLISH_WEB_V2";
const app = document.querySelector("#app");

const defaultState = {
  selectedBookId: "pep_g5_first",
  selectedUnitId: "pep_g5_first_u1",
  practiceCount: 5,
  points: 0,
  coins: 0,
  stageStars: {},
  wordProgress: {},
  grammarProgress: {},
  keyboardStars: {},
  keyboardMistakes: {},
  keyboardKeystrokes: 0,
  keyboardCorrect: 0,
  keyboardTimeouts: 0,
  keyboardRounds: 0,
  keyboardFailures: 0,
  frogRounds: 0,
  frogFailures: 0,
  frogLivesLost: 0,
  frogBestScore: 0
};

let state = loadState();
let view = "books";
let activeWords = [];
let activeStage = 0;
let toastTimer = null;
let gameTimer = null;

function loadState() {
  try {
    return { ...defaultState, ...JSON.parse(localStorage.getItem(STORAGE_KEY) || "{}") };
  } catch {
    return { ...defaultState };
  }
}

function saveState() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

function updateState(patch) {
  state = { ...state, ...patch };
  saveState();
}

function currentBook() {
  return BOOKS.find((book) => book.id === state.selectedBookId) || BOOKS[0];
}

function currentUnit() {
  return currentBook().units.find((unit) => unit.id === state.selectedUnitId) || currentBook().units[0];
}

function stageKey(unitId, stage) {
  return `${unitId}:${stage}`;
}

function stageStars(unitId, stage) {
  return state.stageStars[stageKey(unitId, stage)] || 0;
}

function stageUnlocked(unitId, stage) {
  return stage === 0 || stageStars(unitId, stage - 1) > 0;
}

function unitUnlocked(book, index) {
  return index === 0 || stageStars(book.units[index - 1].id, 3) > 0;
}

function completedStages(unitId) {
  return [0, 1, 2, 3].filter((stage) => stageStars(unitId, stage) > 0).length;
}

function sampleWords(unit = currentUnit()) {
  const shuffled = [...unit.words].sort(() => Math.random() - .5);
  return state.practiceCount === 0 ? shuffled : shuffled.slice(0, state.practiceCount);
}

function clearGameTimer() {
  clearInterval(gameTimer);
  clearTimeout(gameTimer);
  gameTimer = null;
}

function navigate(next) {
  clearGameTimer();
  view = next;
  render();
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function shuffle(values) {
  return [...values].sort(() => Math.random() - .5);
}

function showToast(message) {
  document.querySelector(".toast")?.remove();
  const toast = document.createElement("div");
  toast.className = "toast";
  toast.textContent = message;
  document.body.append(toast);
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.remove(), 1800);
}

function speak(text) {
  if (!("speechSynthesis" in window)) {
    showToast("当前浏览器不支持语音朗读");
    return;
  }
  speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.lang = "en-US";
  utterance.rate = .82;
  speechSynthesis.speak(utterance);
}

function sound(correct = true) {
  try {
    const Audio = window.AudioContext || window.webkitAudioContext;
    const context = new Audio();
    const oscillator = context.createOscillator();
    const gain = context.createGain();
    oscillator.connect(gain);
    gain.connect(context.destination);
    oscillator.frequency.value = correct ? 720 : 170;
    gain.gain.setValueAtTime(.12, context.currentTime);
    gain.gain.exponentialRampToValueAtTime(.001, context.currentTime + .18);
    oscillator.start();
    oscillator.stop(context.currentTime + .2);
  } catch {
    // Audio is optional.
  }
}

function topbar() {
  const book = currentBook();
  const unit = currentUnit();
  const back = view === "books" ? "" : `<button class="icon-button" data-action="back">← 返回</button>`;
  return `
    <header class="topbar">
      ${back}
      <div class="brand">
        <img src="./icons/icon-192.png" alt="">
        <div>
          <div class="brand-title">西瓜英语</div>
          <div class="brand-subtitle">${book.shortName} · Unit ${unit.number} · ${escapeHtml(unit.topic)}</div>
        </div>
      </div>
      <div class="topbar-spacer"></div>
      <button class="icon-button" data-action="books">📚 教材</button>
      <button class="icon-button" data-action="training">⌨ 训练中心</button>
      <div class="hud-pill">积分 <strong>${state.points}</strong></div>
      <div class="hud-pill">金币 <strong>${state.coins}</strong></div>
      <button class="icon-button" data-action="report">📊 家长报告</button>
    </header>
  `;
}

function shell(content) {
  app.innerHTML = `<div class="app-shell">${topbar()}<main class="main">${content}</main></div>`;
  bindGlobal();
}

function bindGlobal() {
  app.querySelector('[data-action="books"]')?.addEventListener("click", () => navigate("books"));
  app.querySelector('[data-action="training"]')?.addEventListener("click", () => navigate("training"));
  app.querySelector('[data-action="report"]')?.addEventListener("click", () => navigate("report"));
  app.querySelector('[data-action="back"]')?.addEventListener("click", () => {
    if (view === "units") navigate("books");
    else if (["map", "training"].includes(view)) navigate("units");
    else if (view === "keyboard" || view === "frog") navigate("training");
    else navigate("map");
  });
}

function render() {
  clearGameTimer();
  const renderers = {
    books: renderBooks,
    units: renderUnits,
    map: renderMap,
    learning: renderLearning,
    spelling: renderSpelling,
    grammar: renderGrammar,
    boss: renderBoss,
    training: renderTraining,
    keyboard: renderKeyboard,
    frog: renderFrog,
    report: renderReport
  };
  renderers[view]();
}

function renderBooks() {
  const cards = BOOKS.map((book) => {
    const completed = book.units.filter((unit) => stageStars(unit.id, 3) > 0).length;
    const active = book.id === state.selectedBookId;
    return `
      <button class="book-card ${active ? "active" : ""}" data-book="${book.id}">
        <div class="book-badge">${book.shortName}</div>
        <h2>${book.grade} 年级${book.volumeLabel}</h2>
        <div class="muted">${book.units.length} 个单元 · ${book.units.reduce((sum, unit) => sum + unit.words.length, 0)} 个词/短语</div>
        <div class="book-progress"><span style="width:${completed / book.units.length * 100}%"></span></div>
        <strong>${completed} / ${book.units.length} 单元已通关</strong>
      </button>
    `;
  }).join("");

  shell(`
    <section class="screen">
      <h1 class="screen-title">选择英语探险教材</h1>
      <p class="screen-lead">人教版 PEP 三至六年级上下册 · 进度保存在当前学习机浏览器</p>
      <div class="books-grid">${cards}</div>
    </section>
  `);

  app.querySelectorAll("[data-book]").forEach((card) => card.addEventListener("click", () => {
    const book = BOOKS.find((item) => item.id === card.dataset.book);
    updateState({ selectedBookId: book.id, selectedUnitId: book.units[0].id });
    navigate("units");
  }));
}

function renderUnits() {
  const book = currentBook();
  const cards = book.units.map((unit, index) => {
    const unlocked = unitUnlocked(book, index);
    const complete = stageStars(unit.id, 3) > 0;
    return `
      <button class="unit-card ${unlocked ? "" : "locked"} ${complete ? "complete" : ""}" data-unit="${unit.id}" ${unlocked ? "" : "disabled"}>
        <div class="unit-number">${unit.number}</div>
        <h2>Unit ${unit.number} · ${escapeHtml(unit.title)}</h2>
        <p class="muted">${escapeHtml(unit.topic)} · ${unit.words.length} 个词/短语</p>
        <strong>${complete ? "已通关 ✓" : unlocked ? `${completedStages(unit.id)} / 4 关` : "🔒 前一单元未通关"}</strong>
        <div class="stage-dots">
          ${["词汇", "拼写", "语法", "Boss"].map((label, stage) => `<span class="stage-dot ${stageStars(unit.id, stage) ? "done" : ""}">${label}</span>`).join("")}
        </div>
      </button>
    `;
  }).join("");

  shell(`
    <section class="screen">
      <h1 class="screen-title">${book.series} · ${book.grade} 年级${book.volumeLabel}</h1>
      <p class="screen-lead">按顺序完成每个单元，击败 Boss 后自动解锁下一单元</p>
      <div class="unit-grid">${cards}</div>
    </section>
  `);

  app.querySelectorAll("[data-unit]").forEach((card) => card.addEventListener("click", () => {
    updateState({ selectedUnitId: card.dataset.unit });
    navigate("map");
  }));
}

function renderMap() {
  const unit = currentUnit();
  const nodes = [
    ["words", "新词启航营", "五步多通道学习", "🌱", 0, "linear-gradient(#34d399,#15803d)"],
    ["spelling", "键盘拼写营", "完整输入单词与空格", "⌨", 1, "linear-gradient(#38bdf8,#1d4ed8)"],
    ["grammar", "语法思考工坊", "情境发现与主动输出", "💡", 2, "linear-gradient(#c084fc,#7e22ce)"],
    ["boss", "时空遗忘兽 Boss", "单元综合挑战", "🐉", 3, "linear-gradient(#fb7185,#be123c)"]
  ];
  const nodeHtml = nodes.map(([id, title, subtitle, icon, stage, background]) => {
    const unlocked = stageUnlocked(unit.id, stage);
    const stars = stageStars(unit.id, stage);
    return `
      <button class="map-node" data-node="${id}" data-stage="${stage}" ${unlocked ? "" : "disabled"}>
        <div class="node-icon" style="background:${background}">${unlocked ? icon : "🔒"}</div>
        <div class="node-info">
          <strong>${title}</strong>
          <div class="stars">${"★".repeat(stars)}${"☆".repeat(3 - stars)}</div>
          <small>${unlocked ? subtitle : "完成上一关后解锁"}</small>
        </div>
      </button>
    `;
  }).join("");
  const options = [5, 10, 15, 0].map((count) => `
    <button data-count="${count}" class="${state.practiceCount === count ? "active" : ""}">${count || "全部"}${count ? "词" : ""}</button>
  `).join("");

  shell(`
    <section class="screen">
      <div class="map-panel">
        <div class="map-heading">
          <p>UNIT ${unit.number} · ${escapeHtml(unit.topic)}</p>
          <h1>${escapeHtml(unit.title)}</h1>
        </div>
        <svg class="route" viewBox="0 0 1160 600" preserveAspectRatio="none" aria-hidden="true">
          <path class="shadow" d="M135 410 C280 410 300 205 480 205 C650 205 650 410 815 405 C970 400 965 180 1080 155"/>
          <path class="road" d="M135 410 C280 410 300 205 480 205 C650 205 650 410 815 405 C970 400 965 180 1080 155"/>
          <path class="line" d="M135 410 C280 410 300 205 480 205 C650 205 650 410 815 405 C970 400 965 180 1080 155"/>
        </svg>
        ${nodeHtml}
        <div class="practice-selector">
          <strong>本次随机练习</strong>${options}<span>词库 ${unit.words.length} 个词/短语</span>
        </div>
      </div>
    </section>
  `);

  app.querySelectorAll("[data-count]").forEach((button) => button.addEventListener("click", () => {
    updateState({ practiceCount: Number(button.dataset.count) });
    renderMap();
  }));
  app.querySelectorAll(".map-node").forEach((node) => node.addEventListener("click", () => {
    activeStage = Number(node.dataset.stage);
    activeWords = sampleWords();
    navigate(node.dataset.node === "words" ? "learning" : node.dataset.node);
  }));
}

function completeStage(stage, stars = 3) {
  const unit = currentUnit();
  const key = stageKey(unit.id, stage);
  const first = !state.stageStars[key];
  updateState({
    stageStars: { ...state.stageStars, [key]: Math.max(stars, state.stageStars[key] || 0) },
    points: state.points + (first ? (stage === 3 ? 100 : 30) : 0),
    coins: state.coins + (first ? (stage === 3 ? 50 : 15) : 0)
  });
}

function renderLearning() {
  let index = 0;
  let step = 1;
  let selected = null;
  let spelling = "";
  let errors = 0;

  function draw() {
    const word = activeWords[index];
    if (!word) {
      completeStage(0);
      showToast("新词启航完成，已解锁键盘拼写营");
      navigate("map");
      return;
    }
    const stepNames = ["认读听音", "语境例句", "听音辨义", "看中选英", "完整拼写"];
    const tabs = stepNames.map((name, tabIndex) => `<div class="step-tab ${step === tabIndex + 1 ? "active" : ""}">${tabIndex + 1}. ${name}</div>`).join("");
    let body = "";
    if (step === 1) {
      body = `<div><div class="muted">${word.phonetic}</div><div class="study-word">${escapeHtml(word.word)}</div><div class="study-meaning">${escapeHtml(word.meaning)}</div><button class="secondary-button" data-speak>🔊 听标准发音</button></div>`;
    } else if (step === 2) {
      body = `<div class="dialogue"><strong>Teacher:</strong> Can you read “${escapeHtml(word.word)}”?<br><strong>Student:</strong> Yes. ${escapeHtml(word.example)}</div>`;
    } else if (step === 3 || step === 4) {
      const pool = activeWords.filter((item) => item.id !== word.id);
      const options = shuffle([word, ...shuffle(pool).slice(0, 3)]);
      body = `
        <div style="width:100%">
          <h2>${step === 3 ? "听发音，选择正确中文" : `“${escapeHtml(word.meaning)}”对应哪个英文？`}</h2>
          ${step === 3 ? `<button class="secondary-button" data-speak>🔊 播放单词</button>` : ""}
          <div class="choice-grid" style="margin:18px auto">
            ${options.map((option) => {
              const text = step === 3 ? option.meaning : option.word;
              const className = selected ? (option.id === word.id ? "correct" : selected === option.id ? "wrong" : "") : "";
              return `<button class="choice-button ${className}" data-option="${option.id}" ${selected ? "disabled" : ""}>${escapeHtml(text)}</button>`;
            }).join("")}
          </div>
        </div>
      `;
    } else {
      const target = normalizeTarget(word.word);
      body = `
        <div>
          <div class="study-meaning">${escapeHtml(word.meaning)}</div>
          <div class="spelling-slots" style="margin:22px auto">${slots(target, spelling)}</div>
          <div class="muted">物理键盘输入；词组空格必须键入</div>
          <div class="feedback">错误 ${errors} 次 · ${spelling ? "继续输入" : "从第一个字母开始"}</div>
        </div>
      `;
    }

    shell(`
      <section class="screen lesson-layout">
        <aside class="panel side-stats">
          <div class="muted">新词启航</div>
          <h2>${index + 1} / ${activeWords.length}</h2>
          <div class="progress-track"><span style="width:${(index + (step - 1) / 5) / activeWords.length * 100}%"></span></div>
          <div class="stat-block"><span>当前步骤</span><strong>${stepNames[step - 1]}</strong></div>
          <div class="stat-block"><span>本词错误</span><strong>${errors}</strong></div>
        </aside>
        <div class="panel lesson-main">
          <div class="step-tabs">${tabs}</div>
          <div class="study-card">${body}</div>
          <div class="lesson-actions">
            ${step === 5 ? `<button class="secondary-button" data-hint>提示下一个字母</button><button class="icon-button" data-answer>显示答案</button>` : ""}
            ${step === 5 || step < 3 || selected ? `<button class="primary-button" data-next>${step === 5 ? "下一个单词" : "继续"}</button>` : ""}
          </div>
        </div>
      </section>
    `);

    app.querySelectorAll("[data-speak]").forEach((button) => button.addEventListener("click", () => speak(word.word)));
    app.querySelectorAll("[data-option]").forEach((button) => button.addEventListener("click", () => {
      selected = button.dataset.option;
      sound(selected === word.id);
      if (selected !== word.id) errors += 1;
      draw();
    }));
    app.querySelector("[data-next]")?.addEventListener("click", () => {
      if (step < 5) {
        step += 1;
        selected = null;
      } else if (spelling === normalizeTarget(word.word)) {
        recordMastery(word, errors);
        index += 1;
        step = 1;
        spelling = "";
        errors = 0;
      } else {
        showToast("请先完整拼写，或使用提示/答案");
      }
      draw();
    });
    app.querySelector("[data-hint]")?.addEventListener("click", () => {
      const target = normalizeTarget(word.word);
      if (spelling.length < target.length) {
        spelling += target[spelling.length];
        errors += 1;
      }
      draw();
    });
    app.querySelector("[data-answer]")?.addEventListener("click", () => {
      spelling = normalizeTarget(word.word);
      errors += 1;
      draw();
    });
  }

  function keyHandler(event) {
    if (view !== "learning" || step !== 5 || event.repeat) return;
    const char = event.key.length === 1 ? event.key.toLowerCase() : "";
    if (!/[a-z ]/.test(char)) return;
    event.preventDefault();
    const word = activeWords[index];
    const target = normalizeTarget(word.word);
    if (char === target[spelling.length]) {
      spelling += char;
      if (spelling === target) sound(true);
    } else {
      errors += 1;
      spelling = "";
      recordWordError(word.id, char);
      sound(false);
    }
    draw();
  }
  window.addEventListener("keydown", keyHandler, { signal: navigationSignal() });
  draw();
}

function normalizeTarget(value) {
  return value.toLowerCase().replace(/[^a-z\s]/g, "").replace(/\s+/g, " ").trim();
}

function slots(target, typed, hint = "") {
  return [...target].map((char, index) => {
    const entered = typed[index];
    const className = entered ? "filled" : hint[index] ? "hinted" : "";
    const value = entered || hint[index] || (char === " " ? "␣" : char);
    return `<span class="letter-slot ${char === " " ? "space" : ""} ${className}">${escapeHtml(value)}</span>`;
  }).join("");
}

function recordWordError(wordId, char) {
  const old = state.wordProgress[wordId] || { errors: 0, letters: {}, mastered: false };
  state.wordProgress = {
    ...state.wordProgress,
    [wordId]: {
      ...old,
      errors: old.errors + 1,
      letters: { ...old.letters, [char]: (old.letters[char] || 0) + 1 }
    }
  };
  saveState();
}

function recordMastery(word, errors = 0) {
  const old = state.wordProgress[word.id] || { errors: 0, letters: {}, mastered: false };
  const first = !old.mastered;
  updateState({
    wordProgress: {
      ...state.wordProgress,
      [word.id]: { ...old, errors: old.errors + errors, mastered: true, reviewedAt: Date.now() }
    },
    points: state.points + (first ? 5 : 0),
    coins: state.coins + (first ? 3 : 0)
  });
}

let navigationController = new AbortController();
function navigationSignal() {
  navigationController.abort();
  navigationController = new AbortController();
  return navigationController.signal;
}

function renderSpelling() {
  let index = 0;
  let typed = "";
  let errors = 0;

  function draw() {
    if (index >= activeWords.length) {
      completeStage(1);
      showToast("键盘拼写完成，已解锁语法工坊");
      navigate("map");
      return;
    }
    const word = activeWords[index];
    const target = normalizeTarget(word.word);
    shell(`
      <section class="screen lesson-layout">
        <aside class="panel side-stats">
          <div class="muted">键盘拼写营</div>
          <h2>${index + 1} / ${activeWords.length}</h2>
          <div class="progress-track"><span style="width:${index / activeWords.length * 100}%"></span></div>
          <div class="stat-block"><span>本词错误</span><strong>${errors}</strong></div>
          <div class="stat-block"><span>输入规则</span><strong style="font-size:14px">错一个字母立即清空重拼</strong></div>
        </aside>
        <div class="panel lesson-main">
          <div class="study-card">
            <div>
              <div class="study-meaning">${escapeHtml(word.meaning)}</div>
              <div class="spelling-slots" style="margin:26px auto">${slots(target, typed)}</div>
              <button class="secondary-button" data-speak>🔊 听发音</button>
              <div class="feedback">词组空格显示为 ␣，必须按空格键</div>
            </div>
          </div>
        </div>
      </section>
    `);
    app.querySelector("[data-speak]").addEventListener("click", () => speak(word.word));
  }

  function keyHandler(event) {
    if (view !== "spelling" || event.repeat) return;
    const char = event.key.length === 1 ? event.key.toLowerCase() : "";
    if (!/[a-z ]/.test(char)) return;
    event.preventDefault();
    const word = activeWords[index];
    const target = normalizeTarget(word.word);
    if (char === target[typed.length]) {
      typed += char;
      if (typed === target) {
        sound(true);
        recordMastery(word, errors);
        setTimeout(() => {
          index += 1;
          typed = "";
          errors = 0;
          draw();
        }, 320);
      } else draw();
    } else {
      sound(false);
      errors += 1;
      recordWordError(word.id, char);
      typed = "";
      draw();
    }
  }
  window.addEventListener("keydown", keyHandler, { signal: navigationSignal() });
  draw();
}

function renderGrammar() {
  const grammar = currentUnit().grammar;
  let phase = 1;
  let selected = null;

  function draw() {
    const options = grammar.options.map((option, index) => `
      <button class="choice-button ${selected !== null ? (index === grammar.answer ? "correct" : selected === index ? "wrong" : "") : ""}" data-option="${index}" ${selected !== null ? "disabled" : ""}>${escapeHtml(option)}</button>
    `).join("");
    shell(`
      <section class="screen">
        <div class="panel grammar-card">
          <div class="muted">语法思考工坊 · ${escapeHtml(grammar.tag)}</div>
          <h1>${escapeHtml(grammar.title)}</h1>
          ${phase === 1 ? `
            <div class="grammar-example"><strong>课本情境</strong><br>${escapeHtml(grammar.pattern)}</div>
            <div class="grammar-example"><strong>观察任务</strong><br>先找主语和关键词，再观察句子的语序。</div>
            <div class="lesson-actions"><button class="primary-button" data-next>我发现规律了</button></div>
          ` : phase === 2 ? `
            <h2>核心结构</h2>
            <div class="study-card" style="min-height:260px"><div><div class="study-word" style="font-size:34px">${escapeHtml(grammar.pattern)}</div><p>先认关键词，再看语序，最后完整表达。</p></div></div>
            <div class="lesson-actions"><button class="primary-button" data-next>开始主动输出</button></div>
          ` : `
            <h2>${escapeHtml(grammar.practice)}</h2>
            <div class="choice-grid">${options}</div>
            <div class="feedback">${selected === null ? "请选择答案" : selected === grammar.answer ? "回答正确！" : grammar.explanation}</div>
            ${selected !== null ? `<div class="lesson-actions"><button class="primary-button" data-finish>完成语法关</button></div>` : ""}
          `}
        </div>
      </section>
    `);
    app.querySelector("[data-next]")?.addEventListener("click", () => {
      phase += 1;
      draw();
    });
    app.querySelectorAll("[data-option]").forEach((button) => button.addEventListener("click", () => {
      selected = Number(button.dataset.option);
      const correct = selected === grammar.answer;
      const old = state.grammarProgress[grammar.id] || { attempts: 0, correct: 0 };
      updateState({
        grammarProgress: {
          ...state.grammarProgress,
          [grammar.id]: { attempts: old.attempts + 1, correct: old.correct + (correct ? 1 : 0) }
        }
      });
      sound(correct);
      draw();
    }));
    app.querySelector("[data-finish]")?.addEventListener("click", () => {
      completeStage(2, selected === grammar.answer ? 3 : 1);
      showToast("语法关完成，Boss 已解锁");
      navigate("map");
    });
  }
  draw();
}

function renderBoss() {
  const words = sampleWords().slice(0, 5);
  let index = 0;
  let playerHp = 3;
  let bossHp = 5;
  let submitted = false;

  function draw() {
    if (bossHp === 0) {
      completeStage(3);
      shell(`
        <section class="screen"><div class="panel boss-arena result-overlay" style="position:relative;min-height:560px">
          <div><div class="boss-character">🏆</div><h1>Boss 已被击败</h1><p>下一单元已经解锁</p><button class="primary-button" data-return>返回地图</button></div>
        </div></section>
      `);
      app.querySelector("[data-return]").addEventListener("click", () => navigate("map"));
      return;
    }
    if (playerHp === 0) {
      shell(`
        <section class="screen"><div class="panel boss-arena result-overlay" style="position:relative;min-height:560px">
          <div><div class="boss-character">💥</div><h1>生命耗尽，挑战失败</h1><button class="danger-button" data-retry>重新挑战</button></div>
        </div></section>
      `);
      app.querySelector("[data-retry]").addEventListener("click", renderBoss);
      return;
    }
    const word = words[index % words.length];
    const wrongs = shuffle(currentUnit().words.filter((item) => item.id !== word.id)).slice(0, 3);
    const options = shuffle([word, ...wrongs]);
    shell(`
      <section class="screen">
        <div class="panel boss-arena">
          <div class="boss-head">
            <div class="health-box"><strong>你的生命 ${"♥".repeat(playerHp)}</strong><div class="health-track"><span style="width:${playerHp / 3 * 100}%"></span></div></div>
            <div class="health-box"><strong>遗忘兽 ${bossHp} / 5</strong><div class="health-track"><span style="width:${bossHp / 5 * 100}%;background:#e11d48"></span></div></div>
          </div>
          <div class="boss-character">🐉</div>
          <h2 style="text-align:center">“${escapeHtml(word.word)}”是什么意思？</h2>
          <div class="choice-grid" style="margin:auto">
            ${options.map((option) => `<button class="choice-button" data-answer="${option.id}" ${submitted ? "disabled" : ""}>${escapeHtml(option.meaning)}</button>`).join("")}
          </div>
          <div class="feedback" style="text-align:center">每题只能提交一次 · 连错三题失败 · 答对五题通关</div>
        </div>
      </section>
    `);
    app.querySelectorAll("[data-answer]").forEach((button) => button.addEventListener("click", () => {
      if (submitted) return;
      submitted = true;
      const correct = button.dataset.answer === word.id;
      sound(correct);
      if (correct) bossHp -= 1;
      else playerHp -= 1;
      index += 1;
      setTimeout(() => {
        submitted = false;
        draw();
      }, 350);
    }));
  }
  draw();
}

const keyboardLevels = [
  ["基准键位", "ASDF · JKL; · 空格", "ASDFJKL; "],
  ["食指扩展", "加入 G / H 与空格", "ASDFGHJKL; "],
  ["上排探索", "QWERTYUIOP · 空格", "QWERTYUIOP "],
  ["下排探索", "ZXCVBNM · 空格", "ZXCVBNM "],
  ["全键混合", "26 字母与空格", "ABCDEFGHIJKLMNOPQRSTUVWXYZ "]
];

function renderTraining() {
  const cards = keyboardLevels.map((level, index) => {
    const unlocked = index === 0 || (state.keyboardStars[index - 1] || 0) > 0;
    const stars = state.keyboardStars[index] || 0;
    return `
      <button class="training-card ${unlocked ? "" : "locked"}" data-level="${index}" ${unlocked ? "" : "disabled"}>
        <div class="unit-number">${index + 1}</div>
        <h3>${level[0]}</h3>
        <p>${level[1]}</p>
        <div class="stars">${"★".repeat(stars)}${"☆".repeat(3 - stars)}</div>
        <small>${unlocked ? "点击开始" : "完成前一关解锁"}</small>
      </button>
    `;
  }).join("");
  const frogUnlocked = (state.keyboardStars[0] || 0) > 0;
  shell(`
    <section class="screen">
      <h1 class="screen-title">青蛙打字训练中心</h1>
      <p class="screen-lead">先掌握键位与空格，再挑战完整单词</p>
      <div class="training-grid">${cards}</div>
      <div class="panel flume-entry">
        <div class="frog">🐸</div>
        <div><h2>激流勇进</h2><p>${frogUnlocked ? "三只青蛙都完成整轮单词才算通关" : "完成基准键位并获得 1 星后解锁"}</p></div>
        <button class="primary-button" data-frog ${frogUnlocked ? "" : "disabled"}>${frogUnlocked ? "开始渡河" : "尚未解锁"}</button>
      </div>
    </section>
  `);
  app.querySelectorAll("[data-level]").forEach((card) => card.addEventListener("click", () => {
    sessionStorage.setItem("keyboardLevel", card.dataset.level);
    navigate("keyboard");
  }));
  app.querySelector("[data-frog]").addEventListener("click", () => {
    activeWords = sampleWords();
    navigate("frog");
  });
}

function fingerGuide(key) {
  if (key === " ") return ["双手", "拇指"];
  if ("QAZ".includes(key)) return ["左手", "小指"];
  if ("WSX".includes(key)) return ["左手", "无名指"];
  if ("EDC".includes(key)) return ["左手", "中指"];
  if ("RFVTGB".includes(key)) return ["左手", "食指"];
  if ("YHNUJM".includes(key)) return ["右手", "食指"];
  if ("IK".includes(key)) return ["右手", "中指"];
  if ("OL".includes(key)) return ["右手", "无名指"];
  return ["右手", "小指"];
}

function renderKeyboard() {
  const levelIndex = Number(sessionStorage.getItem("keyboardLevel") || 0);
  const level = keyboardLevels[levelIndex];
  const letters = level[2].replace(/ /g, "");
  const sequence = shuffle([
    ...Array.from({ length: 15 }, () => letters[Math.floor(Math.random() * letters.length)]),
    " ", " ", " "
  ]);
  let question = 0;
  let correct = 0;
  let wrong = 0;
  let timeouts = 0;
  let score = 0;
  let combo = 0;
  let lives = 3;
  let timeLeft = 6;
  let feedback = "错键不扣生命；超时生命 -1、得分 -20";
  let finished = false;

  function finish(failed) {
    clearGameTimer();
    finished = true;
    const accuracy = Math.round(correct * 100 / Math.max(1, correct + wrong + timeouts));
    const stars = failed ? 0 : accuracy >= 95 ? 3 : accuracy >= 85 ? 2 : accuracy >= 70 ? 1 : 0;
    updateState({
      keyboardStars: { ...state.keyboardStars, [levelIndex]: Math.max(stars, state.keyboardStars[levelIndex] || 0) },
      keyboardRounds: state.keyboardRounds + 1,
      keyboardFailures: state.keyboardFailures + (failed ? 1 : 0),
      points: state.points + Math.max(0, stars - (state.keyboardStars[levelIndex] || 0)) * 15,
      coins: state.coins + Math.max(0, stars - (state.keyboardStars[levelIndex] || 0)) * 5
    });
    draw();
  }

  function startTimer() {
    clearGameTimer();
    timeLeft = 6;
    gameTimer = setInterval(() => {
      timeLeft -= 1;
      if (timeLeft <= 0) {
        clearGameTimer();
        timeouts += 1;
        lives -= 1;
        combo = 0;
        score = Math.max(0, score - 20);
        const target = sequence[question];
        const mistakes = { ...state.keyboardMistakes, [target]: (state.keyboardMistakes[target] || 0) + 1 };
        updateState({ keyboardTimeouts: state.keyboardTimeouts + 1, keyboardMistakes: mistakes });
        feedback = `${keyLabel(target)}超时，生命 -1、得分 -20`;
        if (lives === 0) finish(true);
        else {
          draw();
          startTimer();
        }
      } else updateKeyboardStats();
    }, 1000);
  }

  function updateKeyboardStats() {
    const timer = app.querySelector("[data-timer]");
    if (timer) timer.textContent = `${timeLeft}s`;
  }

  function keyLabel(key) {
    return key === " " ? "空格" : key;
  }

  function handleKey(raw) {
    if (finished) return;
    const key = raw.length === 1 ? raw.toUpperCase() : "";
    if (!"QWERTYUIOPASDFGHJKL;ZXCVBNM ".includes(key)) return;
    const target = sequence[question];
    const isCorrect = key === target;
    updateState({
      keyboardKeystrokes: state.keyboardKeystrokes + 1,
      keyboardCorrect: state.keyboardCorrect + (isCorrect ? 1 : 0)
    });
    if (isCorrect) {
      correct += 1;
      combo += 1;
      score += 10 + Math.min(combo, 10) * 2;
      feedback = `正确！${fingerGuide(target).join("")}按下${keyLabel(target)}`;
      sound(true);
      if (question === sequence.length - 1) finish(false);
      else {
        question += 1;
        draw();
        startTimer();
      }
    } else {
      wrong += 1;
      combo = 0;
      const mistakes = { ...state.keyboardMistakes, [target]: (state.keyboardMistakes[target] || 0) + 1 };
      updateState({ keyboardMistakes: mistakes });
      feedback = `正确指法：${fingerGuide(target).join("")}按${keyLabel(target)}；不扣生命`;
      sound(false);
      draw();
      startTimer();
    }
  }

  function keyboardHtml(target) {
    const rows = ["QWERTYUIOP", "ASDFGHJKL;", "ZXCVBNM"];
    return `
      <div class="virtual-keyboard">
        ${rows.map((row) => `<div class="key-row">${[...row].map((key) => `<button class="key ${level[2].includes(key) ? "" : "unavailable"} ${target === key ? "target" : ""}" data-key="${key}" ${level[2].includes(key) ? "" : "disabled"}>${key}</button>`).join("")}</div>`).join("")}
        <div class="key-row"><button class="key space-key ${target === " " ? "target" : ""}" data-key=" ">空格</button></div>
      </div>
    `;
  }

  function draw() {
    const target = sequence[Math.min(question, sequence.length - 1)];
    const accuracy = Math.round(correct * 100 / Math.max(1, correct + wrong + timeouts));
    shell(`
      <section class="screen game-layout">
        <aside class="panel side-stats">
          <div class="muted">键盘启蒙岛</div><h2>第 ${levelIndex + 1} 关<br>${level[0]}</h2>
          <p style="color:#facc15;font-weight:900">${level[1]}</p>
          <div class="progress-track"><span style="width:${(question + 1) / sequence.length * 100}%"></span></div>
          <div class="stat-block"><span>进度</span><strong>${question + 1} / ${sequence.length}</strong></div>
          <div class="stat-block"><span>生命值</span><strong class="hearts">${"♥".repeat(lives)}${"♡".repeat(3 - lives)}</strong></div>
          <div class="stat-block"><span>本题倒计时</span><strong data-timer>${timeLeft}s</strong></div>
          <div class="stat-block"><span>正确率</span><strong>${accuracy}%</strong></div>
          <div class="stat-block"><span>得分</span><strong>${score}</strong></div>
        </aside>
        <div class="game-main">
          <h1>${level[0]}</h1>
          <div class="keyboard-arena">
            ${finished ? `
              <div class="result-overlay"><div><h2>${lives === 0 ? "生命耗尽，闯关失败" : "指法训练完成"}</h2><p>正确率 ${accuracy}% · 击键 ${correct + wrong} 次 · 超时 ${timeouts} 次</p><div class="stars">${"★".repeat(lives ? (accuracy >= 95 ? 3 : accuracy >= 85 ? 2 : accuracy >= 70 ? 1 : 0) : 0)}</div><button class="primary-button" data-back-training>返回选关</button></div></div>
            ` : `<div class="target-orb ${target === " " ? "space" : ""}">${keyLabel(target)}</div>`}
          </div>
          <div class="feedback">${feedback}<span style="float:right;color:#fde047">${fingerGuide(target).join(" · ")} · ${keyLabel(target)}</span></div>
          ${keyboardHtml(target)}
        </div>
      </section>
    `);
    app.querySelectorAll("[data-key]").forEach((button) => button.addEventListener("click", () => handleKey(button.dataset.key)));
    app.querySelector("[data-back-training]")?.addEventListener("click", () => navigate("training"));
  }

  window.addEventListener("keydown", (event) => {
    if (view !== "keyboard" || event.repeat) return;
    if (event.key === " " || event.key === ";" || /^[a-z]$/i.test(event.key)) {
      event.preventDefault();
      handleKey(event.key);
    }
  }, { signal: navigationSignal() });
  draw();
  startTimer();
}

function renderFrog() {
  const wordPool = shuffle(currentUnit().words);
  const fixedCount = Math.min(state.practiceCount || wordPool.length, wordPool.length);
  const frogWordSets = state.practiceCount === 0
    ? [0, 1, 2].map((frogIndex) => {
        const chunkSize = Math.ceil(wordPool.length / 3);
        return wordPool.slice(frogIndex * chunkSize, (frogIndex + 1) * chunkSize);
      }).filter((set) => set.length)
    : [0, 1, 2].map((frogIndex) =>
        Array.from({ length: fixedCount }, (_, index) => wordPool[(frogIndex * fixedCount + index) % wordPool.length])
      );
  const leafLanes = [28, 62, 42, 72, 34];
  const leafGap = 16;
  let frog = 1;
  let words = frogWordSets[0];
  let wordIndex = 0;
  let typed = "";
  let lives = 3;
  let mistakes = 0;
  let score = 0;
  let combo = 0;
  let timeLeft = 18;
  let feedback = "完整拼对当前词，帮助青蛙逆流过河";
  let complete = false;
  let failed = false;
  let inputLocked = false;
  let timerStartedAt = 0;
  let lastRiverMotionAt = 0;
  let riverDrift = 0;
  const riverFallSpeed = 2.2;

  function updateRiverMotion(now = performance.now()) {
    if (lastRiverMotionAt) {
      const elapsedSeconds = Math.min(200, now - lastRiverMotionAt) / 1000;
      riverDrift += elapsedSeconds * riverFallSpeed;
    }
    lastRiverMotionAt = now;
    app.querySelector(".river-arena")?.style.setProperty("--river-drift", `${riverDrift}px`);
    if (wordIndex > 0) {
      app.querySelector(".frog-jumper")?.style.setProperty("--frog-drift", `${riverDrift}px`);
    }
  }

  function startTimer() {
    clearGameTimer();
    timeLeft = 18;
    timerStartedAt = performance.now();
    lastRiverMotionAt = timerStartedAt;
    updateRiverMotion(timerStartedAt);
    gameTimer = setInterval(() => {
      const now = performance.now();
      const elapsed = now - timerStartedAt;
      const nextTime = Math.max(0, Math.ceil((18000 - elapsed) / 1000));
      timeLeft = nextTime;
      updateRiverMotion(now);
      const timerElement = app.querySelector("[data-frog-timer]");
      if (timerElement) timerElement.textContent = `${timeLeft}s`;
      if (elapsed >= 18000) {
        clearGameTimer();
        loseLife("荷叶沉底，生命 -1、得分 -50");
      }
    }, 80);
  }

  function loseLife(message) {
    lives -= 1;
    state.frogLivesLost += 1;
    score = Math.max(0, score - 50);
    mistakes = 0;
    combo = 0;
    typed = "";
    riverDrift = 0;
    timerStartedAt = 0;
    lastRiverMotionAt = 0;
    feedback = message;
    if (lives === 0) {
      failed = true;
      state.frogFailures += 1;
      saveState();
      draw();
    } else {
      saveState();
      draw();
      startTimer();
    }
  }

  function playFrogJump(toBank, onDone) {
    const jumper = app.querySelector(".frog-jumper");
    const activeLeaf = app.querySelector(".river-leaf.active");
    const bank = app.querySelector(".river-bank");
    if (!jumper || !activeLeaf || !bank) {
      onDone();
      return;
    }

    const frogBox = jumper.getBoundingClientRect();
    const leafBox = activeLeaf.getBoundingClientRect();
    const bankBox = bank.getBoundingClientRect();
    const leafX = leafBox.left + leafBox.width / 2 - (frogBox.left + frogBox.width / 2);
    const leafY = leafBox.top + leafBox.height * .25 - (frogBox.top + frogBox.height / 2);
    const bankX = bankBox.left + bankBox.width / 2 - (frogBox.left + frogBox.width / 2);
    const bankY = bankBox.top + bankBox.height / 2 - (frogBox.top + frogBox.height / 2);
    const keyframes = toBank
      ? [
          { transform: "translateX(-50%) rotate(0deg)" },
          { transform: `translate(calc(-50% + ${leafX * .55}px), ${leafY - 72}px) rotate(-9deg)`, offset: .34 },
          { transform: `translate(calc(-50% + ${leafX}px), ${leafY}px) rotate(0deg)`, offset: .58 },
          { transform: `translate(calc(-50% + ${(leafX + bankX) / 2}px), ${bankY - 82}px) rotate(8deg)`, offset: .8 },
          { transform: `translate(calc(-50% + ${bankX}px), ${bankY}px) rotate(0deg)` }
        ]
      : [
          { transform: "translateX(-50%) rotate(0deg)" },
          { transform: `translate(calc(-50% + ${leafX * .5}px), ${leafY - 76}px) rotate(-10deg)`, offset: .5 },
          { transform: `translate(calc(-50% + ${leafX}px), ${leafY}px) rotate(0deg)` }
        ];
    const animation = jumper.animate(keyframes, {
      duration: toBank ? 700 : 620,
      easing: "cubic-bezier(.22,.72,.24,1)",
      fill: "forwards"
    });
    animation.finished.catch(() => {}).then(() => {
      if (view === "frog") onDone();
    });
  }

  function advanceAfterJump(lastWordForFrog) {
    if (lastWordForFrog) {
      if (frog === 3) {
        complete = true;
        state.frogRounds += 1;
        state.frogBestScore = Math.max(state.frogBestScore, score);
        saveState();
        draw();
        return;
      }
      feedback = `第 ${frog} 只青蛙已过河，第 ${frog + 1} 只出发`;
      frog += 1;
      words = frogWordSets[frog - 1];
      wordIndex = 0;
      riverDrift = 0;
      timerStartedAt = 0;
      lastRiverMotionAt = 0;
    } else {
      wordIndex += 1;
      feedback = "漂亮！青蛙跳上了下一片荷叶";
    }
    typed = "";
    inputLocked = false;
    draw();
    startTimer();
  }

  function submit(raw) {
    if (complete || failed || inputLocked) return;
    const key = raw.length === 1 ? raw.toLowerCase() : "";
    if (!/[a-z ]/.test(key)) return;
    const word = words[wordIndex];
    const target = normalizeTarget(word.word);
    if (key === target[typed.length]) {
      typed += key;
      if (typed === target) {
        clearGameTimer();
        timerStartedAt = 0;
        lastRiverMotionAt = 0;
        sound(true);
        recordMastery(word);
        score += 100 + combo * 15;
        combo += 1;
        mistakes = 0;
        const lastWordForFrog = wordIndex === words.length - 1;
        inputLocked = true;
        feedback = lastWordForFrog ? "整轮完成！青蛙跃向对岸" : "拼写正确，青蛙起跳！";
        draw();
        requestAnimationFrame(() => playFrogJump(lastWordForFrog, () => advanceAfterJump(lastWordForFrog)));
      } else draw();
    } else {
      sound(false);
      score = Math.max(0, score - 10);
      typed = "";
      mistakes += 1;
      recordWordError(words[wordIndex].id, key);
      if (mistakes >= 3) {
        lives -= 1;
        state.frogLivesLost += 1;
        mistakes = 0;
        feedback = "当前词累计错键 3 次，生命 -1、得分 -10";
        if (lives === 0) {
          failed = true;
          state.frogFailures += 1;
        }
        saveState();
      } else feedback = `输入错误（${mistakes}/3），得分 -10，本词重拼`;
      draw();
      if (!failed) startTimer();
    }
  }

  function draw() {
    const word = words[wordIndex];
    const target = normalizeTarget(word.word);
    const highlighted = [...target].map((char, index) => index < typed.length ? `<span class="typed">${char === " " ? "␣" : char}</span>` : `<span>${char === " " ? "␣" : char}</span>`).join("");
    const cameraShift = Math.max(0, wordIndex - 3) * leafGap;
    const leafPositions = words.map((leafWord, index) => ({
      leafWord,
      index,
      y: 68 - index * leafGap + cameraShift
    })).filter(({ y }) => y >= -18 && y <= 88);
    const leaves = leafPositions.map(({ leafWord, index, y }) => {
      const leafTarget = normalizeTarget(leafWord.word);
      const leafText = index === wordIndex
        ? highlighted
        : [...leafTarget].map((char) => char === " " ? "␣" : char).join("");
      return `
        <div class="river-leaf ${index === wordIndex ? "active" : ""} ${index < wordIndex ? "passed" : ""}"
          data-leaf-index="${index}"
          style="--leaf-x:${leafLanes[index % leafLanes.length]}%;--leaf-y:${y}%">
          <div class="leaf-word">${leafText}</div>
        </div>
      `;
    }).join("");
    const previousLane = wordIndex === 0 ? 50 : leafLanes[(wordIndex - 1) % leafLanes.length];
    const previousLeafY = wordIndex === 0
      ? 82
      : 68 - (wordIndex - 1) * leafGap + cameraShift - 10;
    const frogDrift = wordIndex === 0 ? 0 : riverDrift;
    const totalPracticeWords = frogWordSets.reduce((sum, set) => sum + set.length, 0);
    shell(`
      <section class="screen game-layout">
        <aside class="panel side-stats">
          <div class="muted">激流勇进</div>
          <h2>第 ${frog} 只青蛙</h2>
          <div class="progress-track"><span style="width:${(wordIndex + 1) / words.length * 100}%"></span></div>
          <div class="stat-block"><span>当前青蛙</span><strong>${wordIndex + 1} / ${words.length} 词</strong></div>
          <div class="stat-block"><span>已送过河</span><strong>${frog - 1} / 3 只</strong></div>
          <div class="stat-block"><span>生命值</span><strong class="hearts">${"♥".repeat(lives)}${"♡".repeat(3 - lives)}</strong></div>
          <div class="stat-block"><span>当前词错键</span><strong>${mistakes} / 3</strong></div>
          <div class="stat-block"><span>荷叶倒计时</span><strong data-frog-timer>${timeLeft}s</strong></div>
          <div class="stat-block"><span>得分</span><strong>${score}</strong></div>
        </aside>
        <div class="game-main">
          <h1>激流勇进</h1>
          <div class="river-arena">
            <div class="river-wave"></div><div class="river-wave"></div><div class="river-wave"></div>
            <div class="river-bank"><span>对岸</span></div>
            <div class="delivered">已过河 ${"🐸".repeat(frog - 1)}${"○".repeat(4 - frog)}</div>
            <div class="river-current" aria-hidden="true">› › ›</div>
            ${leaves}
            <div class="frog-jumper ${inputLocked ? "jumping" : ""}"
              style="--frog-x:${previousLane}%;--frog-y:${previousLeafY}%;--frog-drift:${frogDrift}px">🐸</div>
            ${complete || failed ? `
              <div class="result-overlay"><div><h2>${complete ? "3 只青蛙全部过河" : "生命耗尽，闯关失败"}</h2><p>${complete ? `三只青蛙练习不同词组 · 共完成 ${totalPracticeWords} 次拼写` : `已送达 ${frog - 1} / 3 只`}</p><p>${score} 分 · 剩余生命 ${lives}</p><button class="${complete ? "primary-button" : "danger-button"}" data-retry>再玩一轮</button></div></div>
            ` : ""}
          </div>
          <div class="feedback">${feedback}<span style="float:right">填充 ${typed.length} / ${target.length}</span></div>
          ${frogKeyboard(target[typed.length], inputLocked)}
        </div>
      </section>
    `);
    bindFrogKeyboard(submit);
    app.querySelector("[data-retry]")?.addEventListener("click", renderFrog);
    if (timerStartedAt && !complete && !failed) updateRiverMotion();
  }

  window.addEventListener("keydown", (event) => {
    if (view !== "frog" || event.repeat) return;
    if (event.key === " " || /^[a-z]$/i.test(event.key)) {
      event.preventDefault();
      submit(event.key);
    }
  }, { signal: navigationSignal() });
  draw();
  startTimer();
}

function frogKeyboard(target, disabled = false) {
  return `
    <div class="virtual-keyboard ${disabled ? "input-locked" : ""}">
      ${["QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM"].map((row) => `<div class="key-row">${[...row].map((key) => `<button class="key ${target?.toUpperCase() === key ? "target" : ""}" data-frog-key="${key}" ${disabled ? "disabled" : ""}>${key}</button>`).join("")}</div>`).join("")}
      <div class="key-row"><button class="key space-key ${target === " " ? "target" : ""}" data-frog-key=" " ${disabled ? "disabled" : ""}>空格</button></div>
    </div>
  `;
}

function bindFrogKeyboard(handler) {
  app.querySelectorAll("[data-frog-key]").forEach((button) => button.addEventListener("click", () => handler(button.dataset.frogKey)));
}

function renderReport() {
  const allWords = BOOKS.reduce(
    (bookWords, book) => bookWords.concat(book.units.reduce(
      (unitWords, unit) => unitWords.concat(unit.words),
      []
    )),
    []
  );
  const mastered = Object.values(state.wordProgress).filter((item) => item.mastered).length;
  const errors = allWords
    .map((word) => ({ word, errors: state.wordProgress[word.id]?.errors || 0 }))
    .filter((item) => item.errors > 0)
    .sort((a, b) => b.errors - a.errors)
    .slice(0, 6);
  const weakKeys = Object.entries(state.keyboardMistakes)
    .filter(([, count]) => count > 0)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 6);
  const accuracy = Math.round(state.keyboardCorrect * 100 / Math.max(1, state.keyboardKeystrokes));
  const completed = BOOKS.reduce(
    (count, book) => count + book.units.filter((unit) => stageStars(unit.id, 3)).length,
    0
  );

  shell(`
    <section class="screen">
      <h1 class="screen-title">家长学情报告</h1>
      <p class="screen-lead">所有数据仅保存在当前浏览器本地，不上传个人信息</p>
      <div class="report-grid">
        <div class="panel report-card">
          <h2>学习概览</h2>
          <div class="metric-row">
            <div class="metric"><span>掌握词汇</span><strong>${mastered}</strong></div>
            <div class="metric"><span>通关单元</span><strong>${completed}</strong></div>
            <div class="metric"><span>积分 / 金币</span><strong>${state.points} / ${state.coins}</strong></div>
          </div>
          <p>激流勇进：成功 ${state.frogRounds} 轮 · 失败 ${state.frogFailures} 轮 · 损失 ${state.frogLivesLost} 生命</p>
          <p>键盘启蒙：完成 ${state.keyboardRounds} 轮 · 失败 ${state.keyboardFailures} 轮 · 超时 ${state.keyboardTimeouts} 次</p>
        </div>
        <div class="panel report-card">
          <h2>键盘启蒙报告</h2>
          <div class="metric-row">
            <div class="metric"><span>正确率</span><strong>${accuracy}%</strong></div>
            <div class="metric"><span>累计击键</span><strong>${state.keyboardKeystrokes}</strong></div>
            <div class="metric"><span>最高关卡</span><strong>${Math.max(0, ...Object.keys(state.keyboardStars).map(Number)) + 1}</strong></div>
          </div>
          <h3>易错键</h3>
          <div class="weak-list">${weakKeys.length ? weakKeys.map(([key, count]) => `<div class="weak-item"><strong>${key === " " ? "空格" : key}</strong><span>错 ${count} 次</span></div>`).join("") : "暂无错键记录"}</div>
        </div>
        <div class="panel report-card">
          <h2>词汇薄弱项</h2>
          <div class="weak-list">${errors.length ? errors.map(({ word, errors: count }) => `<div class="weak-item"><span><strong>${escapeHtml(word.word)}</strong> · ${escapeHtml(word.meaning)}</span><span>错 ${count} 次</span></div>`).join("") : "暂无错词记录"}</div>
        </div>
        <div class="panel report-card">
          <h2>本地存档</h2>
          <p>存档会跟随当前浏览器。清理浏览器数据会清空学习进度。</p>
          <div class="lesson-actions"><button class="secondary-button" data-export>导出存档</button><button class="danger-button" data-reset>清空进度</button></div>
        </div>
      </div>
    </section>
  `);
  app.querySelector("[data-export]").addEventListener("click", () => {
    const blob = new Blob([JSON.stringify(state, null, 2)], { type: "application/json" });
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = "西瓜英语学习存档.json";
    link.click();
    URL.revokeObjectURL(link.href);
  });
  app.querySelector("[data-reset]").addEventListener("click", () => {
    if (confirm("确定清空所有本地学习进度吗？")) {
      state = { ...defaultState };
      saveState();
      renderReport();
    }
  });
}

if ("serviceWorker" in navigator) {
  window.addEventListener("load", () => navigator.serviceWorker.register("./service-worker.js").catch(() => {}));
}

render();
