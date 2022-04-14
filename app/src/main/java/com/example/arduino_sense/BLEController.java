package com.example.arduino_sense;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class BLEController {
    private static BLEController instance;
    private BluetoothLeScanner scanner;
    private BluetoothDevice device;
    private BluetoothGatt bluetoothGatt;
    private final BluetoothManager bluetoothManager;
    byte[] value;

    private BluetoothGattCharacteristic btGattChar_led = null;
    private BluetoothGattCharacteristic btGattChar_temp = null;
    private BluetoothGattCharacteristic btGattChar_humidity = null;
    private BluetoothGattCharacteristic btGattChar_mode = null;
    private BluetoothGattCharacteristic btGattChar_speed = null;

    private final ArrayList<BLEControllerListener> listeners = new ArrayList<>();
    private final HashMap<String, BluetoothDevice> devices = new HashMap<>();

    private BLEController(Context ctx) {
        this.bluetoothManager = (BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE);
    }

    public static BLEController getInstance(Context ctx) {
        if(null == instance)
            instance = new BLEController((ctx));

        return instance;
    }

    public void addBLEControllerListener(BLEControllerListener l) {
        if(!this.listeners.contains(l))
            this.listeners.add(l);
    }

    public void removeBLEControllerListener(BLEControllerListener l) {
        this.listeners.remove(l);
    }

    @SuppressLint("MissingPermission")
    public void init() {
        this.devices.clear();
        this.scanner = this.bluetoothManager.getAdapter().getBluetoothLeScanner();
        scanner.startScan(bleCallback);
    }

    private final ScanCallback bleCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if(!devices.containsKey(device.getAddress()) && isThisTheDevice(device)) {
                deviceFound(device);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for(ScanResult sr : results) {
                BluetoothDevice device = sr.getDevice();
                if(!devices.containsKey(device.getAddress()) && isThisTheDevice(device)) {
                    deviceFound(device);
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.i("[BLE]", "scan failed with errorcode: " + errorCode);
        }
    };

    @SuppressLint("MissingPermission")
    private boolean isThisTheDevice(BluetoothDevice device) {
        return null != device.getName() && device.getName().startsWith("Nano");
    }

    private void deviceFound(BluetoothDevice device) {
        this.devices.put(device.getAddress(), device);
        fireDeviceFound(device);
    }

    @SuppressLint("MissingPermission")
    public void connectToDevice(String address) {
        this.device = this.devices.get(address);
        this.scanner.stopScan(this.bleCallback);
        //Log.i("[BLE]", "connect to device " + device.getAddress());
        this.bluetoothGatt = device.connectGatt(null, false, this.bleConnectCallback);
    }

    private final BluetoothGattCallback bleConnectCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("[BLE]", "start service discovery " + bluetoothGatt.discoverServices());
            }else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                btGattChar_temp = null;
                btGattChar_led = null;
                btGattChar_humidity=null;
                btGattChar_mode=null;
                btGattChar_speed=null;
                Log.w("[BLE]", "DISCONNECTED with status " + status);
                fireDisconnected();
            }else {
                Log.i("[BLE]", "unknown state " + newState + " and status " + status);
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                fireConnected();
                List<BluetoothGattService> services = gatt.getServices();
                for (BluetoothGattService service : services) {
                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                    for (BluetoothGattCharacteristic characteristic: characteristics){

                        if (characteristic.getUuid().toString().startsWith("00002a57")){
                            btGattChar_led=characteristic;
                        }
                        if (characteristic.getUuid().toString().startsWith("00002ba3")){
                            btGattChar_mode=characteristic;
                        }
                        if (characteristic.getUuid().toString().startsWith("00002a6e")){
                            btGattChar_temp=characteristic;
                            bluetoothGatt.readCharacteristic(btGattChar_temp);
                        }
                        if (characteristic.getUuid().toString().startsWith("00002a6f")){
                            btGattChar_humidity=characteristic;
                        }
                        if (characteristic.getUuid().toString().startsWith("00002a67")){
                            btGattChar_speed=characteristic;
                        }
                        Log.i("[BLE]", "CONNECTED");

                    }
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.i("TAG", "Characteristic " + characteristic.getUuid() + " written");
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            value = characteristic.getValue();
            btGattChar_temp.setValue(value);

            Log.i("READ", Arrays.toString(value));
            Log.i("REAd", String.valueOf(value[0]));
        }


    };

    private void fireDisconnected() {
        for(BLEControllerListener l : this.listeners)
            l.BLEControllerDisconnected();

        this.device = null;
    }

    private void fireConnected() {
        for(BLEControllerListener l : this.listeners)
            l.BLEControllerConnected();
    }

    @SuppressLint("MissingPermission")
    private void fireDeviceFound(BluetoothDevice device) {
        for(BLEControllerListener l : this.listeners)
            l.BLEDeviceFound(device.getName().trim(), device.getAddress());
    }

    @SuppressLint("MissingPermission")
    //led
    public void sendData(byte [] data) {
        this.btGattChar_led.setValue(data);
        bluetoothGatt.writeCharacteristic(this.btGattChar_led);
    }
    @SuppressLint("MissingPermission")
    public void sendMode(byte [] data) {
        this.btGattChar_mode.setValue(data);
        bluetoothGatt.writeCharacteristic(this.btGattChar_mode);
    }
    @SuppressLint("MissingPermission")
    public void sendSpeed(byte [] data) {
        this.btGattChar_speed.setValue(data);
        bluetoothGatt.writeCharacteristic(this.btGattChar_speed);
    }


    @SuppressLint("MissingPermission")
    public byte[] readi(){
        bluetoothGatt.readCharacteristic(btGattChar_temp);
        return value;
    }
    public String read(){
        return String.valueOf(readi()[0]);
    }

    @SuppressLint("MissingPermission")
    public void disconnect() {
        this.bluetoothGatt.disconnect();
    }
}


