package com.example.arduino_sense
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.opengl.Visibility
import android.os.Bundle
import android.provider.Settings
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

var data = AppData()
class MainActivity : AppCompatActivity(), BLEControllerListener {
    private lateinit var logView: TextView
    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var openControlRoom: Button
    private lateinit var openLogin: Button
    //private lateinit var openSignup: Button
    private var bleController: BLEController? = null
    private lateinit var deviceAddress: String
    lateinit var pref: SharedPreferences
    lateinit var coroutineScope: CoroutineScope
    var dataService = DataService()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pref = this.getSharedPreferences("token", Context.MODE_PRIVATE)
        setContentView(R.layout.activity_main)
        readToken()
        bleController = BLEController.getInstance(this)
        logView = findViewById(R.id.logView)
        logView.movementMethod = ScrollingMovementMethod()
        initConnectButton()
        initDisconnectButton()
        initControlRoomButton()
        initOpenLogin()
        //initOpenSignup()
        statusCheck()
        checkBLESupport()
        checkPermissions()
        disableButtons()
    }

    override fun onBackPressed(){
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Do you want to close the app?")
            .setCancelable(false)
            .setPositiveButton("Yes") {
                    _: DialogInterface?, _: Int ->  exitProcess(0)
            }
            .setNegativeButton("No") {
                    dialog: DialogInterface, _: Int -> dialog.cancel()
            }
        val alert = builder.create()
        alert.show()
    }

    private fun readToken() {
        val token = pref.getString("token", "DEFAULT").toString()
        data.setToken(token)
    }
/*
    private fun initOpenSignup() {
        openSignup = findViewById(R.id.btn_open_signup)
        openSignup.setOnClickListener {
            val intent = Intent(this@MainActivity, SignupActivity::class.java)
            startActivity(intent)
        }
    }

 */

    private fun initOpenLogin() {
        openLogin = findViewById(R.id.btn_open_login)
        openLogin.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setMessage("Do you want to log out?")
                .setCancelable(false)
                .setPositiveButton("Yes") {
                        _: DialogInterface?, _: Int ->  loginOrLogout()
                }
                .setNegativeButton("No") {
                        dialog: DialogInterface, _: Int -> dialog.cancel()
                }
            val alert = builder.create()
            alert.show()

        }
        loginOrLogoutText()
    }

    private fun loginOrLogout() {
        if (data.getToken().startsWith("Bearer")) {
            logout()
            finishAffinity()
            val intent = Intent(this@MainActivity, StartActivity::class.java)
            startActivity(intent)
            bleController!!.disconnect()
        } else {
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginOrLogoutText() {
        openLogin.setText(if (data.getToken().startsWith("Bearer ")) "Logout" else "Login")
    }

    private fun logout() {
        data.setToken("")
        val edit = pref.edit()
        edit.putString("token", "")
        edit.commit()
        loginOrLogoutText()
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

    private fun initControlRoomButton() {
        openControlRoom = findViewById(R.id.switchButton)
        openControlRoom.setOnClickListener {
            if (data.getToken().startsWith("Bearer ")) {
                val intent = Intent(this@MainActivity, ControlRoom::class.java)
                startActivity(intent)
            } else {
                toast("Login first")
            }
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
            log("Without this permission Bluetooth devices cannot be searched!")
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
                    _: DialogInterface?, _: Int -> startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            )
            }
            .setNegativeButton("No") {
                    dialog: DialogInterface, _: Int -> dialog.cancel()
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
        data.setToken(pref.getString("token", "DEFAULT")?: "")

        Log.d("tag","resume")
        deviceAddress = ""
        bleController = BLEController.getInstance(this)
        bleController!!.addBLEControllerListener(this)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            log("[BLE]\tSearching for Arduino nano")
            //toast("[BLE]\tSearching for Arduino nano");
            bleController!!.init()
        }

        if (data.getToken().startsWith("Bearer ")) {    // User is now logged in
            //data.fetchData()
            loginOrLogoutText()
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("tag", "PAUSED")
        bleController!!.removeBLEControllerListener(this)
    }

    override fun bleControllerConnected() {
        log("[BLE]\tConnected")
        runOnUiThread {
            disconnectButton.isEnabled = true
            toast("BLE Connected!");
            openControlRoom.isEnabled = true
        }
    }

    override fun bleControllerDisconnected() {
        log("[BLE]\tDisconnected")
        disableButtons()
        runOnUiThread {
            connectButton.isEnabled = true
            toast("BLE Disconnected!")
        }
    }

    override fun bleDeviceFound(name: String, address: String) {
        log("Device $name found with address $address")
        //Toast.makeText(this, "Device " + name + " found with address " + address, Toast.LENGTH_LONG).show();
        deviceAddress = address
        connectButton.isEnabled = true
    }
}

