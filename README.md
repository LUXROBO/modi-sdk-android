# modi-sdk-android
===============

[![](https://jitpack.io/v/LUXROBO/modi-sdk-android.svg)](https://jitpack.io/#LUXROBO/modi-sdk-android)


Easy😆 and fast💨 MODI SDK API.

* Documentation: https://luxrobo.github.io/modi-sdk-android/index.html


Quickstart
-------

Install the latest MODI SDK API if you haven't installed it yet

Add it in your root build.gradle at the end of repositories:

```gradle
allprojects {
        repositories {
                ...
                maven { url 'https://jitpack.io' }
        }
}
```

Add the dependency

```gradle
dependencies {
        implementation 'com.github.LUXROBO:modi-sdk-android:0.1.1'
}
```

Import `com.luxrobo.modisdk` package and create `ModiManager` Object::

```kotlin
import com.luxrobo.modisdk.core.ModiManager
...
...
private ModiManager mModiManager = ModiManager.getInstance()
```

Initialize ModiManager

```kotlin

var mModiManager = ModiManager().init(context, mModiClient)

private val mModiClient = object : ModiClient {

    override fun stopScan() {
    
    }
    
    override fun onScan() {
    
    }
    
    override fun onScanFailure() {
    
    }
    
    override fun onFoundDevice(bleScanResult : ScanResult) {
    
    }
    
    override fun onDiscoveredService() {
    
    }
    
    override fun onDiscoverServiceFailure() {
    
    }
    
    override fun onConnecting() {
    
    }
    
    override fun onConnected() {
    
    }
    
    override fun onConnectionFailure(e : Throwable) {
    
    }
    
    override fun onDisconnected() {
    
    }
    
    override fun onReceivedData(data: String) {
    
    }
    
    override fun onReceivedData(data: ByteArray) {
    
    }
    
    override fun onOffEvent() {
    
    }
    
    override fun disconnectedByModulePowerOff() {
     
    }
}
```

Scan and Connect::
```kotlin
mModiManager.scan()
...
...
mModiManager.connect(deviceAddress)
```
