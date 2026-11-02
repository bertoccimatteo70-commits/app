package com.lifeos.data

/**
 * Small recursive-descent evaluator used by the calculator mini-app.
 *
 * Supports + - * / % ^, parentheses, unary minus and decimal numbers. It never
 * calls eval or a JS engine, so it works offline and cannot execute code.
 */
object Calculator {

    fun evaluate(expression: String): Double {
        val parser = Parser(expression.replace(" ", "").replace(",", "."))
        val value = parser.parseExpression()
        if (!parser.atEnd()) throw IllegalArgumentException("Espressione non valida")
        if (value.isNaN() || value.isInfinite()) throw IllegalArgumentException("Risultato non valido")
        return value
    }

    /** Formats a result without a trailing ".0" for whole numbers. */
    fun format(value: Double): String {
        if (value == value.toLong().toDouble() && kotlin.math.abs(value) < 1e15) {
            return value.toLong().toString()
        }
        val rounded = String.format(java.util.Locale.US, "%.10f", value).trimEnd('0').trimEnd('.')
        return rounded.ifEmpty { "0" }
    }

    private class Parser(private val source: String) {
        private var index = 0

        fun atEnd(): Boolean = index >= source.length

        fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                when (peek()) {
                    '+' -> { index++; value += parseTerm() }
                    '-' -> { index++; value -= parseTerm() }
                    else -> return value
                }
            }
        }

        private fun parseTerm(): Double {
            var value = parseFactor()
            while (true) {
                when (peek()) {
                    '*' -> { index++; value *= parseFactor() }
                    '/' -> {
                        index++
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw IllegalArgumentException("Divisione per zero")
                        value /= divisor
                    }
                    '%' -> {
                        index++
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw IllegalArgumentException("Divisione per zero")
                        value %= divisor
                    }
                    else -> return value
                }
            }
        }

        private fun parseFactor(): Double {
            var base = parseUnary()
            if (peek() == '^') {
                index++
                base = Math.pow(base, parseFactor())
            }
            return base
        }

        private fun parseUnary(): Double {
            if (peek() == '-') { index++; return -parseUnary() }
            if (peek() == '+') { index++; return parseUnary() }
            return parseAtom()
        }

        private fun parseAtom(): Double {
            if (peek() == '(') {
                index++
                val value = parseExpression()
                if (peek() != ')') throw IllegalArgumentException("Parentesi non bilanciate")
                index++
                return value
            }
            val start = index
            while (!atEnd() && (source[index].isDigit() || source[index] == '.')) index++
            if (start == index) throw IllegalArgumentException("Numero atteso")
            return source.substring(start, index).toDoubleOrNull()
                ?: throw IllegalArgumentException("Numero non valido")
        }

        private fun peek(): Char? = if (atEnd()) null else source[index]
    }
}
