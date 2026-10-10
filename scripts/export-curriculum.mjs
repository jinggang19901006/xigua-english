import { mkdir, readFile, writeFile } from "node:fs/promises";

const sourcePath = new URL(
  "../app/src/main/java/com/magicenglish/quest/LearningData.kt",
  import.meta.url
);
const outputPath = new URL("../src/curriculum-data.js", import.meta.url);

const source = await readFile(sourcePath, "utf8");

function balancedBlock(text, openingParen) {
  let depth = 0;
  let quoted = false;
  let escaped = false;

  for (let index = openingParen; index < text.length; index += 1) {
    const char = text[index];
    if (quoted) {
      if (escaped) escaped = false;
      else if (char === "\\") escaped = true;
      else if (char === '"') quoted = false;
      continue;
    }
    if (char === '"') quoted = true;
    else if (char === "(") depth += 1;
    else if (char === ")") {
      depth -= 1;
      if (depth === 0) return text.slice(openingParen + 1, index);
    }
  }
  throw new Error(`Unclosed block at ${openingParen}`);
}

function decode(value) {
  return JSON.parse(`"${value.replaceAll("\n", "\\n")}"`);
}

function strings(text) {
  return [...text.matchAll(/"((?:\\.|[^"\\])*)"/g)].map((match) => decode(match[1]));
}

function namedString(text, name) {
  const match = text.match(new RegExp(`${name}\\s*=\\s*"((?:\\\\.|[^"\\\\])*)"`));
  if (!match) throw new Error(`Missing ${name}`);
  return decode(match[1]);
}

const supplements = new Map();
const supplementalStart = source.indexOf("private val supplementalVocabulary = mapOf(");
const supplementalOpen = source.indexOf("(", supplementalStart);
const supplementalBody = balancedBlock(source, supplementalOpen);

for (const match of supplementalBody.matchAll(/"([^"]+)"\s+to\s+listOf\(([^)]*)\)/gs)) {
  const pairs = [...match[2].matchAll(/"([^"]+)"\s+to\s+"([^"]+)"/g)].map((pair) => [
    decode(pair[1]),
    decode(pair[2])
  ]);
  supplements.set(match[1], pairs);
}

const books = [];
const bookPattern = /book\((\d+),\s*Volume\.(FIRST|SECOND),\s*listOf\(/g;
let bookMatch;

while ((bookMatch = bookPattern.exec(source))) {
  const grade = Number(bookMatch[1]);
  const volume = bookMatch[2] === "FIRST" ? "first" : "second";
  const listOpen = source.indexOf("(", bookMatch.index + bookMatch[0].lastIndexOf("listOf"));
  const listBody = balancedBlock(source, listOpen);
  const units = [];
  let cursor = 0;

  while (true) {
    const seedStart = listBody.indexOf("seed(", cursor);
    if (seedStart < 0) break;
    const seedOpen = listBody.indexOf("(", seedStart);
    const seedBody = balancedBlock(listBody, seedOpen);
    const seedEnd = seedOpen + seedBody.length + 2;
    const initialStrings = strings(seedBody);
    const title = initialStrings[0];
    const topic = initialStrings[1];
    const pattern = namedString(seedBody, "pattern");
    const practice = namedString(seedBody, "practice");
    const answer = Number(seedBody.match(/answer\s*=\s*(\d+)/)?.[1] ?? 0);
    const optionsMatch = seedBody.match(/options\s*=\s*listOf\(([^)]*)\)/s);
    const practiceOptions = optionsMatch ? strings(optionsMatch[1]) : [];
    const vocabularyArea = seedBody.slice(0, seedBody.indexOf("pattern"));
    const vocabulary = [...vocabularyArea.matchAll(/"([^"]+)"\s+to\s+"([^"]+)"/g)].map(
      (pair) => [decode(pair[1]), decode(pair[2])]
    );
    const number = units.length + 1;
    const bookId = `pep_g${grade}_${volume}`;
    const unitId = `${bookId}_u${number}`;
    const merged = [...vocabulary, ...(supplements.get(unitId) ?? [])];
    const unique = [...new Map(merged.map((pair) => [pair[0].toLowerCase(), pair])).values()];

    units.push({
      id: unitId,
      number,
      title,
      topic,
      words: unique.map(([word, meaning], index) => ({
        id: `${unitId}_w${index + 1}`,
        word,
        kind: /\s/.test(word.trim()) ? "phrase" : "word",
        practiceStages: ["learning", "spelling", "boss", "frog"],
        phonetic: "点击喇叭听标准发音",
        meaning,
        example: `Teacher: Can you read "${word}"? Student: Yes. The word is "${word}".`
      })),
      grammar: {
        id: `${unitId}_g1`,
        title: "核心句型",
        tag: topic,
        pattern,
        practice,
        options: practiceOptions,
        answer,
        explanation: "观察核心句型和空格前后的词，选择语法与语义都正确的答案。"
      }
    });
    cursor = seedEnd;
  }

  books.push({
    id: `pep_g${grade}_${volume}`,
    series: grade === 5 && volume === "first" ? "人教版 PEP（2026秋版）" : "人教版 PEP",
    grade,
    volume,
    volumeLabel: volume === "first" ? "上册" : "下册",
    shortName: `${grade}${volume === "first" ? "上" : "下"}`,
    units
  });
  bookPattern.lastIndex = listOpen + listBody.length + 2;
}

if (books.length !== 8 || books.reduce((sum, book) => sum + book.units.length, 0) !== 46) {
  throw new Error(`Unexpected curriculum shape: ${books.length} books`);
}

await mkdir(new URL("../src/", import.meta.url), { recursive: true });
await writeFile(
  outputPath,
  `// Generated from LearningData.kt by scripts/export-curriculum.mjs.\nexport const BOOKS = ${JSON.stringify(books, null, 2)};\n`,
  "utf8"
);

const wordCount = books.reduce(
  (sum, book) => sum + book.units.reduce((unitSum, unit) => unitSum + unit.words.length, 0),
  0
);
console.log(`Exported ${books.length} books, 46 units, ${wordCount} words.`);
