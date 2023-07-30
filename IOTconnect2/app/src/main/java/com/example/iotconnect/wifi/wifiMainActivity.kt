package com.example.iotconnect.wifi

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.*
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE
import android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_EXCEEDS_MAX_PER_APP
import android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_INVALID
import android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_NOT_ALLOWED
import android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED
import android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_INTERNAL
import android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_REMOVE_INVALID
import android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.iotconnect.databinding.ActivityMainBinding
import com.example.iotconnect.databinding.ActivityWifiMainBinding


private const val REQUEST_CODE_LOCATION_PERMISSION = 1
class wifiMainActivity : AppCompatActivity() {

    companion object {
        private const val SECURITY_TYPE_WPA3 = "WPA3"
        private const val SECURITY_TYPE_WPA2 = "WPA2"
        private const val SECURITY_TYPE_WPA = "WPA"
        private const val SECURITY_TYPE_NA = "N/A"
    }

    private lateinit var suggestionPostConnectionReceiver: BroadcastReceiver
    private lateinit var wifiDeviceAdapter: WifiDeviceAdapter
    private lateinit var wifiDevices: MutableList<WifiDevice>
    private lateinit var recyclerView: RecyclerView
    private lateinit var wifiScanReceiver: BroadcastReceiver
    private lateinit var wifiManager: WifiManager
    private lateinit var binding: ActivityWifiMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWifiMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        recyclerView = binding.recyclerView
        wifiManager = this.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        setupRecyclerView()
        binding.scan.setOnClickListener {
            if(isWifiEnabled(this)) {
                if (hasRequiredRuntimePermissions()) {
                    isScanPerformed = false
                    val success = wifiManager.startScan()
                    if (!success) {
                        scanFailure()
                    }

                } else {
                    requestRelevantRuntimePermissions()
                }
            }
            else {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                startActivity(intent)
                if (hasRequiredRuntimePermissions()) {
                    isScanPerformed = false
                    val success = wifiManager.startScan()
                    if (!success) {
                        scanFailure()
                    }

                } else {
                    requestRelevantRuntimePermissions()
                }
            }
        }
        binding.clear.setOnClickListener {
            wifiDevices.clear()
            wifiDeviceAdapter.notifyDataSetChanged()
        }


    }





    override fun onResume() {
        super.onResume()
        wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                if (success) {
                    scanSuccess()
                } else {
                    scanFailure()
                }
            }
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(wifiScanReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(wifiScanReceiver)
        } catch (e: IllegalArgumentException) {
            // The receiver might not be registered, handle the exception as needed
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            unregisterReceiver(suggestionPostConnectionReceiver)
        } catch (e: IllegalArgumentException) {
            // The receiver might not be registered, handle the exception as needed
        }
    }


    private fun Activity.requestRelevantRuntimePermissions() {
        if(hasRequiredRuntimePermissions()){
            return
        }
        when {
            Build.VERSION.SDK_INT< Build.VERSION_CODES.P -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.CHANGE_WIFI_STATE,
                        Manifest.permission.CHANGE_NETWORK_STATE
                    ),
                    REQUEST_CODE_LOCATION_PERMISSION
                )
                wifiManager.isWifiEnabled = true
            }
            Build.VERSION.SDK_INT == Build.VERSION_CODES.P -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.CHANGE_WIFI_STATE,
                        Manifest.permission.CHANGE_NETWORK_STATE
                    ),
                    REQUEST_CODE_LOCATION_PERMISSION
                )
                if(!isWifiEnabled(this@wifiMainActivity)) {
                    val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                    startActivity(intent)
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.CHANGE_WIFI_STATE,
                        Manifest.permission.CHANGE_NETWORK_STATE
                    ),
                    REQUEST_CODE_LOCATION_PERMISSION
                )
                if(!isWifiEnabled(this@wifiMainActivity)) {
                    Log.i("wewillcome", "${isWifiEnabled(this@wifiMainActivity)}")
                    val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                    startActivity(intent)
                }

            }
        }
    }
    private var isScanPerformed = false
    @SuppressLint("MissingPermission", "NotifyDataSetChanged")
    private fun scanSuccess() {
        if(isScanPerformed){
            return
        }
        if(!isWifiEnabled(this)){
            Toast.makeText(this@wifiMainActivity, "Scan Failed", Toast.LENGTH_SHORT).show()
            return
        }
        val results = wifiManager.scanResults
        Log.i("result", "$results")
          for (scanResult in results) {
            val securityType = when {
                scanResult.capabilities.contains(SECURITY_TYPE_WPA3) -> SECURITY_TYPE_WPA3
                scanResult.capabilities.contains(SECURITY_TYPE_WPA2) -> SECURITY_TYPE_WPA2
                scanResult.capabilities.contains(SECURITY_TYPE_WPA) -> SECURITY_TYPE_WPA
                else -> SECURITY_TYPE_NA
            }
            val wifiDevice = WifiDevice(scanResult.SSID, scanResult.BSSID, scanResult.capabilities, securityType)
            for(i in wifiDevices){
                if(wifiDevice==i){
                    return
                }
            }
            wifiDevices.add(wifiDevice)
        }
        wifiDeviceAdapter.notifyDataSetChanged()
        isScanPerformed = true
    }

    @SuppressLint("MissingPermission", "NotifyDataSetChanged")
    private fun scanFailure() {
        if (isScanPerformed) {
            return
        }
        wifiDevices.clear()
        wifiDeviceAdapter.notifyDataSetChanged()

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            REQUEST_CODE_LOCATION_PERMISSION -> {
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
                        val success = wifiManager.startScan()
                        if (!success) {
                            scanFailure()
                        }
                    }
                    else ->{
                        recreate()
                    }
                }
            }
        }
    }



    private fun Context.hasPermission(permissionType: String) : Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) == PackageManager.PERMISSION_GRANTED
    }

    private fun Context.hasRequiredRuntimePermissions() : Boolean {
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.P){
            Log.i("test", "android<9")
            return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) && hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION) && hasPermission(Manifest.permission.CHANGE_WIFI_STATE)
        }
        else if(Build.VERSION.SDK_INT==Build.VERSION_CODES.P) {
            Log.i("test", "android 9")
            return (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) || hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) && hasPermission(Manifest.permission.CHANGE_WIFI_STATE) && isLocationEnabled()
        }
        else {
            Log.i("test", "android>=10")
            return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) && hasPermission(Manifest.permission.CHANGE_WIFI_STATE) && isLocationEnabled()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun isWifiEnabled(context: Context): Boolean {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiManager.isWifiEnabled
    }




    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        wifiDevices = mutableListOf()
        wifiDeviceAdapter = WifiDeviceAdapter(wifiDevices) { wifiDevice ->
            val wifiConfig = WifiConfiguration()
            wifiConfig.SSID = wifiDevice.ssid
            wifiConfig.BSSID = wifiDevice.bssid
            wifiConfig.priority = 1
            Log.i("correct", "${wifiConfig.SSID}, ${wifiConfig.BSSID}")
            val security = wifiDevice.capabilities
            if (security.contains("WEP")) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Enter WEP Key")
                val input = EditText(this)
                input.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                builder.setView(input)
                builder.setPositiveButton("OK") { dialog, which ->
                    val password = input.text.toString()
                    if (password.matches(Regex("^[0-9a-fA-F]+$"))) {
                        wifiConfig.wepKeys[0] = password
                    } else {
                        wifiConfig.wepKeys[0] = "\"$password\""
                    }

                    wifiConfig.wepTxKeyIndex = 0;
                    wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                    wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                    wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                    wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                    wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                    wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                    wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                    wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){
                        connectByWifiNetworkSuggestion(wifiDevice, password)
                    }
                    else {
                        connectToWifi(wifiConfig)
                    }

                }
                builder.setNegativeButton("Cancel") { dialog, which ->
                    dialog.cancel()
                }
                builder.show()

            } else if (security.contains("WPA")) {
                // WPA/WPA2/WPA3 network
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Enter Password")
                val input = EditText(this)
                input.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                builder.setView(input)
                builder.setPositiveButton("OK") { dialog, which ->
                    val password = input.text.toString()

                    wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                    wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                    wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                    wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                    wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                    wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                    wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                    wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                    wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                    wifiConfig.preSharedKey = "\"" + password + "\""
                    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){
                        connectByWifiNetworkSuggestion(wifiDevice, password)
                    }
                    else {
                        connectToWifi(wifiConfig)
                    }
                }
                builder.setNegativeButton("Cancel") { dialog, which ->
                    dialog.cancel()
                }
                builder.show()

            } else {
                wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                wifiConfig.allowedAuthAlgorithms.clear();
                wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){
                    val ssid = wifiConfig.SSID
                    val bssid = wifiConfig.BSSID
                    val securityType = wifiDevice.securityType
                    Log.i("heman", "$securityType")
                    //wifi(ssid, null, MacAddress.fromString(bssid), securityType)
                    //connectByWifiNetworkSuggestion(wifiDevice, null)
                    //connectToOpenWifi(this, wifiConfig.SSID)
                    clearWifiNetworkSuggestions()
                }
                else {
                    connectToWifi(wifiConfig)
                }
            }
        }
        recyclerView.adapter = wifiDeviceAdapter

    }
    @SuppressLint("MissingPermission")
    private fun connectToWifi(wifiConfig: WifiConfiguration) {
        val networkId = wifiManager.addNetwork(wifiConfig)
        Log.i("testing", "Network ID: $networkId, PreSharedKey: ${wifiConfig.preSharedKey}")

        val disconnected = wifiManager.disconnect()
        val enabled = wifiManager.enableNetwork(networkId, true)
        val reconnected = wifiManager.reconnect()
        Log.i("test", "Disconnected: $disconnected, Enabled: $enabled, Reconnected: $reconnected")

        if (reconnected) {
            // Wait for the connection to be established
            Thread.sleep(5000) // Adjust the delay as needed

            val connectionInfo = wifiManager.connectionInfo
            Log.i("test", "Connection Info: $connectionInfo")
            if (connectionInfo != null && networkId == connectionInfo.networkId) {
                Log.i("test", "Successfully connected to the network: ${wifiConfig.SSID}")
                Toast.makeText(
                    this@wifiMainActivity,
                    "Connected to the network: ${wifiConfig.SSID}",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }
    }




    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.i("status", "Connected")
            //Toast.makeText(this@MainActivity, "Connected", Toast.LENGTH_SHORT).show()
        }

        override fun onLost(network: Network) {
            Log.i("status", "lost")
            //Toast.makeText(this@MainActivity, "Connection Lost", Toast.LENGTH_SHORT).show()

        }

        override fun onUnavailable() {
            Log.i("status", "unavailable")
            //Toast.makeText(this@MainActivity, "Unable to connect", Toast.LENGTH_SHORT).show()

        }
    }



    @RequiresApi(Build.VERSION_CODES.Q)
    private fun connectByWifiNetworkSuggestion(wifi: WifiDevice, pass: String?) {
        Log.d("TAG", "connectByWifiNetworkSuggestion: wifi=$wifi, pass=$pass")
        clearWifiNetworkSuggestions()
        val suggestion = wifi.ssid?.let {
            WifiNetworkSuggestion.Builder()
                .setSsid(it)
        }
        when (wifi.securityType) {
            SECURITY_TYPE_WPA3 -> suggestion!!.setWpa3Passphrase(pass!!)
            SECURITY_TYPE_WPA2 -> suggestion!!.setWpa2Passphrase(pass!!)
            SECURITY_TYPE_WPA -> suggestion!!.setWpa2Passphrase(pass!!)
            SECURITY_TYPE_NA -> suggestion!!.setWpa2Passphrase("")
            else -> suggestion!!.setWpa2Passphrase("")
        }
        val suggestionsList = listOf(suggestion.build())
        val resultValue = wifiManager.addNetworkSuggestions(suggestionsList)
        val resultKey = when (resultValue) {
            STATUS_NETWORK_SUGGESTIONS_SUCCESS -> "STATUS_NETWORK_SUGGESTIONS_SUCCESS"
            STATUS_NETWORK_SUGGESTIONS_ERROR_INTERNAL -> "STATUS_NETWORK_SUGGESTIONS_ERROR_INTERNAL"
            STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED -> "STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED"
            STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE -> "STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE"
            STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_EXCEEDS_MAX_PER_APP -> "STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_EXCEEDS_MAX_PER_APP"
            STATUS_NETWORK_SUGGESTIONS_ERROR_REMOVE_INVALID -> "STATUS_NETWORK_SUGGESTIONS_ERROR_REMOVE_INVALID"
            STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_NOT_ALLOWED -> "STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_NOT_ALLOWED"
            STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_INVALID -> "STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_INVALID"
            else -> ""
        }
        Log.d("TAG", "connectByWifiNetworkSuggestion: result: $resultValue: $resultKey")
        //sendGetRequestToServer()


        val intentFilter = IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)
        suggestionPostConnectionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (!intent?.action.equals(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)) {
                    Log.d("TAG", "SuggestionPostConnectionReceiver: onReceive: Broadcast received")
                    return
                }
                Log.d("TAG", "SuggestionPostConnectionReceiver: onReceive: ")
                Toast.makeText(context, "Connected Successfully", Toast.LENGTH_SHORT).show()
            }
        }
        registerReceiver(suggestionPostConnectionReceiver, intentFilter)
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun clearWifiNetworkSuggestions() {
        val suggestionsList: List<WifiNetworkSuggestion> = emptyList() // Empty list to clear all suggestions
        val result = wifiManager.removeNetworkSuggestions(suggestionsList)
        val resultKey = when (result) {
            STATUS_NETWORK_SUGGESTIONS_SUCCESS -> "STATUS_NETWORK_SUGGESTIONS_SUCCESS"
            else -> ""
        }
        Log.d("TAG", "Clear Suggestions Result: $resultKey")
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        startActivity(intent)
    }


//    fun sendGetRequestToServer() {
//        val url = "http://192.168.4.1"
//
//        Fuel.get(url).responseString { _, _, result ->
//            when (result) {
//                is Result.Success -> {
//                    val responseData = result.get() // The response data as a String
//                    Log.i("finally", "$responseData")
//                }
//                is Result.Failure -> {
//                    val error = result.getException()
//                    error.printStackTrace()
//                    Toast.makeText(this@wifiMainActivity, "error bro", Toast.LENGTH_LONG).show()
//                }
//            }
//        }
//    }


}



