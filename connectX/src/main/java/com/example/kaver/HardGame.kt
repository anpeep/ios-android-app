package com.example.kaver

class HardGame : GameBoard(rows = 10, cols = 10, winCondition = 5) {
    override fun getGameName() = "Hard 5-in-a-row"
}