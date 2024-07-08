package kz.qbox.call.sdk.webrtc

sealed class PeerConnectionError {
    data object LocalSessionDescriptionCreateFailure : PeerConnectionError()

    data object InvalidLocalSessionDescription : PeerConnectionError()

    data object LocalSessionDescriptionSetFailure : PeerConnectionError()
    data object RemoteSessionDescriptionSetFailure : PeerConnectionError()
}