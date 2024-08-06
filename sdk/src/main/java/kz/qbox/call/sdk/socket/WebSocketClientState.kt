package kz.qbox.call.sdk.socket

sealed class WebSocketClientState {
    data object IDLE : WebSocketClientState()
    data object Open : WebSocketClientState()
    data object Closing : WebSocketClientState()
    data object Closed : WebSocketClientState()
    data class Failure(val t: Throwable) : WebSocketClientState()
}