package com.example.jetback

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            enableEdgeToEdge()
            GameScreen()
        }
    }
}

@Composable
fun GameScreen(
    gameViewModel: GameViewModel = viewModel()
) {
    val uiState: GamescreenUiState by gameViewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE0F6E3))
            // Kasutame safeDrawing, et vältida sisu sattumist süsteemiribade alla
            .padding(WindowInsets.safeDrawing.asPaddingValues()),
        horizontalAlignment = Alignment.CenterHorizontally,
        // Eemaldame keskse paigutuse, et staatuse tekst oleks üleval
        verticalArrangement = Arrangement.Top // <-- MUUDATUS
    ) {
        // Paigutame staatuse teksti ja taimeri üles
        Spacer(modifier = Modifier.height(32.dp))
        StatusText(uiState)
        Spacer(modifier = Modifier.height(16.dp))

        // See Box on peamine muudatus. See võimaldab elementidel kattuda.
        Box(
            // Joondame sisu (mängulaua ja nupud) keskele
            contentAlignment = Alignment.Center,
            // Anname Boxile ülejäänud vaba ruumi
            modifier = Modifier.weight(1f)
        ) {
            // Mängulaud joonistatakse esimesena (allapoole)
            GameBoardComposable(
                board = uiState.board,
                gameActive = uiState.gameActive,
                onColumnClicked = { colIndex ->
                    gameViewModel.placeToken(colIndex)
                }
            )

            // Nupud joonistatakse teise ja viimasena (kõige peale)
            // Neid kuvatakse ainult siis, kui mäng ei ole aktiivne.
            if (!uiState.gameActive) {
                // Lisame nuppudele poolläbipaistva tausta, et need paremini esile tuleksid
                Column(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), shape = ButtonDefaults.shape)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Game Over", // Lisatekst annab kasutajale konteksti
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DifficultyButtons(onStartGame = { difficulty ->
                        gameViewModel.startNewGame(difficulty)
                    })
                }
            }
        }

        // Lisame lõppu tühja ruumi, et sisu ei oleks päris ekraani allservas
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun StatusText(uiState: GamescreenUiState) { // <-- LISA SEE PARAMEETER
    val timeFormatted = remember(uiState.elapsedTime) {
        val minutes = uiState.elapsedTime / 60
        val seconds = uiState.elapsedTime % 60
        String.format("%02d:%02d", minutes, seconds)
    }
    val statusText =
        when (val status = uiState.gameStatus) { // Correct: access gameStatus from uiState
            is GameStatus.NotStarted -> "Choose difficulty"
            is GameStatus.InProgress -> "${uiState.currentPlayer} turn"
            is GameStatus.Winner -> "${status.player} WON!"
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
            fontWeight = FontWeight.Bold
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
                                ) {
                                    onColumnClicked(colIndex)
                                }
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
