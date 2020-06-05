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
import com.luxrobo.sample.modidemo.sample_modi_demo.Singleton
import com.luxrobo.sample.modidemo.sample_modi_demo.example_connect.ConnectActivity
import com.polidea.rxandroidble2.scan.ScanResult

class ScanActivity : AppCompatActivity() {

    private lateinit var binding                : ActivityScanBinding

    private lateinit var mModiClient            : ModiClient
    private lateinit var mModiManager           : ModiManager

    private lateinit var adapter                : ScanAdapter

    private val         data            = mutableListOf<ScanResult>()

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

            adapter.removeAll()

            if(!mModiManager.isScanning) {

                mModiManager.scan()

                return@setOnClickListener
            }

            mModiManager.stopScan()

        }

    }

    private fun setModiClient() {

        mModiClient = object : ModiClient {

            override fun onFoundDevice(bleScanResult: ScanResult?) {

//                ModiLog.d("onFoundDevice ${bleScanResult?.bleDevice?.name}")

                data.withIndex()
                    .firstOrNull { it.value.bleDevice.macAddress == bleScanResult!!.bleDevice.macAddress }
                    ?.let {
                        // device already in data list => update
                        data[it.index] = bleScanResult!!

                    }
                    ?: run {
                        // new device => add to data list
                        with(data) {
                            add(bleScanResult!!)
                            sortBy { it.bleDevice.macAddress }
                        }
                }


                Singleton.getInstance().updateScanResult(bleScanResult)

            }

            override fun onDiscoveredService() {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onDiscoverServiceFailure() {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onConnecting() {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onConnected() {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onConnectionFailure(e: Throwable?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onDisconnected() {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onReceivedData(data: ByteArray?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onOffEvent() {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun disconnectedByModulePowerOff() {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        }
    }

    private fun setAdapter() {

        ModiLog.d("setAdapter ")

        adapter = ScanAdapter()
        adapter.setListener(object :
            ScanAdapter.OnItemClickListener {

            override fun onItemClick(deviceAddress: String) {

                //move to connect activity
                val intent = Intent(this@ScanActivity, ConnectActivity::class.java)
                intent.putExtra("deviceAddress", deviceAddress)
                startActivity(intent)
            }
        })

        val layoutManager = LinearLayoutManager(this)
        binding.list.layoutManager = layoutManager
        binding.list.adapter = adapter

        ModiLog.d("setAdapter end")

    }
}
