package io.runtime.mcumgr.ble.util

/**
 * Helper class for managing a counter which rotates between 0 and a max value.
 * Equivalent to unsigned int overflow. This class is not thread safe.
 */
internal class RotatingCounter(private val max: Int) {

    private var value = 0

    fun getAndRotate(): Int {
        val tmp = value
        rotate()
        return tmp
    }

    fun rotate() {
        value = value.rotate()
    }

    /**
     * Create a list of rotation values between the new value (excluded) and
     * the expected value (one rotation above the current [value]).
     *
     * E.g. given a [max] of 255, current [value] of 254, and [newValue] of 3,
     * the returned list would be: {255, 0, 1, 2}
     *
     * If the new value equals the expected value, return the
     */
    fun rotationalDifference(newValue: Int): List<Int>? {
        val expected = value
        if (newValue == expected) {
            return null
        }
        val difference: MutableList<Int> = mutableListOf()
        var i = expected
        while (i != newValue) {
            difference.add(i)
            i = i.rotate()
        }
        return difference
    }

    private fun Int.rotate(): Int {
        return if (this == max) {
            0
        } else {
            this + 1
        }
    }
}
