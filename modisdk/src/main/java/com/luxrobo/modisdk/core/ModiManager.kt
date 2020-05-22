package com.luxrobo.modisdk.core

import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.ParcelUuid
import com.luxrobo.modisdk.client.ModiClient
import com.luxrobo.modisdk.utils.ModiLog
import com.luxrobo.modisdk.utils.isConnected
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleConnection.GATT_MTU_MAXIMUM
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.exceptions.BleAlreadyConnectedException
import com.polidea.rxandroidble2.exceptions.BleDisconnectedException
import com.polidea.rxandroidble2.exceptions.BleScanException
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.io.IOException
import java.net.SocketException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.TimeUnit


class ModiManager : ModiFrameNotifier() {

    private lateinit var rxBleClient: RxBleClient
    private lateinit var bleDevice: RxBleDevice
    private lateinit var context: Context

    private var isAutoConnect = false
    val isScanning: Boolean
        get() = scanDisposable != null

    private var scanDisposable          : Disposable? = null
    private var connectionDisposable    : Disposable? = null
    private var stateDisposable         : Disposable? = null
    private var notificationDispasable  : Disposable? = null
    private var writeableDisposable     : Disposable? = null
    private var readableDisposable      : Disposable? = null

    private val disconnectTriggerSubject = PublishSubject.create<Unit>()
    private lateinit var mRxBleConnection : RxBleConnection
    private lateinit var connectionObservable: Observable<RxBleConnection>
    private lateinit var characteristicUuid: UUID

    private val characteristicsList = HashMap<String, BluetoothGattCharacteristic>()
    private lateinit var mBluetoothGattServices : MutableList<BluetoothGattService>

    private var modiId = byteArrayOf(4)
    private val bytesToWrite = ByteArray(1024) // a kilobyte array

    private var mModiClient: ModiClient? = null

    private lateinit var mModuleManager: ModiModuleManager
    private lateinit var mModiCodeUpdater: ModiCodeUpdater
    private lateinit var bluetoothAdapter: BluetoothAdapter

    fun init(context: Context, client: ModiClient): ModiManager {

        this.context = context
        rxBleClient = RxBleClient.create(context)
        mModiClient = client

        characteristicUuid = UUID.fromString("00008421-0000-1000-8000-00805f9b34fb")

        mModiCodeUpdater = ModiCodeUpdater(this)
        mModuleManager = ModiModuleManager(this)

        attach(mModiCodeUpdater)
        attach(mModuleManager)

        setBluetoothAdapter()

        RxJavaPlugins.setErrorHandler { e ->
            var error = e
            if (error is UndeliverableException) {
                error = e.cause
            }
            if (error is IOException || error is SocketException) {
                // fine, irrelevant network problem or API that throws on cancellation
                return@setErrorHandler
            }
            if (error is InterruptedException) {
                // fine, some blocking code was interrupted by a dispose call
                return@setErrorHandler
            }
            if (error is NullPointerException || error is IllegalArgumentException) {
                // that's likely a bug in the application

                if(Thread.currentThread().uncaughtExceptionHandler != null) {
                    Thread.currentThread().uncaughtExceptionHandler!!
                        .uncaughtException(Thread.currentThread(), error)
                }

                return@setErrorHandler
            }
            if (error is IllegalStateException) {
                // that's a bug in RxJava or in a custom operator
                if(Thread.currentThread().uncaughtExceptionHandler != null) {
                    Thread.currentThread().uncaughtExceptionHandler!!
                        .uncaughtException(Thread.currentThread(), error)
                }
                return@setErrorHandler
            }
            ModiLog.e("Undeliverable exception received, not sure what to do ${error.message}")
        }


        return this
    }

    fun scan() {

        val uuids = arrayOfNulls<UUID>(1)
        uuids[0] =
            UUID.fromString(ModiGattAttributes.convert128UUID(ModiGattAttributes.DEVICE_ADV_SERVICE))

        val parcelUuid = ParcelUuid(uuids[0])

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(parcelUuid)
            .build()

       if (isScanning) {
            scanDisposable?.dispose()
        } else {
            val scanResult = rxBleClient.scanBleDevices(scanSettings, scanFilter)

            scanResult.observeOn(AndroidSchedulers.mainThread())
                .doFinally { scanDispose() }
                .doOnSubscribe { mModiClient!!.onScan() }
                .subscribe({ mModiClient!!.onFoundDevice(it) }, { onScanFailure(it) })
                .let { scanDisposable = it }
        }

    }

    fun setAutoConnect(isAutoConnect: Boolean) {

        this.isAutoConnect = isAutoConnect

    }

