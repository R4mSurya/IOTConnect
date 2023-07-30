package com.example.iotconnect.ble

import android.app.Application
import android.bluetooth.BluetoothGatt

class MyApplication : Application() {
    var bluetoothGatt: BluetoothGatt? = null
}