package com.example.iotconnect.wifi

import android.bluetooth.BluetoothDevice
import android.net.NetworkCapabilities

data class WifiDevice(val ssid: String?, val bssid: String, val capabilities: String, val securityType: String)

