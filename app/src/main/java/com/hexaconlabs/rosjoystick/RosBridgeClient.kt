package com.hexaconlabs.rosjoystick

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Minimal client for the rosbridge_suite v2 JSON protocol over a websocket.
 * Works with rosbridge_websocket_launch.xml from ros-jazzy-rosbridge-suite.
 * @author Shibin AK
 * @version V1.0 (2026-07-29)
 * Website: www.hexaconlabs.com
 */
class RosBridgeClient(
    private val url: String,
    private val onOpen: () -> Unit,
    private val onClosed: () -> Unit,
    private val onError: (String) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null

    fun connect() {
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                onOpen()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onClosed()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onError(t.message ?: "Unknown error")
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Client closing")
        webSocket = null
    }

    /** Tell rosbridge about a topic/type pair before publishing to it. */
    fun advertise(topic: String, type: String) {
        val msg = JSONObject().apply {
            put("op", "advertise")
            put("topic", topic)
            put("type", type)
        }
        webSocket?.send(msg.toString())
    }

    /** Publish a geometry_msgs/Twist, e.g. to /turtle1/cmd_vel */
    fun publishTwist(topic: String, linearX: Double, angularZ: Double) {
        val msg = JSONObject().apply {
            put("op", "publish")
            put("topic", topic)
            put("msg", JSONObject().apply {
                put("linear", JSONObject().apply {
                    put("x", linearX); put("y", 0.0); put("z", 0.0)
                })
                put("angular", JSONObject().apply {
                    put("x", 0.0); put("y", 0.0); put("z", angularZ)
                })
            })
        }
        webSocket?.send(msg.toString())
    }

    /** Publish a std_msgs/String, e.g. to /joystick/direction */
    fun publishString(topic: String, data: String) {
        val msg = JSONObject().apply {
            put("op", "publish")
            put("topic", topic)
            put("msg", JSONObject().apply {
                put("data", data)
            })
        }
        webSocket?.send(msg.toString())
    }
}