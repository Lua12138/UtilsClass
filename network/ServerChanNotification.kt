import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

interface ServerChanNotification {
    fun send(title: String, content: String?): ResponseMessage
}

data class ResponseMessage(val code: Int,
                           val message: String)

open abstract class AbstractNotification(private val sendKey: String) : ServerChanNotification {
    protected fun sendRequest(targetUrl: String, title: String, content: String?): ResponseMessage {
        val connection = URL(targetUrl)
                .openConnection() as HttpURLConnection

        val UTF8 = "utf-8"

        with(connection) {
            doOutput = true
            doInput = true
            useCaches = false
            requestMethod = "POST"
            setRequestProperty("content-type", "application/x-www-form-urlencoded; charset=UTF-8")
            setRequestProperty("connection", "close")
        }

        val body = StringBuilder("sendkey=${this.sendKey}&text=${URLEncoder.encode(title, UTF8)}")

        if (content != null) {
            body.append("&desp=${URLEncoder.encode(content, UTF8)}")
        }

        connection.connect()

        connection.outputStream.use {
            it.write(body.toString().toByteArray())
        }

        val response = connection.inputStream.use { String(it.readBytes()) }
        return try {
            "\"(?:code|errno)\":(?:|\")(\\d+)(?:|\"),\"(?:message|errmsg)\":\"([^\"]*)\"".toRegex()
                    .find(response)
                    .let {
                        ResponseMessage(
                                it?.groupValues?.get(1)?.toInt() ?: -9999,
                                it?.groupValues?.get(2)
                                        ?: "Build-in Unknown Message(regexp match the empty)[$response]"
                        )
                    }
        } catch (e: Throwable) {
            ResponseMessage(-8888, "Build-in Error Message(Regexp not match)[$response]")
        }
    }
}

class ServerChanNotificationImpl(private val sendKey: String) : AbstractNotification(sendKey) {
    override fun send(title: String, content: String?): ResponseMessage {
        return this.sendRequest("https://sc.ftqq.com/${this.sendKey}.send", title, content)
    }
}

class PushBearNotificationImpl(private val sendKey: String) : AbstractNotification(sendKey) {
    override fun send(title: String, content: String?): ResponseMessage {
        return this.sendRequest("https://pushbear.ftqq.com/sub", title, content)
    }
}
