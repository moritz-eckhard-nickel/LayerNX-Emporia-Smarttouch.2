package com.layernx.launcher.core

/** LayerNX accent palette. Every preset keeps white text at WCAG AA contrast (≥ 4.5 : 1). */
object Accent {

    const val VIOLET: Int = 0xFF552FFB.toInt()
    const val BLUE: Int = 0xFF1F5FE0.toInt()
    const val GREEN: Int = 0xFF0B7A5C.toInt()
    const val RED: Int = 0xFFC9352A.toInt()
    const val PINK: Int = 0xFFB4256F.toInt()
    const val TEAL: Int = 0xFF0B6F86.toInt()

    val PRESETS: List<Int> = listOf(VIOLET, BLUE, GREEN, RED, PINK, TEAL)

    const val DEFAULT: Int = VIOLET

    /** Returns [color] if it is a known preset, otherwise the LayerNX default. */
    fun sanitize(color: Int): Int = if (color in PRESETS) color else DEFAULT

    fun withAlpha(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)

    /** Mixes [color] with white; used for icon tints on dark panels (#552FFB → ≈ #BBACFD). */
    fun lighten(color: Int, amount: Float): Int {
        val a = amount.coerceIn(0f, 1f)
        fun mix(c: Int) = (c + (255 - c) * a + 0.5f).toInt().coerceIn(0, 255)
        val r = mix(color shr 16 and 0xFF)
        val g = mix(color shr 8 and 0xFF)
        val b = mix(color and 0xFF)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    fun relativeLuminance(color: Int): Double {
        fun channel(c: Int): Double {
            val s = c / 255.0
            return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(color shr 16 and 0xFF) +
            0.7152 * channel(color shr 8 and 0xFF) +
            0.0722 * channel(color and 0xFF)
    }

    fun contrast(a: Int, b: Int): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }
}
