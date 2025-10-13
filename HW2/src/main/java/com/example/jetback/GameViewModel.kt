package com.example.jetback;

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// ... other imports
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    companion object {
        private const val GAME_BOARD_KEY = "gameBoard"
    }

    // 2. Restore state from the handle on init
    private var gameBoard: GameBoard? = savedStateHandle[GAME_BOARD_KEY]
    private val _uiState = MutableStateFlow(
        if (gameBoard != null) {
            GamescreenUiState(
                board = gameBoard!!.board,
                currentPlayer = gameBoard!!.currentPlayer,
                gameActive = true, // Assume game was in progress
                gameStatus = GameStatus.InProgress,
                elapsedTime = 0L // Note: Timer state is not saved here yet
            )
        } else {
            GamescreenUiState() // Default initial state
        }
    )
    val uiState: StateFlow<GamescreenUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    init {
        // If the game was active upon recreation, restart the timer
        if (_uiState.value.gameActive) {
            startTimer()
        }
    }

    fun startNewGame(difficulty: Difficulty) {
        val newGameBoard = when (difficulty) {
            Difficulty.EASY -> SmallGame()
            Difficulty.MEDIUM -> MediumGame()
            Difficulty.HARD -> HardGame()
        }

        // 3. Save state to the handle when it changes
        savedStateHandle[GAME_BOARD_KEY] = newGameBoard
        gameBoard = newGameBoard

        _uiState.update {
            GamescreenUiState(
                board = newGameBoard.board,
                currentPlayer = newGameBoard.currentPlayer,
                gameActive = true,
                gameStatus = GameStatus.InProgress,
                elapsedTime = 0L
            )
        }
        startTimer()
    }

    fun placeToken(col: Int) {
        val board = gameBoard ?: return
        if (!_uiState.value.gameActive) return

        val row = board.placeToken(col) ?: return
        _uiState.update { currentState ->
            currentState.copy(board = board.board)
        }

        val lastMove = Pair(row, col)

        if (board.checkWinner(lastMove)) {
            _uiState.update { currentState ->
                currentState.copy(
                    board = board.board.deepCopy(),
                    gameActive = false,
                    gameStatus = GameStatus.Winner(board.currentPlayer)
                )
            }
        } else if (board.isBoardFull()) {
            _uiState.update { currentState ->
                currentState.copy(
                    board = board.board.deepCopy(),
                    gameActive = false,
                    gameStatus = GameStatus.Draw
                )
            }
        } else {
            board.switchPlayer()
            _uiState.update { currentState ->
                currentState.copy(
                    board = board.board.deepCopy(),
                    currentPlayer = board.currentPlayer
                )
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _uiState.update { state ->
                    if (state.gameActive) state.copy(elapsedTime = state.elapsedTime + 1)
                    else state
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}

inline fun <reified T> Array<Array<T>>.deepCopy(): Array<Array<T>> {
    return Array(size) { i -> this[i].copyOf() }
}