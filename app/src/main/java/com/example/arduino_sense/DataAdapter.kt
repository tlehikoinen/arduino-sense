package com.example.arduino_sense

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.arduino_sense.databinding.DataItemBinding

class SensorDataHolder(
    private val binding: DataItemBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(sensorData: TempHumidJsonModel) {
        binding.sensorData = sensorData
    }
}
class DataAdapter(
    private val sensorData: List<TempHumidJsonModel>?
    ) : RecyclerView.Adapter<SensorDataHolder>() {

    private lateinit var binding: DataItemBinding

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): SensorDataHolder {
        binding = DataItemBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.data_item, viewGroup, false)

        return SensorDataHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: SensorDataHolder, position: Int) {
        val data = sensorData?.get(position)
        if (data != null) {
            val modifiedData = parseData(data)
            viewHolder.bind(modifiedData)
        }
    }

    override fun getItemCount() = sensorData?.size ?: 0

    fun parseData(data: TempHumidJsonModel): TempHumidJsonModel {
        val parsedDay = data.date.substring(0, 10)
        val parsedTime = data.date.substring(11,19)
        return data.copy(date = parsedDay.plus( " ").plus(parsedTime))
    }
}