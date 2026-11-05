package net.shotakosaka.wormquant

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WormQuantSettingsTest {

    @Test
    fun testDefaults() {
        val settings = WormQuantSettings()

        assertEquals("", settings.directoryPath)
        assertFalse(settings.useRegex)
        assertEquals(FilenameParser.DEFAULT_REGEX, settings.regexPattern)
        assertEquals("_", settings.delimiter)
        assertEquals(6, settings.mappings.size)
        assertTrue(settings.extractDateFromXml)
        assertEquals(false, settings.isRecursive)
    }

    @Test
    fun testSaveAndLoadSettings() {
        val custom = WormQuantSettings(
            directoryPath = "/tmp/sample_images",
            useRegex = true,
            regexPattern = "^(?<genotype>.*)$",
            delimiter = "-",
            mappings = listOf(
                ParameterMapping("Strain", "1"),
                ParameterMapping("Species", "C. elegans")
            ),
            extractDateFromXml = false,
            isRecursive = true
        )
        custom.save()

        val loaded = WormQuantSettings.load()
        assertEquals("/tmp/sample_images", loaded.directoryPath)
        assertTrue(loaded.useRegex)
        assertEquals("^(?<genotype>.*)$", loaded.regexPattern)
        assertEquals("-", loaded.delimiter)
        assertEquals(2, loaded.mappings.size)
        assertEquals("Strain", loaded.mappings[0].name)
        assertEquals("1", loaded.mappings[0].rangeText)
        assertEquals("Species", loaded.mappings[1].name)
        assertEquals("C. elegans", loaded.mappings[1].rangeText)
        assertFalse(loaded.extractDateFromXml)
        assertEquals(true, loaded.isRecursive)
    }
}
