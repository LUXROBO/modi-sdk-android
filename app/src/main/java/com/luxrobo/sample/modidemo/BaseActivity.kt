package com.luxrobo.sample.modidemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.luxrobo.modisdk.client.ModiClient
import com.luxrobo.modisdk.core.ModiManager
import com.luxrobo.modisdk.core.ModiModuleManager
import com.luxrobo.modisdk.core.ModiPlayManager
import com.luxrobo.modisdk.utils.ModiLog
import com.polidea.rxandroidble2.scan.ScanResult

open class BaseActivity : AppCompatActivity() {

    lateinit var mModiClient            : ModiClient
    lateinit var mModiManager           : ModiManager
    lateinit var mPlayManager           : ModiPlayManager
    lateinit var mModeModuleManager     : ModiModuleManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    fun setModiManager() {

        mModiManager = ModiManager().init(this, mModiClient)
        mPlayManager = ModiPlayManager(mModiManager)
        mModeModuleManager = mModiManager.moduleMgr()
    }

}