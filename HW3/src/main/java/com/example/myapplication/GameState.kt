package com.example.myapplication

import android.os.Parcelable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
class GameState : Parcelable {
    @IgnoredOnParcel
    lateinit var coroutineScope: CoroutineScope

    @IgnoredOnParcel
    private var gameBoard: GameBoard? = null

    @IgnoredOnParcel
    private var timerJob: Job? = null
    var board by mutableStateOf(Array(6) { Array<Player?>(7) { null } })
    var currentPlayer by mutableStateOf(Player.RED)
    var gameActive by mutableStateOf(false)
    var gameStatus by mutableStateOf<GameStatus>(GameStatus.NotStarted)
    var elapsedTime by mutableStateOf(0L)


    fun startNewGame(difficulty: Difficulty) {

        val newGameBoard = when (difficulty) {
            Difficulty.EASY -> SmallGame()
            Difficulty.MEDIUM -> MediumGame()
            Difficulty.HARD -> HardGame()
        }
        this.gameBoard = newGameBoard


        board = newGameBoard.board.deepCopy()
        currentPlayer = newGameBoard.currentPlayer
        gameActive = true
        gameStatus = GameStatus.InProgress
        elapsedTime = 0L

        startTimer()
    }


    fun placeToken(col: Int) {
        val currentBoard = gameBoard ?: return
        if (!gameActive) return


        val row = currentBoard.placeToken(col)

        if (row != null) {

            board = currentBoard.board.deepCopy()


            if (currentBoard.checkWinner(Pair(row, col))) {
                gameStatus = GameStatus.Winner(currentBoard.currentPlayer)
                gameActive = false
                timerJob?.cancel()
            } else if (currentBoard.isBoardFull()) {
                gameStatus = GameStatus.Draw
                gameActive = false
                timerJob?.cancel()
            } else {

                currentBoard.switchPlayer()

                currentPlayer = currentBoard.currentPlayer
            }
        }
    }

    fun resumeTimer() {
        timerJob?.cancel()
        if (!gameActive) return

        val startTime = System.currentTimeMillis() - elapsedTime * 1000
        timerJob = coroutineScope.launch {
            while (isActive) {
                delay(1000)
                elapsedTime = (System.currentTimeMillis() - startTime) / 1000
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = coroutineScope.launch {
            while (isActive && gameActive) {
                delay(1000)
                elapsedTime++
            }
        }
    }

    fun cleanUp() {
        timerJob?.cancel()
    }
}

fun Array<Array<Player?>>.deepCopy(): Array<Array<Player?>> {
    return Array(size) { i -> this[i].clone() }
}

@Parcelize
enum class Difficulty : Parcelable {
    EASY, MEDIUM, HARD
}