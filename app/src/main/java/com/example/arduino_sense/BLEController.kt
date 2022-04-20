package com.example.arduino_sense

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class BLEController private constructor(ctx: Context) {
    private var scanner: BluetoothLeScanner? = null
    private var device: BluetoothDevice? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var descriptor: BluetoothGattDescriptor? = null
    private val bluetoothManager: BluetoothManager = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private lateinit var tempvalue: ByteArray
    private lateinit var modeValue: ByteArray
    private lateinit var speedValue: ByteArray
    private var btGattCharLed: BluetoothGattCharacteristic? = null
    private var btGattCharTemp: BluetoothGattCharacteristic? = null
    private var btGattCharHumidity: BluetoothGattCharacteristic? = null
    private var btGattCharMode: BluetoothGattCharacteristic? = null
    private var btGattCharSpeed: BluetoothGattCharacteristic? = null
    private val listeners = ArrayList<BLEControllerListener>()
    private val devices = HashMap<String, BluetoothDevice>()

    fun addBLEControllerListener(l: BLEControllerListener) {
        if (!listeners.contains(l)) listeners.add(l)
    }

    fun removeBLEControllerListener(l: BLEControllerListener) {
        listeners.remove(l)
    }

    @SuppressLint("MissingPermission")
    fun init() {
        devices.clear()
        scanner = bluetoothManager.adapter.bluetoothLeScanner
        scanner!!.startScan(bleCallback)
    }

    private val bleCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (!devices.containsKey(device.address) && isThisTheDevice(device)) {
                deviceFound(device)
            }
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            for (sr in results) {
                val device = sr.device
                if (!devices.containsKey(device.address) && isThisTheDevice(device)) {
                    deviceFound(device)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.i("[BLE]", "scan failed with error code: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    private fun isThisTheDevice(device: BluetoothDevice): Boolean {
        return null != device.name && device.name.startsWith("Nano")
    }

    private fun deviceFound(device: BluetoothDevice) {
        devices[device.address] = device
        fireDeviceFound(device)
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(address: String) {
        device = devices[address]
        scanner!!.stopScan(bleCallback)
        //Log.i("[BLE]", "connect to device " + device.getAddress());
        bluetoothGatt = device!!.connectGatt(null, false, bleConnectCallback)
    }

    private val bleConnectCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i("[BLE]", "start service discovery " + bluetoothGatt!!.discoverServices())
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    btGattCharTemp = null
                    btGattCharLed = null
                    btGattCharHumidity = null
                    btGattCharMode = null
                    btGattCharSpeed = null
                    Log.w("[BLE]", "DISCONNECTED with status $status")
                    fireDisconnected()
                }
                else -> {
                    Log.i("[BLE]", "unknown state $newState and status $status")
                }
            }
        }


        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                fireConnected()
                val services = gatt.services
                for (service in services) {
                    val characteristics = service.characteristics
                    for (characteristic in characteristics) {
                        if (characteristic.uuid.toString().startsWith("00002a57")) {
                            btGattCharLed = characteristic
                        }
                        if (characteristic.uuid.toString().startsWith("00002ba3")) {
                            btGattCharMode = characteristic
                            bluetoothGatt!!.readCharacteristic(btGattCharMode)
                        }
                        if (characteristic.uuid.toString().startsWith("00002a6e")) {
                            btGattCharTemp = characteristic
                            bluetoothGatt!!.readCharacteristic(btGattCharTemp)
                        }
                        if (characteristic.uuid.toString().startsWith("00002a6f")) {
                            btGattCharHumidity = characteristic
                        }
                        if (characteristic.uuid.toString().startsWith("00002a67")) {
                            btGattCharSpeed = characteristic
                            gatt.setCharacteristicNotification(btGattCharSpeed, true)
                            descriptor = characteristic.getDescriptor(UUID
                                .fromString("00002902-0000-1000-8000-00805f9b34fb"))
                            descriptor!!.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                            bluetoothGatt!!.writeDescriptor(descriptor)

                        }
                        Log.i("[BLE]", "CONNECTED")
                    }
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.i("BluetoothGattCallback", "Wrote to characteristic $uuid | value: ${value.toString()}")
                    }
                    BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> {
                        Log.e("BluetoothGattCallback", "Write exceeded connection ATT MTU!")
                    }
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
                        Log.e("BluetoothGattCallback", "Write not permitted for $uuid!")
                    }
                    else -> {
                        Log.e("BluetoothGattCallback", "Characteristic write failed for $uuid, error: $status")
                    }
                }
            }

//            super.onCharacteristicWrite(gatt, characteristic, status)
//            Log.i("TAG", "Characteristic " + characteristic.uuid + " written")
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    if (characteristic == btGattCharTemp) {
                        Log.d("REAd", "get temp")

                        tempvalue = characteristic.value
                        btGattCharTemp!!.value = tempvalue
                        Log.i("REAd", tempvalue[0].toString())
                    }
                    if(characteristic==btGattCharMode){
                        Log.i("REAd", "get char mode")
                        modeValue = characteristic.value
                        btGattCharMode!!.value=modeValue
                        Log.i("REAd", modeValue[0].toString())
                    }
                    Log.i("BluetoothGattCallback", "Readcharacteristic $characteristic")
                }
                BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> {
                    Log.e("BluetoothGattCallback", "Read exceeded connection ATT MTU!")
                }
                BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
                    Log.e("BluetoothGattCallback", "Read not permitted for!")
                }
                else -> {
                    Log.e("BluetoothGattCallback", "Characteristic read failed, error: $status")
                }
            }
        }
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            if (characteristic == btGattCharSpeed) {
                Log.d("REAd", "get speed")
                speedValue = characteristic.value
                Log.i("REAd", speedValue[0].toString())
            }
        }
    }


    private fun fireDisconnected() {
        for (l in listeners) l.bleControllerDisconnected()
        device = null
    }

    private fun fireConnected() {
        for (l in listeners) l.bleControllerConnected()
    }

    @SuppressLint("MissingPermission")
    private fun fireDeviceFound(device: BluetoothDevice) {
        for (l in listeners) l.bleDeviceFound(device.name.trim { it <= ' ' }, device.address)
    }

    @SuppressLint("MissingPermission") //led
    fun sendLEDData(data: ByteArray?) {
        btGattCharLed!!.value = data
        bluetoothGatt!!.writeCharacteristic(btGattCharLed)
    }

    @SuppressLint("MissingPermission")
    fun sendMode(data: ByteArray?) {
        btGattCharMode!!.value = data
        bluetoothGatt!!.writeCharacteristic(btGattCharMode)
    }

    @SuppressLint("MissingPermission")
    fun getMode(): ByteArray {
        bluetoothGatt!!.readCharacteristic(btGattCharMode)
        Thread.sleep(200)
        return btGattCharMode!!.value
    }


    @SuppressLint("MissingPermission")
    fun sendSpeed(data: ByteArray?) {
        btGattCharSpeed!!.value = data
        bluetoothGatt!!.writeCharacteristic(btGattCharSpeed)
    }

    @SuppressLint("MissingPermission")
    fun readi(): ByteArray {
        bluetoothGatt!!.readCharacteristic(btGattCharTemp)
        return tempvalue
    }

    fun read(): String {
        return readi()[0].toString()
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt!!.disconnect()
    }

    companion object {
        private var instance: BLEController? = null
        fun getInstance(ctx: Context): BLEController? {
            if (null == instance) instance = BLEController(
                ctx
            )
            return instance
        }
    }

}