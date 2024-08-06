package kz.qbox.call.sdk.socket

import kz.qbox.call.sdk.logging.Logger
import kz.qbox.call.sdk.safeShutdown
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.TimeUnit

const val TAG = "WebSocketClient"

object WebSocketClient : WebSocketListener() {

    private val httpClient by lazy(LazyThreadSafetyMode.NONE) {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private var webSocketClient: WebSocket? = null

    private var _webSocketClientState: WebSocketClientState = WebSocketClientState.IDLE
        set(value) {
            field = value
            listener?.onWebSocketClientStateChange(value)
        }

    val webSocketClientState: WebSocketClientState
        get() = _webSocketClientState

    private var listener: Listener? = null

    fun connect(url: String, token: String, listener: Listener): Boolean {
        WebSocketClient.listener = listener

        return createWebSocketClient(url = url, token = token)
    }

    fun disconnect(): Boolean {
        Logger.debug(TAG, "disconnect()")
        return if (webSocketClient == null) {
            Logger.warn(TAG, "disconnect() -> [Already disconnected]")
            false
        } else {
            safeShutdown(
                name = "webSocketClient",
                action = {
                    webSocketClient?.close(4996, "disconnect") == true
                },
                onComplete = {
                    webSocketClient = null
                }
            )
        }
    }

    fun sendMessage(message: JSONObject): Boolean {
        val body = message.toString()
        val enqueued = webSocketClient?.send(body) == true
        Logger.debug(TAG, "sendMessage() -> body: $body, enqueued: $enqueued")
        return enqueued
    }

    fun shutdown() {
        Logger.debug(TAG, "shutdown()")

//        httpClient.dispatcher.executorService.shutdown()

        disconnect()
    }

    fun removeListeners() {
        listener = null
    }

    private fun createWebSocketClient(url: String, token: String): Boolean {
        Logger.debug(TAG, "createWebSocketClient() -> url: $url, token: $token")

        if (webSocketClient == null) {
            try {
                webSocketClient = httpClient.newWebSocket(
                    Request.Builder()
                        .url("$url?token=$token")
                        .build(),
                    this
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return webSocketClient != null
        } else {
            if (webSocketClientState == WebSocketClientState.Open) return true
            safeShutdown(
                name = "webSocketClient",
                action = {
                    webSocketClient?.cancel()
                    true
                },
                onComplete = {
                    webSocketClient = null
                }
            )
            _webSocketClientState = WebSocketClientState.IDLE
            return createWebSocketClient(url = url, token = token)
        }
    }

    /**
     * [WebSocketListener]
     */

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Logger.debug(TAG, "onOpen()")
        _webSocketClientState = WebSocketClientState.Open
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Logger.debug(TAG, "onMessage() -> text: $text")

        val message = try {
            JSONObject(text)
        } catch (e: JSONException) {
            e.printStackTrace()
            null
        }

        if (message != null) {
            listener?.onWebSocketMessage(message)
        }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        Logger.debug(TAG, "onMessage() -> bytes: ${bytes.hex()}")
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Logger.debug(TAG, "onClosing() -> code: $code, reason: $reason")
        _webSocketClientState = WebSocketClientState.Closing
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Logger.debug(TAG, "onClosed() -> code: $code, reason: $reason")
        _webSocketClientState = WebSocketClientState.Closed
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        t.printStackTrace()
        _webSocketClientState = WebSocketClientState.Failure(t)
    }

    interface Listener {
        fun onWebSocketClientStateChange(state: WebSocketClientState)
        fun onWebSocketMessage(message: JSONObject)
    }
}