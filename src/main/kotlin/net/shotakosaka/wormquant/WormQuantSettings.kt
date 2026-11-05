package net.shotakosaka.wormquant

import ij.Prefs

data class WormQuantSettings(
    val directoryPath: String = DEFAULT_DIRECTORY,
    val useRegex: Boolean = DEFAULT_USE_REGEX,
    val regexPattern: String = FilenameParser.DEFAULT_REGEX,
    val delimiter: String = DEFAULT_DELIMITER,
    val mappings: List<ParameterMapping> = defaultMappings(),
    val extractDateFromXml: Boolean = DEFAULT_EXTRACT_DATE_FROM_XML,
    val isRecursive: Boolean = DEFAULT_RECURSIVE
) {
    companion object {
        const val DEFAULT_DIRECTORY = ""
        const val DEFAULT_USE_REGEX = false
        const val DEFAULT_DELIMITER = "_"
        const val DEFAULT_EXTRACT_DATE_FROM_XML = true
        const val DEFAULT_RECURSIVE = false

        private const val PREF_KEY_DIRECTORY = "wormquant.directoryPath"
        private const val PREF_KEY_USE_REGEX = "wormquant.useRegex"
        private const val PREF_KEY_REGEX = "wormquant.regexPattern"
        private const val PREF_KEY_DELIMITER = "wormquant.delimiter"
        private const val PREF_KEY_MAPPINGS = "wormquant.mappings"
        private const val PREF_KEY_EXTRACT_DATE = "wormquant.extractDateFromXml"
        private const val PREF_KEY_RECURSIVE = "wormquant.isRecursive"

        fun defaultMappings(): List<ParameterMapping> = listOf(
            ParameterMapping("Genotype", "1-3"), // TODO: これどうする？
            ParameterMapping("Auxin", "4"),
            ParameterMapping("RNAi", "5"),
            ParameterMapping("Date", "6"),
            ParameterMapping("Group", "7"),
            ParameterMapping("Exposure", "8")
        )

        fun load(): WormQuantSettings {
            val directory = Prefs.get(PREF_KEY_DIRECTORY, DEFAULT_DIRECTORY) ?: DEFAULT_DIRECTORY
            val useRegex = Prefs.get(PREF_KEY_USE_REGEX, DEFAULT_USE_REGEX)
            val regex = Prefs.get(PREF_KEY_REGEX, FilenameParser.DEFAULT_REGEX) ?: FilenameParser.DEFAULT_REGEX
            val delimiter = Prefs.get(PREF_KEY_DELIMITER, DEFAULT_DELIMITER) ?: DEFAULT_DELIMITER
            val mappingsStr = Prefs.get(PREF_KEY_MAPPINGS, null as String?)
            val mappings = if (mappingsStr.isNullOrBlank()) {
                defaultMappings()
            } else {
                parseMappingsString(mappingsStr)
            }
            val extractDate = Prefs.get(PREF_KEY_EXTRACT_DATE, DEFAULT_EXTRACT_DATE_FROM_XML)
            val recursive = Prefs.get(PREF_KEY_RECURSIVE, DEFAULT_RECURSIVE)
            return WormQuantSettings(
                directoryPath = directory,
                useRegex = useRegex,
                regexPattern = regex,
                delimiter = delimiter,
                mappings = mappings,
                extractDateFromXml = extractDate,
                isRecursive = recursive
            )
        }

        private fun parseMappingsString(str: String): List<ParameterMapping> {
            return str.split(";").mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size >= 2) {
                    val name = parts[0].trim()
                    val value = parts.drop(1).joinToString(":").trim()
                    ParameterMapping(name, value)
                } else null
            }
        }
    }

    fun save() {
        Prefs.set(PREF_KEY_DIRECTORY, directoryPath)
        Prefs.set(PREF_KEY_USE_REGEX, useRegex)
        Prefs.set(PREF_KEY_REGEX, regexPattern)
        Prefs.set(PREF_KEY_DELIMITER, delimiter)
        Prefs.set(PREF_KEY_MAPPINGS, serializeMappings(mappings))
        Prefs.set(PREF_KEY_EXTRACT_DATE, extractDateFromXml)
        Prefs.set(PREF_KEY_RECURSIVE, isRecursive)
    }

    private fun serializeMappings(list: List<ParameterMapping>): String {
        return list.joinToString(";") { "${it.name}:${it.rangeText}" }
    }
}
