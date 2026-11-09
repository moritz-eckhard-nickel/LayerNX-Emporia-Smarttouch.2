package com.layernx.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleRankingTest {

    private val emporiaBrowser = RoleCandidate("com.emporia.browser", "Browser", isSystemApp = true)
    private val firefox = RoleCandidate("org.mozilla.firefox", "Firefox", isSystemApp = false)
    private val aospDialer = RoleCandidate("com.android.dialer", "Telefon", isSystemApp = true)
    private val emporiaContacts = RoleCandidate("com.emporia.contacts", "Kontakte", isSystemApp = true)
    private val mtkCamera = RoleCandidate("com.mediatek.camera", "Kamera", isSystemApp = true)
    private val openCamera = RoleCandidate("net.sourceforge.opencamera", "Open Camera", isSystemApp = false)

    @Test
    fun detectsEmporiaPackagesOnly() {
        assertTrue(RoleRanking.isOemPackage("com.emporia.browser"))
        assertTrue(RoleRanking.isOemPackage("com.emporia"))
        assertFalse(RoleRanking.isOemPackage("com.emporiax.app"))
        assertFalse(RoleRanking.isOemPackage("com.android.dialer"))
        assertFalse(RoleRanking.isOemPackage("com.mediatek.camera"))
    }

    @Test
    fun installedAppBeatsEmporiaSystemDefault() {
        // Stock firmware: the Emporia browser is Android's default for links.
        val chosen = RoleRanking.choose(listOf(emporiaBrowser, firefox), "com.emporia.browser", avoidOem = true)
        assertEquals(firefox, chosen)
    }

    @Test
    fun userChosenSystemDefaultWins() {
        val chosen = RoleRanking.choose(listOf(openCamera, mtkCamera), "com.mediatek.camera", avoidOem = true)
        assertEquals(mtkCamera, chosen)
    }

    @Test
    fun aospAppPreferredOverEmporiaApp() {
        val chosen = RoleRanking.choose(listOf(emporiaContacts, aospDialer), null, avoidOem = true)
        assertEquals(aospDialer, chosen)
    }

    @Test
    fun installedAppPreferredOverPreinstalledWhenNoDefault() {
        val chosen = RoleRanking.choose(listOf(mtkCamera, openCamera), null, avoidOem = true)
        assertEquals(openCamera, chosen)
    }

    @Test
    fun emporiaAppIsLastResortSoRolesNeverBreak() {
        val chosen = RoleRanking.choose(listOf(emporiaBrowser), "com.emporia.browser", avoidOem = true)
        assertEquals(emporiaBrowser, chosen)
    }

    @Test
    fun avoidanceCanBeSwitchedOff() {
        val chosen = RoleRanking.choose(listOf(firefox, emporiaBrowser), "com.emporia.browser", avoidOem = false)
        assertEquals(emporiaBrowser, chosen)
    }

    @Test
    fun rankingIsCompleteAndDeduplicated() {
        val ranked = RoleRanking.rank(listOf(emporiaBrowser, firefox, firefox, aospDialer), null, avoidOem = true)
        assertEquals(listOf(firefox, aospDialer, emporiaBrowser), ranked)
    }

    @Test
    fun noCandidatesMeansNoChoice() {
        assertNull(RoleRanking.choose(emptyList(), "com.android.dialer", avoidOem = true))
    }
}
