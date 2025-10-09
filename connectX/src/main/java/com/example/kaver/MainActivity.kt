package com.example.kaver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

//    var mutable variable
// val constant variable
// konstruktori muutujasse saab private sisse kirjutada, koodi ei saa sinna kirjutad
// peab konstruktorisse init {} või mitu paneme
// keyword constructor primaryga kutsud
// companion object
// saab olla null String?
// puudub ternary, kõik tagastab midagi mitte ? True : False
// let kui bl on väärtus
// let ja it tagastavad nagu return
// elvis kui ei olnud nii ?: s
// with(person { ei pea person.name = asja ja viimase rea väärtus tagastatakse returnita. muidu return
// apply muudab this.parameeter otse välja kui ennem tehtud var
// also ei muuda tagastatavat objekti aga saab välja kutsuda
// takeif kui tingimus vastab tõele
// activity on 1 ekraanivaade, peab manifestis deklareerima
// üks acticty Main
// oncreate UI.
// new pole
// startActivityFOrResult
// createChooser vali rakendus kuidas sõnumit saata
// intent filter mida ma tahan vastu võtta, mis data, mis liiki sõnumid, mis kategoorias
// START_STICKY peab taustal tööle jääma kui mingi error ja START_REDELIVER_INTENT
// onPausis registerRecifer logimine kordistub LocalBroadcastManager sest kõik instantsid saavad sõnumi, mitte panna onResume või onPaus ära võtta
// activity vajutad nupu läheb taustateenus mille peale hakkab saatma sõnumit. nupust käima ja nupust seisma. Broadcastimisega infovahetus. saadab kuupäeva kellaaega taustateenusest. add new service alt.

// kolm levelit (easy, med, hard) ehk 3 incude tagi. includeis peab üle kirjutama android:layot height ja width, match parent?
// selleks et rotatida, on res kataloogis ./layout
// kui xmliss ainult 1 asi siis failinime järgi leiab
// kui süsteemile jätta scaling siis kaotab resulatsiooni.
// res/layout_xlarge_land/my_layout.xml
// kui kõik toimub ühe layouti pealt siis pole vaja teha seda konfigutatsiooni ja pole katoloogi vaja
// pole vaja teha muutumatut teksti composableiks. jetbackis paned construktori ja recreatid sama konstruktori uue instantsiga
// @composable ei tagasta midagi. teda saab kutsuda teise composable poolt
// column ja row ja box on composable function ja sinna sisse saad panna composable elements
// modifier tähendab et update. Modifier.clip(circleShape) või Modifier.align(bottom)
// modifier saad sa ka kuju ja värvi disainida

import android.content.*
import android.widget.*


class MainActivity : AppCompatActivity() {

    private lateinit var gridSmall: GridLayout
    private lateinit var gridMedium: GridLayout
    private lateinit var gridLarge: GridLayout

    private lateinit var buttonEasy: Button
    private lateinit var buttonMedium: Button
    private lateinit var buttonHard: Button
    private lateinit var timerText: TextView
    private lateinit var broadcastReceiver: BroadcastReceiver
    private lateinit var turnText: TextView
    private lateinit var currentGame: GameBoard
    private var gameActive = false //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gamescreen)

