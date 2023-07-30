package com.example.iotconnect.ble
import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.iotconnect.R

class ScanResultAdapter(
    private val items: List<ScanResult>,
    private val onClickListener: ((device: ScanResult) -> Unit)
) : RecyclerView.Adapter<ScanResultAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context).inflate(R.layout.row_scan_result, parent, false)
        return ViewHolder(adapterLayout, onClickListener)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }
    @SuppressLint("MissingPermission")
    class ViewHolder(
        private val view: View,
        private val onClickListener: ((device: ScanResult) -> Unit)
    ) : RecyclerView.ViewHolder(view) {
        private val deviceNameTextView: TextView = view.findViewById(R.id.device_name)
        private val macAddressTextView: TextView = view.findViewById(R.id.mac_address)
        fun bind(result: ScanResult) {
            deviceNameTextView.text = result.device.name ?: "Unnamed"
            macAddressTextView.text = result.device.address
            view.setOnClickListener { onClickListener.invoke(result) }
        }
    }
}
