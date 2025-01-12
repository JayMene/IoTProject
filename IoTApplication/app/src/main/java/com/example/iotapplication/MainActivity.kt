package com.example.iotapplication

import android.app.Activity
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
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

class MainActivity : ComponentActivity() {

    private val TAG = "BluetoothConnectivity";
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothSocket: BluetoothSocket? = null
    private var bluetoothDevice: BluetoothDevice? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private lateinit var enableBluetoothLauncher: ActivityResultLauncher<Intent>



    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
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
        setContent {
            BluetoothUI()

        }
        setContentView(R.layout.activity_main)

        // Find the WebView by its ID
        val webView = findViewById<WebView>(R.id.webView)

        // Enable JavaScript (if needed)
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true

        // Load the MJPEG stream URL
        val streamUrl = "http://192.168.0.109:8080/?action=stream"
        webView.loadUrl(streamUrl)
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
                outputStream = bluetoothSocket?.outputStream

                runOnUiThread {
                    Toast.makeText(this, "Connected to raspberry Pi", Toast.LENGTH_SHORT).show()
                }

                // Send a test message
                outputStream?.write("Hello from Android!".toByteArray())
            }  catch (e: IOException) {
                Log.e(TAG, "Error connecting to device", e)
                }
        }.start()
    }

    @Composable
    fun BluetoothUI() {
        Button(onClick = {
            requestBluetoothPerms()
        }) {
            Text("Connect to Raspberry Pi")
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        IoTApplicationTheme {
            BluetoothUI()
        }
    }

}



