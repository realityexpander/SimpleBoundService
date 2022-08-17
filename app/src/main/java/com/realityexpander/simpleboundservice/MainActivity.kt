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

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound.value = false
        }
    }

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
                                }
                            }
                        ) {
                            Text(text = "Start Download")
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        LinearProgressIndicator( progress = progress)
                        Spacer(modifier = Modifier.height(64.dp))

                        // start service and bind
                        Button(
                            onClick = {
                                if (!isBound) {
                                    Intent(this@MainActivity, BoundService::class.java).also { intent ->
                                        bindService(intent, connection, BIND_AUTO_CREATE)
                                    }
                                    this@MainActivity.isBound.value = true
                                } else {
                                    Toast.makeText(this@MainActivity, "Service already bound", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text(text = "Start Service")
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Stop service and unbind
                        Button(
                            onClick = {
                                if (isBound) {
                                    service.cancelDownload()
                                    stopService(Intent(this@MainActivity, BoundService::class.java))
                                    unbindService(connection)
                                    this@MainActivity.isBound.value = false
                                } else {
                                    Toast.makeText(this@MainActivity, "Service is not bound", Toast.LENGTH_SHORT).show()
                                }
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
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (isBound.value == false) {
            Intent(this, BoundService::class.java).also { intent ->
                bindService(intent, connection, BIND_AUTO_CREATE)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound.value == true) {
            unbindService(connection)
        }
    }
}

