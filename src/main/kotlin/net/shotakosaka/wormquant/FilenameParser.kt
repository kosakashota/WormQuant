package net.shotakosaka.wormquant

class FilenameParser(
    val useRegex: Boolean = false,
    val regexPattern: String = DEFAULT_REGEX,
    val delimiter: String = "_",
    val mappings: List<ParameterMapping> = emptyList()
) {

    companion object {
        const val DEFAULT_REGEX = "" // TODO: デフォルトどうする
    }

    private val compiledRegex: Regex? by lazy {
        if (useRegex && regexPattern.isNotBlank()) {
            try { Regex(regexPattern) } catch (e: Exception) { null }
        } else null
    }

    fun parse(filename: String): Map<String, String>? {
        return if (useRegex) {
            parseWithRegex(filename)
        } else {
            parseWithDelimiter(filename)
        }
    }

    private fun parseWithRegex(filename: String): Map<String, String>? {
        val regex = compiledRegex ?: return null
        val match = regex.find(filename) ?: return null

        val result = mutableMapOf<String, String>()
        val groupNames = extractNamedGroups(regexPattern)
        for (name in groupNames) {
            result[name] = match.groups[name]?.value ?: ""
        }
        return result
    }

    private fun parseWithDelimiter(filename: String): Map<String, String>? {
        val tokens = splitTokens(filename)
        val result = mutableMapOf<String, String>()

        for (mapping in mappings) {
            if (mapping.isConstant()) {
                result[mapping.name] = mapping.getConstantValue()

            } else {
                val range = mapping.toZeroBasedRange() ?: return null

                if (range.first < 0 || range.last >= tokens.size || range.first > range.last) {
                    return null
                }

                result[mapping.name] = (range.first..range.last).joinToString(delimiter) { tokens[it] }
            }
        }

        return result
    }

    fun extractNamedGroups(pattern: String): List<String> {
        val groupRegex = Regex("""\(\?<([a-zA-Z][a-zA-Z0-9]*)>""")

        return groupRegex.findAll(pattern).map { it.groupValues[1] }.toList()
    }

    fun extractRawValue(tokens: List<String>, mapping: ParameterMapping): String? {
        if (mapping.isConstant()) {
            return mapping.getConstantValue()
        }

        val range = mapping.toZeroBasedRange() ?: return null
        if (range.first < 0 || range.last >= tokens.size || range.first > range.last) {
            return null
        }

        return (range.first..range.last).joinToString(delimiter) { tokens[it] }
    }

    fun splitTokens(filename: String): List<String> {
        val baseName = filename.substringBeforeLast('.')

        if (delimiter.isEmpty())
            return listOf(baseName)
        else
            return baseName.split(delimiter)
    }
}
