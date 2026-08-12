package com.example.studyenglish.data

/**
 * オリジナル単語帳用のCSVパーサー。
 * 想定フォーマット: english,japanese,phonetic,example （phonetic/exampleは省略可）
 * 1行目が "english,japanese..." のヘッダーの場合は自動でスキップする。
 * ダブルクォート囲み・""エスケープに対応した簡易パーサー（フィールド内改行は非対応）。
 */
object CsvParser {

    data class WordRow(
        val english: String,
        val japanese: String,
        val phonetic: String? = null,
        val example: String? = null,
    )

    data class ParseResult(val rows: List<WordRow>, val skipped: Int)

    fun parseWords(lines: List<String>): ParseResult {
        val rows = mutableListOf<WordRow>()
        var skipped = 0
        var isFirstLine = true
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val fields = splitCsvLine(line)
            if (isFirstLine) {
                isFirstLine = false
                val f0 = fields.getOrNull(0)?.trim()?.lowercase()
                val f1 = fields.getOrNull(1)?.trim()?.lowercase()
                if (f0 == "english" && f1 == "japanese") continue // ヘッダー行はスキップ
            }
            val english = fields.getOrNull(0)?.trim().orEmpty()
            val japanese = fields.getOrNull(1)?.trim().orEmpty()
            if (english.isEmpty() || japanese.isEmpty()) {
                skipped++
                continue
            }
            val phonetic = fields.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() }
            val example = fields.getOrNull(3)?.trim()?.takeIf { it.isNotEmpty() }
            rows.add(WordRow(english = english, japanese = japanese, phonetic = phonetic, example = example))
        }
        return ParseResult(rows, skipped)
    }

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    sb.append('"')
                    i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    result.add(sb.toString())
                    sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }
}
