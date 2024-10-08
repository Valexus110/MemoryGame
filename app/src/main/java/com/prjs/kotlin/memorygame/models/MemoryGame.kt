package com.prjs.kotlin.memorygame.models

import com.prjs.kotlin.memorygame.utils.BoardSize
import com.prjs.kotlin.memorygame.utils.DEFAULT_ICONS

class MemoryGame(private val boardSize: BoardSize, customImages: List<String>?) {

    val cards: List<MemoryCard> = if (customImages == null) {
        val chosenImages = DEFAULT_ICONS.shuffled().take(boardSize.getNumPairs())
        val randomizedImages = (chosenImages + chosenImages).shuffled()
        randomizedImages.map { MemoryCard(it) }
    } else {
        val randomizedImages = (customImages + customImages).shuffled()
        randomizedImages.map {
            MemoryCard(it.hashCode(), it)
        }
    }
    var numPairsFound = 0

    private var numCardFlips = 0
    private var indexOfSelectedCard: Int? = null

    fun flipCard(position: Int): Boolean {
        numCardFlips++
        val card = cards[position]
        var foundMatch = false
        if (indexOfSelectedCard == null) {
            restoresCards()
            indexOfSelectedCard = position
        } else {
            foundMatch = checkForMatch(indexOfSelectedCard ?: 0, position)
            indexOfSelectedCard = null
        }
        card.isFaceUp = !card.isFaceUp
        return foundMatch
    }

    private fun checkForMatch(position1: Int, position2: Int): Boolean {
        if (cards[position1].identifier != cards[position2].identifier) {
            return false
        }
        cards[position1].isMatched = true
        cards[position2].isMatched = true
        numPairsFound++
        return true

    }

    private fun restoresCards() {
        for (card in cards) {
            if (!card.isMatched) {
                card.isFaceUp = false
            }
        }
    }

    fun haveWonGame(): Boolean {
        return numPairsFound == boardSize.getNumPairs()
    }

    fun isCardFaceUp(position: Int): Boolean {
        return cards[position].isFaceUp
    }

    fun getNumMoves(): Int {
        return numCardFlips / 2
    }

}