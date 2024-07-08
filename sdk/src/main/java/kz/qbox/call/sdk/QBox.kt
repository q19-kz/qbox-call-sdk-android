package kz.qbox.call.sdk

object QBoxSDK {

    private var isLoggingEnabled: Boolean = false

    fun init(
        isLoggingEnabled: Boolean = false
    ): Boolean = setLoggingEnabled(isLoggingEnabled)

    fun isLoggingEnabled(): Boolean = isLoggingEnabled

    fun setLoggingEnabled(isLoggingEnabled: Boolean): Boolean = this.run {
        QBoxSDK.isLoggingEnabled = isLoggingEnabled
        QBoxSDK.isLoggingEnabled
    }

}