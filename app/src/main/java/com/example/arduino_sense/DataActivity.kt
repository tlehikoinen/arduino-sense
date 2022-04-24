package com.example.arduino_sense

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.BaseObservable
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.arduino_sense.databinding.ActivityDataBinding
import com.example.arduino_sense.databinding.ControlRoomLayoutBinding

import com.example.arduino_sense.databinding.DataItemBinding
class AdapterData : BaseObservable() {

}
class DataActivity : AppCompatActivity() {
    lateinit var binding: ActivityDataBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_data)
        val recyclerView = binding.rvSensorData
        val sensorData = data.getData()
        val adapter = DataAdapter(sensorData)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

}