package net.shotakosaka.wormquant

import ij.IJ
import ij.measure.Measurements
import ij.measure.ResultsTable
import java.io.File
import java.util.Locale

class ImageQuantifier(
    private val parser: FilenameParser,
    private val extractDateFromXml: Boolean = true,
    private val xmlReader: MetadataXmlReader = MetadataXmlReader()
) {

    fun findTiffImages(directory: File, recursive: Boolean): List<File> {
        val isTiff = { f: File ->
            f.isFile && (f.name.endsWith(".tif", ignoreCase = true) || f.name.endsWith(".tiff", ignoreCase = true)) // TODO: TIFF以外の形式もサポートするか検討
        }
        val files = if (recursive) {
            directory.walkTopDown().filter(isTiff).toList()
        } else {
            directory.listFiles()?.filter(isTiff)?.toList() ?: emptyList()
        }
        return files.sortedBy { it.name }
    }

    fun process(
        imageFiles: List<File>,
        outputCsv: File
    ): Pair<Int, ResultsTable> {
        val rt = ResultsTable()
        var matchedCount = 0
        val total = imageFiles.size

        val baseParamNames = if (parser.useRegex) {
            parser.extractNamedGroups(parser.regexPattern)
        } else {
            parser.mappings.map { it.name }
        }

        val hasDateParam = baseParamNames.any { it.equals("Date", ignoreCase = true) }
        val addAcquisitionDateColumn = extractDateFromXml && !hasDateParam

        val allParamHeaders = if (addAcquisitionDateColumn) {
            listOf("Acquisition_Date") + baseParamNames
        } else {
            baseParamNames
        }

        val headers = allParamHeaders + listOf(
            "Filename", "Area", "Mean", "Min", "Max", "IntDen", "RawIntDen"
        )

        IJ.log("--- WormQuant: Starting quantification of $total image(s) ---")
        val startTime = System.currentTimeMillis()

        try {
            outputCsv.bufferedWriter().use { writer ->
                writer.write(headers.joinToString(",") + "\n")

                for ((index, file) in imageFiles.withIndex()) {
                    val currentNum = index + 1

                    // ImageJの進捗バーとステータスバーを更新
                    IJ.showProgress(currentNum, total)
                    IJ.showStatus("Analysing image $currentNum of $total: ${file.name}")

                    val parsedParams = parser.parse(file.name)?.toMutableMap()
                    if (parsedParams == null) {
                        IJ.log("[$currentNum/$total] Skipped (unmatched): ${file.name}")
                        continue
                    }

                    // 有効な場合はLeica MetaData XMLから撮影日時を取得
                    val xmlDate = if (extractDateFromXml) xmlReader.extractAcquisitionDate(file) else null
                    if (xmlDate != null) {
                        if (hasDateParam) {
                            val dateKey = baseParamNames.first { it.equals("Date", ignoreCase = true) }
                            parsedParams[dateKey] = xmlDate
                        } else if (addAcquisitionDateColumn) {
                            parsedParams["Acquisition_Date"] = xmlDate
                        }
                    }

                    val measurement = measureImage(file)
                    if (measurement == null) {
                        IJ.log("[$currentNum/$total] Failed to open image: ${file.name}")
                        continue
                    }

                    val row = allParamHeaders.map { parsedParams[it] ?: "" } + listOf(
                        file.name,
                        measurement.area,
                        measurement.mean,
                        measurement.min,
                        measurement.max,
                        measurement.intDen,
                        measurement.rawIntDen
                    )

                    writer.write(row.joinToString(",") + "\n")

                    rt.incrementCounter()
                    headers.zip(row).forEach { (h, v) ->
                        rt.addValue(h, v)
                    }

                    matchedCount++
                    IJ.log("[$currentNum/$total] Analysed: ${file.name} (Mean: ${measurement.mean}, Area: ${measurement.area})")
                }
            }
        } finally {
            IJ.showProgress(1.0)
            IJ.showStatus("")
        }

        val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
        IJ.log(String.format(Locale.UK, "--- WormQuant: Finished! Processed %d of %d image(s) in %.2f seconds ---", matchedCount, total, elapsed))

        return Pair(matchedCount, rt)
    }

    private fun measureImage(file: File): Measurement? {
        val imp = IJ.openImage(file.absolutePath) ?: return null

        return try {
            val flags = Measurements.AREA or Measurements.MEAN or Measurements.MIN_MAX
            val stats = imp.getStatistics(flags)
            val rawIntDen = stats.pixelCount.toDouble() * stats.mean
            val intDen = stats.area * stats.mean

            Measurement(
                area = String.format(Locale.UK, "%.2f", stats.area),
                mean = String.format(Locale.UK, "%.3f", stats.mean),
                min = stats.min.toLong().toString(),
                max = stats.max.toLong().toString(),
                intDen = intDen.toLong().toString(),
                rawIntDen = rawIntDen.toLong().toString()
            )
        } finally {
            imp.close()
        }
    }
}
