package kz.qbox.call.sdk.webrtc.constraints

enum class OfferAnswerConstraint constructor(
    override val constraint: String
) : RTCConstraint<Boolean> {
    OFFER_TO_RECEIVE_AUDIO("OfferToReceiveAudio"),

    OFFER_TO_RECEIVE_VIDEO("OfferToReceiveVideo"),

    /**
     * Many codec's and systems are capable of detecting "silence" and changing their behavior in this
     * case by doing things such as not transmitting any media. In many cases, such as when dealing
     * with emergency calling or sounds other than spoken voice, it is desirable to be able to turn
     * off this behavior. This option allows the application to provide information about whether it
     * wishes this type of processing enabled or disabled.
     */
    VOICE_ACTIVITY_DETECTION("VoiceActivityDetection"),

    /**
     * Tries to restart connection after it was in failed or disconnected state
     */
    ICE_RESTART("IceRestart"),

    /**
     * Google specific constraint for BUNDLE enable/disable.
     */
    GOOG_USE_RTP_MUX("googUseRtpMUX")
}