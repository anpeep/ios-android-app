package com.example.gpssportmap.service

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle

class StopConfirmationActivity : Activity() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            AlertDialog.Builder(this)
                .setTitle("Stop session?")
                .setMessage("Do you want to stop the tracking session?")
                .setPositiveButton("Yes") { _, _ ->
                    sendBroadcast(Intent("STOP_CONFIRMED"))
                    finish()
                }
                .setNegativeButton("Cancel") { _, _ -> finish() }
                .setCancelable(true)
                .show()
        }
    }