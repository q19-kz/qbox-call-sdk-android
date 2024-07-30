package kz.qbox.call.sdk

import kz.qbox.call.sdk.logging.Logger

fun safeShutdown(name: String, action: () -> Boolean, onComplete: () -> Unit = {}): Boolean {
    Logger.debug("SafeShutdown", "[BEFORE] safeShutdown() -> name: $name")
    var ok = false
    try {
        ok = action.invoke()
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        onComplete.invoke()
    }
    Logger.debug("SafeShutdown", "[AFTER] safeShutdown() -> name: $name")
    return ok
}