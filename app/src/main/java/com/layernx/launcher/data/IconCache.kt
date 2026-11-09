package com.layernx.launcher.data

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import java.util.concurrent.Executors

/**
 * Loads app icons off the main thread and keeps them as small bitmaps
 * (≈14 KB each at 40dp/hdpi), so scrolling stays smooth on low-RAM phones.
 */
object IconCache {

    private const val MAX_BYTES = 6 * 1024 * 1024

    private val main = Handler(Looper.getMainLooper())
    private val executor = Executors.newFixedThreadPool(2) { r ->
        Thread(r, "layernx-icons").apply { priority = Thread.NORM_PRIORITY - 1 }
    }
    private val cache = object : LruCache<String, Bitmap>(MAX_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }
    private val pending = HashSet<String>()
    private val waiters = HashMap<String, MutableList<(Bitmap) -> Unit>>()

    fun get(entry: AppEntry, sizePx: Int): Bitmap? = cache.get(cacheKey(entry, sizePx))

    /** Delivers the icon on the main thread; immediately if it is cached. */
    fun load(entry: AppEntry, sizePx: Int, resources: Resources, onLoaded: (Bitmap) -> Unit) {
        val key = cacheKey(entry, sizePx)
        cache.get(key)?.let { onLoaded(it); return }
        waiters.getOrPut(key) { mutableListOf() } += onLoaded
        if (!pending.add(key)) return
        val density = resources.displayMetrics.densityDpi
        executor.execute {
            val bitmap = try {
                render(entry.info.getBadgedIcon(density), sizePx)
            } catch (e: RuntimeException) {
                null
            }
            main.post {
                pending -= key
                val callbacks = waiters.remove(key).orEmpty()
                if (bitmap != null) {
                    cache.put(key, bitmap)
                    callbacks.forEach { it(bitmap) }
                }
            }
        }
    }

    fun clear() = cache.evictAll()

    fun render(drawable: Drawable, sizePx: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        return bitmap
    }

    private fun cacheKey(entry: AppEntry, sizePx: Int) = "${entry.key}@$sizePx"
}
