package kz.qbox.call.sdk

sealed class CallEvent {
    data object Connect : CallEvent()
    data object Hangup : CallEvent()
}