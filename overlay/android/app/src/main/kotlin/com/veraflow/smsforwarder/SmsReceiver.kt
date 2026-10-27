package com.veraflow.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/** Called by Android for every incoming SMS. Only senders you listed are kept. */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (!Store.enabled(context)) return
        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        // Long messages arrive in parts: join the parts that share a sender.
        val bySender = LinkedHashMap<String, StringBuilder>()
        var ts = System.currentTimeMillis()
        for (m in msgs) {
            val from = m.originatingAddress ?: continue
            bySender.getOrPut(from) { StringBuilder() }.append(m.messageBody ?: "")
            if (m.timestampMillis > 0) ts = m.timestampMillis
        }
        var any = false
        for ((from, text) in bySender) {
            if (Store.matches(context, from)) { Store.enqueue(context, from, text.toString(), ts); any = true }
        }
        if (any) Forwarder.schedule(context)
    }
}
