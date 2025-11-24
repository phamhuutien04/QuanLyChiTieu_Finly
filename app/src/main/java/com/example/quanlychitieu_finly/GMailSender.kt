package com.example.quanlychitieu_finly

import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object GMailSender {

    // Thay bằng email của bạn (người gửi)
    private const val SENDER_EMAIL = "sidat3241@gmail.com"
    // Thay bằng MẬT KHẨU ỨNG DỤNG (16 ký tự), KHÔNG phải mật khẩu đăng nhập gmail
    private const val SENDER_PASSWORD = "wtxg soyr vjnb zymp"

    fun sendEmail(recipientEmail: String, subject: String, body: String, onResult: (Boolean) -> Unit) {
        val props = Properties()
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.starttls.enable"] = "true"
        props["mail.smtp.host"] = "smtp.gmail.com"
        props["mail.smtp.port"] = "587"

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD)
            }
        })

        try {
            val message = MimeMessage(session)
            message.setFrom(InternetAddress(SENDER_EMAIL))
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
            message.subject = subject
            message.setText(body)

            // Gửi mail trong background thread để không chặn UI
            Thread {
                try {
                    Transport.send(message)
                    onResult(true) // Gửi thành công
                } catch (e: Exception) {
                    e.printStackTrace()
                    onResult(false) // Gửi thất bại
                }
            }.start()

        } catch (e: Exception) {
            e.printStackTrace()
            onResult(false)
        }
    }
}