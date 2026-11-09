package com.layernx.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class T9Test {

    @Test
    fun encodesLettersToKeypadDigits() {
        assertEquals("52783", T9.encode("Karte"))
        assertEquals("3473369", T9.encode("Firefox"))
        assertEquals("7777", T9.encode("pqrs"))
        assertEquals("9999", T9.encode("wxyz"))
    }

    @Test
    fun foldsUmlautsAccentsAndLigatures() {
        assertEquals("uhr", T9.fold("Ühr"))
        assertEquals("strasse", T9.fold("Straße"))
        assertEquals("cafe", T9.fold("Café"))
        assertEquals(T9.encode("Uhr"), T9.encode("Ühr"))
    }

    @Test
    fun keepsDigitsAndSeparatesWords() {
        assertEquals("2048", T9.encode("2048"))
        assertEquals("3473369 36287", T9.encode("Firefox Focus"))
    }

    @Test
    fun digitQueryRanksPrefixBeforeWordBeforeInfix() {
        assertEquals(0, T9.rank("Firefox", "347"))
        assertEquals(1, T9.rank("Open Camera", "226"))
        assertEquals(2, T9.rank("Firefox", "336")) // "efo" inside "firefox"
    }

    @Test
    fun digitQueryMatchesWordStarts() {
        assertEquals(0, T9.rank("Telegram", "835"))
        assertEquals(1, T9.rank("Simple Gallery", "4255"))
        assertEquals(T9.NO_MATCH, T9.rank("Telegram", "999"))
    }

    @Test
    fun singleDigitDoesNotMatchInsideWords() {
        // '7' would match almost everything if infix matches were allowed for one key press.
        assertEquals(T9.NO_MATCH, T9.rank("Kamera", "7"))
        assertEquals(0, T9.rank("Rechner", "7"))
    }

    @Test
    fun textQueryIsCaseAndAccentInsensitive() {
        assertEquals(0, T9.rank("Einstellungen", "ein"))
        assertEquals(1, T9.rank("Open Camera", "cam"))
        assertEquals(2, T9.rank("Einstellungen", "stell"))
        assertEquals(0, T9.rank("Ühr", "uhr"))
        assertTrue(T9.matches("Straße", "strasse"))
        assertFalse(T9.matches("Kamera", "xyz"))
    }

    @Test
    fun emptyQueryMatchesEverything() {
        assertEquals(0, T9.rank("Anything", ""))
        assertEquals(0, T9.rank("Anything", "   "))
    }

    @Test
    fun recognisesDigitQueries() {
        assertTrue(T9.isDigitQuery("0123"))
        assertFalse(T9.isDigitQuery("12a"))
        assertFalse(T9.isDigitQuery(""))
    }
}
