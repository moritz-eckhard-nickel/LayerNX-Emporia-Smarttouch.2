package com.layernx.launcher

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.media.AudioManager
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
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
import com.layernx.launcher.data.NotificationStore
import com.layernx.launcher.data.Prefs
import com.layernx.launcher.system.BackgroundApps
import com.layernx.launcher.system.Launcher
import com.layernx.launcher.system.NotificationAccess
import com.layernx.launcher.system.NotificationsService
import com.layernx.launcher.system.Ringer
import com.layernx.launcher.system.SystemActions
import com.layernx.launcher.ui.Palette
import com.layernx.launcher.ui.Picker
import com.layernx.launcher.ui.PickerItem
import com.layernx.launcher.ui.RolePickers
import com.layernx.launcher.ui.Surfaces
import com.layernx.launcher.ui.SystemBars
import com.layernx.launcher.ui.fitToWidth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The LayerNX home screen ("Zen" layout from the mockup).
 *
 * Keypad (only while the home screen is in front):
 * - 0–9, *, #   → dialer with that digit typed (ACTION_DIAL – nothing is ever called automatically)
 * - Call key     → phone app
 * - Camera key   → chosen camera app (fixes the stock key opening only the Emporia camera)
 * - Envelope key → messages app (WhatsApp when installed)
 * - Menu key     → customize
 * - D-pad / OK   → move between and open the visible buttons
 *
 * Header (right to left): ⚙ customize · 🔔 ringer (Ton → Vibration → Stumm) ·
 * ! close background apps · ⓘ notifications. "LAYERNX" hides itself when space runs out.
 */
class HomeActivity : Activity() {

    private lateinit var launcher: Launcher
    private lateinit var prefs: Prefs

    private lateinit var root: ScrollView
    private lateinit var brand: TextView
    private lateinit var gear: ImageButton
    private lateinit var ringer: ImageButton
    private lateinit var closeApps: ImageButton
    private lateinit var notifications: ImageButton
    private lateinit var badge: TextView
    private lateinit var banner: LinearLayout
    private lateinit var clock: TextClock
    private lateinit var date: TextView
    private lateinit var greeting: TextView
    private lateinit var allApps: LinearLayout
    private lateinit var dock: List<LinearLayout>