        timerText = findViewById(R.id.timerText)
        turnText = findViewById(R.id.whosturn)
        // Register service time updates (if you use DemoService)
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == C.IntentBackgroundServiceTime) {
                    val time = intent.getStringExtra(C.IntentBackgroundServiceTimePayload)
                    timerText.text = time
                }
            }
        }
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastReceiver, IntentFilter(C.IntentBackgroundServiceTime))

        // Find all grids and buttons
        gridSmall = findViewById(R.id.gridLayoutSmall)
        gridMedium = findViewById(R.id.gridLayoutMedium)
        gridLarge = findViewById(R.id.gridLayoutLarge)

        buttonEasy = findViewById(R.id.buttonEasy)
        buttonMedium = findViewById(R.id.buttonMedium)
        buttonHard = findViewById(R.id.buttonHard)

        // Hide all boards at start
        gridSmall.visibility = View.GONE
        gridMedium.visibility = View.GONE
        gridLarge.visibility = View.GONE
        updateTurnText(null)

        // === Difficulty Buttons ===
        buttonEasy.setOnClickListener {
            startNewGame(SmallGame(), gridSmall)
        }

        buttonMedium.setOnClickListener {
            startNewGame(MediumGame(), gridMedium)
        }

        buttonHard.setOnClickListener {
            startNewGame(HardGame(), gridLarge)
        }
    }

    private fun startNewGame(game: GameBoard, grid: GridLayout) {
        if (gameActive) return // 🔒 ignore clicks if game already active

        gameActive = true
        currentGame = game
        Log.d("Game", "Started ${game.getGameName()} (${game.rows}x${game.cols})")

        // 🔹 Hide difficulty buttons
        buttonEasy.visibility = View.GONE
        buttonMedium.visibility = View.GONE
        buttonHard.visibility = View.GONE

        // 🔹 Show the selected board
        showBoard(grid, game)
        updateTurnText(game.currentPlayer)

        startService(Intent(this, DemoService::class.java))
    }

    private fun showBoard(grid: GridLayout, game: GameBoard) {
        // Hide other boards
        gridSmall.visibility = View.GONE
        gridMedium.visibility = View.GONE
        gridLarge.visibility = View.GONE

        // Show only the selected one
        grid.visibility = View.VISIBLE

        createBoard(grid, game)
    }
    fun updateTurnText(player: GameBoard.Player?) {
        when (player) {
            GameBoard.Player.RED -> {
                turnText.setText(R.string.red_turn)
                turnText.setTextColor(Color.rgb(244, 154, 194))
            }
            GameBoard.Player.YELLOW -> {
                turnText.setText(R.string.blue_turn)
                turnText.setTextColor(Color.rgb(45,58,110))
            }
            null -> {
                turnText.setText(R.string.select_difficulty)
                turnText.setTextColor(Color.DKGRAY)
        }

    }}

    private fun updateBoardUI(
        grid: GridLayout,
        game: GameBoard,
        buttons: Array<Array<ToggleButton?>>
    ) {
        for (r in 0 until game.rows) {
            for (c in 0 until game.cols) {
                val player = game.board[r][c]
                val btn = buttons[r][c]
                when (player) {
                    GameBoard.Player.RED -> btn?.setBackgroundColor(Color.rgb(45,58,110))
                    GameBoard.Player.YELLOW -> btn?.setBackgroundColor(Color.rgb(244, 154, 194))
                    else -> btn?.setBackgroundColor(Color.LTGRAY)
                }
            }
        }
    }

    private fun createBoard(grid: GridLayout, game: GameBoard) {
        grid.removeAllViews()
        grid.rowCount = game.rows
        grid.columnCount = game.cols

        val cellButtons = Array(game.rows) { arrayOfNulls<ToggleButton>(game.cols) }

        for (r in 0 until game.rows) {
            for (c in 0 until game.cols) {
                val button = ToggleButton(this).apply {
                    id = View.generateViewId()
                    text = ""
                    textOn = ""
                    textOff = ""
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 100
                        height = 100
                        rowSpec = GridLayout.spec(r, 1, 1f)
                        columnSpec = GridLayout.spec(c, 1, 1f)
                        setMargins(2, 2, 2, 2)
                    }
                    setBackgroundColor(Color.LTGRAY)
                }

                button.setOnClickListener {
                    if (!gameActive) return@setOnClickListener
                    val col = c
                    val placed = game.placeToken(col)

                    if (placed) {
                        updateBoardUI(grid, game, cellButtons)

                        // Check for winner or draw
                        if (game.checkWinner()) {
                            Toast.makeText(this, "${game.currentPlayer.name} wins!", Toast.LENGTH_LONG).show()
                            endGame()
                            updateTurnText(null)

                            stopService(Intent(this, DemoService::class.java))
                        } else if (game.isBoardFull()) {
                            Toast.makeText(this, "It's a draw!", Toast.LENGTH_LONG).show()
                            endGame()
                            updateTurnText(null)
                            stopService(Intent(this, DemoService::class.java))

                        } else {

                            game.switchPlayer()
                            updateTurnText(game.currentPlayer)
                        }
                    } else {
                        Toast.makeText(this, "Column full!", Toast.LENGTH_SHORT).show()
                    }
                }

                grid.addView(button)
                cellButtons[r][c] = button
            }
        }
    }
    private fun endGame() {
        gameActive = false
        updateTurnText(null)

        // 🔹 Hide all grids
        gridSmall.visibility = View.GONE
        gridMedium.visibility = View.GONE
        gridLarge.visibility = View.GONE

        // 🔹 Show difficulty buttons again
        buttonEasy.visibility = View.VISIBLE
        buttonMedium.visibility = View.VISIBLE
        buttonHard.visibility = View.VISIBLE
    }
}

