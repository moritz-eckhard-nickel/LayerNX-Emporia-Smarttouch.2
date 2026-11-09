package com.layernx.launcher

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.format.DateFormat
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import com.layernx.launcher.core.Accent
import com.layernx.launcher.core.AppRole
import com.layernx.launcher.data.AppRepository
import com.layernx.launcher.data.Prefs
import com.layernx.launcher.system.Launcher
import com.layernx.launcher.system.SystemActions
import com.layernx.launcher.ui.Palette
import com.layernx.launcher.ui.RolePickers
import com.layernx.launcher.ui.Surfaces
import com.layernx.launcher.ui.SystemBars
import com.layernx.launcher.ui.fitToWidth
import java.util.Locale

/**
 * The LayerNX home screen ("Zen" layout from the mockup).
 *
 * Keypad (only while the home screen is in front):
 * - 0–9, *, #   → dialer with that digit typed (ACTION_DIAL – nothing is ever called automatically)
 * - Call key     → phone app
 * - Camera key   → chosen camera app (fixes the stock key opening only the Emporia camera)
 * - Envelope key → messages app
 * - Menu key     → customize
 * - D-pad / OK   → move between and open the visible buttons
 */
class HomeActivity : Activity() {

    private lateinit var launcher: Launcher
    private lateinit var prefs: Prefs

    private lateinit var root: ScrollView
    private lateinit var brand: TextView
    private lateinit var gear: ImageButton
    private lateinit var banner: LinearLayout
    private lateinit var clock: TextClock
    private lateinit var date: TextClock
    private lateinit var accentLine: View
    private lateinit var greeting: TextView
    private lateinit var allApps: LinearLayout
    private lateinit var dock: List<LinearLayout>
    private lateinit var softLeft: TextView
    private lateinit var softCenter: TextView
    private lateinit var softRight: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemBars.setup(window)
        setContentView(R.layout.activity_home)
        launcher = Launcher(this)
        prefs = launcher.prefs

        root = findViewById(R.id.home_root)
        brand = findViewById(R.id.brand)
        gear = findViewById(R.id.gear)
        banner = findViewById(R.id.default_banner)
        clock = findViewById(R.id.clock)
        date = findViewById(R.id.date)
        accentLine = findViewById(R.id.accent_line)
        greeting = findViewById(R.id.greeting)
        allApps = findViewById(R.id.all_apps)
        dock = listOf(findViewById(R.id.dock_0), findViewById(R.id.dock_1), findViewById(R.id.dock_2))
        softLeft = findViewById(R.id.softkey_left)
        softCenter = findViewById(R.id.softkey_center)
        softRight = findViewById(R.id.softkey_right)

