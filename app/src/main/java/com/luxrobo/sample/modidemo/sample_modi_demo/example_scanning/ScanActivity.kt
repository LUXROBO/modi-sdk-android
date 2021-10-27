package com.luxrobo.sample.modidemo.sample_modi_demo.example_scanning

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.luxrobo.modisdk.client.ModiClient
import com.luxrobo.modisdk.core.ModiManager
import com.luxrobo.modisdk.utils.ModiLog
import com.luxrobo.sample.modidemo.R
import com.luxrobo.sample.modidemo.databinding.ActivityScanBinding
import com.luxrobo.sample.modidemo.sample_modi_demo.example_connect.ConnectActivity
import com.polidea.rxandroidble2.scan.ScanResult

class ScanActivity : AppCompatActivity() {

    private lateinit var binding                : ActivityScanBinding

    private lateinit var mModiClient            : ModiClient
    private lateinit var mModiManager           : ModiManager

    private lateinit var adapter                : ScanAdapter

    private var scanData                  = listOf<ScanResult>()
    private var scanResultData            = mutableListOf<ScanResult>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_scan)
        init()


    }

    private fun init() {

        setModiClient()
        setModiManager()
        setListener()
        setAdapter()
    }



    private fun setModiManager() {

        mModiManager = ModiManager().init(this, mModiClient)
    }

    private fun setListener() {

        binding.btnScan.setOnClickListener {

            scanResultData.clear()
            adapter.removeAll()

            if(!mModiManager.isScanning) {

                mModiManager.turnOnBluetooth()
                mModiManager.scan()

                return@setOnClickListener
            }

            mModiManager.stopScan()

        }

    }

    private fun setModiClient() {

        mModiClient = object : ModiClient {

            override fun onFoundDevice(bleScanResult: ScanResult?) {


               scanData
                   .withIndex()
                   .firstOrNull {
                       it.value.bleDevice == bleScanResult?.bleDevice
                   }
                   ?.let {

                       scanResultData[it.index] = bleScanResult!!
                   }
                   ?: run {
                       with(scanResultData) {

                           if(bleScanResult?.bleDevice?.name?.startsWith("MODI+") == true) {
                               add(bleScanResult)

                               sortBy { it.bleDevice.macAddress }

                               scanData = this
                               adapter.setItemList(this)

                           }

                       }
                   }

            }

            override fun onDiscoveredService() {

            }

            override fun onDiscoverServiceFailure() {

            }

            override fun onConnecting() {

            }

            override fun onConnected() {

            }

            override fun onConnectionFailure(e: Throwable?) {

            }

            override fun onDisconnected() {

            }

            override fun onScan() {
                binding.btnScan.text = "stop"

            }

            override fun stopScan() {

                binding.btnScan.text = "scan"
            }

            override fun onScanFailure() {

                ModiLog.e("scan failed")
            }

            override fun onReceivedData(data: String?) {

            }

            override fun onReceivedData(data: ByteArray?) {

            }

            override fun onOffEvent() {

            }

            override fun disconnectedByModulePowerOff() {

            }

        }
    }

    private fun setAdapter() {

        adapter = ScanAdapter()
        adapter.setListener(object :
            ScanAdapter.OnItemClickListener {

            override fun onItemClick(deviceAddress: String) {

                ModiLog.d("onItemClick $deviceAddress")

                mModiManager.connect(deviceAddress)
//                //move to connect activity
//                val intent = Intent(this@ScanActivity, ConnectActivity::class.java)
//                intent.putExtra("deviceAddress", deviceAddress)
//                startActivity(intent)
            }
        })

        val layoutManager = LinearLayoutManager(this)
        binding.list.layoutManager = layoutManager
        binding.list.adapter = adapter

    }
}
