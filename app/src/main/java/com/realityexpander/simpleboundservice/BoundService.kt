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

    private var isCancelled = false

    private var dataString: String? = null

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        dataString = intent?.getStringExtra("EXTRA_DATA")
        dataString?.let { str ->
            println("dataString: $str")
        }

        //return START_NOT_STICKY // if android kills service, dont restart it
        return START_STICKY // if android kills service, start it again & don't send last intent
        //return START_REDELIVER_INTENT // if android kills service, send last intent to service

        //return super.onStartCommand(intent, flags, startId)
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

    fun getMessage(): Flow<String?> {
        return flow {
            while(!isCancelled) {
                if(dataString != null) {
                    emit(dataString)
                    dataString = null
                }
                delay(100)
            }
        }
    }

}