// NGSL(英語頻度でレベル分け) + JMdict(日本語訳の逆引き) + Tatoeba(フレーズ) から vocab.json を生成
const XLSX = require("xlsx");
const fs = require("fs");
const path = require("path");

// ---- 明らかな誤りだけを直す最小限の既定補正（最頻出の基本語） ----
const OVERRIDE = {
  go: "行く（いく）", come: "来る（くる）", people: "人々（ひとびと）",
  get: "得る（える）", like: "好き（すき）", good: "良い（よい）",
  time: "時間（じかん）", day: "日（ひ）", man: "男（おとこ）",
  woman: "女（おんな）", child: "子供（こども）", work: "働く（はたらく）",
  use: "使う（つかう）", find: "見つける（みつける）", give: "与える（あたえる）",
  tell: "伝える（つたえる）", ask: "尋ねる（たずねる）", need: "必要（ひつよう）",
  feel: "感じる（かんじる）", leave: "去る（さる）", call: "呼ぶ（よぶ）",
  friend: "友達（ともだち）", relationship: "関係（かんけい）", amount: "量（りょう）",
};

// ---- 1. NGSL ----
const wb = XLSX.readFile(path.join(__dirname, "ngsl.xlsx"));
const rows = XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]]);
let ngsl = rows
  .filter(r => String(r.Wordlist || "").includes("NGSL") && r.Lemma && Number(r.Rank))
  .map(r => ({ word: String(r.Lemma).trim(), rank: Number(r.Rank) }))
  .filter(r => /^[a-z][a-z'-]*$/i.test(r.word));
ngsl.sort((a, b) => a.rank - b.rank);
{ const seen = new Set(); ngsl = ngsl.filter(r => { const k = r.word.toLowerCase(); if (seen.has(k)) return false; seen.add(k); return true; }); }

const STOP = new Set(("a an the this that these those i you he she it we they me him her us them my your his its our their mine yours " +
  "am is are was were be been being do does did have has had will would shall should can could may might must " +
  "and or but so if then than as of to in on at by for with from into onto up down out off over under about " +
  "not no yes very too also just only even still yet more most much many few some any all each every other " +
  "who whom whose which what when where why how there here " +
  "one two three four five six seven eight nine ten s t re ve ll d m o").split(/\s+/));
ngsl = ngsl.filter(r => !STOP.has(r.word.toLowerCase()));

// ---- 2. JMdict 逆引き（第一義・日常語優先） ----
const JM = path.join(__dirname, "jmdict");
const files = fs.readdirSync(JM).filter(f => /^term_bank_\d+\.json$/.test(f));
const index = new Map();
function commonness(t) {
  if (/\bnews1k\b|\bichi1\b/.test(t)) return 3;
  if (/\bnews[2-4]k\b|\bspec1\b|\bichi2\b/.test(t)) return 2;
  if (/\bgai1\b|\bnews[5-9]k\b|\bnews1[0-2]k\b|\bspec2\b|\bgai2\b/.test(t)) return 1;
  return 0;
}
function addGloss(gloss, term, reading, common, score) {
  if (!gloss || typeof gloss !== "string") return;
  let g = gloss.trim().toLowerCase().replace(/^to /, "");
  if (!/^[a-z][a-z '-]*$/.test(g)) return;
  const cur = index.get(g);
  if (!cur || common > cur.common || (common === cur.common && score > cur.score)) {
    index.set(g, { term, reading, common, score });
  }
}
for (const f of files) {
  const arr = JSON.parse(fs.readFileSync(path.join(JM, f)));
  for (const e of arr) {
    const term = e[0], reading = e[1], defTags = e[2] || "", glossary = e[5] || [], score = e[4] || 0, termTags = e[7] || "";
    if (/\bname\b/.test(defTags)) continue;
    if (glossary.length) addGloss(glossary[0], term, reading, commonness(termTags), score);
  }
}
function jpFor(word) {
  if (OVERRIDE[word]) return OVERRIDE[word];
  const jp = index.get(word.toLowerCase());
  if (!jp) return null;
  return (jp.term !== jp.reading && /[一-鿿]/.test(jp.term)) ? `${jp.term}（${jp.reading}）` : jp.term;
}

// ---- 3. レベル分け（NGSL rank） ----
const bands = [
  { name: "初級（基礎英単語）", desc: "英語の最頻出・基礎語彙", max: 500, prefix: "基礎" },
  { name: "中級", desc: "日常でよく使う語彙", max: 1200, prefix: "中級" },
  { name: "高校・一般", desc: "入試・実用でよく出る語彙", max: 2000, prefix: "応用" },
  { name: "社会人・発展", desc: "発展的な語彙", max: 99999, prefix: "発展" },
];
const LESSON_SIZE = 20;
const courses = [];
for (const b of bands) {
  const words = ngsl
    .filter(r => r.rank <= b.max && (b === bands[0] || r.rank > bands[bands.indexOf(b) - 1].max))
    .map(r => ({ english: r.word, japanese: jpFor(r.word) }))
    .filter(w => w.japanese);
  const lessons = [];
  for (let i = 0; i < words.length; i += LESSON_SIZE) {
    lessons.push({
      title: `${b.prefix} ${Math.floor(i / LESSON_SIZE) + 1}`,
      words: words.slice(i, i + LESSON_SIZE).map(w => ({ english: w.english, japanese: w.japanese, type: "word" })),
    });
  }
  courses.push({ name: b.name, description: b.desc, lessons });
}

// ---- 4. Tatoeba フレーズ ----
const lines = fs.readFileSync(path.join(__dirname, "jpn.txt"), "utf8").split("\n");
const seenP = new Set();
const phrases = [];
for (const line of lines) {
  const c = line.split("\t");
  if (c.length < 2) continue;
  let eng = c[0].trim(), jp = c[1].trim();
  const wc = eng.split(/\s+/).length;
  if (wc < 2 || wc > 3 || eng.length > 20 || jp.length > 12 || /[0-9]/.test(eng)) continue;
  eng = eng.replace(/[.!?]+$/, ""); jp = jp.replace(/[。！？]+$/, "");
  const k = eng.toLowerCase();
  if (seenP.has(k)) continue; seenP.add(k);
  phrases.push({ english: eng, japanese: jp, type: "idiom" });
  if (phrases.length >= 120) break;
}
const pLessons = [];
for (let i = 0; i < phrases.length; i += 15) pLessons.push({ title: `フレーズ ${Math.floor(i / 15) + 1}`, words: phrases.slice(i, i + 15) });
courses.push({ name: "よく使うフレーズ（熟語）", description: "日常でよく使う短い表現", lessons: pLessons });

const out = {
  attribution: "単語の見出し語: NGSL (Browne, Culligan & Phillips, CC BY-SA 4.0) / 日本語訳: JMdict (© EDRDG, CC BY-SA 4.0) / フレーズ: Tatoeba Project (CC BY 2.0 FR)",
  courses,
};
fs.writeFileSync(path.join(__dirname, "vocab.json"), JSON.stringify(out));

console.log("=== 生成サマリ ===");
let total = 0;
courses.forEach(c => { const n = c.lessons.reduce((s, l) => s + l.words.length, 0); total += n; console.log(`  ${c.name}: ${c.lessons.length}レッスン / ${n}語`); });
console.log("  合計:", total, "語 /", (fs.statSync(path.join(__dirname, "vocab.json")).size / 1024).toFixed(0), "KB");
console.log("\n=== 初級サンプル ===");
courses[0].lessons[0].words.slice(0, 12).forEach(w => console.log(`  ${w.english} => ${w.japanese}`));
