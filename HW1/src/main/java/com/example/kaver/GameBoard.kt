package com.example.kaver

import java.io.Serializable

abstract class GameBoard(
    val rows: Int,
    val cols: Int,
    val winCondition: Int
) : Serializable {  // because we want game to continue if we rotate
    companion object {
        enum class Player { RED, BLUE }  // no constants, only magic
    }

    var currentPlayer: Player = Player.RED
        private set  // immutable

    val board = Array(rows) { arrayOfNulls<Player>(cols) }

    abstract fun getGameName(): String

    fun placeToken(col: Int): Int? {
        if (col < 0 || col >= cols) return null

        for (row in rows - 1 downTo 0) {
            if (board[row][col] == null) {
                board[row][col] = currentPlayer
                return row  // control if we have a winner
            }
        }
        return null // full column
    }

    fun switchPlayer() {
        currentPlayer = when (currentPlayer) {
            Player.RED -> Player.BLUE
            Player.BLUE -> Player.RED
        }
    }

    fun checkWinner(lastMove: Pair<Int, Int>): Boolean {
        val (row, col) = lastMove
        val player = board[row][col] ?: return false // must have player

        val directions = listOf(
            Pair(0, 1),   // right
            Pair(1, 0),   // down
            Pair(1, 1),   // down-right
            Pair(1, -1)   // down-left
        )

        return directions.any { (dr, dc) -> // one way + opposite way + middle button
            val count = 1 + countInDirection(row, col, dr, dc, player) + countInDirection(
                row,
                col,
                -dr,
                -dc,
                player
            )
            count >= winCondition
        }
    }

    private fun countInDirection(
        startRow: Int,
        startCol: Int,
        dRow: Int,
        dCol: Int,
        player: Player
    ): Int {
        var count = 0
        var r = startRow + dRow
        var c = startCol + dCol
        while (r in 0 until rows && c in 0 until cols && board[r][c] == player) {
            count++
            r += dRow
            c += dCol
        }
        return count
    }

    fun isBoardFull(): Boolean {
        return board[0].all { it != null }  // just uppest row
    }
}

class SmallGame : GameBoard(rows = 4, cols = 4, winCondition = 3), Serializable {
    override fun getGameName() = "Small 3-in-a-row"
}

class HardGame : GameBoard(rows = 10, cols = 10, winCondition = 5), Serializable {
    override fun getGameName() = "Hard 5-in-a-row"
}

class MediumGame : GameBoard(rows = 6, cols = 7, winCondition = 4), Serializable {
    override fun getGameName() = "Medium 4-in-a-row"
}
