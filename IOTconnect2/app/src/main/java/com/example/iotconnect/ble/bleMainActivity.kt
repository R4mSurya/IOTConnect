package com.example.iotconnect.ble

import android.Manifest
import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.iotconnect.databinding.ActivityBleMainBinding
import com.example.iotconnect.databinding.ActivityMainBinding
import org.greenrobot.eventbus.EventBus
import java.util.UUID



private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val RUNTIME_PERMISSION_REQUEST_CODE = 2
@SuppressLint("MissingPermission")
class bleMainActivity : AppCompatActivity() {
    val meaningsList = mutableListOf<String>()
    private lateinit var binding: ActivityBleMainBinding
    private var latestScanResult: ScanResult? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBleMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.button.setOnClickListener {
            if(isScanning) {
                Log.i("status", "Scanning is stopped")
                stopBleScan()

            }
            else {
                Log.i("status", "Scanning is started")
                startBleScan()

            }
        }
        setupRecyclerView()
        binding.clear.setOnClickListener {
            stopBleScan()
            scanResults.clear()
            scanResultAdapter.notifyDataSetChanged()

        }

    }





    private fun Activity.requestRelevantRuntimePermissions() {
        if(hasRequiredRuntimePermissions()){
            return
        }
        when {
            Build.VERSION.SDK_INT<Build.VERSION_CODES.S -> {
                requestLocationPermission()
            }
            Build.VERSION.SDK_INT>=Build.VERSION_CODES.S -> {
                requestBluetoothPermissions()
            }
        }
    }

    private fun requestBluetoothPermissions() {
        runOnUiThread {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Bluetooth Permission Required")
            builder.setMessage("Starting from Android 12, the system requires apps to be granted Bluetooth access in order to scan for and connect to BLE devices.")
            builder.setCancelable(false)
            builder.setPositiveButton(android.R.string.ok) {dialog, which->
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), RUNTIME_PERMISSION_REQUEST_CODE)
            }
            builder.show()
        }
    }

    private fun requestLocationPermission() {
        runOnUiThread {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Location Permission Required")
            builder.setMessage("Starting from Android M (6.0), the system requires apps to be granted location access in order to scan for BLE devices.")
            builder.setCancelable(false)
            builder.setPositiveButton(android.R.string.ok) {dialog, which->
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), RUNTIME_PERMISSION_REQUEST_CODE)
            }
            builder.show()
        }
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            RUNTIME_PERMISSION_REQUEST_CODE -> {
                val containsPermanentDenial = permissions.zip(grantResults.toTypedArray()).any {
                    it.second == PackageManager.PERMISSION_DENIED &&
                            !ActivityCompat.shouldShowRequestPermissionRationale(this, it.first)
                }
                val containsDenial = grantResults.any { it == PackageManager.PERMISSION_DENIED }
                val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

                when {
                    containsPermanentDenial -> {
                        //TODO
                    }
                    containsDenial -> {
                        requestRelevantRuntimePermissions()
                    }
                    allGranted && hasRequiredRuntimePermissions() -> {
                        startBleScan()
                    }
                    else ->{
                        recreate()
                    }
                }
            }
        }
    }

    private val blescanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()


    private val bluetoothAdapter : BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }


    override fun onResume() {
        super.onResume()
        if(!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }
    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val btIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(btIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            ENABLE_BLUETOOTH_REQUEST_CODE-> {
                if(resultCode!=Activity.RESULT_OK){
                    promptEnableBluetooth()
                }
            }
        }
    }


    private fun Context.hasPermission(permissionType: String) : Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) == PackageManager.PERMISSION_GRANTED
    }

    private fun Context.hasRequiredRuntimePermissions() : Boolean {
        return if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
            hasPermission(Manifest.permission.BLUETOOTH_SCAN) && hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        }
        else {
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }




    private fun startBleScan() {
        if(!hasRequiredRuntimePermissions()){
            requestRelevantRuntimePermissions()
        }
        else {
            blescanner.startScan(null, scanSettings, scanCallback)
            isScanning = true
        }
    }

    private fun stopBleScan() {
        blescanner.stopScan(scanCallback)
        isScanning = false
    }

    private var isScanning = false
    set(value) {
        field = value
        runOnUiThread {
            binding.button.text = if (value) "Stop Scan" else "Start Scan"
        }
    }

    private val scanResults = mutableListOf<ScanResult>()
    private val scanResultAdapter: ScanResultAdapter by lazy {
        ScanResultAdapter(scanResults) { result ->
            if (isScanning) {
                stopBleScan()
            }
            with(result.device) {
                Log.w("ScanResultAdapter", "Connecting to $address")
                connectGatt(this@bleMainActivity, false, gattCallback)
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val indexQuery = scanResults.indexOfFirst { it.device.address == result.device.address }
            if (indexQuery != -1) { // A scan result already exists with the same address
                scanResults[indexQuery] = result
                latestScanResult = result
                scanResultAdapter.notifyItemChanged(indexQuery)
            } else {
                with(result.device) {
                    Log.i("found", "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address")
                }
                scanResults.add(result)
                scanResultAdapter.notifyItemInserted(scanResults.size - 1)
            }
        }

    }



    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully connected to $deviceAddress")
                    gatt.discoverServices()
                    val rssiValue = latestScanResult?.rssi ?: 0
                    meaningsList.clear()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully disconnected from $deviceAddress")
                    this@bleMainActivity.runOnUiThread(Runnable {
                        Toast.makeText(
                            this@bleMainActivity,
                            "Disconnected from the device!",
                            Toast.LENGTH_SHORT
                        ).show()
                    })
                    gatt.close()
                }
            }
            else {
                Log.w("BluetoothGattCallback", "Error $status encountered for $deviceAddress! Disconnecting...")
                this@bleMainActivity.runOnUiThread(Runnable {
                    Toast.makeText(
                        this@bleMainActivity,
                        "Cannot connect to this device!",
                        Toast.LENGTH_SHORT
                    ).show()
                })
                gatt.close()
            }
        }
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                val services: List<BluetoothGattService> = gatt.services
                for (service in services) {
                    val characteristics: List<BluetoothGattCharacteristic> = service.characteristics
                    for (characteristic in characteristics) {
                        if(characteristic.uuid == UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")){
                            Log.i("datavalue", "${UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")}")
                            Log.i("datavalue", "${characteristic.uuid}")
                            gatt.setCharacteristicNotification(characteristic, true)
                            val descriptor = characteristic.getDescriptor(
                                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                            )

                            if (descriptor != null) {
                                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                gatt.writeDescriptor(descriptor)
                            }
                        }
                    }
                }

                Log.w("BluetoothGattCallback", "Discovered ${services.size} services for ${device.address}")
                printGattTable()
                gatt.readRemoteRssi()
            }
        }


        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val heartRateServiceUuid = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
            val heartRateMeasurementUuid = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")

            if (characteristic.service.uuid == heartRateServiceUuid && characteristic.uuid == heartRateMeasurementUuid) {
                val heartRateValue = parseHeartRateMeasurement(characteristic)
                val event1 = HeartRateEvent(heartRateValue)
                EventBus.getDefault().post(event1)
                Log.i("Heart Rate", "Heart Rate Measurement: $heartRateValue BPM")
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.i("BluetoothGattCallback", "Read characteristic $uuid:\n${value.contentToString()}")
                        val dataValue = parseHeartRateMeasurement(characteristic)
                        Log.i("datavalue", "$dataValue")
                    }
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                        Log.e("BluetoothGattCallback", "Read not permitted for $uuid!")
                    }
                    else -> {
                        Log.e("BluetoothGattCallback", "Characteristic read failed for $uuid, error: $status")
                    }
                }
            }
        }

        fun parseHeartRateMeasurement(characteristic: BluetoothGattCharacteristic): Int {
            val data = characteristic.value
            val flags = data[0].toInt()
            val format: Int = if ((flags and 0x01) != 0) {
                BluetoothGattCharacteristic.FORMAT_UINT16
            } else {
                BluetoothGattCharacteristic.FORMAT_UINT8
            }
            return parseHeartRateValue(data, format)
        }

        private fun parseHeartRateValue(data: ByteArray, format: Int): Int {
            return if (format == BluetoothGattCharacteristic.FORMAT_UINT8) {
                data[1].toInt() and 0xFF
            } else {
                val value = data[1].toInt() and 0xFF
                val value2 = data[2].toInt() and 0xFF
                (value2 shl 8) + value
            }
        }



        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BluetoothGattCallback", "RSSI value: $rssi")
                val bundle = Bundle()
                val serviceList = gatt?.services?.map { it.uuid.toString() } ?: emptyList()
                bundle.putStringArrayList("services", ArrayList(serviceList))
                bundle.putStringArrayList("meanings", ArrayList(meaningsList))
                bundle.putString("name", gatt?.device?.name ?: "")
                bundle.putString("macaddress", gatt?.device?.address ?: "")
                bundle.putInt("rssi", rssi)

                val intent = Intent(this@bleMainActivity, bleMainActivity2::class.java)
                intent.putExtras(bundle)
                val app = application as MyApplication
                app.bluetoothGatt = gatt
                startActivity(intent)

            } else {
                Log.w("BluetoothGattCallback", "Error reading RSSI: $status")
            }
        }
    }





    private fun setupRecyclerView() {
        binding.scanResultsRecyclerView.apply {
            adapter = scanResultAdapter
            layoutManager = LinearLayoutManager(
                this@bleMainActivity,
                RecyclerView.VERTICAL,
                false
            )
            isNestedScrollingEnabled = false
        }

        val animator = binding.scanResultsRecyclerView.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }




    private fun BluetoothGatt.printGattTable() {
        if (services.isEmpty()) {
            Log.i("printGattTable", "No service and characteristic available, call discoverServices() first?")
            return
        }
        services.forEach { service ->
            val characteristicsTable = service.characteristics.joinToString(
                separator = "\n|--",
                prefix = "|--"
            ) { it.uuid.toString() }
            val meaning = BluetoothUuidLookup.getMeaning(service.uuid.toString())
            if (meaning != null) {
                // Use the meaning of the UUID
                Log.i("meaning", "UUID ${service.uuid} has the meaning: $meaning")
            } else {
                // Meaning not found for the UUID
                Log.i("meaning", "Meaning not found for UUID ${service.uuid}")
            }
            Log.i("printGattTable", "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable"
            )
        }
    }
    private fun getUUIDMeaning(uuid: String): String {
        val meaning = BluetoothUuidLookup.getMeaning(uuid)
        return meaning ?: "Meaning not found for UUID $uuid"
    }
    fun BluetoothGattCharacteristic.isReadable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)


    private fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean {
        return properties and property != 0
    }

}