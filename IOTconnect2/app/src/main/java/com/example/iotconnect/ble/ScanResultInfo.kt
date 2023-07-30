package com.example.iotconnect.ble

import android.bluetooth.BluetoothDevice

data class ScanResultInfo(val name: String?, val macAddress: String, val bluetoothDevice: BluetoothDevice?) {
    override fun toString(): String {
        val formattedName = name ?: "Unknown"
        return "$formattedName\n$macAddress"
    }
}