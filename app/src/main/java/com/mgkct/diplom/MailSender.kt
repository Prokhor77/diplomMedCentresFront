package com.mgkct.diplom

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

suspend fun sendEmail(recipientEmail: String, key: String) = withContext(Dispatchers.IO) {
    val username = "prohor.odinets@yandex.by"
    val password = "hgrgaosbzvtxdxam"

    val props = Properties().apply {
        put("mail.smtp.auth", "true")
        put("mail.smtp.starttls.enable", "true")
        put("mail.smtp.host", "smtp.yandex.com")
        put("mail.smtp.port", "587")
    }

    val session = Session.getInstance(props, object : javax.mail.Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication {
            return PasswordAuthentication(username, password)
        }
    })

    try {
        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(username))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
            subject = "Ваш ключ"

            // HTML-контент письма
            val htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Ваш ключ</title>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            background-color: #f4f4f9;
                            color: #333;
                            padding: 20px;
                        }
                        .key-container {
                            background-color: #fff;
                            padding: 20px;
                            border-radius: 5px;
                            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
                        }
                        .key {
                            font-size: 24px;
                            font-weight: bold;
                            color: #4CAF50;
                            background-color: #f1f1f1;
                            padding: 10px;
                            border-radius: 5px;
                        }
                        h1 {
                            color: #333;
                        }
                    </style>
                </head>
                <body>
                    <div class="key-container">
                        <h1>Здравствуйте!</h1>
                        <p>Ваш уникальный ключ для доступа:</p>
                        <div class="key">$key</div>
                        <p>Пожалуйста, используйте этот ключ в своем приложении. Если вы не запрашивали этот ключ, игнорируйте это письмо.</p>
                    </div>
                </body>
                </html>
            """
            setContent(htmlContent, "text/html; charset=UTF-8")
        }

        Transport.send(message)
    } catch (e: Exception) {
        e.printStackTrace()
        // Добавь здесь логирование или обработку ошибок при необходимости
    }
}
