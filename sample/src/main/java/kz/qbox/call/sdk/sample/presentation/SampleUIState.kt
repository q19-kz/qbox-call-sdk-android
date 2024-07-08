package kz.qbox.call.sdk.sample.presentation

import com.twilio.audioswitch.AudioDevice

data class SampleUIState(
    val webSocketState: String? = null,
    val webRTCState: String? = null,
    val callEvent: String? = null,
    val isMuted: Boolean = false,
    val audioDevice: AudioDevice? = null
)