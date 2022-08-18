package com.realityexpander.simpleboundservice

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

class BoundService: Service() {
    private val binder = LocalBinder()

    private var isCancelled = false

    private var dataString: String? = null

    @Volatile var c: Int = 0

    val messageFlow = MutableStateFlow<String>("")

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        dataString = intent?.getStringExtra("EXTRA_DATA")
        dataString?.let { str ->
            println("received dataString: $str")
            sendMessage(str)

            if(str.contains("loop")) {
                loopThread()
            }
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
        c = 0
        stopSelf() // always stops the service
    }

    private fun sendMessage(message: String) {
        messageFlow.value = message
    }

    private fun loopThread() {
        isCancelled = false
        c = 0

        Thread {
            while (!isCancelled) {
                Thread.sleep(500)
                sendMessage("LOOPER ${c++}")
            }
        }.start()
    }

}