package com.example.arduino_sense

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import android.graphics.Color
import android.util.Log
import kotlinx.coroutines.launch


// Arduino mode => send 0 for auto, 1 for user controlled
enum class FanModes {
    USER("Auto mode", Color.BLACK, byteArrayOf(1)),
    AUTO("User mode", Color.WHITE, byteArrayOf(0));

    // custom properties with default values
    var btn_text: String? = null
    var btn_color: Int? = null
    var to_arduino: ByteArray? = null

    constructor()

    // custom constructor
    constructor(btn_text: String, btn_color: Int, to_arduino: ByteArray) {
        this.btn_text = btn_text
        this.to_arduino = to_arduino
        this.btn_color = btn_color
    }
}

enum class LedMode {
    OFF(Color.GRAY, byteArrayOf(0)),
    ON(Color.rgb(206,147,216), byteArrayOf(1));

    var led_color: Int? = null
    var to_arduino: ByteArray? = null

    constructor()
    constructor(led_color: Int, to_arduino: ByteArray) {
        this.led_color = led_color
        this.to_arduino = to_arduino
    }
}

class AppData : BaseObservable() {

    private var humidity = 0
    private var temperature = 0
    private var speed_of_fan_user = 0
    private var speed_of_fan_auto = 0
    private var mode = FanModes.USER
    private var ledMode: LedMode = LedMode.OFF
    private var username = ""
    private var token = ""
    private var sensorData: List<TempHumidJsonModel>? = null
    private var isDataViewEnabled: Boolean = false
    private var isFanOffEnabled: Boolean = true
    private var fanTextColor: Int = 0

    @Bindable
    fun getIsFanOffEnabled(): Boolean {
        return isFanOffEnabled

    }
    fun setIsFanOfEnabled(value: Boolean) {
        isFanOffEnabled = value
        notifyPropertyChanged(BR.isFanOffEnabled)
        notifyPropertyChanged(BR.fanTextColor)
    }

    @Bindable
    fun getFanTextColor(): Int {
        return if (isFanOffEnabled) {
            Color.CYAN
        } else {
            Color.GRAY
        }
    }

    @Bindable
    fun getIsViewEnabled(): Boolean {
        return isDataViewEnabled
    }

    fun setIsViewEnabled(value: Boolean) {
        isDataViewEnabled = value
        notifyPropertyChanged(BR.isViewEnabled)
    }


    fun fetchData() {
        var dataService = DataService()
        Log.d("fetch", username)

        Log.d("track", "Fetch started")
        dataService.fetchUserData(username, object : DataService.DataCallback {
            override fun onSuccess(data: List<TempHumidJsonModel>?) {
                Log.d("fetch", "fetch data success")
                sensorData = data
                setIsViewEnabled(true)  // Data view can only be opened after successful request
            }
            override fun onFailure(message: String) {
                Log.e("fetch", "data fetch failed")
                setIsViewEnabled(false)
            }
        })
        notifyPropertyChanged(BR.sensorData)
    }
    @Bindable
    fun getData(): List<TempHumidJsonModel>? {
        return sensorData
    }

    fun getToken(): String {
        return token
    }
    fun setToken(value: String) {
        token = value
    }
    @Bindable
    fun getLedMode(): LedMode {
        return ledMode
    }

    @Bindable
    fun getUsername(): String {
        return username
    }
    fun setUsername(value: String) {
        username = value
    }

    fun setLedMode(value: LedMode) {
        ledMode = value
        notifyPropertyChanged(BR.ledMode)
    }

    fun toggleLed() {
        ledMode = if (ledMode == LedMode.OFF) LedMode.ON else LedMode.OFF
        notifyPropertyChanged(BR.ledMode)
    }

    @Bindable
    fun getMode(): FanModes {
        return mode
    }
    fun setMode(value: FanModes) {
        setIsFanOfEnabled(true)
        mode = value
        notifyPropertyChanged(BR.mode)
    }
    fun toggleMode() {
        setIsFanOfEnabled(true)
        mode = if (mode == FanModes.USER) FanModes.AUTO else FanModes.USER
        notifyPropertyChanged(BR.mode)
    }

    @Bindable
    fun getSpeed(): Int {
        return if (mode == FanModes.USER) {
            speed_of_fan_user
        } else {
            speed_of_fan_auto
        }
    }
    fun setSpeedUser(value: Int) {
        setIsFanOfEnabled(true)
        speed_of_fan_user = value
        notifyPropertyChanged(BR.speed)
    }
    fun setSpeedAuto(value: Int) {
        speed_of_fan_auto = value
        notifyPropertyChanged(BR.speed)
    }

    @Bindable
    fun getHumidity(): String {
        return humidity.toString()
    }
    fun setHumidity(value: Int) {
        humidity = value
        notifyPropertyChanged(BR.humidity)
    }

    @Bindable
    fun getTemperature(): String {

        return temperature.toString()
    }
    fun setTemperature(value: Int) {
        temperature = value
        notifyPropertyChanged(BR.temperature)
    }
}
