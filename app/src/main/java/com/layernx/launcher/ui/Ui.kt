package com.layernx.launcher.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsets
import com.layernx.launcher.core.Accent
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

/** LayerNX design tokens, taken from launcher-mockup.html (see docs/DESIGN.md). */
object Palette {
    const val BG: Int = 0xFF0C0D10.toInt()
    const val PANEL: Int = 0xFF1A1C22.toInt()
    const val TEXT: Int = 0xFFF8F8FC.toInt()
    const val MUTED: Int = 0xFFA0A3B1.toInt()
    const val BORDER: Int = 0xFF32323F.toInt()
    const val BORDER_SOFT: Int = 0xFF292B33.toInt()
    const val INPUT: Int = 0xFF242630.toInt()
    const val INPUT_BORDER: Int = 0xFF414451.toInt()
    const val SOFTKEY_BORDER: Int = 0xFF42444F.toInt()
    const val FOCUS_RING: Int = Color.WHITE
    const val RIPPLE: Int = 0x33FFFFFF
}

fun Context.dp(value: Float): Int = (value * resources.displayMetrics.density).roundToInt()
fun Context.dp(value: Int): Int = dp(value.toFloat())

/** Factory for the LayerNX surfaces. All drawables are new instances (never shared between views). */
object Surfaces {

    fun rounded(
        context: Context,
        fill: Int,
        radiusDp: Float,
        strokeColor: Int? = null,
        strokeDp: Float = 1f,
    ): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = context.dp(radiusDp).toFloat()
        setColor(fill)
        if (strokeColor != null) setStroke(max(1, context.dp(strokeDp)), strokeColor)
    }

    fun oval(fill: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(fill)
    }

    /**
     * Interactive surface: ripple on touch, and a 2.5dp white focus ring drawn 3dp inside the
     * edge when reached with the keypad (the mockup's `:focus-visible` outline).
     * [ringOnSelected] is for ListView rows, which report keypad focus as "selected".
     */
    fun interactive(
        context: Context,
        radiusDp: Float,
        ringOnSelected: Boolean = false,
        oval: Boolean = false,
        base: () -> Drawable,
    ): Drawable {
        val states = StateListDrawable()
        val focusState = if (ringOnSelected) android.R.attr.state_selected else android.R.attr.state_focused
        states.addState(intArrayOf(focusState), withRing(context, base(), radiusDp, oval))
        if (ringOnSelected) {
            states.addState(intArrayOf(android.R.attr.state_focused), withRing(context, base(), radiusDp, oval))
        }
        states.addState(intArrayOf(), base())
        val mask = if (oval) oval(Color.WHITE) else rounded(context, Color.WHITE, radiusDp)
        return RippleDrawable(ColorStateList.valueOf(Palette.RIPPLE), states, mask)
    }

    private fun withRing(context: Context, base: Drawable, radiusDp: Float, oval: Boolean): Drawable {
        val inset = context.dp(3f)
        val ring = GradientDrawable().apply {
            shape = if (oval) GradientDrawable.OVAL else GradientDrawable.RECTANGLE
            if (!oval) cornerRadius = max(0, context.dp(radiusDp) - inset).toFloat()
            setColor(Color.TRANSPARENT)
            setStroke(context.dp(2.5f), Palette.FOCUS_RING)
        }
        return LayerDrawable(arrayOf(base, ring)).apply {
            setLayerInset(1, inset, inset, inset, inset)
        }
    }

    fun panelButton(context: Context, radiusDp: Float = 18f) =
        interactive(context, radiusDp) { rounded(context, Palette.PANEL, radiusDp) }

    fun accentButton(context: Context, accent: Int, radiusDp: Float = 18f) =
        interactive(context, radiusDp) { rounded(context, accent, radiusDp) }

    fun outlinedButton(context: Context, radiusDp: Float = 18f, border: Int = Palette.BORDER) =
        interactive(context, radiusDp) { rounded(context, Color.TRANSPARENT, radiusDp, border) }

    fun listRow(context: Context, radiusDp: Float = 14f) =
        interactive(context, radiusDp, ringOnSelected = true) { rounded(context, Palette.PANEL, radiusDp) }

    fun settingsRow(context: Context, radiusDp: Float = 12f) =
        interactive(context, radiusDp) { rounded(context, Color.TRANSPARENT, radiusDp, Palette.BORDER_SOFT) }

    fun input(context: Context): Drawable {
        val states = StateListDrawable()
        states.addState(
            intArrayOf(android.R.attr.state_focused),
            rounded(context, Palette.INPUT, 10f, Color.WHITE, 2f),
        )
        states.addState(intArrayOf(), rounded(context, Palette.INPUT, 10f, Palette.INPUT_BORDER))
        return states
    }

    /** Zen background: base colour plus the accent glow at 90 % / 35 % of the screen. */
    fun zenBackground(accent: Int): Drawable {
        val glow = GradientDrawable().apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            setGradientCenter(0.9f, 0.35f)
            colors = intArrayOf(Accent.withAlpha(accent, 0x26), Accent.withAlpha(accent, 0))
        }
        return object : LayerDrawable(arrayOf(ColorDrawable(Palette.BG), glow)) {
            override fun onBoundsChange(bounds: android.graphics.Rect) {
                super.onBoundsChange(bounds)
                // CSS: radial-gradient(ellipse at 90% 35%, …, transparent 65%) → farthest corner × 0.65
                val radius = hypot(bounds.width() * 0.9, bounds.height() * 0.65) * 0.65
                glow.gradientRadius = max(1.0, radius).toFloat()
            }
        }
    }
}

