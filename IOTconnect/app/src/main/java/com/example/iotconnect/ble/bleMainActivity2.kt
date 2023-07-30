package com.example.iotconnect.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothProfile
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.iotconnect.R
import com.example.iotconnect.databinding.ActivityBleMain2Binding
import com.example.iotconnect.databinding.ActivityBleMainBinding
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe



class bleMainActivity2 : AppCompatActivity() {
    lateinit var name : TextView
    lateinit var address : TextView
    lateinit var rssid : TextView
    lateinit var disconnt : Button
    private var bluetoothGatt: BluetoothGatt? = null
    private lateinit var binding : ActivityBleMain2Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBleMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        name = findViewById(R.id.name2)
        address = findViewById(R.id.macAddress)
        rssid = findViewById(R.id.rssid)

        Toast.makeText(this@bleMainActivity2, "Successfully connected!", Toast.LENGTH_SHORT).show()
        val bundle = intent.extras
        if (bundle != null){
            name.text = "Name = ${bundle.getString("name")}"
            address.text = "BSSID = ${bundle.getString("macaddress")}"
            rssid.text = "RSSI = ${bundle.getInt("rssi")} dBm"

            val app = application as MyApplication
            bluetoothGatt = app.bluetoothGatt
        }

        disconnt = findViewById(R.id.discont)
        disconnt.setOnClickListener {
            disconnect()
            Toast.makeText(this@bleMainActivity2, "Disconnected Successfully", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    @Subscribe
    fun onHeartRateEvent(event: HeartRateEvent) {
        val heartRate = event.heartRate
        Log.i("halem", "$heartRate")
        runOnUiThread {
            binding.heartvalue.text = "Heart Rate : "+heartRate.toString()+" bpm"
        }
    }


    @SuppressLint("MissingPermission")
    private fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }


}