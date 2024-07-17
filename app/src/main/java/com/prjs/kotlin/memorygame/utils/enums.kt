package com.prjs.kotlin.memorygame.utils

enum class FlowStatus {
    Success,
    Error,
    HandleImages,
}

enum class BoardSize(val numCards: Int) {
    EASY(numCards = 8),
    EASY_2(numCards = 12),
    EASY_3(numCards = 16),
    MEDIUM(numCards = 20),
    HARD(numCards = 24);


    fun getWidth(): Int {
        return when (this) {
            EASY -> 2
            EASY_2 -> 3
            EASY_3 -> 4
            MEDIUM -> 4
            HARD -> 4
        }
    }

    fun getHeight(): Int {
        return numCards / getWidth()
    }

    fun getNumPairs(): Int {
        return numCards / 2
    }

    companion object {
        fun getByValue(value: Int) = entries.first {
            it.numCards == value
        }
    }
}