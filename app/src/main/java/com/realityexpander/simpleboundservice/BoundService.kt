package com.realityexpander.simpleboundservice

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class BoundService: Service() {
    private val binder = LocalBinder()

    //@Volatile
    private var isCancelled = false

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    inner class LocalBinder : Binder() {
        fun getService(): BoundService = this@BoundService
    }

    fun cancelDownload() {
        isCancelled = true
    }

    fun getProgress(): Flow<Float> {

        var progress = 0f
        isCancelled = false

        return flow {
            while (progress <= 1f && !isCancelled) {
                progress += 0.1f
                delay(500)
                emit(progress)
            }
        }
    }

    fun stopService() {
        stopSelf() // always stops the service
    }

}