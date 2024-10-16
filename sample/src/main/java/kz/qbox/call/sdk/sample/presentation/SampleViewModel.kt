package kz.qbox.call.sdk.sample.presentation

import android.media.AudioManager
import android.util.Log
import androidx.lifecycle.ViewModel
import com.twilio.audioswitch.AudioDevice
import com.twilio.audioswitch.AudioSwitch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kz.qbox.call.sdk.CallEvent
import kz.qbox.call.sdk.CallManager
import kz.qbox.call.sdk.QBoxSDK
import kz.qbox.call.sdk.socket.WebSocketClientState
import kz.qbox.call.sdk.webrtc.PeerConnectionClient
import kz.qbox.call.sdk.webrtc.PeerConnectionClientState
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import org.webrtc.PeerConnection
import java.io.IOException
import java.util.concurrent.TimeUnit

class SampleViewModel(
    private val audioManager: AudioManager?,
    private val audioSwitch: AudioSwitch,
    peerConnectionClient: PeerConnectionClient
) : ViewModel(), CallManager.Listener {

    companion object {
        private const val TAG = "SampleViewModel"

        private const val BASE_URL = "dial.vlx.kz"

        private const val CALLER = "87007654321"
        private const val DESTINATION = "777"
    }

    private val _uiState = MutableStateFlow(SampleUIState())
    val uiState: StateFlow<SampleUIState> = _uiState.asStateFlow()

    private val httpClient by lazy(LazyThreadSafetyMode.NONE) {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    setLevel(HttpLoggingInterceptor.Level.BASIC)
                }
            )
            .build()
    }

    private val callManager = CallManager(
        peerConnectionClient = peerConnectionClient,
        listener = this
    )

    private var token: String? = null

    init {
        QBoxSDK.init(
            isLoggingEnabled = true,
            webSocketUrl = "wss://${BASE_URL}/websocket"
        )

        audioSwitch.start { audioDevices, selectedAudioDevice ->
            Log.d(
                TAG, "audioSwitch.start() -> " +
                        "audioDevices: $audioDevices, selectedAudioDevice: $selectedAudioDevice"
            )

            _uiState.value = _uiState.value.copy(audioDevice = selectedAudioDevice)
        }

        generateToken(
            onResponse = { token ->
                this.token = token

                callManager.init(token = token)
            },
            onFailure = {
                it.printStackTrace()
            }
        )
    }

    fun getAudioOutputDevices(): List<AudioDevice> =
        audioSwitch.availableAudioDevices

    fun onAudioOutputDeviceSelected(audioDevice: AudioDevice) =
        audioSwitch.selectDevice(audioDevice)

    fun onMute(): Boolean {
        audioManager?.isMicrophoneMute = true
        return if (audioManager?.isMicrophoneMute == true) {
            _uiState.value = _uiState.value.copy(isMuted = true)
            true
        } else {
            false
        }
    }

    fun onUnmute(): Boolean {
        audioManager?.isMicrophoneMute = false
        return if (audioManager?.isMicrophoneMute == false) {
            _uiState.value = _uiState.value.copy(isMuted = false)
            true
        } else {
            false
        }
    }

    fun onDTMFButtonPressed(symbol: String): Boolean =
        callManager.onDTMFButtonPressed(symbol)

    fun onDisconnect() {
        callManager.onDestroy()
    }

    fun onReconnect(): Boolean {
        if (callManager.getWebSocketClientState() == WebSocketClientState.Open) {
            Log.w(TAG, "onReconnect() -> [WebSocketClient active]")
            return false
        }

        if (callManager.getPeerConnectionClientState() == PeerConnectionClientState.Created) {
            Log.w(TAG, "onReconnect() -> [PeerConnectionClient active]")
            return false
        }

        val cachedToken = token
        if (cachedToken.isNullOrBlank()) {
            generateToken(
                onResponse = { token ->
                    this.token = token

                    callManager.init(token = token)
                },
                onFailure = {
                    it.printStackTrace()
                }
            )
        } else {
            callManager.init(token = cachedToken)
        }

        return true
    }

    fun onHangup(): Boolean =
        callManager.onHangup()

    private fun generateToken(
        onResponse: (token: String) -> Unit,
        onFailure: (e: Exception) -> Unit
    ) {
        httpClient.newCall(
            Request.Builder()
                .url("https://${BASE_URL}/api/generate")
                .method(
                    "POST",
                    JSONObject(
                        mapOf(
                            "caller" to CALLER,
                            "dest" to DESTINATION
                        )
                    ).toString().toRequestBody("application/json".toMediaTypeOrNull())
                )
                .build()
        ).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val body = response.body ?: throw IOException("Unexpected body")
                    val bodyString = body.string()
                    val json = JSONObject(bodyString)
                    onResponse.invoke(json.getString("token"))
                } else {
                    onFailure.invoke(Exception("Response issue"))
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                onFailure.invoke(e)
            }
        })
    }

    /**
     * [kz.qbox.call.sdk.CallManager.Listener]
     */

    override fun onCallEvent(event: CallEvent) {
        Log.d(TAG, "onCallEvent() -> event: $event")

        _uiState.value = _uiState.value.copy(callEvent = event.toString())

        if (event is CallEvent.Hangup) {
            audioSwitch.stop()
        }
    }

    /**
     * [kz.qbox.call.sdk.socket.WebSocketClient.Listener]
     */

    override fun onWebSocketStateChange(state: WebSocketClientState) {
        _uiState.value = _uiState.value.copy(webSocketState = state.toString())
    }

    override fun onWebRTCPeerConnectionChange(state: PeerConnection.PeerConnectionState?) {
        _uiState.value = _uiState.value.copy(webRTCState = state?.name)
    }

    /**
     * [ViewModel]
     */

    override fun onCleared() {
        super.onCleared()

        Log.d(TAG, "onCleared()")

        token = null

        audioSwitch.stop()

        callManager.onDestroy()

        httpClient.dispatcher.executorService.shutdown()
    }

}