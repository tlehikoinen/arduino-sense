package com.example.arduino_sense

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.lang.Exception

class ControlRoom: AppCompatActivity() {
    private lateinit var disconnectButton: Button
    private lateinit var tempButton: Button
    private lateinit var autoButton: Button
    private lateinit var switchLEDButton: ImageButton
    private lateinit var nickname: EditText
    private var bleController: BLEController? = null
    private var isLEDOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bleController = BLEController.getInstance(this)
        initSwitchLEDButton()
        initDisconnectButton()
        inittempButton()
        initAutoButton()

    }
    private fun switchLED(bleController: BLEController?, on: Boolean) {
        val ledon = ByteArray(1)
        val ledoff = ByteArray(1)
        ledon[0] = 0x1
        if (!on) {
            bleController!!.sendData(ledoff)
        } else {
            bleController!!.sendData(ledon)
        }
    }

    private fun initSwitchLEDButton() {
        switchLEDButton = findViewById(R.id.imageButton3)
        var i = 0
        switchLEDButton.setOnClickListener{
            val images = intArrayOf(R.drawable.lightsensor, R.drawable.img)
            switchLEDButton.setImageResource(images[i])
            i++
            if (i == 2) i = 0
            isLEDOn = !isLEDOn
            switchLED(bleController,isLEDOn)
            toast("LED switched " + if (isLEDOn) "On" else "Off")
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
    private fun initAutoButton() {
        autoButton = findViewById(R.id.auto_button)
        autoButton.setOnClickListener{
            try {
                val auto = ByteArray(1)
                bleController!!.sendMode(auto)
                toast("AutoMode")
            }catch (e: Exception){
                toast("try again $e")
            }
        }
    }

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