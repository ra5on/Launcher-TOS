package de.umstieg.launcher.core

import java.text.Normalizer
import kotlin.math.max
import kotlin.math.min

/** Normalisierung für die Suche: Kleinbuchstaben, Umlaute/ß, Akzente, Satzzeichen → Leerzeichen. */
object TextNormalizer {
    /** Ergebnis mit Abbildung jedes normalisierten Zeichens auf seinen Index im Original. */
    class Normalized(val text: String, val sourceIndex: IntArray)

    /** [expand] = true: ä → ae, ö → oe, ü → ue; sonst ä → a. ß wird immer ss. */
    fun normalize(s: String, expand: Boolean): Normalized {
        val sb = StringBuilder()
        val map = ArrayList<Int>()
        fun put(ch: Char, i: Int) {
            if (ch == ' ' && (sb.isEmpty() || sb.last() == ' ')) return
            sb.append(ch)
            map.add(i)
        }
        s.forEachIndexed { i, raw ->
            val c = raw.lowercaseChar()
            when {
                c == 'ß' -> { put('s', i); put('s', i) }
                expand && c == 'ä' -> { put('a', i); put('e', i) }
                expand && c == 'ö' -> { put('o', i); put('e', i) }
                expand && c == 'ü' -> { put('u', i); put('e', i) }
                c.isLetterOrDigit() -> {
                    val base = Normalizer.normalize(c.toString(), Normalizer.Form.NFD)
                        .filter { Character.getType(it) != Character.NON_SPACING_MARK.toInt() }
                    if (base.isEmpty()) put(c, i) else base.forEach { put(it, i) }
                }
                else -> put(' ', i)
            }
        }
        while (sb.isNotEmpty() && sb.last() == ' ') {
            sb.setLength(sb.length - 1)
            map.removeAt(map.lastIndex)
        }
        return Normalized(sb.toString(), map.toIntArray())
    }
}

/** Ein Suchtreffer mit Punktzahl und hervorzuhebenden Bereichen im Original-Text. */
data class Ranked<T>(val item: T, val score: Int, val highlight: List<IntRange>, val similar: Boolean)

/**
 * Unscharfe Suche: exakt > Präfix > Wortanfang > Anfangsbuchstaben > Teilstring > Tippfehler
 * (Damerau-Levenshtein, erlaubt 0/1/2 je nach Länge und Einstellung) > Buchstabenfolge.
 */
object FuzzySearch {
    private class Match(val score: Int, val ranges: List<IntRange>, val similar: Boolean)

    /** Erlaubte Tippfehler für eine Suchlänge. */
    fun allowedTypos(length: Int, fuzziness: SearchFuzziness): Int {
        val base = when {
            length <= 3 -> 0
            length <= 6 -> 1
            else -> 2
        }
        return when (fuzziness) {
            SearchFuzziness.STRICT -> max(0, base - 1)
            SearchFuzziness.NORMAL -> base
            SearchFuzziness.GENEROUS -> if (length >= 3) base + 1 else base
        }
    }

    /** Damerau-Levenshtein (optimale Zeichenkettenausrichtung). */
    fun distance(a: String, b: String): Int {
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                d[i][j] = min(min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    d[i][j] = min(d[i][j], d[i - 2][j - 2] + 1)
                }
            }
        }
        return d[a.length][b.length]
    }

    /** Bewertet [label] für [query]; null = kein Treffer. */
    fun score(label: String, query: String, fuzziness: SearchFuzziness = SearchFuzziness.NORMAL): Ranked<String>? {
        val best = listOf(true, false).mapNotNull { expand ->
            val l = TextNormalizer.normalize(label, expand)
            val q = TextNormalizer.normalize(query, expand).text
            if (q.isEmpty() || l.text.isEmpty()) null else match(l, q, fuzziness)
        }.maxByOrNull { it.score } ?: return null
        return Ranked(label, best.score, best.ranges, best.similar)
    }

    fun <T> rank(
        items: List<T>,
        label: (T) -> String,
        query: String,
        fuzziness: SearchFuzziness = SearchFuzziness.NORMAL,
        boost: (T) -> Int = { 0 },
    ): List<Ranked<T>> = items.mapNotNull { item ->
        score(label(item), query, fuzziness)?.let { Ranked(item, it.score + boost(item), it.highlight, it.similar) }
    }.sortedWith(compareByDescending<Ranked<T>> { it.score }.thenBy { label(it.item).lowercase() })

    private fun match(l: TextNormalizer.Normalized, q: String, fuzziness: SearchFuzziness): Match? {
        val t = l.text
        fun range(from: Int, len: Int): IntRange = l.sourceIndex[from]..l.sourceIndex[from + len - 1]
        val words = wordStarts(t)

        if (t == q) return Match(1000, listOf(range(0, q.length)), false)
        if (t.startsWith(q)) return Match(900 - min(99, t.length - q.length), listOf(range(0, q.length)), false)
        words.forEachIndexed { wi, start ->
            if (t.startsWith(q, start)) return Match(800 - min(50, wi), listOf(range(start, q.length)), false)
        }
        if (q.length >= 2 && words.size >= q.length) {
            val initials = words.map { t[it] }.joinToString("")
            if (initials.startsWith(q)) return Match(700, words.take(q.length).map { range(it, 1) }, false)
        }
        val idx = t.indexOf(q)
        if (idx >= 0) return Match(600 - min(99, idx), listOf(range(idx, q.length)), false)

        val allowed = allowedTypos(q.length, fuzziness)
        if (allowed > 0) {
            var bestD = Int.MAX_VALUE
            var bestRange: IntRange? = null
            for (start in words) {
                val word = t.substring(start).substringBefore(' ')
                for (len in max(1, q.length - 1)..min(word.length, q.length + 1)) {
                    val d = distance(q, word.substring(0, len))
                    if (d < bestD) {
                        bestD = d
                        bestRange = range(start, len)
                    }
                }
            }
            if (bestD <= allowed && bestRange != null) return Match(500 - 100 * bestD, listOf(bestRange), true)
        }

        if (fuzziness != SearchFuzziness.STRICT && q.length >= 2) {
            for (start in words) {
                if (t[start] != q[0]) continue
                val ranges = mutableListOf<IntRange>()
                var qi = 0
                var i = start
                while (i < t.length && qi < q.length) {
                    if (t[i] == q[qi]) {
                        ranges += range(i, 1)
                        qi++
                    }
                    i++
                }
                if (qi == q.length) return Match(300, ranges, true)
            }
        }
        return null
    }

    private fun wordStarts(t: String): List<Int> =
        t.indices.filter { i -> t[i] != ' ' && (i == 0 || t[i - 1] == ' ') }
}
