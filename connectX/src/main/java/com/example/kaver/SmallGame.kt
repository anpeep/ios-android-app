package com.example.kaver

class SmallGame : GameBoard(rows = 4, cols = 4, winCondition = 3) {
    override fun getGameName() = "Small 3-in-a-row"
}