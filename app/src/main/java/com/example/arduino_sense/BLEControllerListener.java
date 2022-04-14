package com.example.arduino_sense;

public interface BLEControllerListener {
    void BLEControllerConnected();
    void BLEControllerDisconnected();
    void BLEDeviceFound(String name, String address);
}
