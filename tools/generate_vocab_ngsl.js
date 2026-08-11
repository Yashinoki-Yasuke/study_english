// NGSL(英語頻度でレベル分け) + JMdict(日本語訳の逆引き) + Tatoeba(フレーズ) から vocab.json を生成
const XLSX = require("xlsx");
const fs = require("fs");
const path = require("path");

// ---- 逆引きが不自然/誤りになりやすい高頻度語の補正表（NGSL上位を中心にレビュー） ----
const OVERRIDE = {
  // 動詞
  go: "行く（いく）", come: "来る（くる）", get: "得る（える）", take: "取る（とる）",
  make: "作る（つくる）", see: "見る（みる）", look: "見る（みる）", want: "欲しい（ほしい）",
  give: "与える（あたえる）", find: "見つける（みつける）", tell: "伝える（つたえる）",
  ask: "尋ねる（たずねる）", feel: "感じる（かんじる）", leave: "去る（さる）", call: "呼ぶ（よぶ）",
  use: "使う（つかう）", work: "働く（はたらく）", mean: "意味する（いみする）", show: "見せる（みせる）",
  help: "助ける（たすける）", meet: "会う（あう）", talk: "話す（はなす）", speak: "話す（はなす）",
  play: "遊ぶ（あそぶ）", hear: "聞く（きく）", follow: "従う（したがう）", run: "走る（はしる）",
  move: "動く（うごく）", open: "開ける（あける）", pay: "払う（はらう）", build: "建てる（たてる）",
  hold: "持つ（もつ）", happen: "起こる（おこる）", lead: "導く（みちびく）",
  understand: "理解する（りかいする）", thank: "感謝する（かんしゃする）", include: "含む（ふくむ）",
  stand: "立つ（たつ）", expect: "期待する（きたいする）", send: "送る（おくる）",
  return: "戻る（もどる）", walk: "歩く（あるく）", stop: "止まる（とまる）",
  remember: "覚える（おぼえる）", sit: "座る（すわる）", decide: "決める（きめる）",
  break: "壊す（こわす）", win: "勝つ（かつ）", drive: "運転する（うんてんする）",
  appear: "現れる（あらわれる）", pass: "通る（とおる）", produce: "生産する（せいさんする）",
  agree: "同意する（どういする）", eat: "食べる（たべる）", draw: "描く（かく）",
  raise: "上げる（あげる）", explain: "説明する（せつめいする）", describe: "説明する（せつめいする）",
  cut: "切る（きる）", die: "死ぬ（しぬ）", rise: "上がる（あがる）", grow: "育つ（そだつ）",
  lose: "失う（うしなう）", fall: "落ちる（おちる）", allow: "許す（ゆるす）",
  share: "共有する（きょうゆうする）", receive: "受け取る（うけとる）", choose: "選ぶ（えらぶ）",
  approach: "近づく（ちかづく）", wait: "待つ（まつ）", buy: "買う（かう）", sell: "売る（うる）",
  add: "加える（くわえる）", carry: "運ぶ（はこぶ）", visit: "訪れる（おとずれる）",
  please: "お願いします（おねがいします）", let: "させる", need: "必要（ひつよう）",
  // 名詞
  people: "人々（ひとびと）", time: "時間（じかん）", day: "日（ひ）", man: "男（おとこ）",
  woman: "女（おんな）", child: "子供（こども）", friend: "友達（ともだち）", way: "方法（ほうほう）",
  problem: "問題（もんだい）", world: "世界（せかい）", house: "家（いえ）", book: "本（ほん）",
  money: "お金（おかね）", name: "名前（なまえ）", job: "仕事（しごと）", face: "顔（かお）",
  student: "学生（がくせい）", form: "形（かたち）", head: "頭（あたま）", person: "人（ひと）",
  line: "線（せん）", side: "側（がわ）", interest: "興味（きょうみ）", kind: "種類（しゅるい）",
  sort: "種類（しゅるい）", case: "場合（ばあい）", order: "注文（ちゅうもん）", issue: "課題（かだい）",
  market: "市場（しじょう）", program: "プログラム", relationship: "関係（かんけい）",
  amount: "量（りょう）", matter: "事柄（ことがら）", mind: "心（こころ）", base: "基礎（きそ）",
  past: "過去（かこ）", power: "力（ちから）", force: "強制（きょうせい）", test: "試験（しけん）",
  deal: "取引（とりひき）", water: "水（みず）", view: "景色（けしき）", party: "パーティー",
  product: "製品（せいひん）", rate: "割合（わりあい）", care: "世話（せわ）", effect: "効果（こうか）",
  cause: "原因（げんいん）", letter: "手紙（てがみ）", class: "クラス", minute: "分（ふん）",
  food: "食べ物（たべもの）", color: "色（いろ）", note: "メモ", type: "型（かた）", eye: "目（め）",
  game: "ゲーム", team: "チーム", sense: "感覚（かんかく）", human: "人間（にんげん）",
  staff: "スタッフ", activity: "活動（かつどう）", account: "アカウント", event: "イベント",
  table: "テーブル", detail: "詳細（しょうさい）", phone: "電話（でんわ）", model: "モデル",
  front: "前（まえ）", door: "ドア", site: "サイト", land: "土地（とち）", charge: "料金（りょうきん）",
  sign: "サイン", subject: "科目（かもく）", development: "開発（かいはつ）", town: "町（まち）",
  half: "半分（はんぶん）", answer: "答え（こたえ）", age: "年齢（ねんれい）", price: "価格（かかく）",
  parent: "親（おや）", mother: "母（はは）", father: "父（ちち）", girl: "女の子（おんなのこ）",
  teacher: "先生（せんせい）", shop: "店（みせ）", war: "戦争（せんそう）", situation: "状況（じょうきょう）",
  difference: "違い（ちがい）", role: "役割（やくわり）", story: "物語（ものがたり）",
  // 形容詞・副詞ほか
  good: "良い（よい）", well: "よく", long: "長い（ながい）", little: "小さい（ちいさい）",
  big: "大きい（おおきい）", small: "小さい（ちいさい）", large: "大きい（おおきい）",
  young: "若い（わかい）", bad: "悪い（わるい）", different: "違う（ちがう）", next: "次の（つぎの）",
  like: "好き（すき）", new: "新しい（あたらしい）", old: "古い（ふるい）", high: "高い（たかい）",
  right: "正しい（ただしい）", great: "素晴らしい（すばらしい）",
  first: "最初（さいしょ）", last: "最後（さいご）", own: "自分の（じぶんの）", such: "そのような",
  another: "別の（べつの）", same: "同じ（おなじ）", low: "低い（ひくい）", hard: "難しい（むずかしい）",
  easy: "簡単（かんたん）", free: "自由（じゆう）", full: "いっぱい", real: "本当の（ほんとうの）",
  true: "本当の（ほんとうの）", main: "主な（おもな）", major: "主要な（しゅような）",
  strong: "強い（つよい）", difficult: "難しい（むずかしい）", particular: "特定の（とくていの）",
  near: "近い（ちかい）", short: "短い（みじかい）", quite: "かなり", rather: "むしろ",
  really: "本当に（ほんとうに）", actually: "実際に（じっさいに）", always: "いつも",
  never: "決して（けっして）", often: "よく", sometimes: "時々（ときどき）", almost: "ほとんど",
  maybe: "たぶん", perhaps: "たぶん", probably: "たぶん", soon: "すぐに", already: "すでに",
  ever: "かつて", again: "再び（ふたたび）", together: "一緒に（いっしょに）", around: "周り（まわり）",
  behind: "後ろ（うしろ）", above: "上に（うえに）", forward: "前へ（まえへ）", finally: "ついに",
  else: "他に（ほかに）", lot: "たくさん", while: "間（あいだ）", during: "間（あいだ）",
  once: "一度（いちど）", ago: "前（まえ）", able: "できる", nothing: "何も（なにも）",
  anything: "何でも（なんでも）", everything: "全て（すべて）", remain: "残る（のこる）",
  among: "中で（なかで）", reach: "届く（とどく）", study: "勉強（べんきょう）",
  learn: "学ぶ（まなぶ）", present: "プレゼント", watch: "腕時計（うでどけい）",
  several: "いくつか", bit: "少し（すこし）", although: "けれど", because: "なぜなら",
  // 中級（rank 500-1200 相当）の補正
  nice: "素敵（すてき）", improve: "改善する（かいぜんする）", happy: "幸せ（しあわせ）",
  range: "範囲（はんい）", project: "事業（じぎょう）", opportunity: "機会（きかい）",
  accord: "一致（いっち）", wish: "願い（ねがい）", wear: "着る（きる）", measure: "測る（はかる）",
  certainly: "確かに（たしかに）", national: "国の（くにの）", security: "安全（あんぜん）",
  news: "ニュース", space: "空間（くうかん）", realize: "気づく（きづく）", usually: "普段（ふだん）",
  data: "データ", single: "一つの（ひとつの）", performance: "性能（せいのう）",
  mention: "言及する（げんきゅうする）", save: "保存する（ほぞんする）", total: "合計（ごうけい）",
  material: "材料（ざいりょう）", listen: "聴く（きく）", wrong: "間違い（まちがい）",
  check: "確認する（かくにんする）", complete: "完成する（かんせいする）", pick: "拾う（ひろう）",
  reduce: "減らす（へらす）", ground: "地面（じめん）", arrive: "到着する（とうちゃくする）",
  current: "現在の（げんざいの）", similar: "似ている（にている）", fight: "戦う（たたかう）",
  leader: "リーダー", fine: "元気（げんき）", street: "通り（とおり）", former: "以前の（いぜんの）",
  contact: "連絡（れんらく）", wife: "妻（つま）", prepare: "準備する（じゅんびする）",
  response: "返答（へんとう）", piece: "かけら", president: "大統領（だいとうりょう）",
  store: "店（みせ）", manage: "管理する（かんりする）", firm: "会社（かいしゃ）", fast: "速い（はやい）",
  surprise: "驚き（おどろき）", factor: "要因（よういん）", pretty: "かわいい",
  affect: "影響する（えいきょうする）", relate: "関連する（かんれんする）", financial: "金融（きんゆう）",
  miss: "逃す（のがす）", park: "公園（こうえん）", key: "鍵（かぎ）",
  wonder: "不思議に思う（ふしぎにおもう）", section: "部門（ぶもん）", press: "押す（おす）",
  whatever: "何でも（なんでも）", region: "地域（ちいき）", evening: "夕方（ゆうがた）",
  catch: "捕まえる（つかまえる）", thus: "こうして", skill: "スキル", character: "性格（せいかく）",
  bed: "ベッド", guy: "男の人（おとこのひと）", addition: "追加（ついか）", station: "駅（えき）",
  fail: "失敗する（しっぱいする）", comment: "コメント", alone: "一人（ひとり）", drug: "薬（くすり）",
  tomorrow: "明日（あした）", director: "監督（かんとく）", clearly: "はっきりと",
  department: "部門（ぶもん）", gain: "獲得する（かくとくする）", mark: "印（しるし）",
  achieve: "達成する（たっせいする）", prove: "証明する（しょうめいする）", floor: "床（ゆか）",
  stuff: "物（もの）", wide: "広い（ひろい）", anyone: "誰でも（だれでも）", military: "軍事（ぐんじ）",
  doctor: "医者（いしゃ）", sorry: "ごめんなさい", nation: "国民（こくみん）",
  nearly: "もう少しで（もうすこしで）", link: "つながり", despite: "にもかかわらず",
  introduce: "紹介する（しょうかいする）", advantage: "利点（りてん）", ready: "準備ができた（じゅんびができた）",
  marry: "結婚する（けっこんする）", seek: "求める（もとめる）", capital: "首都（しゅと）",
  popular: "人気（にんき）", red: "赤（あか）", access: "アクセス", treat: "扱う（あつかう）",
  identify: "特定する（とくていする）", brother: "兄弟（きょうだい）", score: "得点（とくてん）",
  organize: "整理する（せいりする）", trip: "旅（たび）", beyond: "を超えて（をこえて）",
  energy: "エネルギー", file: "ファイル", generally: "一般的に（いっぱんてきに）", seat: "席（せき）",
  task: "作業（さぎょう）", associate: "仲間（なかま）", cold: "寒い（さむい）", quarter: "四半期（しはんき）",
  assume: "仮定する（かていする）", propose: "提案する（ていあんする）", fly: "飛ぶ（とぶ）",
  pattern: "パターン", obviously: "明らかに（あきらかに）", bill: "請求書（せいきゅうしょ）",
  central: "中央の（ちゅうおうの）", anyway: "とにかく", dog: "犬（いぬ）", guess: "推測する（すいそくする）",
  basic: "基本的（きほんてき）", male: "男性（だんせい）", reflect: "反映する（はんえいする）",
  dark: "暗い（くらい）", imagine: "想像する（そうぞうする）", okay: "オーケー", post: "投稿（とうこう）",
  conclusion: "結論（けつろん）", everybody: "みんな", professional: "専門的（せんもんてき）",
  maintain: "維持する（いじする）", credit: "信用（しんよう）", discover: "発見する（はっけんする）",
  dead: "死んでいる（しんでいる）", extend: "延ばす（のばす）", variety: "多様（たよう）",
  track: "追跡（ついせき）", female: "女性（じょせい）", responsibility: "責任（せきにん）",
  original: "元の（もとの）", sister: "姉妹（しまい）", easily: "簡単に（かんたんに）",
  fix: "直す（なおす）", yeah: "うん", weight: "重さ（おもさ）", shape: "形（かたち）",
  communication: "コミュニケーション", judge: "判断する（はんだんする）", suddenly: "突然（とつぜん）",
  favorite: "お気に入り（おきにいり）", shoot: "撃つ（うつ）", announce: "発表する（はっぴょうする）",
  independent: "独立した（どくりつした）", recommend: "勧める（すすめる）", survey: "調査（ちょうさ）",
  stick: "棒（ぼう）", request: "依頼（いらい）", wind: "風（かぜ）", none: "一つも（ひとつも）",
  budget: "予算（よさん）", content: "内容（ないよう）", correct: "正解（せいかい）", beat: "叩く（たたく）",
  telephone: "電話（でんわ）", copy: "コピー", aware: "気づいている（きづいている）", advice: "アドバイス",
  ride: "乗る（のる）", remove: "取り除く（とりのぞく）", conduct: "実施する（じっしする）",
  title: "タイトル", executive: "幹部（かんぶ）", primary: "第一の（だいいちの）",
  connection: "接続（せつぞく）", inform: "知らせる（しらせる）", straight: "まっすぐ", trust: "信頼（しんらい）",
  wonderful: "素晴らしい（すばらしい）", flat: "平ら（たいら）", fair: "公平（こうへい）",
  additional: "追加の（ついかの）", hang: "吊るす（つるす）", pair: "ペア", cheap: "安い（やすい）",
  progress: "進歩（しんぽ）", truth: "真実（しんじつ）", nobody: "誰も（だれも）", examine: "調べる（しらべる）",
  reply: "返事（へんじ）", transfer: "移す（うつす）", slightly: "わずかに", intend: "意図する（いとする）",
  dinner: "夕食（ゆうしょく）", slow: "遅い（おそい）", regular: "定期的（ていきてき）",
  federal: "連邦の（れんぽうの）", reveal: "明らかにする（あきらかにする）", percentage: "パーセンテージ",
  status: "地位（ちい）", crime: "犯罪（はんざい）", decline: "減少（げんしょう）", decade: "10年（じゅうねん）",
  consumer: "消費者（しょうひしゃ）", favor: "好意（こうい）", dry: "乾いた（かわいた）", partner: "パートナー",
  institution: "機関（きかん）", eventually: "結局（けっきょく）", importance: "重要性（じゅうようせい）",
  distance: "距離（きょり）", guide: "案内（あんない）", mistake: "誤り（あやまり）",
  satisfy: "満足させる（まんぞくさせる）", cool: "涼しい（すずしい）", expert: "専門家（せんもんか）",
  surface: "表面（ひょうめん）", library: "図書館（としょかん）", excellent: "優秀（ゆうしゅう）",
  edge: "端（はし）", lift: "持ち上げる（もちあげる）", advertise: "宣伝する（せんでんする）",
  select: "選択する（せんたくする）", annual: "毎年の（まいとしの）", fully: "完全に（かんぜんに）",
  presence: "存在（そんざい）", crowd: "群衆（ぐんしゅう）", gas: "ガス", shift: "シフト",
  category: "分類（ぶんるい）", secretary: "秘書（ひしょ）", quick: "素早い（すばやい）",
  cook: "料理する（りょうりする）", cry: "泣く（なく）",
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
