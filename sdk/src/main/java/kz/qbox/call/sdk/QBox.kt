package kz.qbox.call.sdk

object QBoxSDK {

    private var isLoggingEnabled: Boolean = false
    private var webSocketUrl: String? = null

    fun init(
        isLoggingEnabled: Boolean = false,
        webSocketUrl: String,
    ): Boolean {
        setLoggingEnabled(isLoggingEnabled)
        setWebSocketUrl(webSocketUrl)
        return true
    }

    fun isLoggingEnabled(): Boolean = isLoggingEnabled

    fun setLoggingEnabled(isLoggingEnabled: Boolean): Boolean = run {
        this.isLoggingEnabled = isLoggingEnabled
        this.isLoggingEnabled
    }

    fun getWebSocketUrl(): String? = webSocketUrl

    fun setWebSocketUrl(value: String?): String? = run {
        this.webSocketUrl = value
        this.webSocketUrl
    }

}