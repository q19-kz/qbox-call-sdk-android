package kz.qbox.call.sdk.webrtc.constraints

import org.webrtc.MediaConstraints

internal fun MediaConstraints.addConstraints(constraints: RTCConstraints<*, *>): Boolean =
    mandatory.addAll(constraints.mandatoryKeyValuePairs) && optional.addAll(constraints.optionalKeyValuePairs)

internal fun MediaConstraints.addConstraints(vararg constraints: RTCConstraints<*, *>) =
    constraints.forEach { addConstraints(it) }

internal fun <T : RTCConstraint<E>, E> RTCConstraints<T, E>.toMediaConstraints(): MediaConstraints =
    MediaConstraints().apply {
        addConstraints(this@toMediaConstraints)
    }