package kz.qbox.call.sdk.webrtc

sealed class PeerConnectionClientState {
    data object IDLE : PeerConnectionClientState()
    data object Created : PeerConnectionClientState()
    data object Disposing : PeerConnectionClientState()
    data object Disposed : PeerConnectionClientState()
}