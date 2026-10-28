package com.veraflow.smsforwarder

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Forwarder {
    private val sendLock = Any()

    fun settingsReady(c: Context): String? {
        if (Store.get(c, "to").isBlank()) return "Add the address to send to"
        if (Store.get(c, "user").isBlank()) return "Add your Gmail address"
        if (Store.get(c, "pass").isBlank()) return "Add your app password"
        return null
    }

    fun mail(c: Context, to: String, subject: String, body: String) {
        val host = Store.get(c, "host", "smtp.gmail.com").ifBlank { "smtp.gmail.com" }
        val port = Store.get(c, "port", "465").toIntOrNull() ?: 465
        Mailer.send(host, port, Store.get(c, "user"), Store.get(c, "pass").replace(" ", ""), to, subject, body)
    }

    /** Sends everything waiting. Returns true when the queue is empty afterwards. */
    fun flush(c: Context): Boolean = synchronized(sendLock) {
        val q = Store.queue(c)
        if (q.length() == 0) return true
        if (settingsReady(c) != null) return true // nothing can be sent until the settings are complete
        val keep = JSONArray()
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        for (i in 0 until q.length()) {
            val item = q.getJSONObject(i)
            val sender = item.getString("sender")
            try {
                val body = "SMS from " + sender + ":\n" + item.getString("body") + "\n\nSMS received: " + fmt.format(Date(item.getLong("ts")))
                mail(c, Store.get(c, "to"), "SMS from $sender", body)
                Store.log(c, sender, true, "Sent")
            } catch (e: Exception) {
                val tries = item.optInt("tries", 0) + 1
                if (tries < 20) { item.put("tries", tries); keep.put(item) }
                Store.log(c, sender, false, (e.message ?: "Could not send").take(100) + if (tries < 20) " (will retry)" else " (gave up)")
            }
        }
        Store.setQueue(c, keep)
        keep.length() == 0
    }

    fun schedule(c: Context) {
        val js = c.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val job = JobInfo.Builder(7, ComponentName(c, RetryService::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setBackoffCriteria(30_000L, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
            .build()
        js.schedule(job)
    }
}
