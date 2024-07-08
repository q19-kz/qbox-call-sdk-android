package kz.qbox.call.sdk.webrtc.constraints

enum class AudioBooleanConstraint constructor(
    override val constraint: String
) : RTCConstraint<Boolean> {
    DISABLE_AUDIO_PROCESSING("echoCancellation"),
    ECHO_CANCELLATION("googEchoCancellation"),
    ECHO_CANCELLATION_2("googEchoCancellation2"),
    DELAY_AGNOSTIC_ECHO_CANCELLATION("googDAEchoCancellation"),
    AUTO_GAIN_CONTROL("googAutoGainControl"),
    AUTO_GAIN_CONTROL_2("googAutoGainControl2"),
    NOISE_SUPPRESSION("googNoiseSuppression"),
    NOISE_SUPPRESSION_2("googNoiseSuppression2"),
    INTELLIGIBILITY_ENHANCER("intelligibilityEnhancer"),
    LEVEL_CONTROL("levelControl"),
    HIGH_PASS_FILTER("googHighpassFilter"),
    TYPING_NOISE_DETECTION("googTypingNoiseDetection"),
    AUDIO_MIRRORING("googAudioMirroring")
}