package com.layernx.launcher.core

import java.text.Normalizer
import java.util.Locale

/**
 * Search helper for keypad phones.
 *
 * A query made only of digits is interpreted as a T9 key sequence
 * ("5276" matches "Karte" and "Last.fm"), any other query as plain text.
 * Matching is case- and accent-insensitive ("o" matches "Ö").
 */
object T9 {

    /** Lower rank = better match. [NO_MATCH] means the label does not match. */
    const val NO_MATCH = -1

    private val KEYS: Map<Char, Char> = buildMap {
        val layout = listOf("abc", "def", "ghi", "jkl", "mno", "pqrs", "tuv", "wxyz")
        layout.forEachIndexed { index, letters ->
            letters.forEach { put(it, '2' + index) }
        }
    }

    private val COMBINING_MARKS = Regex("\\p{Mn}+")

    /** Lower-cases [text], strips diacritics and expands ligatures (ß → ss). */
    fun fold(text: String): String {
        val lowered = text.lowercase(Locale.ROOT)
            .replace("ß", "ss")
            .replace("æ", "ae")
            .replace("œ", "oe")
            .replace("ø", "o")
            .replace("ł", "l")
        return COMBINING_MARKS.replace(Normalizer.normalize(lowered, Normalizer.Form.NFD), "")
    }

    /** Converts [text] to its keypad digits; anything that is not a letter or digit becomes a space. */
    fun encode(text: String): String = buildString {
        for (c in fold(text)) {
            append(
                when {
                    c in '0'..'9' -> c
                    else -> KEYS[c] ?: ' '
                },
            )
        }
    }

    fun isDigitQuery(query: String): Boolean = query.isNotEmpty() && query.all { it in '0'..'9' }

    /**
     * Ranks [label] against [query]:
     * 0 = whole label starts with the query, 1 = a word starts with it, 2 = contained somewhere.
     */
    fun rank(label: String, query: String): Int {
        val q = query.trim()
        if (q.isEmpty()) return 0
        return if (isDigitQuery(q)) {
            rankIn(encode(label), q, allowInfix = q.length >= 2)
        } else {
            val folded = fold(q).replace(Regex("\\s+"), " ")
            if (folded.isEmpty()) 0 else rankIn(fold(label), folded, allowInfix = true)
        }
    }

    private fun rankIn(haystack: String, needle: String, allowInfix: Boolean): Int {
        val compact = haystack.replace(" ", "")
        val words = haystack.split(' ', '.', '-', '_').filter { it.isNotEmpty() }
        return when {
            haystack.startsWith(needle) || compact.startsWith(needle.replace(" ", "")) -> 0
            words.any { it.startsWith(needle) } -> 1
            allowInfix && (haystack.contains(needle) || compact.contains(needle.replace(" ", ""))) -> 2
            else -> NO_MATCH
        }
    }

    fun matches(label: String, query: String): Boolean = rank(label, query) != NO_MATCH
}
