package com.example.kaver

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val allGrids: List<GridLayout> by lazy {
        listOf(
            findViewById(R.id.gridLayoutSmall),
            findViewById(R.id.gridLayoutMedium),
            findViewById(R.id.gridLayoutLarge)
        )
    }
    private lateinit var activeGrid: GridLayout
    private lateinit var difficultyButtons: List<Button>
    private lateinit var timerText: TextView
    private lateinit var timerService: TimerService
    private var isBound = false
    private lateinit var turnText: TextView
    private lateinit var currentGame: GameBoard
    private var gameActive = false
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as TimerService.TimerBinder
            timerService = binder.service
            isBound = true

            lifecycleScope.launch {
                timerService.timeFlow.collect { time ->
                    timerText.text = time
                }
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    companion object {
        private const val CELL_SIZE_DP = 80
        private const val CELL_MARGIN_DP = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gamescreen)

        timerText = findViewById(R.id.timerText)
        turnText = findViewById(R.id.turn)
        difficultyButtons = listOf(
            findViewById(R.id.buttonEasy),
            findViewById(R.id.buttonMedium),
            findViewById(R.id.buttonHard)
        )
        setupDifficultyButtons()

        if (savedInstanceState != null) {
            restoreGameState(savedInstanceState)
        } else {
            Log.d("MainActivity", "No saved instance state, setting up initial view.")
            updateTurnText(null)
        }
    }

    private fun restoreGameState(savedInstanceState: Bundle) {
        Log.d("MainActivity", "Restoring from savedInstanceState")

        gameActive = savedInstanceState.getBoolean("GAME_ACTIVE", false)
        timerText.text = savedInstanceState.getString("TIMER_TEXT", "00:00")

        if (!gameActive) {
            updateTurnText(null)
            allGrids.forEach { it.visibility = View.GONE }
            difficultyButtons.forEach { it.visibility = View.VISIBLE }
            return
        }
        currentGame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            savedInstanceState.getSerializable("CURRENT_GAME", GameBoard::class.java)
        } else {
            @Suppress("DEPRECATION")
            savedInstanceState.getSerializable("CURRENT_GAME") as? GameBoard
        } ?: return
        val gridId = when (currentGame) {
            is SmallGame -> R.id.gridLayoutSmall
            is MediumGame -> R.id.gridLayoutMedium
            is HardGame -> R.id.gridLayoutLarge
            else -> throw IllegalStateException("Unknown game type")
        }
        allGrids.forEach { it.visibility = View.GONE }
        activeGrid = findViewById(gridId)
        activeGrid.visibility = View.VISIBLE
        difficultyButtons.forEach { it.visibility = View.GONE }
        createBoard(activeGrid, currentGame)
        updateBoardUI(activeGrid, currentGame)
        updateTurnText(currentGame.currentPlayer)

        Log.d("MainActivity", "Re-binding to TimerService after restoring active game.")
        Intent(this, TimerService::class.java).also { intent ->
            bindService(intent, connection, BIND_AUTO_CREATE)
        }
    }

    private fun setupDifficultyButtons() {
        difficultyButtons[0].setOnClickListener {
            startNewGame(SmallGame(), R.id.gridLayoutSmall)
        }
        difficultyButtons[1].setOnClickListener {
            startNewGame(MediumGame(), R.id.gridLayoutMedium)
        }
        difficultyButtons[2].setOnClickListener {
            startNewGame(HardGame(), R.id.gridLayoutLarge)
        }
    }

    private fun startNewGame(game: GameBoard, gridId: Int) {
        if (gameActive) return

        Intent(this, TimerService::class.java).also { intent ->
            intent.action = TimerService.ACTION_START_FRESH
            startService(intent)
        }

        gameActive = true
        currentGame = game
        Log.d("Game", "Started ${game.getGameName()} (${game.rows}x${game.cols})")

        difficultyButtons.forEach { it.visibility = View.GONE }
        allGrids.forEach { it.visibility = View.GONE }
        activeGrid = findViewById(gridId)

        activeGrid.visibility = View.VISIBLE
        createBoard(activeGrid, game)
        updateTurnText(game.currentPlayer)
        Intent(this, TimerService::class.java).also { intent ->
            bindService(intent, connection, BIND_AUTO_CREATE)
        }
    }

    private fun getPlayerColor(player: GameBoard.Companion.Player?): Int {
        return when (player) {
            GameBoard.Companion.Player.RED -> R.color.red
            GameBoard.Companion.Player.BLUE -> R.color.blue
            else -> R.color.darkGreen
        }
    }

    fun updateTurnText(player: GameBoard.Companion.Player?) {
        val textResId = when (player) {
            GameBoard.Companion.Player.RED -> R.string.red_turn
            GameBoard.Companion.Player.BLUE -> R.string.blue_turn
            null -> R.string.select_difficulty
        }
        val colorResId = getPlayerColor(player)

        turnText.setText(textResId)  // ContextCompat for colors
        turnText.setTextColor(ContextCompat.getColor(this, colorResId))
    }

    private fun updateBoardUI(grid: GridLayout, game: GameBoard) {
        for (r in 0 until game.rows) {
            for (c in 0 until game.cols) {
                val player = game.board[r][c]
                val index = r * game.cols + c
                val btn = grid.getChildAt(index) as? ToggleButton
                val color = if (player != null) {
                    ContextCompat.getColor(this, getPlayerColor(player))
                } else {
                    Color.LTGRAY
                }
                btn?.setBackgroundColor(color)
            }
        }
    }

    private fun createBoard(grid: GridLayout, game: GameBoard) {
        grid.removeAllViews()
        grid.rowCount = game.rows
        grid.columnCount = game.cols

        for (r in 0 until game.rows) {
            for (c in 0 until game.cols) {
                val button = ToggleButton(this).apply {
                    id = View.generateViewId()
                    text = ""; textOn = ""; textOff = ""
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = CELL_SIZE_DP
                        height = CELL_SIZE_DP
                        rowSpec = GridLayout.spec(r, 1, 1f)
                        columnSpec = GridLayout.spec(c, 1, 1f)
                        setMargins(CELL_MARGIN_DP, CELL_MARGIN_DP, CELL_MARGIN_DP, CELL_MARGIN_DP)
                    }
                    setBackgroundColor(Color.LTGRAY)
                }

                button.setOnClickListener {
                    it.takeIf { gameActive }?.let {
                        handlePlayerMove(c, game)
                    }
                }
                grid.addView(button)
            }
        }
    }

    private fun handlePlayerMove(column: Int, game: GameBoard) {
        val row = game.placeToken(column)  // row index or 0

        if (row != null) {
            updateBoardUI(activeGrid, game)
            checkGameState(game, Pair(row, column))
        } else {
            Toast.makeText(this, "Full", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkGameState(game: GameBoard, lastMove: Pair<Int, Int>) {
        val winner = game.checkWinner(lastMove)

        if (winner) {
            Toast.makeText(this, "${game.currentPlayer.name} won!", Toast.LENGTH_LONG).show()
            endGame()
        } else if (game.isBoardFull()) {
            Toast.makeText(this, "Draw!", Toast.LENGTH_LONG).show()
            endGame()
        } else {
            game.switchPlayer()
            updateTurnText(game.currentPlayer)
        }
    }

    private fun endGame() {
        gameActive = false
        updateTurnText(null)
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
        Log.d("MainActivity", "TimerService stopped.")
        allGrids.forEach { it.visibility = View.GONE }
        difficultyButtons.forEach { it.visibility = View.VISIBLE }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d("MainActivity", "onSaveInstanceState called, gameActive is $gameActive")
        outState.putBoolean("GAME_ACTIVE", gameActive)
        outState.putBoolean("GAME_ACTIVE", gameActive)
        outState.putString("TIMER_TEXT", timerText.text.toString())
        if (this::currentGame.isInitialized) {
            outState.putSerializable("CURRENT_GAME", currentGame)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            Log.d("MainActivity", "onStop called, unbinding from service.")
            unbindService(connection)
            isBound = false
        }
    }

}