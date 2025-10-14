package com.example.myapplication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable

abstract class GameBoard(
    val rows: Int,
    val cols: Int,
    val winCondition: Int
) : Serializable {

    var currentPlayer: Player = Player.RED
        private set

    val board = Array(rows) { arrayOfNulls<Player>(cols) }

    abstract fun getGameName(): String

    fun placeToken(col: Int): Int? {
        if (col < 0 || col >= cols) return null

        for (row in rows - 1 downTo 0) {
            if (board[row][col] == null) {
                board[row][col] = currentPlayer
                return row
            }
        }
        return null
    }

    fun switchPlayer() {
        currentPlayer = when (currentPlayer) {
            Player.RED -> Player.BLUE
            Player.BLUE -> Player.RED
        }
    }

    fun checkWinner(lastMove: Pair<Int, Int>): Boolean {
        val (row, col) = lastMove
        val player = board[row][col] ?: return false

        val directions = listOf(
            Pair(0, 1),
            Pair(1, 0),
            Pair(1, 1),
            Pair(1, -1)
        )

        return directions.any { (dr, dc) ->
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
        return board[0].all { it != null }
    }
}

@Parcelize
enum class Player : Parcelable {
    RED, BLUE
}
