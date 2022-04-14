package com.example.arduino_sense
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.text.method.ScrollingMovementMethod
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity(), BLEControllerListener {
    private lateinit var logView: TextView
    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var openControlRoom: Button
    private var bleController: BLEController? = null
    private var deviceAddress: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.connect_layout)
        bleController = BLEController.getInstance(this)
        logView = findViewById(R.id.logView)
        logView.movementMethod = ScrollingMovementMethod()
        initConnectButton()
        initDisconnectButton()
        initControRoomButton()
        statusCheck()
        checkBLESupport()
        checkPermissions()
        disableButtons()
    }

    private fun initConnectButton() {
        connectButton = findViewById(R.id.connectButton)
        connectButton.setOnClickListener {
            connectButton.isEnabled = false
            log("Connecting...")
            //toast("Connecting");
            bleController!!.connectToDevice(deviceAddress)
        }
    }

    private fun initDisconnectButton() {
        disconnectButton = findViewById(R.id.disconnectButton)
        disconnectButton.setOnClickListener {
            disconnectButton.isEnabled = false
            log("Disconnecting...")
            //toast("Disconnecting");
            bleController!!.disconnect()
        }
    }

    private fun initControRoomButton() {
        openControlRoom = findViewById(R.id.switchButton)
        openControlRoom.setOnClickListener {
            val intent = Intent(this@MainActivity, ControlRoom::class.java)
            startActivity(intent)
        }
    }

    private fun disableButtons() {
        runOnUiThread {
            connectButton.isEnabled = false
            disconnectButton.isEnabled = false
            openControlRoom.isEnabled = false
        }
    }

    @SuppressLint("SetTextI18n")
    private fun log(text: String) {
        runOnUiThread {
            logView.text = "${logView.text}\n$text"
        }
    }

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            log("\"Access Fine Location\" permission not granted yet!")
            log("Whitout this permission Blutooth devices cannot be searched!")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                42
            )
        }
    }

    private fun statusCheck() {
        val manager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
        }
    }

    private fun buildAlertMessageNoGps() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes") {
                    dialog: DialogInterface?, id: Int -> startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            )
            }
            .setNegativeButton("No") {
                    dialog: DialogInterface, id: Int -> dialog.cancel()
            }
        val alert = builder.create()
        alert.show()
    }

    private fun checkBLESupport() {
        // Check if BLE is supported on the device.
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            toast("BLE not supported!")
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
                val enableBTIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBTIntent, 1)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        deviceAddress = null
        bleController = BLEController.getInstance(this)
        bleController!!.addBLEControllerListener(this)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            log("[BLE]\tSearching for Arduino nano")
            //toast("[BLE]\tSearching for Arduino nano");
            bleController!!.init()
        }
    }

    override fun onPause() {
        super.onPause()
        bleController!!.removeBLEControllerListener(this)
    }

    override fun BLEControllerConnected() {
        log("[BLE]\tConnected")
        runOnUiThread {
            disconnectButton.isEnabled = true
            //toast("BLE Connected!");
            openControlRoom.isEnabled = true
        }
    }

    override fun BLEControllerDisconnected() {
        log("[BLE]\tDisconnected")
        disableButtons()
        runOnUiThread {
            connectButton.isEnabled = true
            toast("BLE Disconnected!")
        }
    }

    override fun BLEDeviceFound(name: String, address: String) {
        log("Device $name found with address $address")
        //Toast.makeText(this, "Device " + name + " found with address " + address, Toast.LENGTH_LONG).show();
        deviceAddress = address
        connectButton.isEnabled = true
    }
}

