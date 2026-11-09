package com.layernx.launcher.ui

import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import android.view.View
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import com.layernx.launcher.AppsActivity
import com.layernx.launcher.HomeActivity
import com.layernx.launcher.R
import com.layernx.launcher.SettingsActivity
import com.layernx.launcher.core.AppRole
import com.layernx.launcher.data.AppEntry
import com.layernx.launcher.data.Prefs
import com.layernx.launcher.data.RoleResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Emporia TOUCHsmart.2: 480×854 px at 240 dpi = 320×569 dp. Status bar (24 dp) and the
 * three-button navigation bar (50 dp) leave ≈ 497 dp for the launcher – that is what we render.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], qualifiers = "de-rDE-w320dp-h497dp-port-hdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LauncherUiTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        context.getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE).edit().clear().commit()
        FakeDevice.install()
    }

    @Test
    fun homeScreenShowsMockupStructure() {
        Prefs(context).userName = "Moritz"
        val activity = Robolectric.buildActivity(HomeActivity::class.java).setup().visible().get()
        FakeDevice.settle()

        val greeting = activity.findViewById<TextView>(R.id.greeting)
        assertEquals("Hallo, Moritz.", greeting.text.toString())
        val labels = listOf(R.id.dock_0, R.id.dock_1, R.id.dock_2).map {
            activity.findViewById<View>(it).findViewById<TextView>(R.id.dock_label).text.toString()
        }
        assertEquals(listOf("Telefon", "Nachrichten", "Kamera"), labels)

        // Always German, regardless of the system language.
        val date = activity.findViewById<TextView>(R.id.date).text.toString()
        val weekdays = "Montag|Dienstag|Mittwoch|Donnerstag|Freitag|Samstag|Sonntag"
        val months = "Januar|Februar|März|April|Mai|Juni|Juli|August|September|Oktober|November|Dezember"
        assertTrue(date, Regex("^($weekdays), \\d{1,2}\\. ($months)$").matches(date))

        // Everything must fit on the real screen without scrolling.
        val root = activity.findViewById<ScrollView>(R.id.home_root)
        val content = root.getChildAt(0)
        assertTrue("Home content ${content.height}px exceeds ${root.height}px", content.height <= root.height)
        FakeDevice.screenshot(activity.window.decorView, "home")
    }

    @Test
    fun longestNameStillFitsOnScreen() {
        Prefs(context).userName = "Maximilian-Alexand" // 18 characters = the input limit
        val activity = Robolectric.buildActivity(HomeActivity::class.java).setup().visible().get()
        FakeDevice.settle()
        val root = activity.findViewById<ScrollView>(R.id.home_root)
        val content = root.getChildAt(0)
        assertTrue("Home content ${content.height}px exceeds ${root.height}px", content.height <= root.height)
        FakeDevice.screenshot(activity.window.decorView, "home_long_name")
    }

    @Test
    fun homeScreenSurvivesLargeFontScale() {
        RuntimeEnvironment.setFontScale(1.3f)
        Prefs(context).userName = "Moritz"
        val activity = Robolectric.buildActivity(HomeActivity::class.java).setup().visible().get()
        FakeDevice.settle()
        val root = activity.findViewById<ScrollView>(R.id.home_root)
        val content = root.getChildAt(0)
        FakeDevice.screenshot(content, "home_fontscale_130", fullHeight = maxOf(content.height, root.height))
        // At 130 % the content may scroll, but the dock and soft keys must stay reachable.
        assertTrue(content.height < root.height * 2)
    }

    @Test
    fun keypadFocusShowsRing() {
        val activity = Robolectric.buildActivity(HomeActivity::class.java).setup().visible().get()
        FakeDevice.settle()
        val primary = activity.findViewById<View>(R.id.dock_0)
        // On the device a D-pad press leaves touch mode and focuses this view (focusedByDefault).
        // Robolectric has no ViewRootImpl input stages, so focus it directly.
        assertTrue(primary.isFocusedByDefault)
        primary.isFocusableInTouchMode = true
        assertTrue(primary.requestFocus())
        FakeDevice.settle(100)
        assertTrue(primary.isFocused)
        FakeDevice.screenshot(activity.window.decorView, "home_focus")
    }

    @Test
    fun digitKeyOpensDialerWithNumberAndNeverCalls() {
        val controller = Robolectric.buildActivity(HomeActivity::class.java).setup().visible()
        controller.get().dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_5))
        controller.get().dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_5))

        val started = shadowOf(controller.get()).nextStartedActivity
        assertNotNull(started)
        assertEquals(Intent.ACTION_DIAL, started.action)
        assertEquals("tel:5", started.data.toString())
        assertEquals("com.android.dialer", started.`package`)
    }

    @Test
    fun callKeyOpensAospDialerInsteadOfEmporiaApp() {
        val controller = Robolectric.buildActivity(HomeActivity::class.java).setup().visible()
        controller.get().dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CALL))

        val started = shadowOf(controller.get()).nextStartedActivity
        assertNotNull(started)
        assertEquals("com.android.dialer", started.component?.packageName)
    }

    @Test
    fun backKeyNeverLeavesHome() {
        val activity = Robolectric.buildActivity(HomeActivity::class.java).setup().visible().get()
        activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
        activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK))
        assertTrue(!activity.isFinishing)
    }

    @Test
    fun automaticStandardAppsAvoidEmporia() {
        val prefs = Prefs(context)
        val resolver = RoleResolver(context, prefs)
        assertEquals("org.mozilla.firefox", resolver.resolve(AppRole.BROWSER)?.packageName)
        assertEquals("com.android.dialer", resolver.resolve(AppRole.PHONE)?.packageName)
        assertEquals("com.mediatek.camera", resolver.resolve(AppRole.CAMERA)?.packageName)
        // "Nachrichten" means WhatsApp whenever it is installed – ahead of the SMS default.
        assertEquals("com.whatsapp", resolver.resolve(AppRole.MESSAGES)?.packageName)
        assertEquals(
            listOf("com.whatsapp", "com.emporia.mms"),
            resolver.rankedCandidates(AppRole.MESSAGES).map { it.packageName },
        )

        prefs.setRoleApp(AppRole.BROWSER, "com.emporia.browser")
        val manual = resolver.resolve(AppRole.BROWSER)
        assertEquals("com.emporia.browser", manual?.packageName)
        assertTrue(manual!!.isManual)

        prefs.setRoleApp(AppRole.BROWSER, "uninstalled.app")
        assertEquals("org.mozilla.firefox", resolver.resolve(AppRole.BROWSER)?.packageName)
    }

    @Test
    fun chosenWhatsAppOpensItsStartScreenNotAnEmptyConversation() {
        // Exactly what the user did: pick WhatsApp by hand for "Nachrichten".
        Prefs(context).setRoleApp(AppRole.MESSAGES, "com.whatsapp")
        val controller = Robolectric.buildActivity(HomeActivity::class.java).setup().visible()
        controller.get().findViewById<View>(R.id.dock_1).performClick()

        val started = shadowOf(controller.get()).nextStartedActivity
        assertNotNull("Nothing was started", started)
        assertEquals(Intent.ACTION_MAIN, started.action)
        assertTrue(started.hasCategory(Intent.CATEGORY_LAUNCHER))
        assertEquals("com.whatsapp", started.component?.packageName)
        assertTrue(started.component?.className != "com.whatsapp.Conversation")
        assertTrue(started.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun everyOfferedAppCanBeOpened() {
        val resolver = RoleResolver(context, Prefs(context))
        AppRole.entries.forEach { role ->
            resolver.rankedCandidates(role).forEach { candidate ->
                val intent = resolver.launchIntent(role, candidate.packageName)
                assertNotNull("${candidate.packageName} for $role", intent)
                assertTrue(
                    "${candidate.packageName} for $role would be opened with ${intent!!.action} ${intent.data}",
                    intent.data == null,
                )
            }
        }
    }

    @Test
    fun appListFiltersWithT9Keys() {
        val activity = Robolectric.buildActivity(AppsActivity::class.java).setup().visible().get()
        FakeDevice.settle()
        val list = activity.findViewById<ListView>(R.id.apps_list)
        assertEquals(FakeDevice.APPS.size, list.adapter.count)
        FakeDevice.screenshot(activity.window.decorView, "apps")

        // "347" = F-I-R
        listOf(KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_7).forEach {
            activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, it))
            activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, it))
        }
        FakeDevice.settle(50)
        val matches = (0 until list.adapter.count).map { (list.adapter.getItem(it) as AppEntry).label }
        // Prefix match first, then "Telegram" (t-e-l-E-G-R-a-m contains 3-4-7).
        assertEquals(listOf("Firefox", "Telegram"), matches)
        FakeDevice.screenshot(activity.window.decorView, "apps_t9")

        // Clear key (F1 on the Emporia keypad) removes one digit at a time.
        activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F1))
        assertEquals("34", activity.findViewById<TextView>(R.id.search).text.toString())
    }

    @Test
    fun hiddenAndEmporiaAppsCanBeRemovedFromList() {
        val prefs = Prefs(context)
        prefs.hideOemApps = true
        val activity = Robolectric.buildActivity(AppsActivity::class.java).setup().visible().get()
        val list = activity.findViewById<ListView>(R.id.apps_list)
        val labels = (0 until list.adapter.count).map { (list.adapter.getItem(it) as AppEntry).packageName }
        assertTrue(labels.none { it.startsWith("com.emporia.") })
        assertTrue("org.mozilla.firefox" in labels)
    }

    @Test
    fun settingsScreenRendersAllSections() {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java).setup().visible().get()
        FakeDevice.settle()
        val roles = activity.findViewById<android.view.ViewGroup>(R.id.roles_section)
        // 11 roles + 2 toggles + hidden apps
        assertEquals(AppRole.entries.size + 3, roles.childCount)
        val browserRow = roles.getChildAt(AppRole.BROWSER.ordinal)
        assertEquals(
            "Firefox · automatisch",
            browserRow.findViewById<TextView>(R.id.row_subtitle).text.toString(),
        )
        val root = activity.findViewById<ScrollView>(R.id.settings_root)
        val content = root.getChildAt(0)
        FakeDevice.screenshot(root, "settings_top")
        FakeDevice.screenshot(content, "settings_full", fullHeight = content.height)
    }

    @Test
    fun rolePickerListsCandidatesWithAutomaticFirst() {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java).setup().visible().get()
        val prefs = Prefs(activity)
        RolePickers.chooseAppForRole(activity, prefs, RoleResolver(activity, prefs), AppRole.BROWSER) {}
        FakeDevice.settle()
        val dialog = org.robolectric.shadows.ShadowDialog.getLatestDialog()
        assertNotNull(dialog)
        val list = dialog.findViewById<ListView>(R.id.picker_list)
        val titles = (0 until list.adapter.count).map { (list.adapter.getItem(it) as PickerItem).title.toString() }
        assertEquals(listOf("Automatisch", "Firefox", "Browser", "Andere App wählen …"), titles)
        FakeDevice.screenshot(dialog.window!!.decorView, "picker_browser")
    }
}
