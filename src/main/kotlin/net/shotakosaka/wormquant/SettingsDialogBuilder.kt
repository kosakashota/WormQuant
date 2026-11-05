package net.shotakosaka.wormquant

import ij.IJ
import ij.io.DirectoryChooser
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.io.File
import javax.swing.AbstractAction
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.KeyStroke
import javax.swing.ListSelectionModel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableModel

class SettingsDialogBuilder(
    private val settings: WormQuantSettings = WormQuantSettings.load()
) {

    private val xmlReader = MetadataXmlReader()

    fun show(): WormQuantSettings? {
        if (GraphicsEnvironment.isHeadless()) {
            return settings
        }

        val parentFrame = IJ.getInstance()
        val dialog = JDialog(parentFrame, "WormQuant", true)
        dialog.layout = BorderLayout(10, 10)

        var resultSettings: WormQuantSettings? = null

        // 1. ディレクトリとサンプルファイル名
        val dirField = JTextField(settings.directoryPath, 35)
        val browseBtn = JButton("Browse...")

        val sampleFileField = JTextField(35)
        val initialSample = detectSampleFile(settings.directoryPath)
        sampleFileField.text = initialSample.ifEmpty { "" }

        // XML撮影日時の自動取得チェックボックスとプレビュー
        val autoDateBox = JCheckBox("Auto-detect date from MetaData XML", settings.extractDateFromXml)
        val detectedDateLabel = JLabel("Detected XML Date: None").apply {
            font = Font(Font.MONOSPACED, Font.PLAIN, 11)
            foreground = Color(80, 80, 80)
        }

        // 正規表現モード 消してもいいかも？
        val useRegexBox = JCheckBox("Use regular expression", settings.useRegex)
        val regexField = JTextField(settings.regexPattern, 40)
        val regexPreviewLabel = JLabel("Regex Preview: -").apply {
            font = Font(Font.MONOSPACED, Font.PLAIN, 11)
            foreground = Color(60, 60, 60)
        }

        // 区切り文字
        val delimiterField = JTextField(settings.delimiter, 4)
        val tokenCardsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 6))
        val tokenScrollPane = JScrollPane(tokenCardsPanel).apply {
            preferredSize = Dimension(680, 75)
            minimumSize = Dimension(680, 75)
            maximumSize = Dimension(Short.MAX_VALUE.toInt(), 75)
            border = BorderFactory.createTitledBorder("Tokens")
        }

        // パラメータmap table
        val tableModel = object : DefaultTableModel(
            arrayOf("Parameter Name", "Range or Constant", "Preview"),
            0
        ) {
            override fun isCellEditable(row: Int, column: Int): Boolean = column != 2
        }

        settings.mappings.forEach {
            tableModel.addRow(arrayOf(it.name, it.rangeText, ""))
        }

        val mappingTable = JTable(tableModel).apply {
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            rowHeight = 24
            tableHeader.reorderingAllowed = false
        }
        val tableScrollPane = JScrollPane(mappingTable).apply {
            preferredSize = Dimension(680, 180)
            border = BorderFactory.createTitledBorder("Parameter Mappings")
        }

        // テーブルの下のボタン
        val moveUpAction: () -> Unit = {
            val selected = mappingTable.selectedRow
            if (selected > 0) {
                tableModel.moveRow(selected, selected, selected - 1)
                mappingTable.setRowSelectionInterval(selected - 1, selected - 1)
            }
        }

        val moveDownAction: () -> Unit = {
            val selected = mappingTable.selectedRow
            if (selected in 0 until tableModel.rowCount - 1) {
                tableModel.moveRow(selected, selected, selected + 1)
                mappingTable.setRowSelectionInterval(selected + 1, selected + 1)
            }
        }

        val moveUpBtn = JButton("▲").apply {
            toolTipText = "Move selected parameter up"
            preferredSize = Dimension(42, 26)
            addActionListener { moveUpAction() }
        }
        val moveDownBtn = JButton("▼").apply {
            toolTipText = "Move selected parameter down"
            preferredSize = Dimension(42, 26)
            addActionListener { moveDownAction() }
        }
        val addRowBtn = JButton("+ Add").apply {
            toolTipText = "Add a parameter"
        }
        val removeRowBtn = JButton("- Remove").apply {
            toolTipText = "Remove the selected parameter"
        }

        // UI状態の切り替え
        val updateEnabledStates = {
            val regexMode = useRegexBox.isSelected
            regexField.isEnabled = regexMode
            regexPreviewLabel.isEnabled = regexMode

            delimiterField.isEnabled = !regexMode
            tokenScrollPane.isEnabled = !regexMode
            tokenCardsPanel.isEnabled = !regexMode
            mappingTable.isEnabled = !regexMode
            moveUpBtn.isEnabled = !regexMode
            moveDownBtn.isEnabled = !regexMode
            addRowBtn.isEnabled = !regexMode
            removeRowBtn.isEnabled = !regexMode
        }

        // プレビューの更新
        val updatePreview: () -> Unit = {
            val dirPath = dirField.text.trim()
            val filename = sampleFileField.text.trim()
            val isRegex = useRegexBox.isSelected
            val isAutoDate = autoDateBox.isSelected

            // サンプルファイルのXML撮影日時を確認
            if (isAutoDate && dirPath.isNotEmpty() && filename.isNotEmpty()) {
                val sampleFile = File(dirPath, filename)
                val xmlDate = xmlReader.extractAcquisitionDate(sampleFile)
                if (xmlDate != null) {
                    val xmlFile = xmlReader.findXmlFile(sampleFile)
                    val relPath = xmlFile?.name ?: "XML"
                    detectedDateLabel.text = "<html><b>Detected XML Date:</b> <font color='#147828'>$xmlDate</font> (from $relPath)</html>"
                } else {
                    detectedDateLabel.text = "<html><b>Detected XML Date:</b> None found for sample file</html>"
                }
            } else {
                detectedDateLabel.text = "<html><b>Detected XML Date:</b> Disabled or unavailable</html>"
            }

            if (isRegex) {
                val pattern = regexField.text.trim()
                try {
                    val regex = Regex(pattern)
                    val match = regex.find(filename)
                    if (match != null) {
                        val tempParser = FilenameParser(useRegex = true, regexPattern = pattern)
                        val groupNames = tempParser.extractNamedGroups(pattern)
                        val matchText = if (groupNames.isNotEmpty()) {
                            groupNames.joinToString(", ") { name -> "$name=${match.groups[name]?.value ?: ""}" }
                        } else {
                            "Match found (No named groups defined)"
                        }
                        regexPreviewLabel.text = "<html><b>Match:</b> $matchText</html>"
                        regexPreviewLabel.foreground = Color(20, 120, 40)
                    } else {
                        regexPreviewLabel.text = "<html><b>No match</b> with sample filename</html>"
                        regexPreviewLabel.foreground = Color(180, 40, 40)
                    }
                } catch (e: Exception) {
                    regexPreviewLabel.text = "<html><b>Invalid Regex:</b> ${e.message}</html>"
                    regexPreviewLabel.foreground = Color(180, 40, 40)
                }
            } else {
                val delimiter = delimiterField.text
                val parser = FilenameParser(useRegex = false, delimiter = delimiter)
                val tokens = parser.splitTokens(filename)

                tokenCardsPanel.removeAll()
                if (tokens.isEmpty() || (tokens.size == 1 && tokens[0].isEmpty())) {
                    tokenCardsPanel.add(JLabel("No tokens to display"))
                } else {
                    for (i in tokens.indices) {
                        val indexLabel = JLabel("#${i + 1}").apply {
                            font = Font(Font.SANS_SERIF, Font.BOLD, 10)
                            foreground = Color(30, 90, 180)
                            alignmentX = Component.CENTER_ALIGNMENT
                        }
                        val valLabel = JLabel(tokens[i]).apply {
                            font = Font(Font.MONOSPACED, Font.PLAIN, 12)
                            alignmentX = Component.CENTER_ALIGNMENT
                        }
                        val card = JPanel().apply {
                            layout = BoxLayout(this, BoxLayout.Y_AXIS)
                            background = Color(245, 248, 255)
                            border = BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(Color(180, 205, 240), 1),
                                BorderFactory.createEmptyBorder(3, 8, 3, 8)
                            )
                            add(indexLabel)
                            add(Box.createVerticalStrut(2))
                            add(valLabel)
                        }
                        tokenCardsPanel.add(card)
                    }
                }
                tokenCardsPanel.revalidate()
                tokenCardsPanel.repaint()

                for (r in 0 until tableModel.rowCount) {
                    val name = tableModel.getValueAt(r, 0)?.toString() ?: ""
                    val rangeOrConst = tableModel.getValueAt(r, 1)?.toString() ?: ""
                    val mapping = ParameterMapping(name, rangeOrConst)

                    val display = if (mapping.isConstant()) {
                        "[Constant] ${mapping.getConstantValue()}"
                    } else {
                        val rawVal = parser.extractRawValue(tokens, mapping)
                        rawVal ?: "Invalid index / range"
                    }
                    tableModel.setValueAt(display, r, 2)
                }
            }
        }

        // 入力監視リスナー
        val docListener = object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = updatePreview()
            override fun removeUpdate(e: DocumentEvent) = updatePreview()
            override fun changedUpdate(e: DocumentEvent) = updatePreview()
        }
        delimiterField.document.addDocumentListener(docListener)
        sampleFileField.document.addDocumentListener(docListener)
        dirField.document.addDocumentListener(docListener)
        regexField.document.addDocumentListener(docListener)
        tableModel.addTableModelListener { e ->
            if (e.column != 2) updatePreview()
        }

        useRegexBox.addActionListener {
            updateEnabledStates()
            updatePreview()
        }
        autoDateBox.addActionListener {
            updatePreview()
        }

        browseBtn.addActionListener {
            val chooser = DirectoryChooser("Select Image Directory")
            val chosen = chooser.directory ?: return@addActionListener
            dirField.text = chosen
            val detected = detectSampleFile(chosen)
            if (detected.isNotEmpty()) {
                sampleFileField.text = detected
            }
        }

        addRowBtn.addActionListener {
            tableModel.addRow(arrayOf("Param${tableModel.rowCount + 1}", "1", ""))
            updatePreview()
        }
        removeRowBtn.addActionListener {
            val selected = mappingTable.selectedRow
            if (selected >= 0) {
                tableModel.removeRow(selected)
                updatePreview()
            }
        }

        // 上部 ディレクトリ、サンプルファイル名、XML自動取得か
        val topPanel = JPanel(GridBagLayout()).apply {
            border = BorderFactory.createEmptyBorder(8, 12, 4, 12)
            val c = GridBagConstraints().apply {
                fill = GridBagConstraints.HORIZONTAL
                insets = Insets(4, 4, 4, 4)
            }

            c.gridx = 0; c.gridy = 0; c.weightx = 0.0
            add(JLabel("Image Directory:"), c)
            c.gridx = 1; c.weightx = 1.0
            add(dirField, c)
            c.gridx = 2; c.weightx = 0.0
            add(browseBtn, c)

            c.gridx = 0; c.gridy = 1; c.weightx = 0.0
            add(JLabel("Sample Filename:"), c)
            c.gridx = 1; c.gridwidth = 2; c.weightx = 1.0
            add(sampleFileField, c)

            c.gridx = 0; c.gridy = 2; c.gridwidth = 1; c.weightx = 0.0
            add(JLabel("Metadata Options:"), c)
            c.gridx = 1; c.gridwidth = 2; c.weightx = 1.0
            val xmlRow = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
                add(autoDateBox)
                add(Box.createHorizontalStrut(10))
                add(detectedDateLabel)
            }
            add(xmlRow, c)
        }

        // 中央 上部 + テーブルセクション
        val topSection = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            val regexPanel = JPanel(BorderLayout(5, 5)).apply {
                border = BorderFactory.createTitledBorder("Regular Expression")
                val topRegex = JPanel(BorderLayout(5, 5)).apply {
                    add(useRegexBox, BorderLayout.WEST)
                    add(regexField, BorderLayout.CENTER)
                }
                add(topRegex, BorderLayout.NORTH)
                add(regexPreviewLabel, BorderLayout.SOUTH)
            }
            add(regexPanel)
            add(Box.createVerticalStrut(4))

            val delimRow = JPanel(FlowLayout(FlowLayout.LEFT, 5, 2)).apply {
                add(JLabel("Delimiter:"))
                add(delimiterField)
            }
            add(delimRow)
            add(tokenScrollPane)
        }

        // テーブル操作ボタン
        val tableControls = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply {
            add(moveUpBtn)
            add(moveDownBtn)
            add(Box.createHorizontalStrut(6))
            add(addRowBtn)
            add(removeRowBtn)
        }

        // 中央のテーブルセクション
        val tableSection = JPanel(BorderLayout(0, 4)).apply {
            add(tableScrollPane, BorderLayout.CENTER)
            add(tableControls, BorderLayout.SOUTH)
        }

        val centerPanel = JPanel(BorderLayout(0, 6)).apply {
            border = BorderFactory.createEmptyBorder(0, 12, 0, 12)
            add(topSection, BorderLayout.NORTH)
            add(tableSection, BorderLayout.CENTER)
        }

        // 下部パネル オプション・アクション
        val recursiveBox = JCheckBox("Include subdirectories", settings.isRecursive)

        val bottomOptionsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 15, 5)).apply {
            add(recursiveBox)
        }

        val okBtn = JButton("OK")
        val cancelBtn = JButton("Cancel")
        val actionButtonsPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 10, 5)).apply {
            add(cancelBtn)
            add(okBtn)
        }

        val bottomPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(4, 12, 10, 12)
            add(bottomOptionsPanel, BorderLayout.WEST)
            add(actionButtonsPanel, BorderLayout.EAST)
        }

        okBtn.addActionListener {
            val dirPath = dirField.text.trim()
            if (dirPath.isEmpty() || !File(dirPath).isDirectory) {
                JOptionPane.showMessageDialog(
                    dialog,
                    "Please select a valid image directory.",
                    "Invalid Directory",
                    JOptionPane.ERROR_MESSAGE
                )
                return@addActionListener
            }

            val isRegex = useRegexBox.isSelected
            val regexPattern = regexField.text.trim()

            if (isRegex) {
                try {
                    Regex(regexPattern)
                } catch (e: Exception) {
                    JOptionPane.showMessageDialog(
                        dialog,
                        "Invalid Regular Expression:\n${e.message}",
                        "Invalid Regex",
                        JOptionPane.ERROR_MESSAGE
                    )
                    return@addActionListener
                }
            }

            val mappings = mutableListOf<ParameterMapping>()
            if (!isRegex) {
                for (r in 0 until tableModel.rowCount) {
                    val name = tableModel.getValueAt(r, 0)?.toString()?.trim() ?: ""
                    val rangeOrConst = tableModel.getValueAt(r, 1)?.toString()?.trim() ?: ""
                    if (name.isNotEmpty() && rangeOrConst.isNotEmpty()) {
                        mappings.add(ParameterMapping(name, rangeOrConst))
                    }
                }
                if (mappings.isEmpty()) {
                    JOptionPane.showMessageDialog(
                        dialog,
                        "Please define at least one parameter mapping.",
                        "No Parameters",
                        JOptionPane.ERROR_MESSAGE
                    )
                    return@addActionListener
                }
            }

            val updated = WormQuantSettings(
                directoryPath = dirPath,
                useRegex = isRegex,
                regexPattern = regexPattern,
                delimiter = delimiterField.text,
                mappings = mappings.ifEmpty { settings.mappings },
                extractDateFromXml = autoDateBox.isSelected,
                isRecursive = recursiveBox.isSelected
            )
            updated.save()
            resultSettings = updated
            dialog.dispose()
        }

        cancelBtn.addActionListener {
            dialog.dispose()
        }

        dialog.add(topPanel, BorderLayout.NORTH)
        dialog.add(centerPanel, BorderLayout.CENTER)
        dialog.add(bottomPanel, BorderLayout.SOUTH)

        updateEnabledStates()
        updatePreview()

        dialog.pack()
        dialog.minimumSize = Dimension(740, 600)
        dialog.setLocationRelativeTo(parentFrame)
        dialog.isVisible = true

        return resultSettings
    }

    private fun detectSampleFile(dirPath: String): String {
        if (dirPath.isEmpty()) return ""
        val dir = File(dirPath)
        if (!dir.isDirectory) return ""
        val isTiff = { f: File ->
            f.isFile && (f.name.endsWith(".tif", ignoreCase = true) || f.name.endsWith(".tiff", ignoreCase = true))
        }
        val firstFile = dir.walkTopDown().firstOrNull(isTiff)
        return firstFile?.name ?: ""
    }
}
