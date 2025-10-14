package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameScreen()
        }
    }
}


@Composable
fun GameScreen() {
    val gameState = rememberGameState()

    DisposableEffect(Unit) {
        onDispose { gameState.cleanUp() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE0F6E3))
            .padding(WindowInsets.safeDrawing.asPaddingValues()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        StatusText(
            elapsedTime = gameState.elapsedTime,
            currentPlayer = gameState.currentPlayer,
            gameStatus = gameState.gameStatus
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.weight(1f)
        ) {
            GameBoardComposable(
                board = gameState.board,
                gameActive = gameState.gameActive,
                onColumnClicked = { col -> gameState.placeToken(col) }
            )

            if (!gameState.gameActive && gameState.gameStatus != GameStatus.NotStarted) {
                Column(
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            shape = ButtonDefaults.shape
                        )
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Game Over",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DifficultyButtons(onStartGame = { diff -> gameState.startNewGame(diff) })
                }
            } else if (gameState.gameStatus == GameStatus.NotStarted) {
                DifficultyButtons(onStartGame = { diff -> gameState.startNewGame(diff) })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}


@Composable
fun GameBoardComposable(
    board: Array<Array<Player?>>,
    gameActive: Boolean,
    onColumnClicked: (Int) -> Unit
) {
    if (board.isEmpty() || board[0].isEmpty()) return

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(16.dp)
            .aspectRatio(board[0].size.toFloat() / board.size.toFloat())
    ) {
        val circleSize = maxWidth / board[0].size

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            board.forEach { rowArray ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowArray.forEachIndexed { colIndex, player ->
                        Box(
                            modifier = Modifier
                                .size(circleSize)
                                .clickable(
                                    enabled = gameActive,
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { onColumnClicked(colIndex) }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    color = when (player) {
                                        Player.RED -> Color.Red
                                        Player.BLUE -> Color.Blue
                                        null -> Color.LightGray
                                    },
                                    radius = (size.minDimension / 2.0f) * 0.8f
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun rememberGameState(): GameState {
    val scope = rememberCoroutineScope()
    val gameState = rememberSaveable {
        GameState()
    }

    gameState.coroutineScope = scope

    DisposableEffect(gameState.gameActive) {
        if (gameState.gameActive) {
            gameState.resumeTimer()
        }
        onDispose { }
    }

    return gameState
}


@Composable
fun StatusText(
    elapsedTime: Long,
    currentPlayer: Player,
    gameStatus: GameStatus
) {
    val minutes = elapsedTime / 60
    val seconds = elapsedTime % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)


    val statusText = when (gameStatus) {
        is GameStatus.NotStarted -> "Choose difficulty"
        is GameStatus.InProgress -> "${currentPlayer.name}'s turn"
        is GameStatus.Winner -> "${gameStatus.player.name} WON!"
        is GameStatus.Draw -> "Draw!"

    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = timeFormatted,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 8.dp)
        )

        Text(
            text = statusText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 8.dp)
        )
    }
}

@Composable
fun DifficultyButtons(onStartGame: (Difficulty) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { onStartGame(Difficulty.EASY) }) { Text("Easy") }
        Button(onClick = { onStartGame(Difficulty.MEDIUM) }) { Text("Medium") }
        Button(onClick = { onStartGame(Difficulty.HARD) }) { Text("Hard") }
    }
}