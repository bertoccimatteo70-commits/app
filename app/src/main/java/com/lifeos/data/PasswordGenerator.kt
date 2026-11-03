package com.lifeos.data

import kotlin.random.Random

/**
 * Generates random passwords for the vault. The alphabet excludes visually
 * ambiguous characters (l, I, O, 1, 0) so a generated password can be typed
 * from a paper copy without confusion.
 */
object PasswordGenerator {

    const val MIN_LENGTH = 8
    const val MAX_LENGTH = 64

    private const val LOWER = "abcdefghijkmnopqrstuvwxyz"
    private const val UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    private const val DIGITS = "23456789"
    private const val SYMBOLS = "!@#\$%&*?+-_=."

    /**
     * Builds a password of [length] characters containing at least one character
     * from every enabled group. The result is shuffled so the guaranteed
     * characters are not always in the same positions.
     */
    fun generate(
        length: Int = 20,
        useUpper: Boolean = true,
        useDigits: Boolean = true,
        useSymbols: Boolean = true,
        random: Random = Random.Default
    ): String {
        val groups = buildList {
            add(LOWER)
            if (useUpper) add(UPPER)
            if (useDigits) add(DIGITS)
            if (useSymbols) add(SYMBOLS)
        }
        val size = length.coerceIn(MIN_LENGTH, MAX_LENGTH)
        val alphabet = groups.joinToString("")
        val characters = ArrayList<Char>(size)

        // One character per group first, so every enabled class is present.
        groups.forEach { group -> characters.add(group[random.nextInt(group.length)]) }
        while (characters.size < size) {
            characters.add(alphabet[random.nextInt(alphabet.length)])
        }
        characters.shuffle(random)
        return characters.joinToString("")
    }
}
