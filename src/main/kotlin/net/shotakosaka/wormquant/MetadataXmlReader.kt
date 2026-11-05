package net.shotakosaka.wormquant

import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class MetadataXmlReader {

    companion object {
        private val START_TIME_REGEX = Regex("""<StartTime>(.*?)</StartTime>""", RegexOption.IGNORE_CASE)
        private val CH_SUFFIX_REGEX = Regex("""_ch\d+$""", RegexOption.IGNORE_CASE)

        private val INPUT_FORMAT_WITH_MILLIS = SimpleDateFormat("MM/dd/yyyy h:mm:ss a.SSS", Locale.US)
        private val INPUT_FORMAT_WITHOUT_MILLIS = SimpleDateFormat("MM/dd/yyyy h:mm:ss a", Locale.US)
        private val OUTPUT_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    /**
     * 画像ファイルに対応するLeicaメタデータXMLから撮影日を取得
     */
    fun extractAcquisitionDate(imageFile: File): String? {
        val xmlFile = findXmlFile(imageFile) ?: return null
        return try {
            val content = xmlFile.readText()
            val match = START_TIME_REGEX.find(content) ?: return null
            val rawTime = match.groupValues[1].trim()
            formatDate(rawTime)
        } catch (e: Exception) {
            null
        }
    }

    fun findXmlFile(imageFile: File): File? {
        val parent = imageFile.parentFile ?: return null
        val baseName = imageFile.nameWithoutExtension.replace(CH_SUFFIX_REGEX, "")

        val candidates = listOf(
            File(parent, "MetaData/${baseName}_Properties.xml"),
            File(parent, "MetaData/${baseName}.xml"),
            File(parent, "${baseName}_Properties.xml"),
            File(parent, "${baseName}.xml")
        )

        return candidates.firstOrNull { it.isFile && it.exists() }
    }

    fun formatDate(rawTime: String): String {
        return try {
            val date = try {
                INPUT_FORMAT_WITH_MILLIS.parse(rawTime)
            } catch (e: Exception) {
                INPUT_FORMAT_WITHOUT_MILLIS.parse(rawTime)
            } ?: return rawTime
            OUTPUT_FORMAT.format(date)
        } catch (e: Exception) {
            rawTime
        }
    }
}
