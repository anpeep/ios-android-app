package com.example.kaver

class MediumGame : GameBoard(rows = 6, cols = 7, winCondition = 4) {
    override fun getGameName() = "Medium 4-in-a-row"
}