import { mkdir } from "node:fs/promises";
import { chromium } from "playwright";

const baseUrl = process.env.APP_URL || "http://127.0.0.1:4173";
const browser = await chromium.launch();

try {
  const page = await browser.newPage({ viewport: { width: 1280, height: 800 } });
  const errors = [];

  page.on("pageerror", (error) => errors.push(error.message));
  page.on("console", (message) => {
    if (message.type() === "error") errors.push(message.text());
  });

  await mkdir("artifacts", { recursive: true });
  await page.goto(baseUrl, { waitUntil: "networkidle" });
  await page.getByText("My Wonderful Day").waitFor();

  const rootBox = await page.locator("#root").boundingBox();
  if (!rootBox || Math.round(rootBox.width) !== 1280 || Math.round(rootBox.height) !== 800) {
    throw new Error(`Unexpected tablet viewport: ${JSON.stringify(rootBox)}`);
  }

  await page.screenshot({ path: "artifacts/map-1280x800.png" });

  await page.getByRole("button", { name: /家长学情报告/ }).click();
  await page.getByText("家长学情监控中心").waitFor();
  await page.screenshot({ path: "artifacts/parent-report-1280x800.png" });

  await page.getByRole("button", { name: /返回闯关/ }).first().click();
  await page.getByText("新词启航营").waitFor();
  await page.getByText("新词启航营").click();
  await page.getByText("单词探索 1 / 10").waitFor();
  await page.screenshot({ path: "artifacts/word-learning-1280x800.png" });

  await page.getByRole("button", { name: /返回地图/ }).click();
  await page.getByText("键盘拼写营").click();
  await page.getByText("键盘拼写模式").waitFor();

  await page.getByRole("button", { name: /返回地图/ }).click();
  await page.getByText("语法思考工坊").click();
  await page.getByText("情境引导发现").waitFor();

  await page.getByRole("button", { name: /返回地图/ }).click();
  await page.getByText("时空遗忘兽 Boss").click();
  await page.getByText("时空遗忘巨兽 Boss").waitFor();

  const largePage = await browser.newPage({ viewport: { width: 1920, height: 1200 } });
  await largePage.goto(baseUrl, { waitUntil: "networkidle" });
  const largeRootBox = await largePage.locator("#root").boundingBox();
  if (
    !largeRootBox ||
    Math.round(largeRootBox.width) !== 1920 ||
    Math.round(largeRootBox.height) !== 1200
  ) {
    throw new Error(`Unexpected large tablet viewport: ${JSON.stringify(largeRootBox)}`);
  }
  await largePage.screenshot({ path: "artifacts/map-1920x1200.png" });

  if (errors.length) {
    throw new Error(`Browser errors:\n${errors.join("\n")}`);
  }

  console.log("UI verification passed: all learning views at 1280x800 and 1920x1200.");
} catch (error) {
  console.error(error);
  process.exitCode = 1;
} finally {
  await browser.close();
}
