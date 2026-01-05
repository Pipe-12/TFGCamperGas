package com.example.campergas.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguageTest {

    @Test
    fun `toLocaleList returns empty list for SYSTEM language`() {
        // Arrange
        val language = AppLanguage.SYSTEM

        // Act
        val localeList = language.toLocaleList()

        // Assert
        assertTrue(localeList.isEmpty)
    }

    @Test
    fun `toLocaleList returns correct locale for SPANISH`() {
        // Arrange
        val language = AppLanguage.SPANISH

        // Act
        val localeList = language.toLocaleList()

        // Assert
        assertFalse(localeList.isEmpty)
        assertEquals("es", localeList[0]?.language)
    }

    @Test
    fun `toLocaleList returns correct locale for ENGLISH`() {
        // Arrange
        val language = AppLanguage.ENGLISH

        // Act
        val localeList = language.toLocaleList()

        // Assert
        assertFalse(localeList.isEmpty)
        assertEquals("en", localeList[0]?.language)
    }

    @Test
    fun `toLocaleList returns correct locale for CATALAN`() {
        // Arrange
        val language = AppLanguage.CATALAN

        // Act
        val localeList = language.toLocaleList()

        // Assert
        assertFalse(localeList.isEmpty)
        assertEquals("ca", localeList[0]?.language)
    }

    @Test
    fun `fromLanguageTag returns correct enum for valid tags`() {
        // Assert
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromLanguageTag("es"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLanguageTag("en"))
        assertEquals(AppLanguage.CATALAN, AppLanguage.fromLanguageTag("ca"))
    }

    @Test
    fun `fromLanguageTag is case insensitive`() {
        // Assert
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromLanguageTag("ES"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLanguageTag("EN"))
        assertEquals(AppLanguage.CATALAN, AppLanguage.fromLanguageTag("CA"))
    }

    @Test
    fun `fromLanguageTag returns SYSTEM for null or empty tag`() {
        // Assert
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTag(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTag(""))
    }

    @Test
    fun `fromLanguageTag returns SYSTEM for unknown tag`() {
        // Assert
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTag("fr"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTag("de"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTag("unknown"))
    }

    @Test
    fun `all languages have correct language tags`() {
        // Assert
        assertEquals("", AppLanguage.SYSTEM.languageTag)
        assertEquals("es", AppLanguage.SPANISH.languageTag)
        assertEquals("en", AppLanguage.ENGLISH.languageTag)
        assertEquals("ca", AppLanguage.CATALAN.languageTag)
    }
}
