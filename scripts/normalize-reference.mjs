import { readFile, writeFile } from "node:fs/promises";

const path = new URL("../index.html", import.meta.url);
const html = await readFile(path, "utf8");
const styleEnd = html.indexOf("</style>") + "</style>".length;
const bodyStart = html.indexOf("<body", styleEnd);
const bodyTagEnd = html.indexOf(">", bodyStart) + 1;
const sourceStart = html.indexOf('<script type="text/babel">', bodyTagEnd);

if (styleEnd < "</style>".length || bodyStart < 0 || sourceStart < 0) {
  throw new Error("Unable to locate the reference document boundaries.");
}

const root =
  '  <div id="root" class="tablet-viewport bg-gradient-to-b from-[#38bdf8] via-[#60a5fa] to-[#93c5fd]"></div>';
const normalized = `${html.slice(0, styleEnd)}
</head>
${html.slice(bodyStart, bodyTagEnd)}
${root}

  ${html.slice(sourceStart)}`;

await writeFile(path, normalized);
