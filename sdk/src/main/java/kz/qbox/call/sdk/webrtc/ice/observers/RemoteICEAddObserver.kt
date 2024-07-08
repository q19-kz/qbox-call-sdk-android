package kz.qbox.call.sdk.webrtc.ice.observers

import kz.qbox.call.sdk.logging.Logger
import org.webrtc.AddIceObserver

class RemoteICEAddObserver(
    private val id: Int
) : AddIceObserver {

    companion object {
        private const val TAG = "RemoteICEAddObserver"
    }

    override fun onAddSuccess() {
        Logger.debug(TAG, "onAddSuccess() -> id: $id")
    }

    override fun onAddFailure(p0: String?) {
        Logger.debug(TAG, "onAddFailure() -> id: $id, p0: $p0")
    }

}