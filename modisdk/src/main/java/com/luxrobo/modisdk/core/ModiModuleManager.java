package com.luxrobo.modisdk.core;

import android.os.Handler;
import android.os.Looper;
import com.luxrobo.modisdk.listener.ModiModuleManagerListener;
import com.luxrobo.modisdk.client.ModiFrameObserver;
import com.luxrobo.modisdk.utils.ModiLog;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;


public class ModiModuleManager implements ModiFrameObserver, Runnable {

    private static final byte MODULE_STATE_UNKNOWN = (byte) 0xFF;
    private static final int MODULE_TIMEOUT_PERIOD = 2000;
    private static final int MODULE_CHECK_PERIOD = 500;

    private ConcurrentHashMap<Integer, ModiModule> mModuleMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, ModiModule> mDisabledModuleMap = new ConcurrentHashMap<>();

    private ModiModuleManagerListener mListener = null;
    private ModiManager mModiMananger;
    private ModiModule mRootmodule = null;
    private Handler mHandler;

    ModiModuleManager(ModiManager manager) {
        mModiMananger = manager;
        mHandler = new Handler(Looper.getMainLooper());

        ModiLog.i("ModiModuleManager init");

    }


    public void setListener(ModiModuleManagerListener listener) {
        mListener = listener;
    }

    public boolean discoverModules() {

        ModiLog.i("ModiModuleManager discoverModules");

        mModiMananger.sendData(ModiProtocol.discoverModule(0xFFF, (byte) 0x0));
        return true;
    }

    // 현재 connect 된 모듈 list
    public ArrayList<ModiModule> getModules() {
        ArrayList<ModiModule> modules = new ArrayList<>();

        for (ConcurrentHashMap.Entry<Integer, ModiModule> entry : mModuleMap.entrySet()) {
            ModiModule module = entry.getValue();
            modules.add(module);
        }

        return modules;
    }

    public void setRootModule(int uuid) {
        if (mRootmodule != null) {
            if (uuid == mRootmodule.uuid) {
                return;
            }

            ModiLog.i("root module uuid chaged" + mRootmodule.toString() + "to " + uuid);
            resetAllModules();
        }

        ModiModule rootModule = ModiModule.makeModule(0x0000, uuid, 0, 0, new Timestamp(System.currentTimeMillis()));
        ModiLog.i("set Root Module " + rootModule.toString());

        mRootmodule = rootModule;

        updateModule(uuid & 0xFFF, rootModule);

        mHandler.postDelayed(this, MODULE_CHECK_PERIOD);
    }

    public void resetAllModules() {
        if (mRootmodule != null) {
            ModiLog.i("expire root module : " + mRootmodule.toString());
            expireAllModules();
            mRootmodule = null;
        }
    }

    public ModiModule getModule(int uuid) {
        return mModuleMap.get(uuid & 0xFFF);
    }

    public int getModuleState(int uuid) {
        int key = uuid & 0xFFF;
        ModiModule m = mModuleMap.get(key);
        if (m != null) {
            return m.state;
        }

        return MODULE_STATE_UNKNOWN;
    }

    public int getModuleVersion(int uuid) {
        int key = uuid & 0xFFF;
        if (mModuleMap.containsKey(key)) {
            ModiModule m = mModuleMap.get(key);

            ModiLog.d(m.toString() + "version : " + m.version);

            return m.version;
        } else if (mDisabledModuleMap.containsKey(key)) {
            ModiModule m = mDisabledModuleMap.get(key);

            ModiLog.d(m.toString() + "last version : " + m.version);
            return m.version;
        }

        ModiLog.d(uuid + " can't find version");
        return 16690;
    }

    @Override
    public void onModiFrame(ModiFrame frame) {
        int cmd = frame.cmd();
        switch (cmd) {
            case 0x07:
            case 0x00: {
                updateModuleTime(frame.sid());
                break;
            }
            case 0x05: {
                updateModule(frame.sid(), frame.data());
                break;
            }
            case 0x0A: {
                updateModuleState(frame.sid(), frame.data());
                break;
            }
            default:
                break;
        }
    }

    private void updateModule(int moduleKey, byte[] moduleData) {
        if (!mModuleMap.containsKey(moduleKey)) {
            int uuid = ByteBuffer.wrap(Arrays.copyOfRange(moduleData, 0, 4)).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
            int typeCode = ByteBuffer.wrap(Arrays.copyOfRange(moduleData, 4, 6)).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();
            int version = ByteBuffer.wrap(Arrays.copyOfRange(moduleData, 6, 8)).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();
            int state = getModuleState(moduleKey);
            Timestamp time = new Timestamp(System.currentTimeMillis());

            if (version == 10 || version == 0) {
                version = 16690;
            }

            ModiModule module = ModiModule.makeModule(typeCode, uuid, version, state, time);
            mModuleMap.put(moduleKey, module);

            ModiLog.i(module.getString() + " Connected. os-version = " + version);

            removeDisableMapModule(moduleKey);

            if (mListener != null) {
                mListener.onConnectModule(this, module);
            }
        } else {
            updateModuleTime(moduleKey);
        }
    }

