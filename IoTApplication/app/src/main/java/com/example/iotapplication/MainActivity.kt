package com.example.iotapplication

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.iotapplication.ui.theme.IoTApplicationTheme
import java.io.*
import java.util.UUID
import androidx.fragment.*
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.*
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Callback
import okhttp3.Response
import okhttp3.Call

class MainActivity : ComponentActivity() {

    private val TAG = "BluetoothConnectivity";
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private lateinit var enableBluetoothLauncher: ActivityResultLauncher<Intent>
    val channelId = "motion_alerts"
    val channelName = "Alerts"


    private val raspPiAddress = "B8:27:EB:4E:36:F0"

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        isGranted: Boolean ->
        if (isGranted) {
            connectToDevice()
        } else {
            Toast.makeText(this, "Bluetooth permission not granted", Toast.LENGTH_SHORT).show()
        }
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createNotificationChannel()
        requestNotificationPermission()
        val btnStartRecord = findViewById<Button>(R.id.startButton)
        val btnStopRecord = findViewById<Button>(R.id.stopButton)

        btnStartRecord.setOnClickListener {
            startRecording()
        }

        btnStopRecord.setOnClickListener {
            stopRecording()
        }
        val webView = findViewById<WebView>(R.id.webView)
        val webSettings = webView.settings
        val streamUrl = "http://192.168.0.109:8080/?action=stream"
        webView.loadUrl(streamUrl)
        webView.settings.javaScriptEnabled = true
        webView.setOnTouchListener { _, _ -> true }


        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.getAdapter()

        enableBluetoothLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Bluetootah not enabled", Toast.LENGTH_SHORT).show()
            }
        }

        if (!bluetoothAdapter.isEnabled) {
            requestBluetoothPerms()
            val enableBT = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBT)
        } else {
            requestBluetoothPerms()
        }

    }
    private fun requestBluetoothPerms() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            connectToDevice()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    private fun connectToDevice() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Bluetooth Connect permission not granted")
            return
        }
        val device = bluetoothAdapter.getRemoteDevice(raspPiAddress)
        Thread {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"))
                bluetoothSocket?.connect()

                inputStream = bluetoothSocket?.inputStream

                runOnUiThread {
                    Toast.makeText(this, "Connected to raspberry Pi", Toast.LENGTH_SHORT).show()
                }
                readDataFromInputStream(inputStream)


            }  catch (e: IOException) {
                Log.e(TAG, "Error connecting to device", e)
                }
        }.start()
    }

    private fun startRecording() {
        sendCommandToPi("start")
    }

    private fun stopRecording() {
        sendCommandToPi("stop")
    }

    private fun sendCommandToPi(command: String) {
        val url = "http://192.168.0.109:5000/$command"
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("MainActivity", "Command sent: $command")
                }
            }
        })
    }
    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notifications for motion detection"

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
    private fun readDataFromInputStream(inputStream: InputStream?) {
        val buffer = ByteArray(1024) // Buffer to store incoming data
        var bytes: Int

        try {
            while (true) {
                bytes = inputStream?.read(buffer) ?: -1
                if (bytes > 0) {
                    val receivedMessage = String(buffer, 0, bytes)
                    if (receivedMessage.contains("Motion Detected")) {
                        showNotification("Motion Detected!")
                    } else if (receivedMessage.contains("Button Pressed")) {
                        showNotification("Button Pressed!")

                    }
                    Log.d("BluetoothMessage", "Received: $receivedMessage")
                }
            }
        } catch (e: IOException) {
            Log.e("BluetoothMessage", "Error reading data", e)
        }
    }


    private fun showNotification(message: String) {

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Motion Alert")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(1, notification)
        Log.d("Notification", "Notification sent")
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Request the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}



