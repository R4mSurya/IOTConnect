package com.example.iotconnect.wifi

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.iotconnect.R

class WifiDeviceAdapter(
    private val wifiDevices: List<WifiDevice>,
    private val onItemClick: (WifiDevice) -> Unit
) : RecyclerView.Adapter<WifiDeviceAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context).inflate(R.layout.row_scan_result_wifi, parent, false)
        return ViewHolder(adapterLayout, onItemClick)
    }

    override fun getItemCount() = wifiDevices.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = wifiDevices[position]
        holder.bind(item)
    }
    @SuppressLint("MissingPermission")
    class ViewHolder(
        private val view: View,
        private val onItemClick: ((WifiDevice) -> Unit)
    ) : RecyclerView.ViewHolder(view) {
        private val ssid: TextView = view.findViewById(R.id.ssid)
        private val bssid: TextView = view.findViewById(R.id.bssid)
        fun bind(wifiDevice: WifiDevice) {
            ssid.text = wifiDevice.ssid
            bssid.text = wifiDevice.bssid
            view.setOnClickListener {
                onItemClick(wifiDevice)
            }
        }
    }
}
