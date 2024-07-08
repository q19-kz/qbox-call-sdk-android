package kz.qbox.call.sdk.socket

sealed class WebSocketState {
    data object IDLE : WebSocketState()
    data object Open : WebSocketState()
    data object Closing : WebSocketState()
    data object Closed : WebSocketState()
    data class Failure(val t: Throwable) : WebSocketState()
}