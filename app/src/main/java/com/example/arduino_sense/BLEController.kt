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

enum class ActionType {
    writeDescriptor, readCharacteristic, writeCharacteristic
}

class BLEController private constructor(ctx: Context) {
    private var scanner: BluetoothLeScanner? = null
    private var device: BluetoothDevice? = null
    private lateinit var bluetoothGatt: BluetoothGatt
    private lateinit var descriptor: BluetoothGattDescriptor
    private val bluetoothManager: BluetoothManager = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private lateinit var btGattCharLed: BluetoothGattCharacteristic
    private lateinit var btGattCharTemp: BluetoothGattCharacteristic
    private lateinit var btGattCharHumidity: BluetoothGattCharacteristic
    private lateinit var btGattCharMode: BluetoothGattCharacteristic
    private lateinit var btGattCharSpeed: BluetoothGattCharacteristic
    private val listeners = ArrayList<BLEControllerListener>()
    private val devices = HashMap<String, BluetoothDevice>()

    inner class Action(val type: ActionType, val `object`: Any)

    private val bleQueue: Queue<Action> = LinkedList<Action>()

    fun writeDescriptor(descriptor: BluetoothGattDescriptor) {
        addAction(ActionType.writeDescriptor, descriptor)
    }

    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        addAction(ActionType.readCharacteristic, characteristic)
    }

    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic) {
        addAction(ActionType.writeCharacteristic, characteristic)
    }

    private fun addAction(actionType: ActionType, `object`: Any) {
        bleQueue.add(Action(actionType, `object`))
        // if there is only 1 item in the queue, then process it. If more than
        // 1,
        // we handle asynchronously in the callback.
        if (bleQueue.size === 1) nextAction()
    }

    @SuppressLint("MissingPermission")
    private fun nextAction() {
        if (bleQueue.isEmpty()) return
        val action: Action = bleQueue.element()
        if (ActionType.writeDescriptor == action.type) {
            bluetoothGatt.writeDescriptor(
                action
                    .`object` as BluetoothGattDescriptor
            )
        } else if (ActionType.writeCharacteristic == action.type) {
            bluetoothGatt
                .writeCharacteristic(
                    action
                        .`object` as BluetoothGattCharacteristic
                )
        } else if (ActionType.readCharacteristic == action.type) {
            bluetoothGatt
                .readCharacteristic(
                    action
                        .`object` as BluetoothGattCharacteristic
                )
        } else {
            Log.e("BLEQueue", "Undefined Action found")
        }
    }

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
//                    btGattCharTemp = null
//                    btGattCharLed = null
//                    btGattCharHumidity = null
//                    btGattCharMode = null
//                    btGattCharSpeed = null
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
                            readCharacteristic(btGattCharLed)   // Led state is read at start, Currently Arduino switches led off on ble disconnect.
                        }
                        if (characteristic.uuid.toString().startsWith("00002ba3")) {
                            btGattCharMode = characteristic
                            readCharacteristic(btGattCharMode)
                        }
                        if (characteristic.uuid.toString().startsWith("00002a6e")) {
                            btGattCharTemp = characteristic
                            gatt.setCharacteristicNotification(btGattCharTemp, true)
                            descriptor = characteristic.getDescriptor(UUID
                                .fromString("00002902-0000-1000-8000-00805f9b34fb"))
                            descriptor!!.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                            writeDescriptor(descriptor!!)
                            readCharacteristic(btGattCharTemp)
                        }
                        if (characteristic.uuid.toString().startsWith("00002a6f")) {
                            btGattCharHumidity = characteristic
                            gatt.setCharacteristicNotification(btGattCharHumidity, true)
                            descriptor = characteristic.getDescriptor(UUID
                                .fromString("00002902-0000-1000-8000-00805f9b34fb"))
                            descriptor!!.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                            writeDescriptor(descriptor!!)
                            readCharacteristic(btGattCharHumidity)
                        }
                        if (characteristic.uuid.toString().startsWith("00002a67")) {
                            btGattCharSpeed = characteristic
                            gatt.setCharacteristicNotification(btGattCharSpeed, true)
                            descriptor = characteristic.getDescriptor(UUID
                                .fromString("00002902-0000-1000-8000-00805f9b34fb"))
                            descriptor!!.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                            writeDescriptor(descriptor!!)
                        }
                        Log.i("[BLE]", "CONNECTED")
                    }
                }
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    Log.i("BluetoothGattCallback", "Wrote to descriptor $gatt")
                }
                BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> {
                    Log.e("BluetoothGattCallback", "Write exceeded connection ATT MTU!")
                }
                BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
                    Log.e("BluetoothGattCallback", "Write not permitted!")
                }
                else -> {
                    Log.e("BluetoothGattCallback", "Characteristic description write failed error: $status")
                }
            }
            bleQueue.remove()
            nextAction()
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
            bleQueue.remove()
            nextAction()
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
                        data.setTemperature(characteristic.value[0].toString().toInt())
                    }
                    if(characteristic == btGattCharMode){
                        data.setMode(if (characteristic.value[0].toString().toInt() == 0) Modes.AUTO else Modes.USER)
                    }
                    if (characteristic == btGattCharHumidity) {
                        data.setHumidity(characteristic.value[0].toString().toInt())
                    }
                    if (characteristic == btGattCharSpeed) {
                        if (data.getMode() == Modes.USER) {
                            data.setSpeedUser(characteristic.value[0].toString().toInt())
                        } else {
                            data.setSpeedAuto(characteristic.value[0].toString().toInt())
                        }
                    }
                    if (characteristic == btGattCharLed) {
                        data.setLedMode(if (characteristic.value[0].toString().toInt() == 0) LedMode.OFF else LedMode.ON)
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
            bleQueue.remove()
            nextAction()
        }
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            //Log.d("Tag", "charasteristic changed")
            if (characteristic == btGattCharSpeed) {
                data.setSpeedAuto(characteristic.value[0].toString().toInt())
            }
            if (characteristic == btGattCharTemp) {
                data.setTemperature(characteristic.value[0].toString().toInt())
            }
            if (characteristic == btGattCharHumidity) {
                data.setHumidity(characteristic.value[0].toString().toInt())
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
        writeCharacteristic(btGattCharLed)
    }

    @SuppressLint("MissingPermission")
    fun sendMode(data: ByteArray?) {
        btGattCharMode!!.value = data
        writeCharacteristic(btGattCharMode)
    }

    @SuppressLint("MissingPermission")
    fun getMode(): ByteArray {
        readCharacteristic(btGattCharMode)
        return btGattCharMode!!.value
    }

    @SuppressLint("MissingPermission")
    fun sendSpeed(data: ByteArray?) {
        btGattCharSpeed!!.value = data
        writeCharacteristic(btGattCharSpeed)
    }

    @SuppressLint("MissingPermission")
    fun readTemp() {
        readCharacteristic(btGattCharTemp)
    }

    @SuppressLint("MissingPermission")
    fun readSpeed() {
        readCharacteristic(btGattCharSpeed)
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
