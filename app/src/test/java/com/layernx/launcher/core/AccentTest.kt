package com.layernx.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentTest {

    @Test
    fun everyPresetKeepsWhiteTextReadable() {
        Accent.PRESETS.forEach { color ->
            val ratio = Accent.contrast(color, 0xFFFFFFFF.toInt())
            assertTrue("Preset #%08X has contrast %.2f".format(color, ratio), ratio >= 4.5)
        }
    }

    @Test
    fun defaultIsLayerNxViolet() {
        assertEquals(0xFF552FFB.toInt(), Accent.DEFAULT)
    }

    @Test
    fun unknownColoursFallBackToDefault() {
        assertEquals(Accent.DEFAULT, Accent.sanitize(0xFF123456.toInt()))
        assertEquals(Accent.GREEN, Accent.sanitize(Accent.GREEN))
    }

    @Test
    fun alphaAndLightenAreExact() {
        assertEquals(0x26552FFB, Accent.withAlpha(Accent.VIOLET, 0x26))
        assertEquals(0xFFFFFFFF.toInt(), Accent.lighten(Accent.VIOLET, 1f))
        assertEquals(Accent.VIOLET, Accent.lighten(Accent.VIOLET, 0f))
    }
}
