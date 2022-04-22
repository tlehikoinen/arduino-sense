package com.example.arduino_sense

import android.media.Image
import android.util.Log
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable

// Arduino mode => send 0 for auto, 1 for user controlled
enum class Modes {
    USER("Auto mode", byteArrayOf(1)),
    AUTO("User mode", byteArrayOf(0));

    // custom properties with default values
    var btn_text: String? = null
    var to_arduino: ByteArray? = null

    constructor()

    // custom constructor
    constructor(btn_text: String, to_arduino: ByteArray) {
        this.btn_text = btn_text
        this.to_arduino = to_arduino
    }
}

enum class LedMode {
    OFF(R.drawable.candle_on, byteArrayOf(0)),
    ON(R.drawable.candle_off, byteArrayOf(1));

    var picture: Int? = null
    var to_arduino: ByteArray? = null

    constructor()
    constructor(picture: Int, to_arduino: ByteArray) {
        this.picture = picture
        this.to_arduino = to_arduino
    }
}
class AppData : BaseObservable() {

    private var humidity = 0
    private var temperature = 0
    private var speed_of_fan_user = 0
    private var speed_of_fan_auto = 0
    private var mode = Modes.USER


    private var ledMode: LedMode = LedMode.OFF

    @Bindable
    fun getLedMode(): LedMode {
        return ledMode
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
    fun getMode(): Modes {
        return mode
    }
    fun setMode(value: Modes) {
        mode = value
        notifyPropertyChanged(BR.mode)
    }
    fun toggleMode() {
        mode = if (mode == Modes.USER) Modes.AUTO else Modes.USER
        notifyPropertyChanged(BR.mode)
    }

    @Bindable
    fun getSpeed(): Int {
        return if (mode == Modes.USER) {
            speed_of_fan_user
        } else {
            speed_of_fan_auto
        }
    }
    fun setSpeedUser(value: Int) {
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