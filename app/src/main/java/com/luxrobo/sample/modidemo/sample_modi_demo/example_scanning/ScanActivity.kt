package com.luxrobo.sample.modidemo.sample_modi_demo.example_scanning

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.luxrobo.modisdk.callback.ModiCodeUpdaterCallback
import com.luxrobo.modisdk.client.ModiClient
import com.luxrobo.modisdk.core.ModiManager
import com.luxrobo.modisdk.core.ModiPlayManager
import com.luxrobo.modisdk.core.ModiStream
import com.luxrobo.modisdk.enums.CodeUpdateError
import com.luxrobo.modisdk.enums.PlayCommand
import com.luxrobo.modisdk.enums.PlayCommandData
import com.luxrobo.modisdk.utils.ModiLog
import com.luxrobo.sample.modidemo.BaseActivity
import com.luxrobo.sample.modidemo.R
import com.luxrobo.sample.modidemo.databinding.ActivityScanBinding
import com.luxrobo.sample.modidemo.sample_modi_demo.example_connect.ConnectActivity
import com.polidea.rxandroidble2.scan.ScanResult

class ScanActivity : BaseActivity() {

    private lateinit var binding                : ActivityScanBinding

    private lateinit var adapter                : ScanAdapter

    private var scanData                  = listOf<ScanResult>()
    private var scanResultData            = mutableListOf<ScanResult>()

    private lateinit var callback                : ModiCodeUpdaterCallback

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

    private fun setListener() {

        callback = object : ModiCodeUpdaterCallback {
            override fun onUpdateSuccess() {
                ModiLog.d("onUpdateSuccess")
            }

            override fun onUpdateProgress(progressCount: Int, total: Int) {
                ModiLog.d("onUpdateProgress  $progressCount / $total")
            }

            override fun onUpdateFailed(error: CodeUpdateError?, reason: String?) {
                ModiLog.d("onUpdateFailed")
            }
        }

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

        binding.btnUpload.setOnClickListener {


            val byteArray = byteArrayOf(0,1,15,1,1,1,34,0,12,1,28,0,10,1,11,0,2,109,121,102,97,97,48,102,54,56,56,10,0,10,1,5,0,1,0,0,0,0,10,0,12,0,1,0,2,1,-31,0,51,1,-37,0,55,1,29,0,10,1,5,0,1,0,0,-128,63,10,0,11,1,1,0,25,11,0,10,1,5,0,1,0,0,0,0,10,0,55,0,56,1,-78,0,50,1,94,0,0,55,1,33,0,20,1,9,0,-120,-109,44,96,48,32,2,0,0,20,0,11,1,1,0,24,11,0,10,1,5,0,1,0,0,-56,66,10,0,55,0,56,1,48,0,21,1,42,0,-101,-26,39,20,32,64,16,0,3,10,1,5,0,1,0,0,-56,66,10,0,10,1,5,0,1,0,0,0,0,10,0,10,1,5,0,1,0,0,0,0,10,0,21,0,56,0,50,0,50,1,55,0,2,56,1,48,0,21,1,42,0,-101,-26,39,20,32,64,16,0,3,10,1,5,0,1,0,0,0,0,10,0,10,1,5,0,1,0,0,0,0,10,0,10,1,5,0,1,0,0,0,0,10,0,21,0,56,0,50,0,30,1,11,0,10,1,5,0,1,0,0,64,64,10,0,30,0,56,0,51,0,2,0,0,0)

            val modiStream = ModiStream.makeStream(
                mModiManager.getConnectedModiUuid() and 0xFFF,
                0,
                ModiStream.STREAM_TYPE.INTERPRETER,
                byteArray
            )
            mModiManager.codeUpdater().startUpdate_plus(modiStream, callback)
        }

        binding.btnInit.setOnClickListener {

            mModiManager.codeUpdater().startReset_plus(callback)
        }

        binding.btnSend.setOnClickListener {

            mPlayManager.modiPlusEvent(0,3,PlayCommand.BUTTON_CLICK, PlayCommandData.PRESSED.value)
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
                ModiLog.e("onDiscoveredService")
            }

            override fun onDiscoverServiceFailure() {

            }

            override fun onConnecting() {

            }

            override fun onConnected() {
                ModiLog.e("onConnected")
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
