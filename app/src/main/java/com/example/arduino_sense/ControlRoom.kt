package com.example.arduino_sense

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.arduino_sense.databinding.ActivityMainBinding
import java.lang.Exception

// Arduino mode => send 0 for auto, 1 for user controlled
enum class Mode { USER, AUTO }
data class FanMode(val btn_text: String, val mode: Mode, val to_arduino: ByteArray)
val modes: List<FanMode> = listOf(
    FanMode("User", Mode.AUTO, byteArrayOf(0)),
    FanMode("Auto", Mode.USER, byteArrayOf(1))
)

data class FanSpeed(var speed: Int)

class ControlRoom: AppCompatActivity() {
    private lateinit var disconnectButton: Button
    private lateinit var tempButton: Button
    private lateinit var modeButton: Button
    private lateinit var fanOffButton: Button
    private lateinit var switchLEDButton: ImageButton
    private lateinit var nickname: EditText
    private lateinit var speedBar: SeekBar
    private var bleController: BLEController? = null
    private var isLEDOn = false
    private var mode = modes[1] // User mode default
//    private lateinit var binding: ActivityMainBinding
//    private var fanSpeed = FanSpeed(2)
    private var fanSpeed = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bleController = BLEController.getInstance(this)
        initButtons()
        initSpeedBar()

//        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
//        binding.fanSpeed = fanSpeed
//        binding.autoButton.setOnClickListener { toggleMode() }

        //

    }

    private fun initButtons() {
        initSwitchLEDButton()
        initDisconnectButton()
        inittempButton()
        initToggleModeButton()
        initFanOffButton()
    }

    private fun initSpeedBar() {
        speedBar = findViewById(R.id.speedBar)

        val seek = findViewById<SeekBar>(R.id.speedBar)
        seek?.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar,
                                           progress: Int, fromUser: Boolean) {
                // write custom code for progress is changed
            }
            override fun onStartTrackingTouch(seek: SeekBar) {
                // write custom code for progress is started
            }
            override fun onStopTrackingTouch(seek: SeekBar) {
                // write custom code for progress is stopped
                changeFanSpeed(seek.progress.toInt())
            }
        })

    }

    private fun changeFanSpeed(speed: Int) {
        toast(speed.toString())
        fanSpeed = speed
        bleController!!.sendSpeed(byteArrayOf(speed.toByte()))

    }
    private fun switchLED(bleController: BLEController?, on: Boolean) {
        val ledon = ByteArray(1)
        val ledoff = ByteArray(1)
        ledon[0] = 0x1
        if (!on) {
            bleController!!.sendLEDData(ledoff)
        } else {
            bleController!!.sendLEDData(ledon)
        }
    }

    private fun initSwitchLEDButton() {
        switchLEDButton = findViewById(R.id.imageButton3)
        switchLEDButton.setOnClickListener{
            val images = intArrayOf(R.drawable.candle_off, R.drawable.candle_on)
            switchLEDButton.setImageResource(images[if (isLEDOn) 1 else 0])
            isLEDOn = !isLEDOn
            switchLED(bleController,isLEDOn)
            toast("LED switched " + if (isLEDOn) "On" else "Off")
        }
    }

    private fun initFanOffButton() {
        fanOffButton = findViewById(R.id.off_button)
        fanOffButton.setOnClickListener{
            mode = modes[1] // Set mode to user

            bleController!!.sendMode(mode.to_arduino)
            Thread.sleep(200);  // Wait before sending new packet
            fanSpeed = 0
            bleController!!.sendSpeed(byteArrayOf(fanSpeed.toByte()))
            modeButton.setText(mode.btn_text)
        }
    }
    private fun inittempButton() {
        tempButton = findViewById(R.id.button23)
        tempButton.setOnClickListener{
            try {
                nickname=findViewById(R.id.nick_name)
                nickname.setText(bleController!!.read())
            }catch (e: Exception){
                toast("try again $e")
            }
        }
    }
    private fun initToggleModeButton() {
        modeButton = findViewById(R.id.auto_button)
        modeButton.setText(mode.btn_text)
        modeButton.setOnClickListener{
            try {
                toast("Toggled mode to ${mode.btn_text}")
                mode = if (mode == modes[0]) modes[1] else modes[0]
                bleController!!.sendMode(mode.to_arduino)
                modeButton.setText(mode.btn_text)

            } catch (e: Exception){
                toast("try again $e")
            }
        }
    }

//    private fun toggleMode() {
//        try {
//            toast("Toggled mode to ${mode.btn_text}")
//            mode = if (mode == modes[0]) modes[1] else modes[0]
//            bleController!!.sendMode(mode.to_arduino)
//            modeButton.setText(mode.btn_text)
//
//        } catch (e: Exception){
//            toast("try again $e")
//        }
//    }

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }
    private fun initDisconnectButton() {
        disconnectButton = findViewById(R.id.button24)
        disconnectButton.setOnClickListener{
            toast("Disconnecting")
            bleController!!.disconnect()
            val intent = Intent(this@ControlRoom, MainActivity::class.java)
            startActivity(intent)
        }
    }
}