        SystemBars.applyPadding(root)
        // Large numerals: sized in dp so a big system font scale cannot push the dock off screen.
        clock.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 80f)
        brand.text = brandText()

        gear.setOnClickListener { openSettings() }
        banner.setOnClickListener { SystemActions.requestDefaultLauncher(this) }
        allApps.setOnClickListener { openApps() }
        softLeft.setOnClickListener { v -> launchRole(AppRole.PHONE, v) }
        softCenter.setOnClickListener { openApps() }
        softRight.setOnClickListener { openApps() }
        dock.forEachIndexed { slot, button ->
            button.setOnClickListener { v -> launchRole(prefs.dockRole(slot), v) }
        }
        // Keypad users land on the primary dock button first.
        dock[0].isFocusedByDefault = true
    }

    override fun onResume() {
        super.onResume()
        AppRepository.refreshIfStale()
        render()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // HOME pressed while already home: return to the resting state.
        root.smoothScrollTo(0, 0)
        if (!root.isInTouchMode) dock[0].requestFocus()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SystemActions.REQUEST_HOME_ROLE && !SystemActions.isDefaultLauncher(this)) {
            // Some firmwares close the role dialog without showing it (e.g. after it was
            // declined twice). Offer the classic "Home app" screen so the button always works.
            SystemActions.openHomeSettings(this)
        }
        render()
    }

    private fun render() {
        val accent = prefs.accent
        val accentTint = ColorStateList.valueOf(Accent.lighten(accent, 0.6f))

        root.background = Surfaces.zenBackground(accent)
        gear.background = Surfaces.interactive(this, 22f, oval = true) { Surfaces.oval(Palette.PANEL) }
        gear.imageTintList = ColorStateList.valueOf(Palette.TEXT)

        val isDefault = SystemActions.isDefaultLauncher(this)
        banner.visibility = if (isDefault) View.GONE else View.VISIBLE
        banner.background = Surfaces.interactive(this, 14f) {
            Surfaces.rounded(this, Palette.PANEL, 14f, accent, 1f)
        }
        findViewById<ImageView>(R.id.default_banner_icon).imageTintList = accentTint

        date.format12Hour = datePattern()
        date.format24Hour = datePattern()

        accentLine.background = Surfaces.rounded(this, accent, 2f)

        val name = prefs.userName
        greeting.text = if (name.isBlank()) getString(R.string.greeting_anonymous) else getString(R.string.greeting_named, name)

        dock.forEachIndexed { slot, button ->
            val role = prefs.dockRole(slot)
            val primary = slot == 0
            button.background = if (primary) Surfaces.accentButton(this, accent, 22f) else Surfaces.panelButton(this, 22f)
            val label = getString(role.labelRes)
            button.findViewById<ImageView>(R.id.dock_icon).apply {
                setImageResource(role.iconRes)
                imageTintList = ColorStateList.valueOf(Palette.TEXT)
            }
            button.findViewById<TextView>(R.id.dock_label).apply {
                text = label
                fitToWidth(maxSp = 12f, minSp = 9f)
            }
            button.contentDescription = label
        }

        allApps.background = Surfaces.outlinedButton(this, 18f)
        softLeft.background = Surfaces.interactive(this, 10f) { Surfaces.rounded(this, android.graphics.Color.TRANSPARENT, 10f) }
        softRight.background = Surfaces.interactive(this, 10f) { Surfaces.rounded(this, android.graphics.Color.TRANSPARENT, 10f) }
        softCenter.background = Surfaces.interactive(this, 7f) {
            Surfaces.rounded(this, android.graphics.Color.TRANSPARENT, 7f, Palette.SOFTKEY_BORDER)
        }
    }

    private fun brandText(): CharSequence {
        val layer = getString(R.string.brand_layer).uppercase(Locale.ROOT)
        val nx = getString(R.string.brand_nx)
        return SpannableStringBuilder(layer).apply {
            val start = length
            append(nx)
            setSpan(ForegroundColorSpan(Palette.TEXT), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(StyleSpan(Typeface.BOLD), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    /** "Montag, 9. November" / "Monday, November 9" – localised by ICU. */
    private fun datePattern(): String {
        val locale = Locale.getDefault()
        val best = DateFormat.getBestDateTimePattern(locale, DATE_SKELETON)
        if (best != DATE_SKELETON) return best
        // Defensive: if the firmware's ICU data cannot expand the skeleton, use the full date.
        return (java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL, locale) as? java.text.SimpleDateFormat)
            ?.toPattern() ?: "EEEE, d MMMM"
    }

    private fun launchRole(role: AppRole, source: View?) {
        if (!launcher.launch(role, source)) {
            Toast.makeText(this, getString(R.string.no_app_for_role, getString(role.labelRes)), Toast.LENGTH_SHORT).show()
            RolePickers.chooseAppForRole(this, prefs, launcher.resolver, role) { render() }
        }
    }

    private fun openApps() {
        startActivity(Intent(this, AppsActivity::class.java))
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.repeatCount > 0) {
            return keyCode in HANDLED_KEYS || super.onKeyDown(keyCode, event)
        }
        val digit = dialChar(keyCode)
        if (digit != null) {
            launcher.dial(digit.toString())
            return true
        }
        when (keyCode) {
            KeyEvent.KEYCODE_CALL -> launchRole(AppRole.PHONE, null)
            KeyEvent.KEYCODE_CAMERA -> launchRole(AppRole.CAMERA, null)
            KeyEvent.KEYCODE_ENVELOPE -> launchRole(AppRole.MESSAGES, null)
            KeyEvent.KEYCODE_EXPLORER -> launchRole(AppRole.BROWSER, null)
            KeyEvent.KEYCODE_MENU -> openSettings()
            KeyEvent.KEYCODE_SOFT_LEFT -> launchRole(AppRole.PHONE, null)
            KeyEvent.KEYCODE_SOFT_RIGHT -> openApps()
            // The home screen is the root: BACK has nowhere to go.
            KeyEvent.KEYCODE_BACK -> Unit
            else -> return super.onKeyDown(keyCode, event)
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean =
        keyCode in HANDLED_KEYS || dialChar(keyCode) != null || super.onKeyUp(keyCode, event)

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean =
        keyCode in HANDLED_KEYS || super.onKeyLongPress(keyCode, event)

    private fun dialChar(keyCode: Int): Char? = when (keyCode) {
        in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> '0' + (keyCode - KeyEvent.KEYCODE_0)
        KeyEvent.KEYCODE_STAR -> '*'
        KeyEvent.KEYCODE_POUND -> '#'
        else -> null
    }

    private companion object {
        const val DATE_SKELETON = "EEEEdMMMM"
        val HANDLED_KEYS = setOf(
            KeyEvent.KEYCODE_CALL,
            KeyEvent.KEYCODE_CAMERA,
            KeyEvent.KEYCODE_ENVELOPE,
            KeyEvent.KEYCODE_EXPLORER,
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_SOFT_LEFT,
            KeyEvent.KEYCODE_SOFT_RIGHT,
            KeyEvent.KEYCODE_BACK,
        )
    }
}
