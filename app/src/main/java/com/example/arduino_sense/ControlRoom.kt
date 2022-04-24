package com.example.arduino_sense

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.arduino_sense.databinding.ControlRoomLayoutBinding
import kotlinx.coroutines.*
import java.util.*

/* Note about seekbar:
Seekbar does not have xml property for being greyed out.
This means the enabled/disabled state can't be binded to mode changes.
It is possible to disable response to thumb activity by returning true from onTouchListener...
... when mode is "Auto".
But to notify user more clearly if the seekbar is enabled for change...
... we are calling setSpeedBarVisibility() from every function that changes state.
This programmatically enables/disables the seekbar.
*/

class ControlRoom: AppCompatActivity() {
    private var bleController: BLEController? = null
    private lateinit var binding: ControlRoomLayoutBinding
    private var dataService = DataService()
    private lateinit var job: Job
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.control_room_layout)
        bleController = BLEController.getInstance(this)

        binding = DataBindingUtil.setContentView(this, R.layout.control_room_layout)
        binding.datas = data
        binding.autoButton.setOnClickListener { toggleMode() }
        binding.btnGetTemp.setOnClickListener { getTemp() }
        binding.offButton.setOnClickListener { turnFanOff() }
        binding.btnDisconnect.setOnClickListener { disconnectBle() }
        binding.speedBar.setOnSeekBarChangeListener(speedBarListener())
        binding.speedBar.setOnTouchListener(speedBarState())    // Note about seekbar
        binding.imgBtnLed.setOnClickListener { toggleLed() }
        binding.btnOpenTempData.setOnClickListener{ openData() }
        initMode()
    }

    override fun onPause() {
        super.onPause()
        job.cancel()
    }
    override fun onResume() {
        super.onResume()
        job = startRepeatingJob(300000) // 5 minutes
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    // Save data to server every n ms
    private fun startRepeatingJob(timeInterval: Long): Job {
        return CoroutineScope(Dispatchers.Default).launch {
            while (NonCancellable.isActive) {
                dataService.postData(data.getToken(),
                    PostDataReq(data.getTemperature().toInt(), data.getHumidity().toInt()))
                data.fetchData()
                delay(timeInterval)
            }
        }
    }

    fun littleEndianConversion(bytes: ByteArray): Int {
        var result = 0
        for (i in bytes.indices) {
            result = result or (bytes[i].toInt() shl 8 * i)
        }
        return result
    }

    fun openData() {
        try {
            val intent = Intent(this, DataActivity::class.java)
            startActivity(intent)
        } catch(e: Exception) {
            toast("Failed")
        }
    }

    fun numberToByteArray (data: Number, size: Int = 4) : ByteArray =
        ByteArray (size) {i -> (data.toLong() shr (i*8)).toByte()}

    private fun initMode() {
        data.setMode(if (littleEndianConversion(bleController!!.getMode()) == 0) Modes.AUTO else Modes.USER)
        bleController!!.readSpeed()
        Log.d("REAd", data.getMode().toString())
        setSpeedBarVisibility()
    }

    private fun speedBarListener(): SeekBar.OnSeekBarChangeListener {

        return object :
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
        }
    }
    fun speedBarState(): View.OnTouchListener {
        // Disable changes on seekbar by in auto mode
        return View.OnTouchListener { p0, p1 -> data.getMode() == Modes.AUTO }
    }

    private fun changeFanSpeed(speed: Int) {
        data.setSpeedUser(speed)
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

    private fun toggleLed() {
        data.toggleLed()
        bleController!!.sendLEDData(data.getLedMode().to_arduino)
    }

    private fun setSpeedBarVisibility() {
        binding.speedBar.isEnabled = data.getMode() == Modes.USER
    }

    private fun turnFanOff() {
        data.setMode(Modes.USER)
        data.setSpeedUser(0)
        bleController!!.sendMode(data.getMode().to_arduino)
        bleController!!.sendSpeed(byteArrayOf(0))
        setSpeedBarVisibility()
    }

    private fun toggleMode() {
        try {
            toast("Toggled mode to ${data.getMode().btn_text}")
            data.toggleMode()
            bleController!!.sendMode(data.getMode().to_arduino)
            bleController!!.sendSpeed(numberToByteArray(data.getSpeed()))
            //binding.speedBar.isEnabled = false
            setSpeedBarVisibility()

        } catch (e: Exception){
            toast("try again $e")
        }
    }

    private fun getTemp() {
        try {
            bleController!!.readTemp()
        } catch (e: Exception){
            toast("try again $e")
        }
    }

    private fun disconnectBle() {
        toast("Disconnecting")
        bleController!!.disconnect()
        val intent = Intent(this@ControlRoom, MainActivity::class.java)
        startActivity(intent)
    }
    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }
}