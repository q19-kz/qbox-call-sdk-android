package kz.qbox.call.sdk.webrtc.sdp.observers

import kz.qbox.call.sdk.logging.Logger
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

class LocalSDPObserver(
    private val onSetSuccess: () -> Unit = {},
    private val onSetFailure: (errorMessage: String?) -> Unit = {}
) : SdpObserver {

    companion object {
        const val TAG = "LocalSDPObserver"
    }

    override fun onCreateSuccess(sessionDescription: SessionDescription?) {
        Logger.debug(TAG, "onCreateSuccess() -> sdp: ${sessionDescription?.type}")
    }

    override fun onCreateFailure(error: String?) {
        Logger.debug(TAG, "onCreateFailure() -> error: $error")
    }

    override fun onSetSuccess() {
        Logger.debug(TAG, "onSetSuccess()")

        onSetSuccess.invoke()
    }

    override fun onSetFailure(error: String?) {
        Logger.debug(TAG, "onSetFailure() -> error: $error")

        onSetFailure.invoke(error)
    }

}