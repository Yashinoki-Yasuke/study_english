// JMdict(単語) + Tatoeba(フレーズ) から アプリ用 vocab.json を生成する
const fs = require("fs");
const path = require("path");

const JMDICT_DIR = path.join(__dirname, "jmdict");
const TATOEBA = path.join(__dirname, "jpn.txt");

// ---- 1. JMdict 読み込み ----
const files = fs.readdirSync(JMDICT_DIR).filter(f => /^term_bank_\d+\.json$/.test(f));
let entries = [];
for (const f of files) {
  entries = entries.concat(JSON.parse(fs.readFileSync(path.join(JMDICT_DIR, f))));
}

// 英訳が「学習に適した語」かどうか
function cleanGloss(gl) {
  if (!gl) return null;
  let g = gl.trim();
  // 単語/短い表現のみ（記号・数字・説明文を除外）
  if (!/^[a-zA-Z][a-zA-Z '-]*$/.test(g)) return null;
  const words = g.split(/\s+/);
  if (words.length > 3) return null;
  if (g.length > 22) return null;
  return g;
}

function tierOf(tags) {
  // 頻度タグから難易度を決定（初級ほど頻出）
  if (/\bnews1k\b|\bichi1\b/.test(tags)) return 0;      // 初級
  if (/\bnews2k\b|\bnews3k\b|\bspec1\b|\bichi2\b/.test(tags)) return 1; // 中級
  if (/\bnews4k\b|\bnews5k\b|\bnews6k\b|\bspec2\b/.test(tags)) return 2; // 高校
  if (/\bgai1\b|\bnews7k\b|\bnews8k\b|\bnews9k\b|\bnews1[0-2]k\b/.test(tags)) return 3; // 社会人
  return -1;
}

const seenEng = new Set();
const seenJp = new Set();
const buckets = [[], [], [], []];

for (const e of entries) {
  const term = e[0], reading = e[1], defTags = e[2] || "", glossary = e[5] || [], termTags = e[7] || "";
  const tags = defTags + " " + termTags;
  // 固有名詞・記号などは除外
  if (/\bname\b|\bunc\b|\bnum\b/.test(defTags)) continue;
  const tier = tierOf(termTags);
  if (tier < 0) continue;
  const eng = cleanGloss(glossary[0]);
  if (!eng) continue;
  const key = eng.toLowerCase();
  if (seenEng.has(key)) continue;
  if (seenJp.has(term)) continue;
  // 読みが漢字と異なる場合はふりがなを付ける
  const jp = (term !== reading && /[一-鿿]/.test(term)) ? `${term}（${reading}）` : term;
  seenEng.add(key);
  seenJp.add(term);
  buckets[tier].push({ english: eng, japanese: jp, type: "word", score: e[4] || 0 });
}

// スコア降順で各難易度から採用
const PER_TIER = 120;
const LESSON_SIZE = 20;
for (const b of buckets) b.sort((a, z) => z.score - a.score);

const tierMeta = [
  { name: "初級（よく使う語）", desc: "最も頻出する基本語彙", lessonPrefix: "基本" },
  { name: "中級", desc: "新聞などで頻出する語彙", lessonPrefix: "中級" },
  { name: "高校・一般", desc: "入試・実用でよく出る語彙", lessonPrefix: "応用" },
  { name: "社会人・発展", desc: "外来語・発展的な語彙", lessonPrefix: "発展" },
];

const courses = [];
buckets.forEach((b, ti) => {
  const chosen = b.slice(0, PER_TIER);
  const lessons = [];
  for (let i = 0; i < chosen.length; i += LESSON_SIZE) {
    const slice = chosen.slice(i, i + LESSON_SIZE).map(w => ({ english: w.english, japanese: w.japanese, type: "word" }));
    lessons.push({ title: `${tierMeta[ti].lessonPrefix} ${Math.floor(i / LESSON_SIZE) + 1}`, words: slice });
  }
  courses.push({ name: tierMeta[ti].name, description: tierMeta[ti].desc, lessons });
});

// ---- 2. Tatoeba フレーズ ----
const lines = fs.readFileSync(TATOEBA, "utf8").split("\n");
const seenPhrase = new Set();
const phrases = [];
for (const line of lines) {
  const cols = line.split("\t");
  if (cols.length < 2) continue;
  let eng = cols[0].trim();
  let jp = cols[1].trim();
  const wc = eng.split(/\s+/).length;
  if (wc < 2 || wc > 3) continue;           // 2〜3語のフレーズ
  if (eng.length > 20) continue;
  if (jp.length > 12) continue;
  if (/[0-9]/.test(eng)) continue;
  eng = eng.replace(/[.!?]+$/, "");
  jp = jp.replace(/[。！？]+$/, "");
  const key = eng.toLowerCase();
  if (seenPhrase.has(key)) continue;
  seenPhrase.add(key);
  phrases.push({ english: eng, japanese: jp, type: "idiom" });
  if (phrases.length >= 100) break;
}
const phraseLessons = [];
for (let i = 0; i < phrases.length; i += 15) {
  phraseLessons.push({ title: `フレーズ ${Math.floor(i / 15) + 1}`, words: phrases.slice(i, i + 15) });
}
courses.push({ name: "よく使うフレーズ（熟語）", description: "日常でよく使う短い表現", lessons: phraseLessons });

const out = {
  attribution: "単語データ: JMdict (Jitendex/JMdict-Yomitan, © EDRDG, CC BY-SA 4.0) / フレーズ: Tatoeba Project (CC BY 2.0 FR)",
  courses,
};

fs.writeFileSync(path.join(__dirname, "vocab.json"), JSON.stringify(out));

// ---- サマリ表示 ----
console.log("=== 生成サマリ ===");
courses.forEach(c => {
  const n = c.lessons.reduce((s, l) => s + l.words.length, 0);
  console.log(`  ${c.name}: ${c.lessons.length}レッスン / ${n}語`);
});
console.log("\n=== サンプル(初級 最初の8語) ===");
courses[0].lessons[0].words.slice(0, 8).forEach(w => console.log(`  ${w.english}  =>  ${w.japanese}`));
console.log("\n=== サンプル(フレーズ 最初の8) ===");
courses[courses.length - 1].lessons[0].words.slice(0, 8).forEach(w => console.log(`  ${w.english}  =>  ${w.japanese}`));
console.log("\nvocab.json 出力サイズ:", (fs.statSync(path.join(__dirname, "vocab.json")).size / 1024).toFixed(0), "KB");