//
//class MainActivity : AppCompatActivity() {
//
//
//        private lateinit var gridSmall: GridLayout
//        private lateinit var gridMedium: GridLayout
//        private lateinit var gridLarge: GridLayout
//
//        private lateinit var buttonEasy: Button
//        private lateinit var buttonMedium: Button
//        private lateinit var buttonHard: Button
//        private lateinit var timerText: TextView
//        private lateinit var broadcastReceiver: BroadcastReceiver
//
//        override fun onCreate(savedInstanceState: Bundle?) {
//            super.onCreate(savedInstanceState)
//            setContentView(R.layout.gamescreen)
//            timerText = findViewById(R.id.timerText)
//
//            // registreeri receiver
//            broadcastReceiver = object : BroadcastReceiver() {
//                override fun onReceive(context: Context?, intent: Intent?) {
//                    if (intent?.action == C.IntentBackgroundServiceTime) {
//                        val time = intent.getStringExtra(C.IntentBackgroundServiceTimePayload)
//                        timerText.text = time
//                    }
//                }
//            }
//            LocalBroadcastManager.getInstance(this)
//                .registerReceiver(broadcastReceiver, IntentFilter(C.IntentBackgroundServiceTime))
//            // Leia kõik view-d
//            gridSmall = findViewById(R.id.gridLayoutSmall)
//            gridMedium = findViewById(R.id.gridLayoutMedium)
//            gridLarge = findViewById(R.id.gridLayoutLarge)
//
//            buttonEasy = findViewById(R.id.buttoneasy)
//            buttonMedium = findViewById(R.id.buttonMedium)
//            buttonHard = findViewById(R.id.buttonHard)
//
//            // Alguses kõik lauad peidus
//            gridSmall.visibility = View.GONE
//            gridMedium.visibility = View.GONE
//            gridLarge.visibility = View.GONE
//
//            // Easy -> 4x4
//            buttonEasy.setOnClickListener {
//                showBoard(gridSmall, 4, 4, "small")
//                startService(Intent(this, DemoService::class.java))
//            }
//
//            // Medium -> 7x6
//            buttonMedium.setOnClickListener {
//                showBoard(gridMedium, 7, 6, "medium")
//                startService(Intent(this, DemoService::class.java))
//            }
//
//            // Hard -> 10x10
//            buttonHard.setOnClickListener {
//                showBoard(gridLarge, 10, 10, "large")
//                startService(Intent(this, DemoService::class.java))
//            }
//
//        }
//
//        private fun showBoard(grid: GridLayout, rows: Int, cols: Int, prefix: String) {
//            // Peidame kõik
//            gridSmall.visibility = View.GONE
//            gridMedium.visibility = View.GONE
//            gridLarge.visibility = View.GONE
//
//            // Näitame ainult valitut
//            grid.visibility = View.VISIBLE
//
//            // Genereerime nupud
//            createBoard(grid, rows, cols, prefix)
//        }
//
//        private fun createBoard(grid: GridLayout, rows: Int, cols: Int, prefix: String) {
//            grid.removeAllViews()
//            grid.rowCount = rows
//            grid.columnCount = cols
//
//            for (r in 0 until rows) {
//                for (c in 0 until cols) {
//                    val toggle = ToggleButton(this).apply {
//                        id = View.generateViewId()
//                        text = ""
//                        textOn = ""
//                        textOff = ""
//                        layoutParams = GridLayout.LayoutParams().apply {
//                            width = 10
//                            height = 10
//                            rowSpec = GridLayout.spec(r, 1, 1f)
//                            columnSpec = GridLayout.spec(c, 1, 1f)
//                            setMargins(1, 1, 1, 1)
//                        }
//                    }
//
//                    toggle.setOnClickListener {
//                        toggle.isChecked = !toggle.isChecked
//                    }
//
//                    grid.addView(toggle)
//                }
//            }
//        }
//    }

//
//    companion object {
//        private val TAG = this::class.java.declaringClass!!.simpleName
//    }
//
//    private lateinit var edit: EditText
//    private lateinit var greet: TextView
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        Log.d(TAG, "onCreate")
//        enableEdgeToEdge()
//        setContentView(R.layout.activity_main)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
//        }
//
//    override fun onStart() {
//        super.onStart()
//        Log.d(TAG, "start")
//    }
//
//    override fun onResume() {
//        super.onResume()
//        Log.d(TAG, "resume")
//    }
//
//    override fun onPause() {
//        super.onPause()
//        Log.d(TAG, "pause")
//    }
//
//
//    override fun onStop() {
//        super.onStop()
//        Log.d(TAG, "stop")
//
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        Log.d(TAG, "destroy")
//    }
//    override fun onRestart() {
//        super.onRestart()
//        Log.d(TAG, "restart")
//    }
//    override fun onSaveInstanceState(outState: Bundle, outPersistableBundle: PersistableBundle) {
//        super.onSaveInstanceState(outState)
//        outState.putString("edit_text", edit.text.toString())
//        outState.putString("greet_text", greet.text.toString())
//        Log.d(TAG, "onSave with bundle")
//    }
//    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
//        outState.putString("edit_text", edit.text.toString())
//        outState.putString("greet_text", greet.text.toString())
//        Log.d(TAG, "onSave")
//    }
//
//    override fun onRestoreInstanceState(savedInstanceState: Bundle?, persistableBundle: PersistableBundle?) {
//        super.onRestoreInstanceState(savedInstanceState, persistableBundle)
//        Log.d(TAG, "onRestore with persistentState")
//    }
//    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
//        super.onRestoreInstanceState(savedInstanceState)
//        Log.d(TAG, "onRestore")
//    }
//    fun buttonLaunchSecondClicked(view: View) {
//        val intent = Intent(this, SecondActivity::class.java)
//        startActivity(intent)
//    }
//
//    fun buttonServiceClicked(view: View) {
//        val intent = Intent(this, DemoService::class.java)
//        startService(intent)
//    }
//
//    fun okClicked(view: View) {
//        edit = findViewById(R.id.edit)
//        greet = findViewById(R.id.greet)
//        greet.text = ""
//        val button: Button = findViewById(R.id.button)
//
//        button.setOnClickListener {
//            greet.text = buildString {
//                append("Hello, ")
//                append(edit.text)
//                append("!")
//            }
//
//        }
//    }


