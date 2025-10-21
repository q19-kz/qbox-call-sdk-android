package kz.qbox.call.sdk

sealed class CallEvent {
    data object Connect : CallEvent()
    data class Hangup(val errorCode: String? = null) : CallEvent()
}