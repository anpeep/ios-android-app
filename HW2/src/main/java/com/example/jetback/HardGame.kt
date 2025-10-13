package com.example.jetback

import java.io.Serializable

class HardGame : GameBoard(rows = 10, cols = 10, winCondition = 5), Serializable {
    override fun getGameName() = "Hard 5-in-a-row"
}