package kz.qbox.call.sdk.webrtc.sdp.observers

import kz.qbox.call.sdk.logging.Logger
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

class OfferObserver(
    private val onCreateSuccess: (localSessionDescription: SessionDescription) -> Unit = {},
    private val onCreateFailure: (errorMessage: String?) -> Unit = {}
) : SdpObserver {

    companion object {
        const val TAG = "OfferObserver"
    }

    override fun onCreateSuccess(sessionDescription: SessionDescription?) {
        Logger.debug(TAG, "onCreateSuccess() -> sdp: ${sessionDescription?.type}")


        val sdp = sessionDescription
            ?: return onCreateFailure.invoke("onCreateSuccess() -> [SDP null]")

        onCreateSuccess.invoke(sdp)
    }

    override fun onCreateFailure(error: String?) {
        Logger.debug(TAG, "onCreateFailure() -> error: $error")

        onCreateFailure.invoke(error)
    }

    override fun onSetSuccess() {
        Logger.debug(TAG, "onSetSuccess()")
    }

    override fun onSetFailure(error: String?) {
        Logger.debug(TAG, "onSetFailure() -> error: $error")
    }

}