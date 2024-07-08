package kz.qbox.call.sdk.sample.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import com.twilio.audioswitch.AudioDevice
import com.twilio.audioswitch.AudioSwitch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kz.qbox.call.sdk.CallEvent
import kz.qbox.call.sdk.CallManager
import kz.qbox.call.sdk.socket.WebSocketState
import kz.qbox.call.sdk.webrtc.PeerConnectionClient
import org.webrtc.PeerConnection

class SampleViewModel(
    private val audioSwitch: AudioSwitch,
    peerConnectionClient: PeerConnectionClient
) : ViewModel(), CallManager.Listener {

    companion object {
        private const val TAG = "SampleViewModel"
    }

    private val _uiState = MutableStateFlow(SampleUIState())
    val uiState: StateFlow<SampleUIState> = _uiState.asStateFlow()

    private val callManager = CallManager(
        peerConnectionClient = peerConnectionClient,
        listener = this
    )

    init {
        audioSwitch.start { audioDevices, selectedAudioDevice ->
            Log.d(
                TAG, "audioSwitch.start() -> " +
                        "audioDevices: $audioDevices, selectedAudioDevice: $selectedAudioDevice"
            )

            _uiState.value = _uiState.value.copy(audioDevice = selectedAudioDevice)
        }

        callManager.init()
    }

    fun getAudioOutputDevices(): List<AudioDevice> =
        audioSwitch.availableAudioDevices

    fun onAudioOutputDeviceSelected(audioDevice: AudioDevice) =
        audioSwitch.selectDevice(audioDevice)

    fun onMute(): Boolean {
        return if (callManager.onMute()) {
            _uiState.value = _uiState.value.copy(isMuted = true)
            true
        } else {
            false
        }
    }

    fun onUnmute(): Boolean {
        return if (callManager.onUnmute()) {
            _uiState.value = _uiState.value.copy(isMuted = false)
            true
        } else {
            false
        }
    }

    fun onDTMFButtonPressed(symbol: String): Boolean =
        callManager.onDTMFButtonPressed(symbol)

    fun onHangup(): Boolean =
        callManager.onHangup()

    /**
     * [kz.qbox.call.sdk.CallManager.Listener]
     */

    override fun onCallEvent(event: CallEvent) {
        _uiState.value = _uiState.value.copy(callEvent = event.toString())
    }

    /**
     * [kz.qbox.call.sdk.socket.WebSocketClient.Listener]
     */

    override fun onWebSocketStateChange(state: WebSocketState) {
        _uiState.value = _uiState.value.copy(webSocketState = state.toString())
    }

    override fun onWebRTCPeerConnectionChange(peerConnectionState: PeerConnection.PeerConnectionState?) {
        _uiState.value = _uiState.value.copy(webRTCState = peerConnectionState?.name)
    }

    /**
     * [ViewModel]
     */

    override fun onCleared() {
        super.onCleared()

        callManager.onDestroy()
    }

}