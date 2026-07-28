package com.hexaconlabs.rosjoystick

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val statusMessages = arrayOf(
        "Booting ROS2 core...",
        "Calibrating turtle sensors...",
        "Initializing cmd_vel nodes...",
        "Feeding the turtle...",
        "Establishing rosbridge tunnel...",
        "Optimizing linear/angular vectors...",
        "Turtle is waking up...",
        "Checking shell integrity...",
        "Systems online. Ready to roll!"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val progressBar = findViewById<ProgressBar>(R.id.splashProgressBar)
        val statusText = findViewById<TextView>(R.id.splashStatusText)
        
        val totalTime = 5000L // 5 seconds
        val interval = 50L // Update every 50ms
        val totalSteps = (totalTime / interval).toInt()

        val handler = Handler(Looper.getMainLooper())
        var currentStep = 0

        val runnable = object : Runnable {
            override fun run() {
                currentStep++
                
                // Update Progress
                val progress = (currentStep * 100 / totalSteps)
                progressBar.progress = progress

                // Update Status Text based on progress
                val messageIndex = (progress * (statusMessages.size - 1) / 100)
                statusText.text = statusMessages[messageIndex]
                
                if (currentStep < totalSteps) {
                    handler.postDelayed(this, interval)
                } else {
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                }
            }
        }

        handler.post(runnable)
    }
}