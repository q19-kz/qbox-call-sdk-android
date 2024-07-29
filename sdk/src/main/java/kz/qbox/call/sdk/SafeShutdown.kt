package kz.qbox.call.sdk

import android.util.Log

fun safeShutdown(name: String, action: () -> Boolean, onComplete: () -> Unit = {}): Boolean {
    Log.d("SafeShutdown", "[BEFORE] safeShutdown() -> name: $name")
    var ok = false
    try {
        ok = action.invoke()
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        onComplete.invoke()
    }
    Log.d("SafeShutdown", "[AFTER] safeShutdown() -> name: $name")
    return ok
}