package kz.qbox.call.sdk.webrtc.audio

import kz.qbox.call.sdk.logging.Logger
import org.webrtc.audio.JavaAudioDeviceModule

class AudioTrackStateCallback : JavaAudioDeviceModule.AudioTrackStateCallback {

    companion object {
        private const val TAG = "AudioTrackStateCallback"
    }

    override fun onWebRtcAudioTrackStart() {
        Logger.debug(TAG, "onWebRtcAudioTrackStart()")
    }

    override fun onWebRtcAudioTrackStop() {
        Logger.debug(TAG, "onWebRtcAudioTrackStop()")
    }

}