    private void updateModule(int moduleKey, ModiModule module) {
        if (!mModuleMap.containsKey(moduleKey)) {
            mModuleMap.put(moduleKey, module);

            ModiLog.i(module.getString() + " Connected");

            if (mListener != null) {
                mListener.onConnectModule(this, module);
            }
        }
    }

    private void updateModuleState(int id, byte[] moduleData) {

        if (!mModuleMap.containsKey(id)) {
            int uuid = ByteBuffer.wrap(Arrays.copyOfRange(moduleData, 0, 4)).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
            int typeCode = ByteBuffer.wrap(Arrays.copyOfRange(moduleData, 4, 6)).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();
            int version = getModuleVersion(id); // ByteBuffer.wrap(Arrays.copyOfRange(moduleData, 6, 8)).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();
            int state = moduleData[6];
            Timestamp time = new Timestamp(System.currentTimeMillis());

            ModiModule module = ModiModule.makeModule(typeCode, uuid, version, state, time);
            mModuleMap.put(id, module);

            ModiLog.i(module.getString() + " Connected. os-version-cached = " + version);

            removeDisableMapModule(id);

            if (mListener != null) {
                mListener.onConnectModule(this, module);
            }
        }

        if (mModuleMap.containsKey(id)) {
            ModiModule m = mModuleMap.get(id);
            m.state = moduleData[6];
            m.lastUpdate = new Timestamp(System.currentTimeMillis());

            ModiLog.i(m.getString() + " update state (" + m.state + ")");

            if (mListener != null) {
                mListener.onUpdateModule(this, m);
            }
        }
    }

    private void updateModuleTime(int id) {
        ModiModule m = mModuleMap.get(id);
        if (m != null) {
            m.lastUpdate = new Timestamp(System.currentTimeMillis());
        }

        else {
            mModiMananger.sendData(ModiProtocol.discoverModule(id, (byte) 0x0));
        }
    }

    private void expireModule(Integer key) {
        if (mModuleMap.containsKey(key)) {
            ModiModule module = mModuleMap.get(key);

            mModuleMap.remove(key);
            mDisabledModuleMap.put(key, module);

            ModiLog.i(module.getString() + " Expired");
            if (mListener != null) {
                mListener.onExpiredModule(this, module);
            }
        }
    }

    private void removeDisableMapModule(int key) {
        if (mDisabledModuleMap.containsKey(key)) {
            mDisabledModuleMap.remove(key);
        }
    }

    private void expireAllModules() {
        ArrayList<Integer> expireList = new ArrayList<>();
        for (ConcurrentHashMap.Entry<Integer, ModiModule> entry : mModuleMap.entrySet()) {
            // MODULE_TIMEOUT_PERIOD 이상 응답이 없는 모듈 expire
            expireList.add(entry.getKey());
        }

        for (Integer key : expireList) {
            expireModule(key);
        }

        mModuleMap.clear();
    }

    private boolean isRootModule(int moduleKey) {

        if (mRootmodule == null)
            return false;

        int rootModuleKey = mRootmodule.uuid & 0xFFF;
        if (rootModuleKey == moduleKey)
            return true;

        return false;
    }

    @Override
    public void run() {

        if (mRootmodule != null) {

            Timestamp currentTime = new Timestamp(System.currentTimeMillis());

            ArrayList<Integer> expireList = new ArrayList<>();

            for (ConcurrentHashMap.Entry<Integer, ModiModule> entry : mModuleMap.entrySet()) {

                if (isRootModule(entry.getKey()))
                    continue;

                ModiModule module = entry.getValue();
                long duration = currentTime.getTime() - module.lastUpdate.getTime();

                // MODULE_TIMEOUT_PERIOD 이상 응답이 없는 모듈 expire
                if (duration > MODULE_TIMEOUT_PERIOD) {
                    ModiLog.i(module.getString() + " add expireList");
                    expireList.add(entry.getKey());
                }
            }

            for (Integer key : expireList) {
                expireModule(key);
            }

            mHandler.postDelayed(this, MODULE_CHECK_PERIOD);
        } else {
            ModiLog.i("Root Module is not set");
//            mHandler.postDelayed(this, 2000);
        }
    }
}
