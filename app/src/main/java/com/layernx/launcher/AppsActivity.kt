package com.layernx.launcher

import android.app.Activity
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.layernx.launcher.core.AppRole
import com.layernx.launcher.core.RoleRanking
import com.layernx.launcher.core.T9
import com.layernx.launcher.data.AppEntry
import com.layernx.launcher.data.AppRepository
import com.layernx.launcher.data.IconCache
import com.layernx.launcher.data.Prefs
import com.layernx.launcher.system.Launcher
import com.layernx.launcher.ui.Palette
import com.layernx.launcher.ui.Picker
import com.layernx.launcher.ui.PickerItem
import com.layernx.launcher.ui.RolePickers
import com.layernx.launcher.ui.Surfaces
import com.layernx.launcher.ui.SystemBars
import com.layernx.launcher.ui.dp

/**
 * "Alle Apps". Keypad: digits filter by T9, the clear key deletes a digit, BACK clears the
 * search and then closes, MENU opens the actions for the highlighted app.
 */
class AppsActivity : Activity(), AppRepository.Listener {

    private lateinit var launcher: Launcher
    private lateinit var prefs: Prefs
    private lateinit var list: ListView
    private lateinit var search: EditText
    private lateinit var empty: TextView
    private lateinit var count: TextView
    private lateinit var adapter: AppsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemBars.setup(window)
        setContentView(R.layout.activity_apps)
        launcher = Launcher(this)
        prefs = launcher.prefs

        SystemBars.applyPadding(findViewById(R.id.apps_root))
        list = findViewById(R.id.apps_list)
        search = findViewById(R.id.search)
        empty = findViewById(R.id.apps_empty)
        count = findViewById(R.id.apps_count)
        adapter = AppsAdapter()

