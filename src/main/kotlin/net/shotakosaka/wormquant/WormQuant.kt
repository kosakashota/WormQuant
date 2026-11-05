package net.shotakosaka.wormquant

import ij.IJ
import ij.plugin.PlugIn
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WormQuant : PlugIn {

    override fun run(arg: String?) {
        val settings = SettingsDialogBuilder().show() ?: return
        val directory = File(settings.directoryPath)

        val parser = FilenameParser(
            useRegex = settings.useRegex,
            regexPattern = settings.regexPattern,
            delimiter = settings.delimiter,
            mappings = settings.mappings
        )
        val quantifier = ImageQuantifier(
            parser = parser,
            extractDateFromXml = settings.extractDateFromXml
        )

        val imageFiles = quantifier.findTiffImages(directory, settings.isRecursive)
        if (imageFiles.isEmpty()) {
            IJ.showMessage("WormQuant", "No TIFF images found in the selected location.")
            return
        }

        // 出力CSVファイル名にディレクトリ名と日時タイムスタンプを含める
        val dirName = directory.name.ifBlank { "results" }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.UK).format(Date())
        val outputFile = File(directory, "${dirName}_quantification_results_$timestamp.csv")
        val (matchedCount, rt) = quantifier.process(imageFiles, outputFile)

        rt.show("Quantification Results")
        IJ.showMessage(
            "Completed",
            "Processed $matchedCount of ${imageFiles.size} image(s).\nResults saved to:\n${outputFile.absolutePath}"
        )
    }
}