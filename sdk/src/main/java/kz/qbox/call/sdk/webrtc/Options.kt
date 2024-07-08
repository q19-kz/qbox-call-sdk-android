package kz.qbox.call.sdk.webrtc

data class Options(
    val isLocalAudioEnabled: Boolean = LOCAL_AUDIO_ENABLED,
    val isRemoteAudioEnabled: Boolean = REMOTE_AUDIO_ENABLED
) {

    companion object {
        const val LOCAL_AUDIO_ENABLED: Boolean = true
        const val REMOTE_AUDIO_ENABLED: Boolean = true
    }

}