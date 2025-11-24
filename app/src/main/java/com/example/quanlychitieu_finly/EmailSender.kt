package com.example.quanlychitieu_finly

import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import kotlin.apply
import kotlin.concurrent.thread

object EmailSender {

    fun sendEmail(toEmail: String, subject: String, message: String, callback: (Boolean) -> Unit) {
        thread {
            try {
                val props = Properties().apply {
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.port", "587")
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                }

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication() =
                        PasswordAuthentication(
                            "kothanhcong050@gmail.com",
                            "ydyg wnhf efkb dzxs"
                        )
                })

                val msg = MimeMessage(session).apply {
                    setFrom(InternetAddress("kothanhcong050@gmail.com"))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                    setSubject(subject)
                    setText(message)
                }

                Transport.send(msg)
                callback(true)
            } catch (e: Exception) {
                callback(false)
            }
        }
    }
}
