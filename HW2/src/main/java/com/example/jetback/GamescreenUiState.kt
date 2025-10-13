package com.example.jetback

// See on sinu andmeklassi eeldatav kuju
data class GamescreenUiState(
    // Tühi laud on hea vaikeväärtus
    val board: Array<Array<Player?>> = emptyArray(),
    // Punane alustab alati vaikimisi
    val currentPlayer: Player = Player.RED,
    // Mäng ei ole alguses aktiivne
    val gameActive: Boolean = false,
    // Alguses pole mäng veel alanud
    val gameStatus: GameStatus = GameStatus.NotStarted,
    // Alguses on aeg 0
    val elapsedTime: Long = 0L
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GamescreenUiState

        if (!board.contentDeepEquals(other.board)) return false
        if (currentPlayer != other.currentPlayer) return false
        if (gameActive != other.gameActive) return false
        if (gameStatus != other.gameStatus) return false
        if (elapsedTime != other.elapsedTime) return false

        return true
    }

    override fun hashCode(): Int {
        var result = board.contentDeepHashCode()
        result = 31 * result + currentPlayer.hashCode()
        result = 31 * result + gameActive.hashCode()
        result = 31 * result + gameStatus.hashCode()
        result = 31 * result + elapsedTime.hashCode()
        return result
    }
}

sealed interface GameStatus {
    data object NotStarted : GameStatus
    data object InProgress : GameStatus
    data class Winner(val player: Player) : GameStatus
    data object Draw : GameStatus
}