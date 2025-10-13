package com.example.jetback

import java.io.Serializable

class MediumGame : GameBoard(rows = 6, cols = 7, winCondition = 4), Serializable {
    override fun getGameName() = "Medium 4-in-a-row"
}