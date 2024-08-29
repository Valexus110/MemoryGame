package com.prjs.kotlin.memorygame.models

import com.prjs.kotlin.memorygame.utils.BoardSize
import org.junit.Assert.*
import org.junit.Test

class MemoryGameTest {
    @Test
    fun `flip card return true`() {
        val boardSize = BoardSize.EASY
        val memory = MemoryGame(boardSize, null)
        memory.flipCard(0)
        val result = memory.flipCard(1)
        assertTrue(result)
    }
}