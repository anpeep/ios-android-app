package com.example.myapplication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
sealed class GameStatus : Parcelable {
    @Parcelize
    data object NotStarted : GameStatus()

    @Parcelize
    data object InProgress : GameStatus()

    @Parcelize
    data class Winner(val player: Player) : GameStatus()

    @Parcelize
    data object Draw : GameStatus()
}
