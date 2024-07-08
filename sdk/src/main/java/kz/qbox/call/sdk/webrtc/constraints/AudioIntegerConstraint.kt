package kz.qbox.call.sdk.webrtc.constraints

enum class AudioIntegerConstraint constructor(
    override val constraint: String
) : RTCConstraint<Int> {
    LEVEL_CONTROL_INITIAL_PEAK_LEVEL_DBFS("levelControlInitialPeakLevelDBFS"),
}