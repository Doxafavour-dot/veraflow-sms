package com.veraflow.smsforwarder

import android.app.job.JobParameters
import android.app.job.JobService

/** Sends waiting messages in the background, and tries again later if the phone is offline. */
class RetryService : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        Thread {
            val done = try { Forwarder.flush(applicationContext) } catch (e: Exception) { false }
            jobFinished(params, !done)
        }.start()
        return true
    }
    override fun onStopJob(params: JobParameters): Boolean = true
}
