package com.realityexpander.simpleboundservice

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import com.realityexpander.simpleboundservice.ui.theme.SimpleBoundServiceTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var service: BoundService
    private val isBound: MutableLiveData<Boolean> = MutableLiveData(false)
    private val connection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val bind = binder as BoundService.LocalBinder
            service = bind.getService()
            isBound.value = true
        }

        // This is only called if the service crashed or the system killed it.
        override fun onServiceDisconnected(name: ComponentName?) {
            isBound.value = false
        }
    }

    lateinit var messageJob: Job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SimpleBoundServiceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    var progress by remember { mutableStateOf(0f) }
                    val scope = rememberCoroutineScope()
                    val isBound: Boolean by isBound.observeAsState(isBound.value ?: false)
                    var messageTextFromService by remember { mutableStateOf("") }
                    var messageTextEntry by remember { mutableStateOf("") }
                    var counter: Int by remember { mutableStateOf(0) }

                    LaunchedEffect(key1 = isBound) {
                        messageJob = scope.launch {
                            if(isBound) {
                                service.messageFlow.collect {
                                    messageTextFromService = it
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Start Download
                        Button(
                            onClick = {
                                if (isBound) {
                                    scope.launch {
                                        service.getProgress().collect {
                                            progress = it
                                        }
                                    }
                                }else {
                                    Toast.makeText(this@MainActivity,
                                        "Service not bound", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text(text = "Start Download")
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Show progress bar
                        LinearProgressIndicator(
                            backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                            progress = progress
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Cancel Download
                        Button(
                            onClick = {
                                if (isBound) {
                                    service.cancelDownload()
                                } else {
                                    Toast.makeText(this@MainActivity,
                                        "Service not bound", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text(text = "Cancel Download")
                        }
                        Spacer(modifier = Modifier.height(64.dp))

                        // start service and bind
                        Button(
                            onClick = {
                                onStartService()
                            }
                        ) {
                            Text(text = "Start Service")
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Stop service and unbind
                        Button(
                            onClick = {
                                onStopService()
                            }
                        ) {
                            Text(text = "Stop Service")
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        if(isBound) {
                            Text(text = "Service is bound")
                        } else {
                            Text(text = "Service is not bound")
                        }
                        Spacer(modifier = Modifier.height(32.dp))


                        // Text entry for message
                        TextField(
                            value = messageTextEntry,
                            onValueChange = {
                                messageTextEntry = it
                            }
                        )

                        // Send message to service
                        Button(
                            onClick = {
                                if (isBound) {
                                    sendMessageToService("$messageTextEntry ${counter++}")
                                } else {
                                    Toast.makeText(this@MainActivity,
                                        "Service not bound", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text(text = "Send Message")
                        }

                        if(messageTextFromService.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = messageTextFromService)
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        onStartService()
    }

//    override fun onStop() {
//        super.onStop()
//        onStopService()
//    }

    override fun onDestroy() {
        super.onDestroy()
        onStopService()
    }

    fun sendMessageToService(message: String) {
        if (isBound.value!!) {
            Intent(this, BoundService::class.java).also { intent ->
                intent.putExtra("EXTRA_DATA", message)
                startService(intent)
            }
        }
    }

    fun onStartService() {
        if (!isBound.value!!) {
            Intent(this, BoundService::class.java).also { intent ->
                bindService(intent, connection, BIND_AUTO_CREATE)
            }
        } else {
            Toast.makeText(this, "Service is already bound", Toast.LENGTH_SHORT).show()
        }
    }

    fun onStopService() {
        if (isBound.value!!) {
            service.cancelDownload()
            service.stopService()
            messageJob.cancel()
            //stopService(Intent(this, BoundService::class.java)) // used to stop the service from outside the service
            unbindService(connection)
            isBound.value = false
        } else {
            Toast.makeText(this, "Service is not bound", Toast.LENGTH_SHORT).show()
        }
    }
}

