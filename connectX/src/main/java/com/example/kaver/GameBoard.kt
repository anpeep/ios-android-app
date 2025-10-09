package com.example.kaver

abstract class GameBoard(
    val rows: Int,
    val cols: Int,
    val winCondition: Int
) {
    enum class Player { RED, YELLOW }

    var currentPlayer: Player = Player.RED
    val board = Array(rows) { Array<Player?>(cols) { null } }

    abstract fun getGameName(): String

    // Place a token (gravity rule)
    fun placeToken(col: Int): Boolean {
        if (col < 0 || col >= cols) return false

        for (row in rows - 1 downTo 0) {
            if (board[row][col] == null) {
                board[row][col] = currentPlayer
                return true
            }
        }
        return false
    }

    fun switchPlayer() {
        currentPlayer = if (currentPlayer == Player.RED) Player.YELLOW else Player.RED
    }

    fun checkWinner(): Boolean {
        val player = currentPlayer
        return checkHorizontal(player) ||
                checkVertical(player) ||
                checkDiagonalDownRight(player) ||
                checkDiagonalUpRight(player)
    }

    private fun checkHorizontal(player: Player): Boolean {
        for (row in 0 until rows) {
            var count = 0
            for (col in 0 until cols) {
                if (board[row][col] == player) {
                    count++
                    if (count >= winCondition) return true
                } else count = 0
            }
        }
        return false
    }

    private fun checkVertical(player: Player): Boolean {
        for (col in 0 until cols) {
            var count = 0
            for (row in 0 until rows) {
                if (board[row][col] == player) {
                    count++
                    if (count >= winCondition) return true
                } else count = 0
            }
        }
        return false
    }

    private fun checkDiagonalDownRight(player: Player): Boolean {
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                var count = 0
                for (i in 0 until winCondition) {
                    if (row + i < rows && col + i < cols && board[row + i][col + i] == player) {
                        count++
                        if (count >= winCondition) return true
                    } else break
                }
            }
        }
        return false
    }

    private fun checkDiagonalUpRight(player: Player): Boolean {
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                var count = 0
                for (i in 0 until winCondition) {
                    if (row - i >= 0 && col + i < cols && board[row - i][col + i] == player) {
                        count++
                        if (count >= winCondition) return true
                    } else break
                }
            }
        }
        return false
    }
    fun isBoardFull(): Boolean {
        return board.all { row -> row.all { it != null } }
    }

}
