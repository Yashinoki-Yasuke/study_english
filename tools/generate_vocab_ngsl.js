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
  // 高校・一般（rank 1200-2000 相当）の補正
  senior: "年上（としうえ）", photo: "写真（しゃしん）", concept: "概念（がいねん）",
  football: "サッカー", neighbor: "隣人（りんじん）", technique: "手法（しゅほう）",
  consequence: "結果（けっか）", circumstance: "事情（じじょう）", mass: "大量（たいりょう）",
  funny: "面白い（おもしろい）", contribute: "貢献する（こうけんする）", speaker: "スピーカー",
  combine: "組み合わせる（くみあわせる）", ticket: "切符（きっぷ）", meal: "食事（しょくじ）",
  colleague: "同僚（どうりょう）", extremely: "非常に（ひじょうに）", plane: "飛行機（ひこうき）",
  commercial: "商業（しょうぎょう）", lady: "ご婦人（ごふじん）", duty: "義務（ぎむ）",
  arrange: "手配する（てはいする）", scheme: "仕組み（しくみ）", unfortunately: "残念ながら（ざんねんながら）",
  appreciate: "感謝する（かんしゃする）", apparently: "どうやら", initial: "初めの（はじめの）",
  pleasure: "喜び（よろこび）", suggestion: "提案（ていあん）", currently: "現在（げんざい）",
  employ: "雇う（やとう）", path: "経路（けいろ）", settle: "落ち着く（おちつく）", aid: "援助（えんじょ）",
  nurse: "看護師（かんごし）", divide: "割る（わる）", investigation: "捜査（そうさ）",
  expand: "拡大する（かくだいする）", jump: "ジャンプ", host: "ホスト", broad: "幅広い（はばひろい）",
  tire: "疲れる（つかれる）", spirit: "精神（せいしん）", actual: "実際の（じっさいの）",
  hardly: "ほとんど〜ない", award: "賞（しょう）", strange: "変な（へんな）", possibly: "もしかすると",
  revenue: "収益（しゅうえき）", enable: "可能にする（かのうにする）", active: "活発（かっぱつ）",
  conclude: "結論づける（けつろんづける）", convince: "納得させる（なっとくさせる）", blow: "吹く（ふく）",
  volume: "音量（おんりょう）", opposite: "正反対（せいはんたい）", sum: "総額（そうがく）",
  monitor: "モニター", egg: "卵（たまご）", shock: "衝撃（しょうげき）", comfortable: "快適（かいてき）",
  carefully: "注意深く（ちゅういぶかく）", recall: "思い出す（おもいだす）", theater: "劇場（げきじょう）",
  totally: "すっかり", dear: "親愛なる（しんあいなる）", oppose: "反対する（はんたいする）",
  taste: "味（あじ）", dangerous: "危険な（きけんな）", sight: "視界（しかい）",
  generate: "生成する（せいせいする）", gift: "贈り物（おくりもの）", deny: "否定する（ひていする）",
  quote: "引用する（いんようする）", minister: "大臣（だいじん）", manner: "やり方（やりかた）",
  square: "四角（しかく）", familiar: "よく知っている（よくしっている）", ignore: "無視する（むしする）",
  destroy: "破壊する（はかいする）", affair: "出来事（できごと）", civil: "市民の（しみんの）",
  locate: "位置する（いちする）", citizen: "市民（しみん）", domestic: "国内（こくない）",
  load: "荷物（にもつ）", troop: "軍隊（ぐんたい）", acquire: "習得する（しゅうとくする）",
  fairly: "まあまあ", wood: "木材（もくざい）", participate: "参加する（さんかする）", tough: "タフ",
  representative: "代表者（だいひょうしゃ）", capacity: "容量（ようりょう）", border: "国境（こっきょう）",
  assessment: "評価（ひょうか）", ad: "広告（こうこく）", hall: "ホール", proper: "きちんとした",
  relax: "リラックスする", tourist: "観光客（かんこうきゃく）", confidence: "自信（じしん）",
  perspective: "視点（してん）", register: "登録する（とうろくする）", asset: "財産（ざいさん）",
  leadership: "リーダーシップ", commitment: "誓約（せいやく）", bright: "明るい（あかるい）",
  slowly: "ゆっくり", bond: "絆（きずな）", hire: "採用する（さいようする）", internal: "内部の（ないぶの）",
  secure: "確保する（かくほする）", label: "ラベル", root: "根（ね）", channel: "チャンネル",
  investigate: "捜査する（そうさする）", friendly: "友好的（ゆうこうてき）", provision: "規定（きてい）",
  concentrate: "集中する（しゅうちゅうする）", plenty: "豊富（ほうふ）", entirely: "全く（まったく）",
  strongly: "強く（つよく）", brand: "ブランド", moral: "道徳的（どうとくてき）",
  combination: "組み合わせ（くみあわせ）", master: "マスター", definitely: "間違いなく（まちがいなく）",
  grade: "成績（せいせき）", nevertheless: "それでも", predict: "予測する（よそくする）",
  previously: "以前に（いぜんに）", wed: "結婚する（けっこんする）", guarantee: "保証（ほしょう）",
  till: "まで", odd: "奇妙（きみょう）", loan: "ローン", narrow: "狭い（せまい）",
  succeed: "成功する（せいこうする）", identity: "正体（しょうたい）", permit: "許可する（きょかする）",
  wild: "野生の（やせいの）", commission: "手数料（てすうりょう）", unique: "独特な（どくとくな）",
  instrument: "楽器（がっき）", investor: "投資家（とうしか）", practical: "実用的（じつようてき）",
  lovely: "愛らしい（あいらしい）", lock: "ロック", sexual: "性的な（せいてきな）",
  increasingly: "ますます", ourselves: "私たち自身（わたしたちじしん）", cast: "投げる（なげる）",
  journey: "旅（たび）", outcome: "成果（せいか）", blame: "責める（せめる）", arise: "生じる（しょうじる）",
  recover: "回復する（かいふくする）", dad: "お父さん（おとうさん）", stretch: "伸ばす（のばす）",
  declare: "宣言する（せんげんする）", retire: "引退する（いんたいする）", tiny: "とても小さい（とてもちいさい）",
  careful: "注意深い（ちゅういぶかい）", suitable: "適した（てきした）", native: "出身の（しゅっしんの）",
  analyze: "分析する（ぶんせきする）", terrible: "ひどい", ordinary: "普通の（ふつうの）",
  selection: "選抜（せんばつ）", anywhere: "どこでも", vision: "視野（しや）", personality: "個性（こせい）",
  fat: "脂肪（しぼう）", entry: "入場（にゅうじょう）", fellow: "仲間（なかま）", chemical: "化学（かがく）",
  capture: "捕らえる（とらえる）", tip: "チップ", discount: "割引（わりびき）", peak: "ピーク",
  chairman: "会長（かいちょう）", proportion: "比率（ひりつ）", disappear: "消える（きえる）",
  shout: "叫ぶ（さけぶ）", constant: "一定の（いっていの）", instruction: "指示（しじ）",
  folk: "民族（みんぞく）", surely: "きっと", guard: "警備（けいび）", cat: "猫（ねこ）",
  joint: "共同の（きょうどうの）", compete: "競う（きそう）", faith: "信仰（しんこう）",
  reduction: "削減（さくげん）", reserve: "予約（よやく）", complaint: "苦情（くじょう）",
  bore: "退屈させる（たいくつさせる）", somehow: "どうにかして", tone: "口調（くちょう）",
  phase: "局面（きょくめん）", rush: "急ぐ（いそぐ）", reject: "拒否する（きょひする）", ban: "禁止（きんし）",
  sick: "病気の（びょうきの）", sky: "空（そら）", column: "コラム", impose: "課す（かす）",
  criminal: "犯罪者（はんざいしゃ）", besides: "その上（そのうえ）", ancient: "古代の（こだいの）",
  coast: "海岸（かいがん）", closely: "密接に（みっせつに）", multiple: "複数の（ふくすうの）",
  yield: "収穫（しゅうかく）", via: "経由で（けいゆで）", county: "郡（ぐん）", unlike: "と違って（とちがって）",
  mobile: "モバイル", implement: "実行する（じっこうする）", everywhere: "いたるところ",
  acknowledge: "承認する（しょうにんする）", reward: "報酬（ほうしゅう）", academic: "学問の（がくもんの）",
  voter: "有権者（ゆうけんしゃ）", meanwhile: "その間に（そのあいだに）", furthermore: "さらに",
  accuse: "告発する（こくはつする）", wage: "賃金（ちんぎん）", construct: "組み立てる（くみたてる）",
  remark: "感想（かんそう）", rare: "珍しい（めずらしい）", gap: "隙間（すきま）", estate: "不動産（ふどうさん）",
  expose: "さらす", alive: "生きている（いきている）", shut: "閉じる（とじる）",
  enormous: "莫大な（ばくだいな）", sweet: "甘い（あまい）", permanent: "永久の（えいきゅうの）",
  pursue: "追求する（ついきゅうする）", tall: "背が高い（せがたかい）", enemy: "敵（てき）",
  appoint: "任命する（にんめいする）", milk: "牛乳（ぎゅうにゅう）", phrase: "フレーズ",
  pilot: "パイロット", merely: "ただ単に（ただたんに）", communicate: "連絡する（れんらくする）",
  injury: "けが", immediate: "即座の（そくざの）", incident: "事件（じけん）", childhood: "子供時代（こどもじだい）",
  draft: "下書き（したがき）", angry: "怒った（おこった）", seed: "種（たね）", salary: "給料（きゅうりょう）",
  imply: "ほのめかす", temporary: "一時的な（いちじてきな）", liberal: "自由主義の（じゆうしゅぎの）",
  competitive: "競争的（きょうそうてき）", truly: "心から（こころから）", hi: "やあ", disk: "ディスク",
  panel: "パネル", prime: "最高の（さいこうの）", appointment: "予約（よやく）",
  emphasize: "強調する（きょうちょうする）", bother: "邪魔する（じゃまする）", initiative: "主導権（しゅどうけん）",
  sharp: "鋭い（するどい）", gray: "灰色（はいいろ）", discipline: "規律（きりつ）",
  disappoint: "がっかりさせる", boss: "上司（じょうし）", assumption: "想定（そうてい）",
  extreme: "極端な（きょくたんな）", crash: "衝突（しょうとつ）", king: "王（おう）",
  capable: "有能な（ゆうのうな）", defeat: "敗北（はいぼく）", proud: "誇りに思う（ほこりにおもう）",
  distinguish: "区別する（くべつする）", nearby: "近くの（ちかくの）", valuable: "貴重な（きちょうな）",
  personally: "個人的に（こじんてきに）", approximately: "およそ", accommodation: "宿泊施設（しゅくはくしせつ）",
  highlight: "ハイライト", chip: "チップ", encounter: "出会う（であう）", breathe: "呼吸する（こきゅうする）",
  urban: "都市の（としの）", southern: "南の（みなみの）", output: "出力（しゅつりょく）", beauty: "美（び）",
  massive: "巨大な（きょだいな）", install: "設置する（せっちする）", calculate: "計算する（けいさんする）",
  mouse: "ネズミ", upper: "上の（うえの）", occupy: "占める（しめる）", outline: "概要（がいよう）",
  sufficient: "十分な（じゅうぶんな）", luck: "運（うん）", preserve: "保つ（たもつ）",
  split: "分割する（ぶんかつする）", swing: "揺れる（ゆれる）", illness: "病気（びょうき）", sudden: "急な（きゅうな）",
  consistent: "一貫した（いっかんした）", originally: "もともと", comfort: "慰め（なぐさめ）",
  severe: "厳しい（きびしい）", prospect: "見込み（みこみ）", plot: "筋書き（すじがき）",
  criterion: "基準（きじゅん）", integrate: "統合する（とうごうする）", convention: "慣習（かんしゅう）",
  retain: "保持する（ほじする）", sequence: "順序（じゅんじょ）", rural: "田舎の（いなかの）",
  rapidly: "急速に（きゅうそくに）", delight: "大喜び（おおよろこび）", lean: "傾く（かたむく）",
  grateful: "感謝している（かんしゃしている）", derive: "由来する（ゆらいする）",
  crucial: "極めて重要な（きわめてじゅうような）", wheel: "車輪（しゃりん）", minority: "少数派（しょうすうは）",
  origin: "起源（きげん）", landscape: "風景（ふうけい）", toy: "おもちゃ", fault: "欠点（けってん）",
  exhibit: "展示する（てんじする）", minor: "些細な（ささいな）", hunt: "狩り（かり）", storm: "嵐（あらし）",
  negotiate: "交渉する（こうしょうする）", emergency: "緊急（きんきゅう）", abandon: "見捨てる（みすてる）",
  // 社会人・発展（rank 2000-2800 相当）の補正
  peer: "同輩（どうはい）", deeply: "深く（ふかく）", smart: "賢い（かしこい）", layer: "層（そう）",
  upset: "動揺した（どうようした）", representation: "表現（ひょうげん）", dispute: "論争（ろんそう）",
  agenda: "議題（ぎだい）", emphasis: "重点（じゅうてん）", silver: "銀色（ぎんいろ）",
  entertainment: "娯楽（ごらく）", undertake: "引き受ける（ひきうける）", gay: "同性愛の（どうせいあいの）",
  slight: "わずかな", framework: "枠組み（わくぐみ）", restrict: "制限する（せいげんする）",
  equivalent: "同等（どうとう）", solid: "固い（かたい）", governor: "知事（ちじ）", uniform: "制服（せいふく）",
  port: "港（みなと）", pitch: "ピッチ", arrival: "到着（とうちゃく）", contemporary: "現代の（げんだいの）",
  gate: "門（もん）", ease: "気楽（きらく）", assure: "断言する（だんげんする）", profile: "プロフィール",
  mood: "気分（きぶん）", episode: "エピソード", crack: "ひび", numerous: "多数の（たすうの）",
  era: "時代（じだい）", coverage: "報道（ほうどう）", nervous: "緊張した（きんちょうした）",
  isolate: "孤立させる（こりつさせる）", eliminate: "排除する（はいじょする）", tight: "きつい",
  secondary: "二次的な（にじてきな）", recruit: "募集する（ぼしゅうする）", string: "糸（いと）",
  persuade: "説得する（せっとくする）", inspire: "奮い立たせる（ふるいたたせる）", grand: "壮大な（そうだいな）",
  hence: "それゆえ", crew: "乗組員（のりくみいん）", false: "偽の（にせの）", assist: "手伝う（てつだう）",
  formula: "公式（こうしき）", perceive: "知覚する（ちかくする）", routine: "日課（にっか）",
  stare: "見つめる（みつめる）", convert: "変換する（へんかんする）", meter: "メートル", truck: "トラック",
  beside: "のそばに", sail: "航海する（こうかいする）", disaster: "災害（さいがい）", pace: "ペース",
  heavily: "激しく（はげしく）", devote: "捧げる（ささげる）", justify: "正当化する（せいとうかする）",
  vital: "不可欠な（ふかけつな）", fascinate: "魅了する（みりょうする）", external: "外部の（がいぶの）",
  spare: "予備の（よびの）", depression: "憂うつ（ゆううつ）", mom: "お母さん（おかあさん）",
  distinction: "相違（そうい）", incorporate: "組み込む（くみこむ）", pour: "注ぐ（そそぐ）",
  sweep: "掃除する（そうじする）", obligation: "責務（せきむ）", sir: "様（さま）",
  evaluate: "評価する（ひょうかする）", perception: "認識（にんしき）", naturally: "自然に（しぜんに）",
  stream: "流れ（ながれ）", muscle: "筋肉（きんにく）", boundary: "境界線（きょうかいせん）",
  scream: "悲鳴（ひめい）", withdraw: "引き出す（ひきだす）", symbol: "象徴（しょうちょう）",
  apartment: "アパート", platform: "プラットフォーム", strain: "重圧（じゅうあつ）", trail: "小道（こみち）",
  loose: "ゆるい", wealth: "富（とみ）", tank: "タンク", tune: "曲（きょく）", invitation: "招待（しょうたい）",
  frighten: "怖がらせる（こわがらせる）", extraordinary: "並外れた（なみはずれた）", reverse: "逆（ぎゃく）",
  mode: "モード", awful: "恐ろしい（おそろしい）", pose: "ポーズ", nowadays: "近頃（ちかごろ）",
  agricultural: "農業の（のうぎょうの）", competitor: "競争相手（きょうそうあいて）", alcohol: "アルコール",
  van: "バン", confident: "自信のある（じしんのある）", overcome: "克服する（こくふくする）", web: "ウェブ",
  substance: "物質（ぶっしつ）", interpret: "通訳する（つうやくする）", inner: "内側の（うちがわの）",
  harm: "傷つける（きずつける）", strip: "剥ぎ取る（はぎとる）", radical: "過激な（かげきな）", loud: "うるさい",
  dirty: "汚い（きたない）", statistic: "統計（とうけい）", iron: "アイロン", broadcast: "放送する（ほうそうする）",
  membership: "会員資格（かいいんしかく）", blind: "盲目の（もうもくの）", bloody: "血だらけの（ちだらけの）",
  ally: "同盟国（どうめいこく）", mature: "成熟した（せいじゅくした）", briefly: "手短に（てみじかに）",
  sustain: "支える（ささえる）", flood: "洪水（こうずい）", crazy: "夢中の（むちゅうの）",
  parallel: "平行の（へいこうの）", gender: "性別（せいべつ）", sponsor: "スポンサー", boot: "ブーツ",
  dealer: "ディーラー", mate: "仲間（なかま）", bowl: "ボウル", frequency: "頻度（ひんど）",
  criticize: "非難する（ひなんする）", tap: "蛇口（じゃぐち）", entitle: "権利を与える（けんりをあたえる）",
  involvement: "関与（かんよ）", exposure: "露出（ろしゅつ）", conventional: "従来型（じゅうらいがた）",
  edit: "編集する（へんしゅうする）", formation: "形成（けいせい）", pleasant: "心地よい（ここちよい）",
  overseas: "海外へ（かいがいへ）", advocate: "擁護者（ようごしゃ）", establishment: "設立（せつりつ）",
  summary: "要約（ようやく）", rough: "荒い（あらい）", recovery: "回復（かいふく）", seal: "封印（ふういん）",
  exact: "厳密な（げんみつな）", spin: "回転する（かいてんする）", infant: "幼児（ようじ）",
  mount: "登る（のぼる）", anticipate: "予想する（よそうする）", dependent: "依存した（いぞんした）",
  chicken: "鶏肉（とりにく）", precisely: "正確に（せいかくに）", rival: "ライバル", offense: "違反（いはん）",
  teenager: "十代（じゅうだい）", admire: "尊敬する（そんけいする）", moderate: "適度な（てきどな）",
  universal: "普遍的な（ふへんてきな）", cigarette: "タバコ", consultant: "コンサルタント",
  historian: "歴史家（れきしか）", cousin: "いとこ", visual: "視覚的な（しかくてきな）", stupid: "愚かな（おろかな）",
  keen: "熱心な（ねっしんな）", ethnic: "民族の（みんぞくの）", twin: "双子（ふたご）",
  clinical: "臨床の（りんしょうの）", eastern: "東の（ひがしの）", forecast: "予報（よほう）",
  segment: "区切り（くぎり）", custom: "風習（ふうしゅう）", adapt: "適応する（てきおうする）", cap: "キャップ",
  prompt: "迅速な（じんそくな）", react: "反応する（はんのうする）", lecture: "講義（こうぎ）",
  compound: "化合物（かごうぶつ）", rescue: "救助（きゅうじょ）", mess: "めちゃくちゃ",
  preference: "好み（このみ）", incentive: "動機付け（どうきづけ）", rapid: "急速な（きゅうそくな）",
  regret: "後悔（こうかい）", dismiss: "解雇する（かいこする）", margin: "余白（よはく）",
  opponent: "対戦相手（たいせんあいて）", resist: "抵抗する（ていこうする）", capability: "能力（のうりょく）",
  stroke: "一撃（いちげき）", dare: "あえてする", barrier: "障壁（しょうへき）", ruin: "台無しにする（だいなしにする）",
  bury: "埋める（うめる）", counsel: "助言（じょげん）", frequent: "頻繁な（ひんぱんな）", counter: "カウンター",
  possess: "所有する（しょゆうする）", float: "浮く（うく）", mad: "気が狂った（きがくるった）",
  greatly: "大いに（おおいに）", visible: "目に見える（めにみえる）", electric: "電動の（でんどうの）",
  wealthy: "裕福な（ゆうふくな）", architecture: "建築（けんちく）", acceptable: "許容できる（きょようできる）",
  journal: "学術誌（がくじゅつし）", successfully: "うまく", burst: "破裂する（はれつする）",
  buyer: "買い手（かいて）", mortgage: "住宅ローン（じゅうたくローン）", promotion: "昇進（しょうしん）",
  champion: "優勝者（ゆうしょうしゃ）", dust: "ほこり", dedicate: "専念する（せんねんする）",
  roughly: "大まかに（おおまかに）", province: "地方（ちほう）", march: "行進（こうしん）",
  evaluation: "査定（さてい）", accomplish: "成し遂げる（なしとげる）", weakness: "弱点（じゃくてん）",
  glance: "ちらっと見る（ちらっとみる）", opera: "オペラ", contest: "コンテスト", govern: "統治する（とうちする）",
  embrace: "抱擁する（ほうようする）", praise: "褒める（ほめる）", silent: "無言の（むごんの）",
  celebration: "お祝い（おいわい）", deficit: "赤字（あかじ）", modify: "変更する（へんこうする）",
  flash: "閃光（せんこう）", profession: "職業（しょくぎょう）", entertain: "楽しませる（たのしませる）",
  assign: "割り当てる（わりあてる）", injure: "けがをさせる", remote: "遠隔（えんかく）", therapy: "治療（ちりょう）",
  orange: "オレンジ", twist: "ねじる", personnel: "職員（しょくいん）", imagination: "想像（そうぞう）",
  disagree: "意見が違う（いけんがちがう）", insight: "洞察（どうさつ）", forever: "永遠に（えいえんに）",
  exceed: "上回る（うわまわる）", pregnant: "妊娠した（にんしんした）", reliable: "頼れる（たよれる）",
  fortune: "幸運（こううん）", pile: "山積み（やまづみ）", pig: "豚（ぶた）", mixture: "混合（こんごう）",
  creature: "生き物（いきもの）", partnership: "提携（ていけい）", penalty: "罰則（ばっそく）",
  chamber: "会議所（かいぎしょ）", fancy: "派手な（はでな）", chat: "おしゃべり", clothing: "衣類（いるい）",
  sake: "ため", tail: "しっぽ", possession: "所持（しょじ）", curious: "好奇心が強い（こうきしんがつよい）",
  tale: "お話（おはなし）", maintenance: "整備（せいび）", consequently: "その結果（そのけっか）",
  pot: "鍋（なべ）", cow: "牛（うし）", constraint: "束縛（そくばく）", scope: "領域（りょういき）",
  pretend: "ふりをする", intense: "強烈な（きょうれつな）", resign: "辞任する（じにんする）", craft: "工芸（こうげい）",
  shell: "殻（から）", damn: "ちくしょう", indication: "兆し（きざし）", neglect: "怠る（おこたる）",
  compose: "構成する（こうせいする）", jail: "拘置所（こうちしょ）", shelter: "避難所（ひなんじょ）",
  carbon: "炭素（たんそ）", trigger: "引き金（ひきがね）", pipe: "パイプ", piano: "ピアノ",
  mystery: "謎（なぞ）", whisper: "ささやく", rear: "後部（こうぶ）", menu: "メニュー", species: "種（しゅ）",
  moon: "月（つき）", presumably: "恐らく（おそらく）", bless: "祝福する（しゅくふくする）",
  airline: "航空会社（こうくうがいしゃ）", cooperation: "協力（きょうりょく）", civilian: "民間人（みんかんじん）",
  composition: "作文（さくぶん）", coin: "コイン", scan: "スキャンする", bunch: "束（たば）",
  racial: "人種の（じんしゅの）", greet: "挨拶する（あいさつする）", sanction: "制裁（せいさい）",
  trick: "いたずら", paragraph: "段落（だんらく）", maker: "メーカー", narrative: "語り（かたり）",
  tissue: "ティッシュ", barely: "かろうじて", invent: "発明する（はつめいする）", tourism: "観光（かんこう）",
  stair: "階段（かいだん）", hesitate: "ためらう", shine: "輝く（かがやく）", motivation: "やる気（やるき）",
  firmly: "しっかりと", interior: "室内（しつない）", stomach: "お腹（おなか）", nowhere: "どこにも〜ない",
  servant: "使用人（しようにん）", liability: "負債（ふさい）", surprisingly: "驚くことに（おどろくことに）",
  extract: "抜き出す（ぬきだす）", bias: "偏見（へんけん）", continuous: "連続的な（れんぞくてきな）",
  golden: "金色の（きんいろの）", stamp: "切手（きって）", guideline: "指針（ししん）",
  biological: "生物学的な（せいぶつがくてきな）", weekly: "毎週の（まいしゅうの）", bite: "噛む（かむ）",
  anxious: "心配な（しんぱいな）", fence: "柵（さく）", quietly: "静かに（しずかに）",
  veteran: "ベテラン", reflection: "反射（はんしゃ）", determination: "決意（けつい）", altogether: "全部で（ぜんぶで）",
  fiction: "フィクション", cluster: "集まり（あつまり）", confusion: "困惑（こんわく）", raw: "生の（なまの）",
  revise: "改訂する（かいていする）", hook: "フック", liquid: "液体（えきたい）", panic: "パニック",
  rice: "米（こめ）", happiness: "幸福（こうふく）", genuine: "本物の（ほんものの）", vessel: "容器（ようき）",
  silly: "ばかげた", transportation: "交通機関（こうつうきかん）", harbor: "港湾（こうわん）", comedy: "コメディ",
  chase: "追いかける（おいかける）", storage: "保管（ほかん）", sheep: "羊（ひつじ）", lover: "恋人（こいびと）",
  portrait: "肖像画（しょうぞうが）", innocent: "無邪気な（むじゃきな）", reasonably: "まずまず",
  distant: "遠く離れた（とおくはなれた）", stranger: "見知らぬ人（みしらぬひと）", grain: "穀物（こくもつ）",
  summarize: "要約する（ようやくする）", leap: "飛び跳ねる（とびはねる）", snap: "スナップ",
  swear: "誓う（ちかう）", shore: "岸（きし）", monthly: "毎月の（まいつきの）", stir: "かき混ぜる（かきまぜる）",
  excitement: "興奮（こうふん）", slice: "薄切り（うすぎり）", wander: "さまよう", subsequently: "その後（そのご）",
  gentle: "穏やかな（おだやかな）", suspend: "停止する（ていしする）", functional: "機能的な（きのうてきな）",
  voluntary: "自発的な（じはつてきな）", pale: "青白い（あおじろい）", stain: "染み（しみ）",
  athlete: "運動選手（うんどうせんしゅ）", tongue: "舌（した）", fool: "ばか", unite: "団結する（だんけつする）",
  gently: "そっと", wipe: "拭く（ふく）", weird: "妙な（みょうな）", fade: "色あせる（いろあせる）",
  hypothesis: "仮説（かせつ）", royal: "王室の（おうしつの）", theoretical: "理論上の（りろんじょうの）",
  curtain: "カーテン", darkness: "暗闇（くらやみ）", listener: "聞き手（ききて）", module: "モジュール",
  cheek: "頬（ほお）", attachment: "愛着（あいちゃく）", grin: "にっこり笑う（にっこりわらう）",
  fortunate: "幸運な（こううんな）", alright: "問題ない（もんだいない）", hello: "こんにちは",
  hunger: "空腹（くうふく）", ashamed: "恥ずかしい（はずかしい）", found: "創設する（そうせつする）",
  thirst: "のどの渇き（のどのかわき）",
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
