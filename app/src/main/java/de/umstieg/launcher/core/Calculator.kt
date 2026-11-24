package de.umstieg.launcher.core

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.abs
import kotlin.math.pow

/** Einfacher Rechner für die Suche: + − × ÷ ^ %, Klammern, Komma oder Punkt als Dezimaltrenner. */
object Calculator {
    /** Nur Ausdrücke mit Ziffer UND Rechenzeichen (eine Zahl allein ist keine Rechnung). */
    fun looksLikeExpression(input: String): Boolean {
        val s = input.trim()
        return s.any { it.isDigit() } && s.drop(1).any { it in "+-−*×x/÷:^%" } &&
            s.all { it.isDigit() || it in "+-−*×x/÷:^%()., " }
    }

    fun evaluate(input: String): Double? {
        if (!looksLikeExpression(input)) return null
        return runCatching { Parser(input.replace(" ", "")).parse() }.getOrNull()?.takeIf { it.isFinite() }
    }

    /** Deutsche Schreibweise, höchstens 10 gültige Stellen. */
    fun format(value: Double): String {
        if (value == 0.0) return "0"
        if (abs(value) >= 1e15 || abs(value) < 1e-9) return value.toString().replace('.', ',')
        val bd = BigDecimal(value).round(MathContext(10)).stripTrailingZeros()
        return bd.toPlainString().replace('.', ',')
    }

    private class Parser(val s: String) {
        var pos = 0

        fun parse(): Double {
            val v = expr()
            require(pos == s.length)
            return v
        }

        private fun peek(): Char? = s.getOrNull(pos)

        private fun expr(): Double {
            var v = term()
            while (true) {
                v = when (peek()) {
                    '+' -> { pos++; v + term() }
                    '-', '−' -> { pos++; v - term() }
                    else -> return v
                }
            }
        }

        private fun term(): Double {
            var v = power()
            while (true) {
                v = when (peek()) {
                    '*', '×', 'x' -> { pos++; v * power() }
                    '/', '÷', ':' -> {
                        pos++
                        val d = power()
                        require(d != 0.0)
                        v / d
                    }
                    else -> return v
                }
            }
        }

        private fun power(): Double {
            val base = unary()
            return if (peek() == '^') {
                pos++
                base.pow(power())
            } else {
                base
            }
        }

        private fun unary(): Double = when (peek()) {
            '-', '−' -> { pos++; -unary() }
            '+' -> { pos++; unary() }
            else -> postfix()
        }

        private fun postfix(): Double {
            var v = atom()
            while (peek() == '%') {
                pos++
                v /= 100.0
            }
            return v
        }

        private fun atom(): Double {
            if (peek() == '(') {
                pos++
                val v = expr()
                require(peek() == ')')
                pos++
                return v
            }
            val start = pos
            while (peek()?.let { it.isDigit() || it == ',' || it == '.' } == true) pos++
            require(pos > start)
            val raw = s.substring(start, pos)
            // „1.234,5“ (Tausenderpunkt + Komma) und „1,5“ / „1.5“
            val normalized = if (raw.contains(',')) raw.replace(".", "").replace(',', '.') else raw
            return normalized.toDouble()
        }
    }
}
