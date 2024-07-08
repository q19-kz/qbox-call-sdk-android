package kz.qbox.call.sdk.socket

import android.util.Log
import kz.qbox.call.sdk.safeShutdown
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.logging.HttpLoggingInterceptor
import okio.ByteString
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

const val TAG = "WebSocketClient"

object WebSocketClient : WebSocketListener() {

    private val httpClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { createHttpClient() }

    private var webSocketClient: WebSocket? = null

    private var _webSocketState: WebSocketState = WebSocketState.IDLE
        set(value) {
            field = value
            listener?.onWebSocketStateChange(value)
        }

    val webSocketState: WebSocketState
        get() = _webSocketState

    private var listener: Listener? = null

    private fun createHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    setLevel(HttpLoggingInterceptor.Level.NONE)
                }
            )
            .build()
    }

    fun connect(url: String, token: String? = null, listener: Listener): Boolean {
        WebSocketClient.listener = listener

        return if (token.isNullOrBlank()) {
            val generatedToken = getToken() ?: return false
            createWebSocketClient(url = url, token = generatedToken)
        } else {
            createWebSocketClient(url = url, token = token)
        }
    }

    fun disconnect() {
        safeShutdown(
            name = "webSocketClient",
            action = {
                webSocketClient?.close(4996, "disconnect")
            },
            onComplete = {
                webSocketClient = null
            }
        )
    }

    fun sendMessage(message: JSONObject): Boolean {
        val body = message.toString()
        val response = webSocketClient?.send(body) == true
        Log.d(TAG, "sendMessage() -> body: $body, response: $response")
        return response
    }

    fun shutdown() {
        httpClient.dispatcher.executorService.shutdown()

        disconnect()
    }

    fun removeListeners() {
        listener = null
    }

    private fun getToken(): String? {
        val response = httpClient.newCall(
            Request.Builder()
                .url("https://dial.vlx.kz/api/generate")
                .method(
                    "POST",
                    JSONObject(
                        mapOf(
                            "caller" to "87782812817",
                            "dest" to "777"
                        )
                    ).toString().toRequestBody("application/json".toMediaTypeOrNull())
                )
                .build()
        ).execute()

        if (response.isSuccessful) {
            val body = response.body ?: throw IOException("Unexpected body")
            val bodyString = body.string()
            val json = JSONObject(bodyString)
            return json.getString("token")
        }

        return null
    }

    private fun createWebSocketClient(url: String, token: String): Boolean {
        if (webSocketClient == null) {
            if (webSocketState is WebSocketState.Closing
                || webSocketState is WebSocketState.Closed
                || webSocketState is WebSocketState.Failure
            ) {
                safeShutdown(
                    name = "webSocketClient",
                    action = {
                        webSocketClient?.cancel()
                    },
                    onComplete = {
                        webSocketClient = null
                    }
                )
            }

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
            webSocketClient = null
            _webSocketState = WebSocketState.IDLE
            return createWebSocketClient(url = url, token = token)
        }
    }

    /**
     * [WebSocketListener]
     */

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d(TAG, "onOpen()")
        _webSocketState = WebSocketState.Open
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d(TAG, "onMessage() -> text: $text")

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
        Log.d(TAG, "onMessage() -> bytes: ${bytes.hex()}")
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "onClosing() -> code: $code, reason: $reason")
        _webSocketState = WebSocketState.Closing
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "onClosed() -> code: $code, reason: $reason")
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