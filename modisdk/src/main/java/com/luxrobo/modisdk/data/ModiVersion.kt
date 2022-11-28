package com.luxrobo.modisdk.data

class ModiVersion() {
    private var osVersion : Int = 0
    private var appVersion : Int = 0

    fun setAppVersion(appVersion : Int) {
        this.appVersion = appVersion
    }

    fun getAppVersion() : Int {
        return appVersion
    }

    fun setOSVersion(OSVersion : Int) {
        this.osVersion = OSVersion
    }

    fun getOSVersion() : Int {
        return osVersion
    }

}