/** Edge-to-edge drawing with explicit insets, identical on Android 8 – 15. */
object SystemBars {

    fun setup(window: Window) {
        @Suppress("DEPRECATION")
        run {
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Palette.BG
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Deprecated on API 35 only because edge-to-edge became the default there.
            @Suppress("DEPRECATION")
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
    }

    /** Adds status bar, navigation bar, cutout and keyboard insets to [view]'s own padding. */
    fun applyPadding(view: View) {
        val left = view.paddingLeft
        val top = view.paddingTop
        val right = view.paddingRight
        val bottom = view.paddingBottom
        view.setOnApplyWindowInsetsListener { v, insets ->
            val l: Int
            val t: Int
            val r: Int
            val b: Int
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val i = insets.getInsets(
                    WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout() or WindowInsets.Type.ime(),
                )
                l = i.left; t = i.top; r = i.right; b = i.bottom
            } else {
                @Suppress("DEPRECATION")
                run {
                    l = insets.systemWindowInsetLeft
                    t = insets.systemWindowInsetTop
                    r = insets.systemWindowInsetRight
                    b = insets.systemWindowInsetBottom
                }
            }
            v.setPadding(left + l, top + t, right + r, bottom + b)
            insets
        }
        if (view.isAttachedToWindow) view.requestApplyInsets()
    }
}

/** Applies the LayerNX accent to a framework [android.widget.Switch]. */
fun android.widget.Switch.applyAccent(accent: Int) {
    val checked = intArrayOf(android.R.attr.state_checked)
    val none = intArrayOf()
    thumbTintList = ColorStateList(arrayOf(checked, none), intArrayOf(Color.WHITE, Palette.MUTED))
    trackTintList = ColorStateList(arrayOf(checked, none), intArrayOf(accent, Palette.INPUT_BORDER))
}

/**
 * Shrinks a single-line label until it fits its width (never below [minSp]); keeps long
 * German words like "Nachrichten" whole even at large system font sizes.
 */
fun android.widget.TextView.fitToWidth(maxSp: Float, minSp: Float) {
    val apply = {
        val available = width - totalPaddingLeft - totalPaddingRight
        if (available > 0) {
            var sizeSp = maxSp
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, sizeSp)
            val label = text.toString()
            while (sizeSp > minSp && paint.measureText(label) > available) {
                sizeSp -= 0.5f
                paint.textSize = android.util.TypedValue.applyDimension(
                    android.util.TypedValue.COMPLEX_UNIT_SP,
                    sizeSp,
                    resources.displayMetrics,
                )
            }
            if (sizeSp != maxSp) setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, sizeSp)
        }
    }
    if (width > 0) apply()
    // Re-fit when the width changes; the listener is registered only once per view.
    (getTag(com.layernx.launcher.R.id.dock_label) as? View.OnLayoutChangeListener)
        ?.let { removeOnLayoutChangeListener(it) }
    val listener = View.OnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
        if (right - left != oldRight - oldLeft) post(apply)
    }
    setTag(com.layernx.launcher.R.id.dock_label, listener)
    addOnLayoutChangeListener(listener)
}
