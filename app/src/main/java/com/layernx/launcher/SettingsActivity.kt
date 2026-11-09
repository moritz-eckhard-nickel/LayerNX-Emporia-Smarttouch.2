package com.layernx.launcher

import android.app.Activity
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.layernx.launcher.core.Accent
import com.layernx.launcher.core.AppRole
import com.layernx.launcher.data.AppRepository
import com.layernx.launcher.data.Prefs
import com.layernx.launcher.data.RoleResolver
import com.layernx.launcher.system.SystemActions
import com.layernx.launcher.ui.Palette
import com.layernx.launcher.ui.Picker
import com.layernx.launcher.ui.PickerItem
import com.layernx.launcher.ui.RolePickers
import com.layernx.launcher.ui.Surfaces
import com.layernx.launcher.ui.SystemBars
import com.layernx.launcher.ui.applyAccent
import com.layernx.launcher.ui.dp

/** "Anpassen": name, accent, dock, standard apps, Emporia handling and system shortcuts. */
class SettingsActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var resolver: RoleResolver
    private lateinit var inflater: LayoutInflater

    private lateinit var nameField: EditText
    private lateinit var swatches: LinearLayout
    private lateinit var dockSection: LinearLayout
    private lateinit var rolesSection: LinearLayout
    private lateinit var systemSection: LinearLayout
    private lateinit var aboutSection: LinearLayout
    private lateinit var done: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemBars.setup(window)
        setContentView(R.layout.activity_settings)
        prefs = Prefs(this)
        resolver = RoleResolver(this, prefs)
        inflater = LayoutInflater.from(this)

        SystemBars.applyPadding(findViewById(R.id.settings_root))
        nameField = findViewById(R.id.name)
        swatches = findViewById(R.id.swatches)
        dockSection = findViewById(R.id.dock_section)
        rolesSection = findViewById(R.id.roles_section)
        systemSection = findViewById(R.id.system_section)
        aboutSection = findViewById(R.id.about_section)
        done = findViewById(R.id.done)

        nameField.background = Surfaces.input(this)
        nameField.setText(prefs.userName)
        nameField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                prefs.userName = s?.toString().orEmpty()
            }
        })
        nameField.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(v.windowToken, 0)
                v.clearFocus()
                true
            } else {
                false
            }
        }
        done.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        AppRepository.refreshIfStale()
        render()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SystemActions.REQUEST_HOME_ROLE && !SystemActions.isDefaultLauncher(this)) {
            SystemActions.openHomeSettings(this)
        }
        render()
    }

    private fun render() {
        val accent = prefs.accent
        done.background = Surfaces.accentButton(this, accent, 13f)
        renderSwatches(accent)
        renderDock()
        renderRoles()
        renderSystem()
        renderAbout()
    }

    // region Accent

    private fun renderSwatches(accent: Int) {
        swatches.removeAllViews()
        val names = listOf(
            R.string.accent_violet, R.string.accent_blue, R.string.accent_green,
            R.string.accent_red, R.string.accent_pink, R.string.accent_teal,
        )
        Accent.PRESETS.forEachIndexed { index, color ->
            if (index > 0) {
                swatches.addView(Space(this), LinearLayout.LayoutParams(0, 1, 1f))
            }
            val selected = color == accent
            val swatch = ImageView(this).apply {
                background = Surfaces.interactive(this@SettingsActivity, 22f, oval = true) { Surfaces.oval(color) }
                isFocusable = true
                isClickable = true
                scaleType = ImageView.ScaleType.CENTER
                if (selected) {
                    setImageResource(R.drawable.ic_check)
                    imageTintList = ColorStateList.valueOf(android.graphics.Color.WHITE)
                }
                val name = getString(names[index])
                contentDescription = if (selected) getString(R.string.accent_selected, name) else name
                setOnClickListener {
                    prefs.accent = color
                    render()
                    // Keep keypad focus on the swatch that was just chosen.
                    swatches.post { swatches.findViewWithTag<View>(color)?.requestFocus() }
                }
                tag = color
            }
            swatches.addView(swatch, LinearLayout.LayoutParams(dp(40), dp(40)))
        }
    }

    // endregion

    // region Rows

    private fun addRow(
        container: ViewGroup,
        iconRes: Int?,
        title: CharSequence,
        subtitle: CharSequence?,
        onClick: () -> Unit,
    ): View {
        val row = inflater.inflate(R.layout.item_row, container, false)
        row.background = Surfaces.settingsRow(this)
        row.isFocusable = true
        row.isClickable = true
        val icon = row.findViewById<ImageView>(R.id.row_icon)
        if (iconRes != null) {
            icon.setImageResource(iconRes)
            icon.imageTintList = ColorStateList.valueOf(Accent.lighten(prefs.accent, 0.6f))
            icon.layoutParams = icon.layoutParams.apply { width = dp(24); height = dp(24) }
        } else {
            icon.visibility = View.GONE
        }
        row.findViewById<TextView>(R.id.row_title).text = title
        row.findViewById<TextView>(R.id.row_subtitle).apply {
            text = subtitle
            visibility = if (subtitle.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
        row.findViewById<ImageView>(R.id.row_trailing).apply {
            setImageResource(R.drawable.ic_chevron)
            imageTintList = ColorStateList.valueOf(Palette.MUTED)
        }
        row.contentDescription = if (subtitle.isNullOrEmpty()) title else "$title, $subtitle"
        row.setOnClickListener { onClick() }
        container.addView(row, spacedParams())
        return row
    }

    private fun addToggle(
        container: ViewGroup,
        title: CharSequence,
        subtitle: CharSequence,
        checked: Boolean,
        onChange: (Boolean) -> Unit,
    ) {
        val row = inflater.inflate(R.layout.item_toggle, container, false)
        row.background = Surfaces.settingsRow(this)
        row.isClickable = true
        row.findViewById<TextView>(R.id.toggle_title).text = title
        row.findViewById<TextView>(R.id.toggle_subtitle).text = subtitle
        val switch = row.findViewById<Switch>(R.id.toggle_switch)
        switch.applyAccent(prefs.accent)
        switch.isChecked = checked
        fun describe() {
            row.contentDescription = "$title, $subtitle, " +
                getString(if (switch.isChecked) R.string.toggle_on else R.string.toggle_off)
        }
        describe()
        row.setOnClickListener {
            switch.isChecked = !switch.isChecked
            describe()
            onChange(switch.isChecked)
        }
        container.addView(row, spacedParams())
    }

    private fun spacedParams() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply { bottomMargin = dp(7) }

    // endregion

    // region Sections

    private fun renderDock(): Unit = keepFocus(dockSection) {
        dockSection.removeAllViews()
        for (slot in 0 until Prefs.DOCK_SIZE) {
            val role = prefs.dockRole(slot)
            val title = getString(R.string.dock_slot, slot + 1)
            addRow(dockSection, role.iconRes, title, getString(role.labelRes)) {
                RolePickers.chooseRole(this, prefs.accent, getString(R.string.section_dock), title, role) { picked ->
                    prefs.setDockRole(slot, picked)
                    render()
                }
            }
        }
    }

    private fun renderRoles(): Unit = keepFocus(rolesSection) {
        rolesSection.removeAllViews()
        AppRole.entries.forEach { role ->
            val resolution = resolver.resolve(role)
            val subtitle = when {
                resolution == null -> getString(R.string.role_value_none)
                resolution.isManual -> resolution.label
                else -> getString(R.string.role_value_auto, resolution.label)
            }
            addRow(rolesSection, role.iconRes, getString(role.labelRes), subtitle) {
                RolePickers.chooseAppForRole(this, prefs, resolver, role) { render() }
            }
        }
        addToggle(
            rolesSection,
            getString(R.string.avoid_oem_title),
            getString(R.string.avoid_oem_subtitle),
            prefs.avoidOemApps,
        ) {
            prefs.avoidOemApps = it
            renderRoles()
        }
        addToggle(
            rolesSection,
            getString(R.string.hide_oem_title),
            getString(R.string.hide_oem_subtitle),
            prefs.hideOemApps,
        ) { prefs.hideOemApps = it }

        val hidden = prefs.hiddenApps
        val hiddenSubtitle = if (hidden.isEmpty()) {
            getString(R.string.hidden_apps_none)
        } else {
            resources.getQuantityString(R.plurals.hidden_apps_count, hidden.size, hidden.size)
        }
        addRow(rolesSection, R.drawable.ic_hidden, getString(R.string.hidden_apps_title), hiddenSubtitle) {
            showHiddenApps()
        }
    }

    private fun showHiddenApps() {
        val hidden = prefs.hiddenApps
        if (hidden.isEmpty()) {
            Toast.makeText(this, R.string.hidden_apps_none, Toast.LENGTH_SHORT).show()
            return
        }
        val items = hidden.map { key ->
            val entry = AppRepository.findByKey(key)
            PickerItem(
                title = entry?.label ?: key.substringBefore('/'),
                app = entry,
                icon = if (entry == null) getDrawable(R.drawable.ic_apps) else null,
            ) {
                prefs.setHidden(key, false)
                render()
            }
        }.sortedBy { it.title.toString().lowercase() }
        Picker.show(this, prefs.accent, getString(R.string.hidden_apps_title), getString(R.string.hidden_apps_picker_title), items)
    }

    private fun renderSystem(): Unit = keepFocus(systemSection) {
        systemSection.removeAllViews()
        val isDefault = SystemActions.isDefaultLauncher(this)
        addRow(
            systemSection,
            R.drawable.ic_home,
            getString(R.string.system_default_launcher),
            getString(if (isDefault) R.string.system_default_launcher_active else R.string.system_default_launcher_inactive),
        ) {
            if (isDefault) SystemActions.openHomeSettings(this) else SystemActions.requestDefaultLauncher(this)
        }
        addRow(
            systemSection,
            R.drawable.ic_apps,
            getString(R.string.system_default_apps),
            getString(R.string.system_default_apps_subtitle),
        ) { SystemActions.openDefaultAppsSettings(this) }
        addRow(systemSection, R.drawable.ic_gear, getString(R.string.system_settings), null) {
            SystemActions.openSystemSettings(this)
        }
        addRow(
            systemSection,
            R.drawable.ic_undo,
            getString(R.string.system_rollback),
            getString(R.string.system_rollback_subtitle),
        ) {
            Toast.makeText(this, R.string.rollback_toast, Toast.LENGTH_LONG).show()
            SystemActions.openHomeSettings(this)
        }
    }

    private fun renderAbout(): Unit = keepFocus(aboutSection) {
        aboutSection.removeAllViews()
        val row = addRow(
            aboutSection,
            R.drawable.ic_info,
            getString(R.string.about_version, BuildConfig.VERSION_NAME),
            getString(R.string.about_authors),
        ) {}
        row.findViewById<View>(R.id.row_trailing).visibility = View.INVISIBLE
        row.isClickable = false
    }

    /** Rebuilding a section must not throw keypad users back to the top of the screen. */
    private inline fun keepFocus(section: ViewGroup, rebuild: () -> Unit) {
        val focused = section.findFocus()
        val index = if (focused != null) {
            (0 until section.childCount).firstOrNull { section.getChildAt(it) === focused }
        } else {
            null
        }
        rebuild()
        if (index != null && !section.isInTouchMode) {
            section.getChildAt(index.coerceAtMost(section.childCount - 1))?.requestFocus()
        }
    }

    // endregion
}
