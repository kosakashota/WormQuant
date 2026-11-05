package net.shotakosaka.wormquant

data class ParameterMapping(
    val name: String,
    val rangeText: String
) {
    /**
     * インデックス範囲（例: "4", "1-3", "2..4"）を表すか判定し、0始まりのIntRangeを返す（定数の場合はnull）
     */
    fun toZeroBasedRange(): IntRange? {
        val trimmed = rangeText.trim()
        if (trimmed.isEmpty()) return null

        // 引用符囲み（"4"など）や'='始まりの文字列は定数として扱う
        if ((trimmed.startsWith('"') && trimmed.endsWith('"') && trimmed.length >= 2) || trimmed.startsWith('=')) {
            return null
        }

        val parts = trimmed.split("-", "..")
        return try {
            if (parts.size == 1) {
                val idx = parts[0].trim().toInt() - 1
                if (idx < 0) null else idx..idx
            } else if (parts.size == 2) {
                val start = parts[0].trim().toInt() - 1
                val end = parts[1].trim().toInt() - 1
                if (start < 0 || end < start) null else start..end
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * トークン範囲ではなく定数値の場合にtrueを返す
     */
    fun isConstant(): Boolean = toZeroBasedRange() == null

    /**
     * 定数値を抽出（先頭の'='や前後の引用符を除去）
     */
    fun getConstantValue(): String {
        val trimmed = rangeText.trim()
        return if (trimmed.startsWith('"') && trimmed.endsWith('"') && trimmed.length >= 2) {
            trimmed.substring(1, trimmed.length - 1)
        } else if (trimmed.startsWith('=')) {
            trimmed.removePrefix("=")
        } else {
            trimmed
        }
    }
}
