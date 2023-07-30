package com.example.iotconnect

import android.content.Intent
import android.nfc.NfcAdapter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.iotconnect.ble.bleMainActivity
import com.example.iotconnect.wifi.wifiMainActivity
import com.example.iotconnect.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var nfcAdapter : NfcAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.splashscreen)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.ble.setOnClickListener {
            val intent = Intent(this@MainActivity, bleMainActivity::class.java)
            startActivity(intent)
        }
        binding.wifi.setOnClickListener {
            val intent = Intent(this@MainActivity, wifiMainActivity::class.java)
            startActivity(intent)
        }
        binding.nfc.setOnClickListener {
            nfcAdapter = NfcAdapter.getDefaultAdapter(this)
            if(nfcAdapter==null){
                Toast.makeText(this, "Your phone does not support Near Field Communication", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this, "Yet to implement", Toast.LENGTH_SHORT).show()
            }
        }
    }
}