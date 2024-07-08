package kz.qbox.call.sdk

import android.util.Log

fun safeShutdown(name: String, action: () -> Unit, onComplete: () -> Unit = {}) {
    Log.d("SafeShutdown", "[BEFORE] safeShutdown() -> name: $name")
    try {
        action.invoke()
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        onComplete.invoke()
    }
    Log.d("SafeShutdown", "[AFTER] safeShutdown() -> name: $name")
}