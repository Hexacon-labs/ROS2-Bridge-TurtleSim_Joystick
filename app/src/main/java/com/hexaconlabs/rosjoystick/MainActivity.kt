/**
 * @author Shibin AK
 * @version V1.0 (2026-07-29)
 * Website: www.hexaconlabs.com
 */

package com.hexaconlabs.rosjoystick

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var joystickView: JoystickView
    private lateinit var statusDot: View
    private lateinit var statusText: TextView
    private lateinit var directionText: TextView
    private lateinit var urlEditText: EditText
    private lateinit var connectButton: Button
    private lateinit var menuButton: ImageButton

    private var rosClient: RosBridgeClient? = null
    private var lastDirection: String = "CENTER"

    // Throttle Twist publishing to ~20 Hz regardless of touch event rate
    private val handler = Handler(Looper.getMainLooper())
    private var pendingX = 0f
    private var pendingY = 0f
    private val publishRunnable = object : Runnable {
        override fun run() {
            publishTwist(pendingX, pendingY)
            handler.postDelayed(this, 50)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        joystickView = findViewById(R.id.joystickView)
        statusDot = findViewById(R.id.statusDot)
        statusText = findViewById(R.id.statusText)
        directionText = findViewById(R.id.directionText)
        urlEditText = findViewById(R.id.urlEditText)
        connectButton = findViewById(R.id.connectButton)
        menuButton = findViewById(R.id.menuButton)

        menuButton.setOnClickListener {
            showInfoDialog()
        }

        connectButton.setOnClickListener {
            val url = urlEditText.text.toString().trim()
            if (url.isEmpty()) {
                statusText.text = "Enter a valid rosbridge URL (ws://IP:9090)"
            } else {
                connectToRosbridge(url)
            }
        }

        joystickView.setOnJoystickMovedListener(object : JoystickView.OnJoystickMovedListener {
            override fun onMoved(x: Float, y: Float) {
                pendingX = x
                pendingY = y
                updateDirection(x, y)
            }

            override fun onReleased() {
                pendingX = 0f
                pendingY = 0f
                updateDirection(0f, 0f)
                publishTwist(0f, 0f)
            }
        })
    }

    private fun connectToRosbridge(url: String) {
        statusText.text = "Connecting to $url ..."
        rosClient?.disconnect()
        handler.removeCallbacks(publishRunnable)

        rosClient = RosBridgeClient(
            url = url,
            onOpen = {
                runOnUiThread {
                    statusText.text = "Connected: $url"
                    tintStatusDot(R.color.status_ok)
                }
                rosClient?.advertise("/turtle1/cmd_vel", "geometry_msgs/Twist")
                rosClient?.advertise("/joystick/direction", "std_msgs/String")
                handler.post(publishRunnable)
            },
            onClosed = {
                runOnUiThread {
                    statusText.text = "Disconnected"
                    tintStatusDot(R.color.status_idle)
                }
                handler.removeCallbacks(publishRunnable)
            },
            onError = { err ->
                runOnUiThread {
                    statusText.text = "Error: $err"
                    tintStatusDot(R.color.status_error)
                }
                handler.removeCallbacks(publishRunnable)
            }
        )
        rosClient?.connect()
    }

    private fun tintStatusDot(colorRes: Int) {
        val drawable = statusDot.background.mutate()
        DrawableCompat.setTint(drawable, ContextCompat.getColor(this, colorRes))
        statusDot.background = drawable
    }

    /** x,y are normalized -1..1, up = +y, right = +x */
    private fun publishTwist(x: Float, y: Float) {
        val linearX = y.coerceIn(-1f, 1f) * MAX_LINEAR_SPEED
        // Pushing right (+x) should turn the turtle right, which is
        // negative angular.z under ROS's right-hand convention.
        val angularZ = -x.coerceIn(-1f, 1f) * MAX_ANGULAR_SPEED
        rosClient?.publishTwist("/turtle1/cmd_vel", linearX.toDouble(), angularZ.toDouble())
    }

    private fun updateDirection(x: Float, y: Float) {
        val deadZone = 0.25f
        val direction = when {
            abs(x) < deadZone && abs(y) < deadZone -> "CENTER"
            abs(y) >= abs(x) && y > 0 -> "UP"
            abs(y) >= abs(x) && y < 0 -> "DOWN"
            x < 0 -> "LEFT"
            else -> "RIGHT"
        }
        if (direction != lastDirection) {
            lastDirection = direction
            directionText.text = "DIRECTION: $direction"
            rosClient?.publishString("/joystick/direction", direction)
        }
    }

    private fun showInfoDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_info, null)
        val dialog = MaterialAlertDialogBuilder(this, android.R.style.Theme_Material_NoActionBar_Fullscreen)
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.closeButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

        dialog.window?.let { window ->
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window.setBackgroundDrawableResource(R.color.black)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(publishRunnable)
        rosClient?.disconnect()
    }

    companion object {
        const val MAX_LINEAR_SPEED = 2.0f
        const val MAX_ANGULAR_SPEED = 2.0f
    }
}
