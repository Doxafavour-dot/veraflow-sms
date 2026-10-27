package com.veraflow.smsforwarder

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/** Settings, the waiting queue and a short log. Private to this app. */
object Store {
    private val lock = Any()
    private fun p(c: Context): SharedPreferences = c.getSharedPreferences("fwd", Context.MODE_PRIVATE)

    fun get(c: Context, k: String, d: String = ""): String = p(c).getString(k, d) ?: d
    fun put(c: Context, k: String, v: String) { p(c).edit().putString(k, v).apply() }
    fun enabled(c: Context) = p(c).getBoolean("enabled", true)
    fun setEnabled(c: Context, v: Boolean) { p(c).edit().putBoolean("enabled", v).apply() }

    fun senders(c: Context): List<String> =
        get(c, "senders").split(',', ';', '\n').map { it.trim().lowercase() }.filter { it.isNotEmpty() }

    /** Matches when the SMS sender equals, or contains, one of the names you listed. */
    fun matches(c: Context, sender: String): Boolean {
        val s = sender.trim().lowercase()
        if (s.isEmpty()) return false
        return senders(c).any { s == it || s.contains(it) }
    }

    fun enqueue(c: Context, sender: String, body: String, ts: Long) = synchronized(lock) {
        val q = JSONArray(get(c, "queue", "[]"))
        q.put(JSONObject().put("id", "$ts-${q.length()}-${sender.hashCode()}").put("sender", sender).put("body", body).put("ts", ts).put("tries", 0))
        put(c, "queue", q.toString())
    }

    fun queue(c: Context): JSONArray = synchronized(lock) { JSONArray(get(c, "queue", "[]")) }
    fun setQueue(c: Context, q: JSONArray) = synchronized(lock) { put(c, "queue", q.toString()) }

    /** The log never stores message text, only the sender, the time and the result. */
    fun log(c: Context, sender: String, ok: Boolean, note: String) = synchronized(lock) {
        val old = JSONArray(get(c, "log", "[]"))
        val out = JSONArray()
        out.put(JSONObject().put("t", System.currentTimeMillis()).put("s", sender).put("ok", ok).put("n", note.take(120)))
        for (i in 0 until minOf(old.length(), 79)) out.put(old.get(i))
        put(c, "log", out.toString())
    }
    fun logJson(c: Context): String = synchronized(lock) { get(c, "log", "[]") }
    fun clearLog(c: Context) = synchronized(lock) { put(c, "log", "[]") }
}
