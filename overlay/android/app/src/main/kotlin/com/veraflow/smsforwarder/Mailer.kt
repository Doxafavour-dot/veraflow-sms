package com.veraflow.smsforwarder

import android.util.Base64
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/** A small SMTP client over SSL (port 465), so the app needs no extra libraries. */
object Mailer {
    fun send(host: String, port: Int, user: String, pass: String, to: String, subject: String, body: String) {
        val plain = Socket()
        plain.soTimeout = 20000
        plain.connect(InetSocketAddress(host, port), 20000)
        val ssl = (SSLSocketFactory.getDefault() as SSLSocketFactory).createSocket(plain, host, port, true) as SSLSocket
        ssl.soTimeout = 20000
        ssl.startHandshake()
        if (!HttpsURLConnection.getDefaultHostnameVerifier().verify(host, ssl.session)) {
            ssl.close(); throw Exception("The mail server's certificate does not match $host")
        }
        val rd = BufferedReader(InputStreamReader(ssl.inputStream, Charsets.UTF_8))
        val wr = ssl.outputStream
        fun read(): Int {
            var code = 0
            while (true) {
                val l = rd.readLine() ?: throw Exception("The mail server closed the connection")
                if (l.length >= 3) code = l.substring(0, 3).toIntOrNull() ?: 0
                if (l.length < 4 || l[3] != '-') return code
            }
        }
        fun cmd(step: String, line: String, vararg ok: Int) {
            wr.write((line + "\r\n").toByteArray(Charsets.UTF_8)); wr.flush()
            val c = read()
            if (c !in ok) throw Exception(if (c == 535) "Gmail refused the sign-in (535). Check the address and the app password" else "Mail server said $c at step: $step")
        }
        try {
            if (read() != 220) throw Exception("The mail server did not greet us")
            cmd("hello", "EHLO smsforwarder", 250)
            cmd("login", "AUTH LOGIN", 334)
            cmd("user name", Base64.encodeToString(user.toByteArray(), Base64.NO_WRAP), 334)
            cmd("password", Base64.encodeToString(pass.toByteArray(), Base64.NO_WRAP), 235)
            cmd("sender", "MAIL FROM:<$user>", 250)
            cmd("recipient", "RCPT TO:<$to>", 250, 251)
            cmd("data", "DATA", 354)
            val date = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US).format(Date())
            val subj = "=?UTF-8?B?" + Base64.encodeToString(subject.toByteArray(Charsets.UTF_8), Base64.NO_WRAP) + "?="
            val b64 = Base64.encodeToString(body.toByteArray(Charsets.UTF_8), Base64.NO_WRAP).chunked(76).joinToString("\r\n")
            val msg = "From: $user\r\nTo: $to\r\nSubject: $subj\r\nDate: $date\r\nMIME-Version: 1.0\r\n" +
                "Content-Type: text/plain; charset=UTF-8\r\nContent-Transfer-Encoding: base64\r\n\r\n$b64\r\n.\r\n"
            wr.write(msg.toByteArray(Charsets.UTF_8)); wr.flush()
            if (read() != 250) throw Exception("The mail server did not accept the message")
            try { cmd("quit", "QUIT", 221) } catch (ignored: Exception) {}
        } finally {
            try { ssl.close() } catch (ignored: Exception) {}
        }
    }
}