    /** Date across midnight / time changes, and the bell when the ringer is changed elsewhere. */
    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.RINGER_MODE_CHANGED_ACTION) updateRinger() else updateDate()
        }
    }

    private val notificationsListener = NotificationStore.Listener { updateBadge() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemBars.setup(window)
        setContentView(R.layout.activity_home)
        launcher = Launcher(this)
        prefs = launcher.prefs

        root = findViewById(R.id.home_root)
        brand = findViewById(R.id.brand)
        gear = findViewById(R.id.gear)
        ringer = findViewById(R.id.ringer)
        closeApps = findViewById(R.id.close_apps)
        notifications = findViewById(R.id.notifications)
        badge = findViewById(R.id.notifications_badge)
        banner = findViewById(R.id.default_banner)
        clock = findViewById(R.id.clock)
        date = findViewById(R.id.date)
        greeting = findViewById(R.id.greeting)
        allApps = findViewById(R.id.all_apps)
        dock = listOf(findViewById(R.id.dock_0), findViewById(R.id.dock_1), findViewById(R.id.dock_2))

        SystemBars.applyPadding(root)
        // Large numerals: sized in dp so a big system font scale cannot push the dock off screen.
        clock.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 80f)
        brand.text = brandText()

        gear.setOnClickListener { openSettings() }
        ringer.setOnClickListener { cycleRinger() }
        closeApps.setOnClickListener { closeBackgroundApps() }
        notifications.setOnClickListener { showNotifications() }
        findViewById<View>(R.id.header).addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> fitBrand() }
        banner.setOnClickListener { SystemActions.requestDefaultLauncher(this) }
        allApps.setOnClickListener { openApps() }
        dock.forEachIndexed { slot, button ->
            button.setOnClickListener { v -> launchRole(prefs.dockRole(slot), v) }
        }
        // Keypad users land on the primary dock button first.
        dock[0].isFocusedByDefault = true
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
        }
        // Protected system broadcasts only, so the receiver need not be exported.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(systemReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(systemReceiver, filter)
        }
        NotificationStore.addListener(notificationsListener)
        updateDate()
        updateRinger()
        updateBadge()
    }

    override fun onStop() {
        unregisterReceiver(systemReceiver)
        NotificationStore.removeListener(notificationsListener)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        AppRepository.refreshIfStale()
        NotificationAccess.reconnectIfNeeded(this)
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
        listOf(notifications, closeApps, ringer, gear).forEach { button ->
            button.background = Surfaces.interactive(this, 22f, oval = true) { Surfaces.oval(Palette.PANEL) }
            button.imageTintList = ColorStateList.valueOf(Palette.TEXT)
        }
        badge.background = Surfaces.rounded(this, accent, 8f)
        updateRinger()
        updateBadge()

        val isDefault = SystemActions.isDefaultLauncher(this)
        banner.visibility = if (isDefault) View.GONE else View.VISIBLE
        banner.background = Surfaces.interactive(this, 14f) {
            Surfaces.rounded(this, Palette.PANEL, 14f, accent, 1f)
        }
        findViewById<ImageView>(R.id.default_banner_icon).imageTintList = accentTint

        updateDate()

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

    /** Hides "LAYERNX" when the header buttons leave it too little room (small screen, big font). */
    private fun fitBrand() {
        val available = brand.width - brand.paddingLeft - brand.paddingRight
        if (available <= 0) return
        val fits = brand.paint.measureText(brand.text.toString()) <= available
        val wanted = if (fits) View.VISIBLE else View.INVISIBLE
        if (brand.visibility != wanted) brand.visibility = wanted
    }

    private fun updateRinger() {
        val state = Ringer.current(this)
        ringer.setImageResource(state.iconRes)
        ringer.contentDescription = getString(state.labelRes)
    }

    private fun cycleRinger() {
        when (val result = Ringer.cycle(this)) {
            is Ringer.Result.Changed -> {
                updateRinger()
                Toast.makeText(this, result.state.toastRes, Toast.LENGTH_SHORT).show()
            }
            Ringer.Result.NeedsDndAccess -> {
                Toast.makeText(this, R.string.ringer_needs_access, Toast.LENGTH_LONG).show()
                NotificationAccess.openDndAccessSettings(this)
            }
            Ringer.Result.Unavailable -> Toast.makeText(this, R.string.settings_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun closeBackgroundApps() {
        if (!closeApps.isEnabled) return
        closeApps.isEnabled = false
        Toast.makeText(this, R.string.close_apps_running, Toast.LENGTH_SHORT).show()
        val packages = AppRepository.apps.map { it.packageName }
        BackgroundApps.closeAll(this, packages) { freeMb ->
            closeApps.isEnabled = true
            if (!isDestroyed) {
                Toast.makeText(this, getString(R.string.close_apps_done, freeMb.toInt()), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateBadge() {
        val count = if (NotificationStore.isConnected) NotificationStore.items.size else 0
        badge.visibility = if (count > 0) View.VISIBLE else View.GONE
        badge.text = if (count > 99) "99+" else count.toString()
        notifications.contentDescription = if (count > 0) {
            getString(R.string.notifications_show) + ", " +
                resources.getQuantityString(R.plurals.notifications_count, count, count)
        } else {
            getString(R.string.notifications_show)
        }
    }

    private fun showNotifications() {
        val accent = prefs.accent
        val title = getString(R.string.notifications_title)
        if (!NotificationAccess.isGranted(this)) {
            Picker.show(
                this, accent, null, title,
                listOf(
                    PickerItem(
                        title = getString(R.string.notifications_access_title),
                        subtitle = getString(R.string.notifications_access_subtitle),
                        icon = getDrawable(R.drawable.ic_info),
                        showChevron = true,
                    ) { NotificationAccess.openListenerSettings(this) },
                ),
            )
            return
        }
        val timeFormat = android.text.format.DateFormat.getTimeFormat(this)
        val items = NotificationStore.items.map { item ->
            val app = AppRepository.findByPackage(item.packageName)
            val appLabel = app?.label ?: item.packageName
            val details = buildList {
                add(appLabel)
                add(timeFormat.format(Date(item.postTime)))
                item.text?.toString()?.takeIf { it.isNotBlank() }?.let { add(it) }
            }.joinToString(" · ")
            PickerItem(
                title = item.title?.toString()?.takeIf { it.isNotBlank() } ?: appLabel,
                subtitle = details,
                app = app,
                icon = if (app == null) notificationIcon(item) else null,
            ) { NotificationAccess.open(this, launcher, item) }
        }
        val clearable = NotificationStore.items.any { it.isClearable }
        Picker.show(
            this, accent, null, title, items,
            emptyText = getString(R.string.notifications_empty),
            secondaryLabel = if (clearable) getString(R.string.notifications_clear_all) else null,
            onSecondary = if (clearable) ({ NotificationsService.dismissAll() }) else null,
        )
    }

    private fun notificationIcon(item: NotificationStore.Item): android.graphics.drawable.Drawable? = try {
        item.smallIcon?.loadDrawable(this)
    } catch (e: RuntimeException) {
        null
    } ?: getDrawable(R.drawable.ic_info)

    /** "Montag, 9. November" – always German, like the rest of the launcher. */
    private fun updateDate() {
        date.text = SimpleDateFormat(DATE_PATTERN, Locale.GERMAN).format(Date())
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
        const val DATE_PATTERN = "EEEE, d. MMMM"
        val HANDLED_KEYS = setOf(
            KeyEvent.KEYCODE_CALL,
            KeyEvent.KEYCODE_CAMERA,
            KeyEvent.KEYCODE_ENVELOPE,
            KeyEvent.KEYCODE_EXPLORER,
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_BACK,
        )
    }
}
