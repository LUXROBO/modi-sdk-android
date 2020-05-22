package com.luxrobo.modisdk.core;

import androidx.annotation.NonNull;

import com.crccalc.Crc32;
import com.crccalc.CrcCalculator;
import com.luxrobo.modisdk.callback.ModiCodeUpdaterCallback;
import com.luxrobo.modisdk.client.ModiFrameObserver;
import com.luxrobo.modisdk.enums.CodeUpdateError;
import com.luxrobo.modisdk.utils.ModiLog;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class ModiCodeUpdater implements ModiFrameObserver {

    private interface ModiFrameFilter {
        boolean filter(ModiFrame frame);
    }

    private class UploadProgressNotifier {
        private int mTotal = 0;
        private int mCount = 0;
        private boolean mDone = false;
        private ModiCodeUpdaterCallback mCallback;
        private static final int PROGRESS_NOTIFY_PERIOD = 150;

        private Timer mTimer;
        private TimerTask mTimerTask;

        public UploadProgressNotifier(int total, ModiCodeUpdaterCallback callback) {
            mTotal = total;
            mCallback = callback;

            mTimer = new Timer();
            mTimerTask = new TimerTask() {
                @Override
                public void run() {
                    if (!mDone) {
                        notifyProgressEvent();
                    }
                }
            };

            mTimer.schedule(mTimerTask, 0, PROGRESS_NOTIFY_PERIOD);
        }

        public int total() {
            return mTotal;
        }

        public int count() {
            return mCount;
        }

        public void addCount(int count) {
            mCount += count;
            if (mCount >= mTotal) {
                mDone = true;
            }
        }

        public void complete() {
            mDone = true;
            notifyProgressEvent();
        }

        public void notifyProgressEvent() {
            if (mCallback != null) {
                mCallback.onUpdateProgress(mCount, mTotal);
                ModiLog.d("notify progress : " + mCount + " / " + mTotal);
            }
        }
    }

    private static final int MODULE_PROGRESS_COUNT_UNIT = 5;

    private ModiManager mManager;
    private LinkedBlockingDeque<ModiFrame> mRecvQueue = new LinkedBlockingDeque<>();
    private ModiCodeUpdaterCallback mCallback;
    private ModiStream mStream;
    private ArrayList<ModiModule> mUpdateTargets;
    private int RetryMaxCount = 5;
    private UploadProgressNotifier mUploadProgressNotifier = null;

    // TODO: Thread deadlock 해결
    private Thread mCodeUpdateThread;
    private boolean mUserEnable;
    private boolean mPnpEnable;

    private boolean mCompleteFlag = false;
    private boolean mRunningFlag = false;
    private int total = 0;

    public ModiCodeUpdater(ModiManager manager) {
        mManager = manager;

        Timer codeUpdateCompleteTimer = new Timer();
        codeUpdateCompleteTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (mCompleteFlag == true) {
                        mCodeUpdateThread.join(100);
                        if (!mCodeUpdateThread.isAlive()) {
                            mCompleteFlag = false;
                            mRunningFlag = false;
                        }
                    }
                } catch (InterruptedException e) {

                }
            }
        }, 0, 200);
    }



    public void startUpdate(@NonNull ModiStream stream, @NonNull ModiCodeUpdaterCallback callback) {

        if (mRunningFlag == true) {
            callback.onUpdateFailed(CodeUpdateError.CODE_NOW_UPDATING, "Code Update Task is running");
            return;
        }


        mUserEnable = false;
        mPnpEnable = false;

        mStream = stream;
        mUpdateTargets = mManager.moduleMgr().getModules();
        mCallback = callback;
        mCompleteFlag = false;

        startThread();
    }

    public void startReset(@NonNull ModiCodeUpdaterCallback callback) {

        if (mRunningFlag == true) {
            callback.onUpdateFailed(CodeUpdateError.CODE_NOW_UPDATING, "Code Update Task is running");
            return;
        }

        mUserEnable = false;
        mPnpEnable = true;

        int uuid = mManager.getConnectedModiUuid();


        mStream = ModiStream.makeStream(uuid & 0xFFF, 0, ModiStream.STREAM_TYPE.INTERPRETER, new byte[0]);

        mUpdateTargets = mManager.moduleMgr().getModules();
        mCallback = callback;
        mCompleteFlag = false;

        startThread();
    }

    public boolean isRunning() {
        return mRunningFlag;
    }

    private void startThread() {
        mCodeUpdateThread = new Thread(new Runnable() {
            @Override
            public void run() {
                runUpdateTask();
            }
        });

        mCodeUpdateThread.start();
    }

    private ModiStream getStream() {
        return mStream;
    }

    private ArrayList<ModiModule> getUpdateTargets() {
        return mUpdateTargets;
    }

    @Override
    public void onModiFrame(ModiFrame frame) {

        if (frame.cmd() != 0x00) {

            mRecvQueue.push(frame);
        }

        while (mRecvQueue.size() > 64) {
            mRecvQueue.pop();
        }
    }

    protected void runUpdateTask() {

        mRunningFlag = true;

        boolean userFlag = mUserEnable;
        boolean pnpFlag = mPnpEnable;

        ModiStream stream = getStream();
        ArrayList<ModiModule> modules = getUpdateTargets();
        ModiLog.i("Connected Modules");
        for (ModiModule m : modules) {
            ModiLog.i("---- Module(" + (m.type != null ? m.type : "") + ", " + Integer.toHexString(m.uuid) + ")");
        }

        total = modules.size() * MODULE_PROGRESS_COUNT_UNIT + stream.streamBody.length;
        progressNotifierStart(total);

        try {

            send(ModiProtocol.setModuleState(0xFFF, ModiProtocol.MODULE_STATE.STOP));

            Thread.sleep(200);
            requestResetStream(stream);

            for (ModiModule module : modules) {
                // ignore network modules
                if (module.type.equals(ModiModule.TYPE_NETWORK)) {
                    progressNotifierAddCount(MODULE_PROGRESS_COUNT_UNIT);
                    continue;
                }

                for (int retry = 0; retry < RetryMaxCount; retry++) {
                    try {
                        requestChangeUpdateMode(module);
                        break;
                    } catch (Exception e) {
                        if (retry == RetryMaxCount - 1) {
                            throw e;
                        }

                        ModiLog.e("requestChangeUpdateMode Retry... count is " + retry);
                    }
                }

                for (int retry = 0; retry < RetryMaxCount; retry++) {
                    try {
                        setPlugAndPlayModule(module, pnpFlag, userFlag);
                        break;
                    } catch (Exception e) {
                        if (retry == RetryMaxCount - 1) {
                            throw e;
                        }

                        ModiLog.e("setPlugAndPlayModule Retry... count is " + retry);
                    }
                }

                progressNotifierAddCount(MODULE_PROGRESS_COUNT_UNIT);
            }

            requestStream(stream);
            send(ModiProtocol.setModuleState(0xFFF, ModiProtocol.MODULE_STATE.RESET));
            Thread.sleep(200);
            progressNotifierComplete();

            if (mCallback != null) {
                mCallback.onUpdateSuccess();
            }
        } catch (InterruptedException e) {
            send(ModiProtocol.setModuleState(0xFFF, ModiProtocol.MODULE_STATE.RESET));
            notifyUpdateFail(CodeUpdateError.MODULE_TIMEOUT, e.getMessage());
        } catch (Exception e) {
            send(ModiProtocol.setModuleState(0xFFF, ModiProtocol.MODULE_STATE.RESET));
            notifyUpdateFail(CodeUpdateError.MODULE_TIMEOUT, e.getMessage());
        }

        progressNotifierComplete();
        mCompleteFlag = true;
    }

    public void notifyUpdateFail(String reason) {

        progressNotifierComplete();
        send(ModiProtocol.setModuleState(0xFFF, ModiProtocol.MODULE_STATE.RESET));
        notifyUpdateFail(CodeUpdateError.MODULE_TIMEOUT, reason);
    }
    private void notifyUpdateFail(CodeUpdateError err, String reason) {
        if (mCallback != null) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // Sleep failed.
            }

            boolean check_ble_connection = mManager.isConnected();

            ModiLog.d(" check_ble_connection : " + check_ble_connection + " reason : " + reason);

            if (!check_ble_connection) {
                mCallback.onUpdateFailed(CodeUpdateError.CONNECTION_ERROR, "check ble connection.");
            } else {
                mCallback.onUpdateFailed(err, reason);
            }
        }

    }

    private void send(byte[] data) {



        try {
            mManager.sendData(data);
            waitForSendDone();
        } catch (Exception e) {
            ModiLog.e("waitForSendDone Failed.");
        }


    }

    private void sendLongWrite(byte[] data) {

        try {

            mManager.sendLongWrite(data);
//            waitForSendDone();
        } catch (Exception e) {
            ModiLog.e("waitForSendDone Failed.");
        }
    }

    public synchronized void waitForSendDone() throws Exception {
        // TODO: Timeout 시 핸들링 개선
        wait(1000);
    }

    public synchronized void notifySendDone(int status) {
        if (status != 0) {
            ModiLog.e("Write Request Failed : " + status);
        } else {

        }

        notifyAll();
    }

    private void requestChangeUpdateMode(ModiModule module) throws Exception {
        final int targetModuleKey = module.uuid & 0xFFF;

        // change update mode
        mRecvQueue.clear();
        send(ModiProtocol.setModuleState(targetModuleKey, ModiProtocol.MODULE_STATE.UPDATE));
        ModiFrame res = waitForModiFrame(1200, getModuleStateFilter(targetModuleKey));
        if (res.data()[6] != ModiProtocol.MODULE_WARNING.FIRMWARE.value) {
            throw new Exception("module is not update mode, " + res.data()[6]);
        }

        ModiLog.i(module.getString() + " enter update mode");

        // change update ready
        mRecvQueue.clear();
        send(ModiProtocol.setModuleState(targetModuleKey, ModiProtocol.MODULE_STATE.UPDATE_READY));
        res = waitForModiFrame(1200, new ModiFrameFilter() {
            @Override
            public boolean filter(ModiFrame frame) {
                if (frame.cmd() == 0x0A && frame.sid() == targetModuleKey && frame.data()[6] != 1) {
                    return true;
                }

                return false;
            }
        });
        if (res.data()[6] != ModiProtocol.MODULE_WARNING.FIRMWARE_READY.value) {
            throw new Exception(module.getString() + " is not update ready, " + res.data()[6]);
        }

        ModiLog.i(module.getString() + " enter update ready");
    }

    private void requestResetStream(final ModiStream stream) throws Exception {
        ModiFrame response;

        final ModiStream resetStream = new ModiStream();
        resetStream.streamType = stream.streamType;
        resetStream.moduleId = stream.moduleId;
        resetStream.streamId = stream.streamId;
        resetStream.streamBody = new byte[0];

        int response_code = ModiStream.STREAM_RESPONSE.SUCCESS.value;

        for (int i = 0; i < RetryMaxCount; i++) {
            try {
                mRecvQueue.clear();


                send(ModiProtocol.streamCommand(resetStream));

                response = waitForModiFrame(5000, getStreamFilter(stream.moduleId, stream.streamId));

                response_code = response.data()[1];

                if (response_code != ModiStream.STREAM_RESPONSE.SUCCESS.value) {
                    ModiLog.e("Stream Reset Error. (" + response_code + ") retry : " + i);
                    continue;
                }

                return;
            } catch (Exception e) {
                ModiLog.e("Stream Reset Response Timeout. ");
            }
        }

        throw new Exception("Modi Stream Command response failed : " + (int) response_code);
    }

    private void requestStream(final ModiStream stream) throws Exception {

        ModiFrame response;

        if (stream.streamBody.length > 0) {
            mRecvQueue.clear();

            send(ModiProtocol.streamCommand(stream));

            response = waitForModiFrame(5000, getStreamFilter(stream.moduleId, stream.streamId));
            if (response.data()[1] != ModiStream.STREAM_RESPONSE.SUCCESS.value) {
                throw new Exception("Modi Stream Command response failed : " + (int) response.data()[1]);
            }

            ArrayList<byte[]> frames = ModiProtocol.streamDataList(stream);

            for (byte[] frame : frames) {
                mRecvQueue.clear();
                send(frame);

//                buffer.put(frame);
                progressNotifierAddCount(frame[6] - 1);
            }

//            sendLongWrite(buffer.array());


            response = waitForModiFrame(5000, getStreamFilter(stream.moduleId, stream.streamId));
            if (response.data()[1] != 0x00) {
                throw new Exception("Modi Stream Command response failed : " + (int) response.data()[1]);
            }
        }
    }

    public static byte[] reverseBlock(byte[] source) {
        byte[] buffer = new byte[8];

        int old = 0;
        for (int i = 0, j = 3; i < 8; i++, j--) {
            buffer[i] = source[j];
            if (j == old) {
                j = i + 5;
                old = i + 1;
            }
        }

        return buffer;
    }


    private void setPlugAndPlayModule(ModiModule module, boolean pnpEnable, boolean userEnable) throws Exception {
        ModiFrame res;
        final int targetModuleKey = module.uuid & 0xFFF;

        ModiLog.i(module.getString() + "flash write started");

        // Send Firmware Erase
        mRecvQueue.clear();

        send(ModiProtocol.firmwareCommand(targetModuleKey, ModiProtocol.FLASH_CMD.ERASE, 0x0801F800, 0));

        // Wait status response
        res = waitForModiFrame(5000, getFirmwareFilter(targetModuleKey));
        if (res.data()[4] != 0x07) {
            throw new Exception(module.getString() + "flash erase error, " + res.data()[4]);
        }

        ModiLog.i(module.getString() + "flash erase success");

        // Send Firmware Data
        byte[] pnpData = new byte[8];
        pnpData[0] = (byte) 0xAA;
        pnpData[1] = (byte) (pnpEnable ? 0 : 1); // pnp mode (0 : enable, 1: disable)
        pnpData[2] = (byte) (userEnable ? 1 : 0); // user task (0 : disable, 1: enable)
        pnpData[3] = (byte) 0x00;

        ByteBuffer id_buffer = ByteBuffer.allocate(4);
        ByteBuffer version_buffer = ByteBuffer.allocate(4);

        id_buffer.order(ByteOrder.LITTLE_ENDIAN).putInt(module.uuid & 0xFFF);
        version_buffer.order(ByteOrder.LITTLE_ENDIAN).putInt(module.version);

        pnpData[4] = id_buffer.get(0);
        pnpData[5] = id_buffer.get(1);
        pnpData[6] = version_buffer.get(0);
        pnpData[7] = version_buffer.get(1);

        send(ModiProtocol.firmwareData(targetModuleKey, 0, pnpData));

        byte[] reverseData = reverseBlock(pnpData);

        String revlog = "";
        for (int i = 0; i < reverseData.length; i++) {
            revlog += Integer.toHexString((int) reverseData[i] & 0xFF) + ", ";
        }

        ModiLog.d("reverse data : " + revlog);

        long crcValue = calculateCrc32(reverseData);

        ModiLog.d("crc : " + Long.toHexString(crcValue));

        mRecvQueue.clear();
        send(ModiProtocol.firmwareCommand(targetModuleKey, ModiProtocol.FLASH_CMD.CHECK_CRC, 0x0801F800, (int) crcValue));

        res = waitForModiFrame(5000, getFirmwareFilter(targetModuleKey));
        if (res.data()[4] != 0x05) {
            throw new Exception(module.getString() + "flash verification error");
        }

        ModiLog.i(module.getString() + "flash verification success");
    }

    private long calculateCrc32(byte[] data) {
        // Send Firmware Verify
        CrcCalculator crc_calc = new CrcCalculator(Crc32.Crc32Mpeg2);

        long value = crc_calc.Calc(data, 0, data.length);

        return value;
    }

    private ModiFrame waitForModiFrame(int timeout, ModiFrameFilter frameFilter) throws Exception {

        Timestamp start_time = new Timestamp(System.currentTimeMillis());
        Timestamp cur_time = start_time;

        while (cur_time.getTime() - start_time.getTime() < timeout) {

            if (!mRecvQueue.isEmpty()) {

                ModiFrame frame = mRecvQueue.pop();

                if (frameFilter.filter(frame)) {

                    return frame;
                }
            } else {

                try {

                    Thread.sleep(100);

                } catch (InterruptedException e) {

                }
            }

            cur_time = new Timestamp(System.currentTimeMillis());
        }

        throw new TimeoutException();
    }

    private ModiFrameFilter getStreamFilter(int moduleKey, int streamId) {
        final AtomicInteger targetStreamKey = new AtomicInteger(moduleKey << 8 | streamId);
        return new ModiFrameFilter() {
            @Override
            public boolean filter(ModiFrame frame) {
                if (frame.cmd() == 0x11) {
                    int streamKey = frame.sid() << 8 | frame.data()[0];
                    if (streamKey == targetStreamKey.get()) {
                        return true;
                    }
                }

                return false;
            }
        };
    }

    private ModiFrameFilter getFirmwareFilter(final int moduleKey) {
        return new ModiFrameFilter() {
            @Override
            public boolean filter(ModiFrame frame) {
                if (frame.cmd() == 0x0C && frame.sid() == moduleKey)
                    return true;
                return false;
            }
        };
    }

    private ModiFrameFilter getModuleStateFilter(final int moduleKey) {
        return new ModiFrameFilter() {
            @Override
            public boolean filter(ModiFrame frame) {
                if (frame.cmd() == 0x0A && frame.sid() == moduleKey) {
                    return true;
                }
                return false;
            }
        };
    }

    private void progressNotifierStart(int total) {
        // ModiLog.d("progressNotifierStart : " + total);
        mUploadProgressNotifier = new UploadProgressNotifier(total, mCallback);
    }

    private void progressNotifierAddCount(int count) {
        // ModiLog.d("progressNotifierAddCount : " + count);
        if (mUploadProgressNotifier != null)
            mUploadProgressNotifier.addCount(count);
    }

    private void progressNotifierComplete() {
        // ModiLog.d("progressNotifierComplete");
        if (mUploadProgressNotifier != null) {
            mUploadProgressNotifier.complete();
        }

        mUploadProgressNotifier = null;
    }
}