        search.background = Surfaces.input(this)
        search.setCompoundDrawablesRelative(
            getDrawable(R.drawable.ic_search)?.mutate()?.apply {
                setTint(Palette.MUTED)
                setBounds(0, 0, dp(18), dp(18))
            },
            null,
            null,
            null,
        )
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = applyFilter()
        })
        search.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                adapter.items.firstOrNull()?.let { launcher.launch(it, null) }
                true
            } else {
                false
            }
        }

        list.adapter = adapter
        list.emptyView = empty
        list.onItemClickListener = AdapterView.OnItemClickListener { _, view, position, _ ->
            adapter.items.getOrNull(position)?.let { launcher.launch(it, view) }
        }
        list.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, position, _ ->
            adapter.items.getOrNull(position)?.let { showActions(it) }
            true
        }

        findViewById<Button>(R.id.apps_back).apply {
            background = Surfaces.accentButton(this@AppsActivity, prefs.accent, 13f)
            setOnClickListener { finish() }
        }

        AppRepository.addListener(this)
        applyFilter()
        list.requestFocus()
    }

    override fun onResume() {
        super.onResume()
        AppRepository.refreshIfStale()
        applyFilter()
    }

    override fun onDestroy() {
        AppRepository.removeListener(this)
        super.onDestroy()
    }

    override fun onAppsChanged(apps: List<AppEntry>) = applyFilter()

    private fun applyFilter() {
        val query = search.text?.toString().orEmpty().trim()
        val hidden = prefs.hiddenApps
        val hideOem = prefs.hideOemApps
        val visible = AppRepository.apps.filter { entry ->
            entry.key !in hidden && !(hideOem && RoleRanking.isOemPackage(entry.packageName))
        }
        adapter.items = if (query.isEmpty()) {
            visible
        } else {
            visible
                .map { it to T9.rank(it.label, query) }
                .filter { it.second != T9.NO_MATCH }
                .sortedBy { it.second } // stable: keeps alphabetical order within a rank
                .map { it.first }
        }
        adapter.notifyDataSetChanged()
        count.text = resources.getQuantityString(R.plurals.apps_count, visible.size, visible.size)
        empty.setText(if (AppRepository.isLoaded) R.string.apps_empty else R.string.apps_loading)
        if (query.isNotEmpty() && adapter.count > 0 && !list.isInTouchMode) list.setSelection(0)
    }

    private fun showActions(entry: AppEntry) {
        val items = listOf(
            PickerItem(getString(R.string.app_action_open), icon = getDrawable(R.drawable.ic_chevron)) {
                launcher.launch(entry, null)
            },
            PickerItem(getString(R.string.app_action_set_default), icon = getDrawable(R.drawable.ic_home), showChevron = true) {
                chooseRoleFor(entry)
            },
            PickerItem(getString(R.string.app_action_info), icon = getDrawable(R.drawable.ic_info)) {
                launcher.openAppInfo(entry)
            },
            PickerItem(getString(R.string.app_action_hide), icon = getDrawable(R.drawable.ic_hidden)) {
                prefs.setHidden(entry.key, true)
                applyFilter()
                Toast.makeText(this, getString(R.string.app_hidden_toast, entry.label), Toast.LENGTH_LONG).show()
            },
        )
        Picker.show(this, prefs.accent, null, entry.label, items)
    }

    private fun chooseRoleFor(entry: AppEntry) {
        val current = AppRole.entries.firstOrNull { prefs.roleApp(it) == entry.packageName }
        RolePickers.chooseRole(this, prefs.accent, entry.label, getString(R.string.choose_role_title), current) { role ->
            prefs.setRoleApp(role, entry.packageName)
            Toast.makeText(
                this,
                getString(R.string.app_default_toast, entry.label, getString(role.labelRes)),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (search.hasFocus()) return super.onKeyDown(keyCode, event)
        val digit = when (keyCode) {
            in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> '0' + (keyCode - KeyEvent.KEYCODE_0)
            else -> null
        }
        when {
            digit != null -> search.append(digit.toString())
            // The keypad's clear key (Linux KEY_BACKSPACE) is mapped to F1 in mtk-kpd.kl.
            keyCode == KeyEvent.KEYCODE_DEL || keyCode == KeyEvent.KEYCODE_F1 -> {
                val text = search.text
                if (text.isNullOrEmpty()) return super.onKeyDown(keyCode, event)
                text.delete(text.length - 1, text.length)
            }
            keyCode == KeyEvent.KEYCODE_MENU -> {
                val selected = list.selectedItemPosition
                adapter.items.getOrNull(selected)?.let { showActions(it) } ?: return super.onKeyDown(keyCode, event)
            }
            else -> return super.onKeyDown(keyCode, event)
        }
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!search.text.isNullOrEmpty()) {
            search.text = null
            list.requestFocus()
            return
        }
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    private fun hideKeyboard() {
        getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(search.windowToken, 0)
        list.requestFocus()
    }

    private inner class AppsAdapter : BaseAdapter() {
        var items: List<AppEntry> = emptyList()
        private val inflater: LayoutInflater = LayoutInflater.from(this@AppsActivity)
        private val iconPx = dp(32)
        private val chevronTint = ColorStateList.valueOf(Palette.MUTED)

        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun hasStableIds() = false

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: inflater.inflate(R.layout.item_row, parent, false).also {
                it.background = Surfaces.listRow(this@AppsActivity)
                it.findViewById<View>(R.id.row_subtitle).visibility = View.GONE
                it.findViewById<ImageView>(R.id.row_trailing).apply {
                    setImageResource(R.drawable.ic_chevron)
                    imageTintList = chevronTint
                }
            }
            val entry = items[position]
            view.findViewById<TextView>(R.id.row_title).text = entry.label
            view.contentDescription = entry.label
            val icon = view.findViewById<ImageView>(R.id.row_icon)
            icon.tag = entry.key
            val cached = IconCache.get(entry, iconPx)
            if (cached != null) {
                icon.setImageBitmap(cached)
            } else {
                icon.setImageDrawable(null)
                IconCache.load(entry, iconPx, resources) { bitmap ->
                    if (icon.tag == entry.key) icon.setImageBitmap(bitmap)
                }
            }
            return view
        }
    }
}
