package com.luxrobo.sample.modidemo.sample_modi_demo.example_scanning

import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.luxrobo.modisdk.callback.ModiCodeUpdaterCallback
import com.luxrobo.modisdk.client.ModiClient
import com.luxrobo.modisdk.core.*
import com.luxrobo.modisdk.data.ModiVersion
import com.luxrobo.modisdk.enums.CodeUpdateError
import com.luxrobo.modisdk.enums.ModiType
import com.luxrobo.modisdk.enums.PlayCommand
import com.luxrobo.modisdk.enums.PlayCommandData
import com.luxrobo.modisdk.listener.ModiModuleManagerListener
import com.luxrobo.modisdk.utils.ModiLog
import com.luxrobo.sample.modidemo.BaseActivity
import com.luxrobo.sample.modidemo.R
import com.luxrobo.sample.modidemo.databinding.ActivityScanBinding
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

            val byteArray = byteArrayOf( 0,1,-112,1,1,1,34,0,12,1,28,0,10,1,11,0,2,109,121,102,97,97,48,102,54,56,56,10,0,10,1,5,0,1,0,0,0,0,10,0,12,0,1,0,2,1,98,1,30,1,11,0,10,1,5,0,1,0,0,-6,68,10,0,30,0,51,1,75,1,55,1,29,0,10,1,5,0,1,0,0,-128,63,10,0,11,1,1,0,25,11,0,10,1,5,0,1,0,0,0,0,10,0,55,0,56,1,34,1,50,1,-106,0,0,55,1,33,0,20,1,9,0,-120,-109,44,96,48,32,5,0,0,20,0,11,1,1,0,24,11,0,10,1,5,0,1,0,0,-56,66,10,0,55,0,56,1,104,0,21,1,20,0,4,33,39,20,17,64,17,0,1,10,1,5,0,1,0,0,-128,63,10,0,21,0,21,1,20,0,56,49,44,96,16,64,17,0,1,10,1,5,0,1,0,0,0,0,10,0,21,0,21,1,20,0,47,45,44,96,16,64,17,0,1,10,1,5,0,1,0,0,32,65,10,0,21,0,21,1,20,0,9,126,44,96,17,64,17,0,1,10,1,5,0,1,0,0,32,66,10,0,21,0,56,0,50,0,50,1,111,0,2,56,1,104,0,21,1,20,0,47,45,44,96,16,64,17,0,1,10,1,5,0,1,0,0,0,0,10,0,21,0,21,1,20,0,9,126,44,96,17,64,17,0,1,10,1,5,0,1,0,0,-128,63,10,0,21,0,21,1,20,0,4,33,39,20,17,64,17,0,1,10,1,5,0,1,0,0,-116,66,10,0,21,0,21,1,20,0,56,49,44,96,16,64,17,0,1,10,1,5,0,1,0,0,-16,65,10,0,21,0,56,0,50,0,30,1,11,0,10,1,5,0,1,0,0,64,64,10,0,30,0,56,0,51,0,2,0,0,0)

            val modiStream = ModiStream.makeStream(
                mModiManager.getConnectedModiUuid() and 0xFFF,
                0,
                ModiStream.STREAM_TYPE.INTERPRETER,
                byteArray
            )
            mModiManager.codeUpdater().startUpdate_plus(modiStream, callback)
        }

        binding.btnInit.setOnClickListener {


            mModeModuleManager.modules.forEach {

                val version = ModiVersion()

                when(it.type) {

                    ModiType.TYPE_NETWORK.type -> {

                    }

                    else -> {
//                        version.setAppVersion(8705)
//                        version.setOSVersion(8451)
//                        mModeModuleManager.setModuleVersion(373236312, version)
                    }
                }
            }

            mModiManager.codeUpdater().startReset_plus(callback)
        }

        binding.btnSend.setOnClickListener {

//            mPlayManager.modiPlusEvent(0,3,PlayCommand.BUTTON_CLICK, PlayCommandData.PRESSED.value)
            mModiManager.sendData(ModiProtocol.startMonitoring(0x03f, 2,0x37a))
        }

        binding.btnDisconnect.setOnClickListener {
//            mModiManager.disconnect()

            mModiManager.sendData(ModiProtocol.stopMonitoring(0x03f, 2,0x37a))
        }


        mModeModuleManager.setListener(object : ModiModuleManagerListener {

            override fun onConnectModule(manager: ModiModuleManager?, module: ModiModule?) {

                Log.d(
                    "Steave",
                    "connectionTest onConnectModule moduleTyle : ${module?.type} , moduleVersion : ${module?.version?.getAppVersion()} , index : ${module?.index} , motorIdx : ${module?.motoridx} , uuid : ${module?.uuid}"
                )
            }

            override fun onExpiredModule(manager: ModiModuleManager?, module: ModiModule?) {
                Log.d(
                    "Steave",
                    "connectionTest onExpiredModule moduleTyle : ${module?.type} , moduleVersion : ${module?.version?.getAppVersion()} , index : ${module?.index} , motorIdx : ${module?.motoridx} uuid : ${ module?.uuid}"
                )
            }

            override fun onUpdateModule(manager: ModiModuleManager?, module: ModiModule?) {
                Log.d(
                    "Steave",
                    "connectionTest onUpdateModule moduleTyle : ${module?.type} , moduleVersion : ${module?.version?.getAppVersion()} , index : ${module?.index} , motorIdx : ${module?.motoridx} uuid : ${ module?.uuid}"
                )
            }

        })

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
                mModeModuleManager.discoverModules()
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
                data?.let {

                    val frame = ModiFrame(it)

                    if (isMainFirmwareVersionProperty(cmd = frame.cmd(), frame.sid())) {
                        val connectedModiVersion = getESPVersion(
                            frame = String(
                                frame.data(),
                                Charsets.US_ASCII
                            )
                        )

                        Log.v("Greg","connectionTest espVersion : $connectedModiVersion")
                    }
                }
            }

            override fun onOffEvent() {

            }

            override fun disconnectedByModulePowerOff() {

            }

        }
    }

    private fun isMainFirmwareVersionProperty(cmd: Int, sid: Int): Boolean {
        return (cmd == 161 && sid == 9)
    }

    private fun getESPVersion(frame: String): Int {

        Log.v("Greg", "BluetoothManager -> getESPVersion -> frame : $frame")

        var espVersion = 0

        if (frame.length == 8) {


            espVersion = Integer.parseInt(
                String.format(
                    "%s%s%s",
                    frame[frame.length - 5],
                    frame[frame.length - 3],
                    frame[frame.length - 1]
                )
            )
        } else if (frame.length == 5) {

            espVersion = Integer.parseInt(
                String.format(
                    "%s%s%s",
                    frame[0],
                    frame[2],
                    frame[4]
                )
            )
        } else {
            espVersion = Integer.parseInt(
                String.format(
                    "%s%s%s",
                    frame[frame.length - 4],
                    frame[frame.length - 2],
                    frame[frame.length]
                )
            )
        }

        Log.v("Greg", "BluetoothManager -> getESPVersion -> espVersion : $espVersion")

        return espVersion
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
