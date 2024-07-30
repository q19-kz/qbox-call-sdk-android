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

    private var _webSocketState: WebSocketState = WebSocketState.IDLE
        set(value) {
            field = value
            listener?.onWebSocketStateChange(value)
        }

    @Suppress("MemberVisibilityCanBePrivate")
    val webSocketState: WebSocketState
        get() = _webSocketState

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
        val response = webSocketClient?.send(body) == true
        Logger.debug(TAG, "sendMessage() -> body: $body, response: $response")
        return response
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
            if (webSocketState == WebSocketState.Open) return true
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
            _webSocketState = WebSocketState.IDLE
            return createWebSocketClient(url = url, token = token)
        }
    }

    /**
     * [WebSocketListener]
     */

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Logger.debug(TAG, "onOpen()")
        _webSocketState = WebSocketState.Open
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
        _webSocketState = WebSocketState.Closing
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Logger.debug(TAG, "onClosed() -> code: $code, reason: $reason")
        _webSocketState = WebSocketState.Closed
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        t.printStackTrace()
        _webSocketState = WebSocketState.Failure(t)
    }

    interface Listener {
        fun onWebSocketStateChange(state: WebSocketState)
        fun onWebSocketMessage(message: JSONObject)
    }
}