    private fun startNotification() {

        mRxBleConnection.setupNotification(characteristicUuid)
            .flatMap { it }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    if(it.size == 16 || it.size == 10) {

                        val stringBuilder = StringBuilder(it.size)
                        for (byteChar in it)
                            stringBuilder.append(String.format("%02X ", byteChar))


                        if (it[0].toInt() != 0) {
                            var msg = "setupNotification Receive Bytes " + it.size + "("

                            for (i in it.indices)
                                msg += Integer.toHexString(it[i].toInt() and 0xFF) + ", "

                            msg += ")"

                            ModiLog.d(msg)

                        }

                        mModiClient!!.onReceivedData(stringBuilder.toString())
                        mModiClient!!.onReceivedData(it)

                        notifyModiFrame(ModiFrame(it))
                    }
                },
                {
                    ModiLog.e("setupNotification error  $it")
//                    onConnectionFailure(it)
                }
            )
            .let {
                notificationDispasable = it
            }


        mRxBleConnection.readCharacteristic(characteristicUuid)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    if (it[0].toInt() != 0) {
                        var msg = "readCharacteristic Receive Bytes " + it.size + "("

                        for (i in it.indices)
                            msg += Integer.toHexString(it[i].toInt() and 0xFF) + ", "

                        msg += ")"

                        ModiLog.e(msg)

                        modiId = it

                    }
                 },
                {
                    ModiLog.e("readCharacteristic $it")
                    onConnectionFailure(it)
                })
            .let {
                readableDisposable = it
            }

        mBluetoothGattServices.forEach {
            if(it.uuid == UUID.fromString(ModiGattAttributes.convert128UUID(ModiGattAttributes.DEVICE_TX_DESC))) {
                it.characteristics.forEach {

                    it.descriptors.forEach {
                        mRxBleConnection.writeDescriptor(it, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    }
                }
            }
        }

        Handler().postDelayed(
            {

                val MODI = ByteArray(16)

                for (i in 0..5) {
                    MODI[i] = 0x00
                }

                MODI[6] = 0x08
                MODI[7] = 0x00
                MODI[8] = 0x02
                MODI[9] = 0x00
                MODI[10] = 0x00
                MODI[11] = 0x00
                MODI[12] = 0x00
                MODI[13] = 0x00
                MODI[14] = 0x00
                MODI[15] = 0x00

                sendData(MODI)

                mModuleManager.setRootModule(getConnectedModiUuid())

            }
            ,1000
        )


    }


    fun connect(macAddress : String) {

        if(isConnected() && bleDevice.macAddress == macAddress) {
            return
        }

        bleDevice = rxBleClient.getBleDevice(macAddress)


//        connectionObservable = prepareConnectionObservable()

        val mtuNegotiationObservableTransformer = ObservableTransformer<RxBleConnection, RxBleConnection> { upstream ->
                upstream.doOnSubscribe { ModiLog.i("MTU", "MTU negotiation is supported") }
                    .flatMapSingle { connection ->
                        connection.requestMtu(GATT_MTU_MAXIMUM)
                            .doOnSubscribe { ModiLog.i("MTU", "Negotiating MTU started") }
                            .doOnSuccess { ModiLog.i("MTU", "Negotiated MTU: $it") }
                            .ignoreElement()
                            .andThen(Single.just(connection))
                    }
            }


        bleDevice.observeConnectionStateChanges()
            .observeOn(AndroidSchedulers.mainThread())
            .takeUntil(disconnectTriggerSubject)
            .subscribe (
                {
                    onConnectionStateChange(it)
                }
                        ,
                {
                    ModiLog.e("observeConnectionStateChanges error : ${it.message}")
                }
            )
            .let { stateDisposable = it }

        bleDevice.establishConnection(false)

            .compose(mtuNegotiationObservableTransformer)
            .flatMapSingle {
                mRxBleConnection = it
                it.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH,0, TimeUnit.MILLISECONDS)
                it.discoverServices()
            }
            .takeUntil(disconnectTriggerSubject)

            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                mBluetoothGattServices = it.bluetoothGattServices
                startNotification()
                mModiClient!!.onDiscoveredService()

                sendData(ModiProtocol.setModuleState(0xFFF, ModiProtocol.MODULE_STATE.RESET))

            }, {
                onConnectionFailure(it)
            })
            .let{
                connectionDisposable = it
            }
    }

    private fun triggerDisconnect() = disconnectTriggerSubject.onNext(Unit)


    private fun onConnectionStateChange(newState: RxBleConnection.RxBleConnectionState) {

        when(newState.name) {

            "CONNECTING" -> {
                mModiClient!!.onConnecting()
            }

            "CONNECTED"-> {
                mModiClient!!.stopScan()
                mModiClient!!.onConnected()


            }

            "DISCONNECTED"-> {
                mModiClient!!.onDisconnected()
            }
        }

    }

    private fun scanDispose() {
        scanDisposable = null
    }

    private fun onScanFailure(throwable: Throwable) {

        if (throwable is BleScanException) {
            mModiClient!!.onScanFailure()
        }
    }

    private fun onConnectionFailure(e : Throwable) {

        if(e is BleAlreadyConnectedException) {
            disconnect()

            Handler().postDelayed({
                mModiClient!!.onConnectionFailure(e)
            },500)

            return
        }

        else if (e is BleDisconnectedException){

            mModiClient!!.onConnectionFailure(e)
        }



    }


    fun isConnected() : Boolean {

        return try {
            bleDevice.isConnected
        } catch (e : Exception) {
            false
        }

    }

    private fun connectionDispose() {

        stateDisposable?.dispose()
        connectionDisposable?.dispose()
        notificationDispasable?.dispose()
        writeableDisposable?.dispose()
        readableDisposable?.dispose()
    }

    fun disconnectPermanently() {
        disconnect()
    }

    fun disconnect() {

        connectionDispose()
        mModuleManager.resetAllModules()
        triggerDisconnect()
    }

    fun stopScan() {

        scanDisposable?.dispose()
        mModiClient!!.stopScan()
    }

    fun getConnectedModiUuid() : Int {

        return try {
            ByteBuffer.wrap(modiId, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
        } catch (e: Exception) {
            ModiLog.e("getConnectedModiUuid Error $e")
            0
        }

    }

    fun moduleMgr() : ModiModuleManager{

//        mModuleManager = ModiModuleManager(this)
        return mModuleManager
    }

    fun codeUpdater(): ModiCodeUpdater {
        return mModiCodeUpdater
    }


    fun getDeviceAddress() : String {

        return bleDevice.macAddress
    }

    fun getGattCharDataRxTx() : BluetoothGattCharacteristic{

        return characteristicsList[ModiGattAttributes.DEVICE_CHAR_TX_RX]!!
    }

    fun getDeviceName() : String? {

        return bleDevice.name
    }

    fun getBleDevice() : RxBleDevice {
        return bleDevice
    }

    fun sendData(data: ByteArray) {

        if (!isConnected()) return

        mRxBleConnection.writeCharacteristic(characteristicUuid, data)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe(
                {

                    var msg = "sendData Transmit Bytes Done" + it.size + "("
                    for (i in it.indices)
                        msg += Integer.toHexString(it[i].toInt() and 0xFF) + ", "

                    msg += ")"
                    ModiLog.v(msg)

                    codeUpdater().notifySendDone(0)
                },
                {
                    ModiLog.e("writeCharacteristic error $it")
//                    onConnectionFailure(it)
                })
            .let {
                writeableDisposable = it
            }

    }

    fun sendLongWrite(data : ByteArray) {

        mRxBleConnection.createNewLongWriteBuilder()

            .setCharacteristicUuid(characteristicUuid)
            .setBytes(data)
            .build()

            .subscribe(
                {
                    var msg = "sendLongWrite Transmit Bytes Done" + it.size + "("
                    for (i in it.indices)
                        msg += Integer.toHexString(it[i].toInt() and 0xFF) + ", "

                    msg += ")"
                    ModiLog.v(msg)
                },
                {

                }
            )
            .let {
                it
            }

    }

    fun getModiId() : ByteArray{
        return modiId
    }

    /**
     * 블루투스 켜기(강제로 블루투스 사용 시)
     */
    private fun setBluetoothAdapter() {

        try {

            val bluetoothManager =
                this.context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            this.bluetoothAdapter = bluetoothManager.adapter

            if (!this.bluetoothAdapter.enable()) {
                bluetoothAdapter.enable()
            }
        } catch (e: Exception) {

            ModiLog.e("setBluetoothAdapter Error: $e")
        }

    }
    /**
     * BluetoothAdapter 활성화
     */
    fun turnOnBluetooth(): Boolean {

        try {

            if (!this.bluetoothAdapter.isEnabled) {

                this.bluetoothAdapter.enable()
            }

            return bluetoothAdapter.isEnabled

        } catch (e: Exception) {

            ModiLog.e("turnOnBluetooth Error $e")
        }

        return false
    }

    fun rebootBluetoothAdapter(): Boolean {


        if (this.bluetoothAdapter.isEnabled) {

            try {
                this.bluetoothAdapter.disable()
                ModiLog.d("Bluetooth Turning off")

                Handler().postDelayed( {

                    bluetoothAdapter.enable()

                },3000)

            } catch (e: Exception) {

                ModiLog.d("Bluetooth turn Off Error $e")
            }

        }

        return bluetoothAdapter.isEnabled
    }

    private fun BluetoothGattCharacteristic.describeProperties(): String =


        mutableListOf<String>().run {

            if (isReadable) add("REQUEST_READ")
            if (isWriteable) add("REQUEST_WRITE")
            if (isNotifiable) add("REQUEST_NOTIFY")
            joinToString(" ")
        }

    private val BluetoothGattCharacteristic.isNotifiable: Boolean
        get() = properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0

    private val BluetoothGattCharacteristic.isReadable: Boolean
        get() = properties and BluetoothGattCharacteristic.PROPERTY_READ != 0

    private val BluetoothGattCharacteristic.isWriteable: Boolean
        get() = properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0

}