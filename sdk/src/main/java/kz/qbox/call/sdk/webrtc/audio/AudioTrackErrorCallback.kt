package kz.qbox.call.sdk.webrtc.audio

import kz.qbox.call.sdk.logging.Logger
import org.webrtc.audio.JavaAudioDeviceModule

class AudioTrackErrorCallback : JavaAudioDeviceModule.AudioTrackErrorCallback {

    companion object {
        private const val TAG = "AudioTrackErrorCallback"
    }

    override fun onWebRtcAudioTrackInitError(p0: String?) {
        Logger.error(TAG, "onWebRtcAudioTrackInitError() -> p0: $p0")
    }

    override fun onWebRtcAudioTrackError(p0: String?) {
        Logger.error(TAG, "onWebRtcAudioTrackError() -> p0: $p0")
    }

    override fun onWebRtcAudioTrackStartError(
        p0: JavaAudioDeviceModule.AudioTrackStartErrorCode?,
        p1: String?
    ) {
        Logger.error(TAG, "onWebRtcAudioTrackStartError() -> p0: $p0, p1: $p1")
    }

}