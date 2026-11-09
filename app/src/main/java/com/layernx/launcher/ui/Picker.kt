package com.layernx.launcher.ui

import android.app.Activity
import android.app.Dialog
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import com.layernx.launcher.R
import com.layernx.launcher.core.Accent
import com.layernx.launcher.data.AppEntry
import com.layernx.launcher.data.IconCache

/** One option in a [Picker]. Either [app] (icon loaded asynchronously) or [icon] (tinted with the accent). */
class PickerItem(
    val title: CharSequence,
    val subtitle: CharSequence? = null,
    val icon: Drawable? = null,
    val app: AppEntry? = null,
    val checked: Boolean = false,
    val showChevron: Boolean = false,
    val onPick: () -> Unit,
)

/**
 * Full-screen LayerNX "shade" with a keypad-navigable list — used for every choice
 * in the launcher (standard apps, dock slots, app actions …).
 */
object Picker {

    fun show(
        activity: Activity,
        accent: Int,
        eyebrow: CharSequence?,
        title: CharSequence,
        items: List<PickerItem>,
        emptyText: CharSequence? = null,
        secondaryLabel: CharSequence? = null,
        onSecondary: (() -> Unit)? = null,
    ): Dialog? {
        if (activity.isFinishing || activity.isDestroyed) return null
        val dialog = Dialog(activity, R.style.Theme_LayerNX_Shade)
        // A dialog's content has no parent to resolve layout params against.
        @android.annotation.SuppressLint("InflateParams")
        val root = LayoutInflater.from(activity).inflate(R.layout.dialog_picker, null)
        dialog.setContentView(root)
        dialog.window?.let { window ->
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            SystemBars.setup(window)
        }
        SystemBars.applyPadding(root.findViewById(R.id.picker_root))

        root.findViewById<TextView>(R.id.picker_eyebrow).apply {
            text = eyebrow
            visibility = if (eyebrow.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
        root.findViewById<TextView>(R.id.picker_title).text = title

        val list = root.findViewById<ListView>(R.id.picker_list)
        list.adapter = Adapter(activity, accent, items)
        list.setOnItemClickListener { _, _, position, _ ->
            dialog.dismiss()
            items[position].onPick()
        }

        root.findViewById<TextView>(R.id.picker_empty).apply {
            text = emptyText
            visibility = if (items.isEmpty() && !emptyText.isNullOrEmpty()) View.VISIBLE else View.GONE
        }
        root.findViewById<Button>(R.id.picker_secondary).apply {
            if (secondaryLabel != null && onSecondary != null) {
                visibility = View.VISIBLE
                text = secondaryLabel
                background = Surfaces.outlinedButton(activity, 13f)
                setOnClickListener {
                    dialog.dismiss()
                    onSecondary()
                }
            }
        }

        root.findViewById<Button>(R.id.picker_close).apply {
            background = Surfaces.accentButton(activity, accent, 13f)
            setOnClickListener { dialog.dismiss() }
        }

        dialog.setOnShowListener {
            val checkedIndex = items.indexOfFirst { it.checked }
            list.requestFocus()
            if (checkedIndex >= 0) list.setSelection(checkedIndex)
        }
        dialog.show()
        return dialog
    }

    private class Adapter(
        private val activity: Activity,
        private val accent: Int,
        private val items: List<PickerItem>,
    ) : BaseAdapter() {

        private val inflater = LayoutInflater.from(activity)
        private val iconPx = activity.dp(32)
        private val tint = android.content.res.ColorStateList.valueOf(Accent.lighten(accent, 0.6f))
        private val checkTint = android.content.res.ColorStateList.valueOf(Accent.lighten(accent, 0.45f))
        private val mutedTint = android.content.res.ColorStateList.valueOf(Palette.MUTED)

        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: inflater.inflate(R.layout.item_row, parent, false).also {
                it.background = Surfaces.listRow(activity)
            }
            val item = items[position]
            val icon = view.findViewById<ImageView>(R.id.row_icon)
            val title = view.findViewById<TextView>(R.id.row_title)
            val subtitle = view.findViewById<TextView>(R.id.row_subtitle)
            val trailing = view.findViewById<ImageView>(R.id.row_trailing)

            title.text = item.title
            subtitle.text = item.subtitle
            subtitle.visibility = if (item.subtitle.isNullOrEmpty()) View.GONE else View.VISIBLE

            icon.tag = item
            val app = item.app
            when {
                app != null -> {
                    icon.imageTintList = null
                    icon.visibility = View.VISIBLE
                    val cached = IconCache.get(app, iconPx)
                    if (cached != null) {
                        icon.setImageBitmap(cached)
                    } else {
                        icon.setImageDrawable(null)
                        IconCache.load(app, iconPx, activity.resources) { bitmap ->
                            if (icon.tag === item) icon.setImageBitmap(bitmap)
                        }
                    }
                }
                item.icon != null -> {
                    icon.visibility = View.VISIBLE
                    icon.setImageDrawable(item.icon)
                    // Vector role icons are tinted; real app icons keep their colours.
                    icon.imageTintList = if (item.icon is android.graphics.drawable.VectorDrawable) tint else null
                }
                else -> {
                    icon.setImageDrawable(null)
                    icon.visibility = View.GONE
                }
            }

            when {
                item.checked -> {
                    trailing.visibility = View.VISIBLE
                    trailing.setImageResource(R.drawable.ic_check)
                    trailing.imageTintList = checkTint
                }
                item.showChevron -> {
                    trailing.visibility = View.VISIBLE
                    trailing.setImageResource(R.drawable.ic_chevron)
                    trailing.imageTintList = mutedTint
                }
                else -> trailing.visibility = View.INVISIBLE
            }
            view.contentDescription = buildString {
                append(item.title)
                if (!item.subtitle.isNullOrEmpty()) append(", ").append(item.subtitle)
                if (item.checked) append(", ").append(activity.getString(R.string.state_selected))
            }
            return view
        }
    }
}
