package com.veraflow.smsforwarder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "fwd").setMethodCallHandler { call, result ->
            val c = applicationContext
            when (call.method) {
                "getState" -> result.success(mapOf(
                    "to" to Store.get(c, "to"),
                    "user" to Store.get(c, "user"),
                    "senders" to Store.get(c, "senders"),
                    "enabled" to Store.enabled(c),
                    "hasPass" to Store.get(c, "pass").isNotBlank(),
                    "perm" to hasSmsPermission(),
                    "queue" to Store.queue(c).length(),
                    "log" to Store.logJson(c)
                ))
                "save" -> {
                    Store.put(c, "to", (call.argument<String>("to") ?: "").trim())
                    Store.put(c, "user", (call.argument<String>("user") ?: "").trim())
                    Store.put(c, "senders", (call.argument<String>("senders") ?: "").trim())
                    val pass = (call.argument<String>("pass") ?: "").trim()
                    if (pass.isNotEmpty()) Store.put(c, "pass", pass)
                    Store.setEnabled(c, call.argument<Boolean>("enabled") ?: true)
                    result.success(true)
                }
                "requestPermission" -> {
                    if (hasSmsPermission()) result.success(true) else {
                        requestPermissions(arrayOf(Manifest.permission.RECEIVE_SMS), 11)
                        result.success(false)
                    }
                }
                "test" -> {
                    val miss = Forwarder.settingsReady(c)
                    if (miss != null) result.success("Not sent: $miss") else {
                        Thread {
                            val out = try {
                                Forwarder.mail(c, Store.get(c, "to"), "Test from SMS Forwarder",
                                    "If you can read this, your phone can send email. Your real alerts will arrive the same way.")
                                Store.log(c, "(test)", true, "Test sent"); "Test email sent. Check your inbox."
                            } catch (e: Exception) {
                                Store.log(c, "(test)", false, e.message ?: "failed"); "Not sent: " + (e.message ?: "unknown error")
                            }
                            runOnUiThread { result.success(out) }
                        }.start()
                    }
                }
                "flush" -> { Forwarder.schedule(c); result.success(true) }
                "clearLog" -> { Store.clearLog(c); result.success(true) }
                else -> result.notImplemented()
            }
        }
    }

    private fun hasSmsPermission(): Boolean =
        Build.VERSION.SDK_INT < 23 || checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
}
