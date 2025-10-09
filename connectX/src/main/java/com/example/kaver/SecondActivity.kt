//package com.example.kaver
//
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.content.IntentFilter
//import android.os.Bundle
//import android.os.PersistableBundle
//import android.util.Log
//import android.view.View
//import android.widget.TextView
//import androidx.activity.enableEdgeToEdge
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.view.ViewCompat
//import androidx.core.view.WindowInsetsCompat
//import androidx.localbroadcastmanager.content.LocalBroadcastManager
//import com.example.kaver.C.IntentBackgroundServiceTimePayload
//
//class SecondActivity : AppCompatActivity() {
//
//    companion object {
//        private val TAG = SecondActivity::class.java.simpleName
//    }
//    lateinit var textViewInfo : TextView
//    private val localReceiverIntentFilter = IntentFilter()
//    private val localReceiver = BroadcastReceiverInActivity()
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        Log.d(TAG, "onCreate")
//        setContentView(R.layout.activity_second) // <-- must use its own layout, not activity_main
//        enableEdgeToEdge()
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
//        localReceiverIntentFilter.addAction(C.IntentBackgroundServiceTime)
//
//        textViewInfo = findViewById(R.id.textViewInfo)
//
//    }
//
//
//    override fun onStart() {
//        super.onStart()
//        Log.d(TAG, "start")
//    }
//
//    override fun onResume() {
//        super.onResume()
//        Log.d(TAG, "resume")
//
//        LocalBroadcastManager.getInstance(this)
//            .registerReceiver(localReceiver, localReceiverIntentFilter)
//    }
//
//    override fun onPause() {
//        super.onPause()
//        Log.d(TAG, "pause")
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(localReceiver)
//
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
//        Log.d(TAG, "onSave with bundle")
//    }
//    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
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
//
//    fun buttonCloseClicked(view: View) {
//        finish()
//    }
//    private inner class BroadcastReceiverInActivity : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent?) {
//            Log.d(
//                TAG,
//                "BroadcastReceiverInMainActivity.onReceive " + (intent?.action ?: "null intent")
//            )
//            when (intent?.action) {
//                C.IntentBackgroundServiceTime -> textViewInfo.text =intent.getStringExtra(IntentBackgroundServiceTimePayload)
//                }
//            }
//        }
//    }
