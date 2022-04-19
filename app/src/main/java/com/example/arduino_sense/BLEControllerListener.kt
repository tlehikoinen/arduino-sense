package com.example.arduino_sense

interface BLEControllerListener {
    fun bleControllerConnected()
    fun bleControllerDisconnected()
    fun bleDeviceFound(name:String, address:String )
}