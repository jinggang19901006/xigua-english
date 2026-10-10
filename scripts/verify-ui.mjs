import { mkdir } from "node:fs/promises";
import { chromium } from "playwright";

const baseUrl = process.env.APP_URL || "http://127.0.0.1:4173";
const browser = await chromium.launch();
const errors = [];

async function assertNoOverflow(page, label) {
  const result = await page.evaluate(() => ({
    width: document.documentElement.scrollWidth,
    viewport: document.documentElement.clientWidth,
    height: document.documentElement.scrollHeight,
    viewportHeight: document.documentElement.clientHeight
  }));
  if (result.width > result.viewport + 2) {
    throw new Error(`${label} horizontal overflow: ${JSON.stringify(result)}`);
  }
}

try {
  await mkdir("artifacts/web-v2", { recursive: true });
  const page = await browser.newPage({ viewport: { width: 1280, height: 800 } });
  page.on("pageerror", (error) => errors.push(error.message));
  page.on("console", (message) => {
    if (message.type() === "error") errors.push(message.text());
  });

  await page.goto(baseUrl, { waitUntil: "networkidle" });
  await page.getByRole("heading", { name: "选择英语探险教材" }).waitFor();
  await page.screenshot({ path: "artifacts/web-v2/books-1280x800.png" });
  await assertNoOverflow(page, "books");

  await page.getByRole("button", { name: /5 年级上册/ }).click();
  await page.getByRole("heading", { name: /2026秋版.*5 年级上册/ }).waitFor();
  await page.getByRole("button", { name: /Unit 1 · Different friends.*34 个词\/短语/ }).click();
  await page.getByRole("heading", { name: "Different friends" }).waitFor();
  await page.getByText("词库 34 个词/短语", { exact: true }).waitFor();
  await page.getByText("新词启航营").waitFor();
  await page.screenshot({ path: "artifacts/web-v2/map-1280x800.png" });
  await assertNoOverflow(page, "map");

  await page.locator('.map-node[data-node="words"]').click();
  await page.locator("[data-next]").click();
  await page.locator("[data-next]").click();
  await page.locator("[data-option]").first().click();
  await page.locator("[data-next]").click();
  await page.locator("[data-option]").first().click();
  await page.locator("[data-next]").click();
  await page.locator("[data-answer]").click();
  await page.locator("[data-next]").waitFor();
  await page.locator('[data-action="back"]').click();

  await page.getByRole("button", { name: /训练中心/ }).click();
  await page.getByRole("heading", { name: "青蛙打字训练中心" }).waitFor();
  await page.getByRole("button", { name: /基准键位/ }).click();
  await page.getByText("错键不扣生命").waitFor();
  await page.getByRole("button", { name: "空格" }).last().waitFor();
  await page.screenshot({ path: "artifacts/web-v2/keyboard-1280x800.png" });
  await assertNoOverflow(page, "keyboard");

  await page.evaluate(() => {
    const key = "XIGUA_ENGLISH_WEB_V2";
    const state = JSON.parse(localStorage.getItem(key));
    state.keyboardStars = { 0: 3, 1: 3, 2: 3, 3: 3 };
    state.stageStars = {
      "pep_g5_first_u1:0": 3,
      "pep_g5_first_u1:1": 3,
      "pep_g5_first_u1:2": 3
    };
    localStorage.setItem(key, JSON.stringify(state));
  });
  await page.reload({ waitUntil: "networkidle" });
  await page.getByRole("button", { name: /训练中心/ }).click();
  await page.getByRole("button", { name: "开始渡河" }).click();
  await page.getByRole("heading", { name: "激流勇进" }).waitFor();
  await page.getByText("已送过河", { exact: false }).waitFor();
  const initialLeafCount = await page.locator(".river-leaf").count();
  if (initialLeafCount < 4) throw new Error(`frog river should show multiple leaves, found ${initialLeafCount}`);
  const leafTopsBefore = await page.locator(".river-leaf").evaluateAll((leaves) =>
    leaves.map((leaf) => leaf.getBoundingClientRect().top)
  );
  await page.waitForTimeout(600);
  const leafTopsAfter = await page.locator(".river-leaf").evaluateAll((leaves) =>
    leaves.map((leaf) => leaf.getBoundingClientRect().top)
  );
  const leafDrops = leafTopsAfter.map((top, index) => top - leafTopsBefore[index]);
  if (Math.min(...leafDrops) <= 0 || Math.max(...leafDrops) - Math.min(...leafDrops) > 2) {
    throw new Error(`frog leaves should descend together: ${leafDrops.join(", ")}`);
  }
  const initialDrop = leafDrops.reduce((sum, drop) => sum + drop, 0) / leafDrops.length;
  const initialFrogBox = await page.locator(".frog-jumper").boundingBox();
  let previousFrogTop = initialFrogBox.y;
  const practicedWords = [];
  await page.screenshot({ path: "artifacts/web-v2/frog-1280x800.png" });
  await assertNoOverflow(page, "frog");
  for (let attempt = 1; attempt <= 15; attempt += 1) {
    const target = (await page.locator(".river-leaf.active .leaf-word").innerText()).replaceAll("␣", " ");
    practicedWords.push(target);
    await page.keyboard.type(target);
    await page.waitForTimeout(760);
    if (attempt === 1) {
      const nextTopsBefore = await page.locator(".river-leaf").evaluateAll((leaves) =>
        leaves.map((leaf) => leaf.getBoundingClientRect().top)
      );
      await page.waitForTimeout(600);
      const nextTopsAfter = await page.locator(".river-leaf").evaluateAll((leaves) =>
        leaves.map((leaf) => leaf.getBoundingClientRect().top)
      );
      const nextDrops = nextTopsAfter.map((top, index) => top - nextTopsBefore[index]);
      const nextDrop = nextDrops.reduce((sum, drop) => sum + drop, 0) / nextDrops.length;
      if (Math.max(...nextDrops) - Math.min(...nextDrops) > 2 || Math.abs(nextDrop - initialDrop) > 1) {
        throw new Error(`frog river speed changed after a correct word: ${initialDrop} -> ${nextDrop}`);
      }
    }
    if (attempt <= 2) {
      const landedFrogBox = await page.locator(".frog-jumper").boundingBox();
      if (landedFrogBox.y >= previousFrogTop - 20) {
        throw new Error(`frog did not keep moving upward: ${previousFrogTop} -> ${landedFrogBox.y}`);
      }
      previousFrogTop = landedFrogBox.y;
    }
    if (attempt === 3) await page.screenshot({ path: "artifacts/web-v2/frog-progress-1280x800.png" });
    if (attempt === 5) await page.getByText("1 / 3 只", { exact: true }).waitFor();
    if (attempt === 10) await page.getByText("2 / 3 只", { exact: true }).waitFor();
  }
  const firstSet = [...practicedWords.slice(0, 5)].sort().join("|");
  const secondSet = [...practicedWords.slice(5, 10)].sort().join("|");
  if (firstSet === secondSet) throw new Error("frog word sets should differ between frogs");
  await page.getByRole("heading", { name: "3 只青蛙全部过河" }).waitFor();
  await page.getByText("共完成 15 次拼写", { exact: false }).waitFor();

  await page.getByRole("button", { name: /家长报告/ }).click();
  await page.getByRole("heading", { name: "家长学情报告" }).waitFor();
  await page.screenshot({ path: "artifacts/web-v2/report-1280x800.png" });
  await assertNoOverflow(page, "report");

  const largePage = await browser.newPage({ viewport: { width: 2048, height: 1536 } });
  largePage.on("pageerror", (error) => errors.push(error.message));
  await largePage.goto(baseUrl, { waitUntil: "networkidle" });
  await largePage.getByRole("heading", { name: "选择英语探险教材" }).waitFor();
  await largePage.screenshot({ path: "artifacts/web-v2/books-2048x1536.png" });
  await assertNoOverflow(largePage, "large books");
  await largePage.close();

  await page.evaluate(() => navigator.serviceWorker.ready);
  await page.reload({ waitUntil: "networkidle" });
  await page.context().setOffline(true);
  await page.reload({ waitUntil: "domcontentloaded" });
  await page.getByText("西瓜英语").first().waitFor();
  await page.context().setOffline(false);

  if (errors.length) throw new Error(`Browser errors:\n${errors.join("\n")}`);
  console.log("Web v2 verification passed at 1280x800, 2048x1536, and offline reload.");
} finally {
  await browser.close();
}
