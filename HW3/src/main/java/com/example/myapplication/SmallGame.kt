package com.example.myapplication

import java.io.Serializable


class SmallGame : GameBoard(rows = 4, cols = 4, winCondition = 3), Serializable {
    override fun getGameName() = "Small 3-in-a-row"
}

