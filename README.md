# IOTConnect
Android app to connect to IOT devices via Bluetooth, Wifi and NFC.
<br>
<br>

**Bluetooth:**
<br>
Scans for BLE devices and connection can be established when needed. Detects heart rate measurement sensor when connected to a board with the before said sensor and displays the heart rate in bpm. Also displays the device name, device address, RSSI value and connection status.
<br>
Has a disconnect option to disconnect from the connected device.
<br>
<br>
**Wifi:**
<br>
Scans for Wifi devices and connection can be established with the necessary permission provided. 
<br>
<br>
*Android>=10:*<br>
Android 10 and above, wifi should be enabled by the user manually to scan for devices. NetworkSuggestionApi has been used here for Android 10 and above. The code is modified to allow only one wifi device to be added in the sugegstion list. When a second device is added, the previous device is removed automatically. This was the only way an user can connect to a wifi device from an app, as Google had removed other ways for security reasons. (WifiNetworkSpecifier api did not function properly).
<br>
<br>
*Android<10:*<br>
The app uses wifiManager and wifiConfiguration to connect to devices.
<br>
<br>
**NFC:**<br>
Not implemented so far. If clicked for NFC option, it displays whether the phone supports NFC feature.
