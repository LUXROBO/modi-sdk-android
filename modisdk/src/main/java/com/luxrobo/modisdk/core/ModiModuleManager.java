package com.luxrobo.modisdk.core;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.luxrobo.modisdk.client.ModiFrameObserver;
import com.luxrobo.modisdk.listener.ModiModuleManagerListener;
import com.luxrobo.modisdk.utils.ModiLog;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


public class ModiModuleManager implements ModiFrameObserver, Runnable {

    private static final byte MODULE_STATE_UNKNOWN = (byte) 0xFF;
    private static final int MODULE_TIMEOUT_PERIOD = 5000;
    private static final int MODULE_CHECK_PERIOD = 5000;
    private int modiDataFrameSize = 16;
    private ConcurrentHashMap<Integer, ModiModule> mModuleMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, ModiModule> mDisabledModuleMap = new ConcurrentHashMap<>();
    private HashMap<Integer, ArrayList<ModiModule>> multiModuleMap = new HashMap<>();
    private ArrayList<String> jsonListForInterpreter = new ArrayList<String>();

    private ModiModuleManagerListener mListener = null;
    private ModiManager mModiMananger;
    private ModiModule mRootmodule = null;
    private Handler mHandler;

    ModiModuleManager(ModiManager manager) {

        ModiLog.i("ModiModuleManager init");

        mModiMananger = manager;
        mHandler = new Handler(Looper.getMainLooper());

        ArrayList<ModiModule> networkList = new ArrayList<>();
        ArrayList<ModiModule> batteryList = new ArrayList<>();
        ArrayList<ModiModule> environmentList = new ArrayList<>();
        ArrayList<ModiModule> imuList = new ArrayList<>();
        ArrayList<ModiModule> micList = new ArrayList<>();
        ArrayList<ModiModule> buttonList = new ArrayList<>();
        ArrayList<ModiModule> dialList = new ArrayList<>();
        ArrayList<ModiModule> joystickList = new ArrayList<>();
        ArrayList<ModiModule> tofList = new ArrayList<>();
        ArrayList<ModiModule> displayList = new ArrayList<>();
        ArrayList<ModiModule> motorList = new ArrayList<>();
        ArrayList<ModiModule> motorBList = new ArrayList<>();
        ArrayList<ModiModule> ledList =new ArrayList<>();
        ArrayList<ModiModule> speakerList = new ArrayList<>();

        multiModuleMap.put(0x0000, networkList);
        multiModuleMap.put(0x0010, batteryList);
        multiModuleMap.put(0x2000, environmentList);
        multiModuleMap.put(0x2010, imuList);
        multiModuleMap.put(0x2020, micList);
        multiModuleMap.put(0x2030, buttonList);
        multiModuleMap.put(0x2040, dialList);
        multiModuleMap.put(0x2070, joystickList);
        multiModuleMap.put(0x2080, tofList);
        multiModuleMap.put(0x4000, displayList);
        multiModuleMap.put(0x4010, motorList);
        multiModuleMap.put(0x4011, motorBList);
        multiModuleMap.put(0x4020, ledList);
        multiModuleMap.put(0x4030, speakerList);
        //MODI PLUS 다중 연결을 위한 리스트
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

    private int addMultiModule(ModiModule module) {

        ArrayList<Integer> emptyArray = new ArrayList<>();
        ArrayList<ModiModule> moduleList = multiModuleMap.get(module.typeCode);

        for (int i = 0; i < moduleList.size() ; i++) {

            if(moduleList.get(i).index != i + 1) {
                emptyArray.add(i);
            }
        }

        if(emptyArray.isEmpty()) {
            moduleList.add(module);
        }

        else {
            moduleList.add(emptyArray.get(0),module);
        }

        return moduleList.indexOf(module)+1;
    }

    private void removeMultiModule(ModiModule module) {

        ArrayList<ModiModule> moduleList = multiModuleMap.get(module.typeCode);

        if(Objects.requireNonNull(moduleList).isEmpty()) return;

        moduleList.remove(module);

        jsonListForInterpreter.remove(module.getJsonData());
    }

    public void setRootModule(int uuid) {
        if (mRootmodule != null) {
            if (uuid == mRootmodule.uuid) {
                return;
            }

            ModiLog.i("root module uuid chaged" + mRootmodule.toString() + "to " + uuid);
            resetAllModules();
        }

        ModiModule rootModule = ModiModule.makeModule(0x0000, uuid, 0, 0, 0, new Timestamp(System.currentTimeMillis()));
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

            ModiLog.d(m.toString() + "version : " + m.subVersion);

            return m.subVersion;
        } else if (mDisabledModuleMap.containsKey(key)) {
            ModiModule m = mDisabledModuleMap.get(key);

            ModiLog.d(m.toString() + "last version : " + m.subVersion);
            return m.subVersion;
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

            for (int i = 0; i < moduleData.length; i++) {
                Log.v("Greg", "ModiModuleManager -> updateModule -> moduleData : " + moduleData[i]);
            }

            int uuid = ByteBuffer.wrap(Arrays.copyOfRange(moduleData, 0, 4)).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
            int typeCode = ByteBuffer.wrap(Arrays.copyOfRange(moduleData, 4, 6)).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();
            int decimalVersionData = ByteBuffer.wrap(Arrays.copyOfRange(moduleData, 6, 8)).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();

            int state = getModuleState(moduleKey);
            Timestamp time = new Timestamp(System.currentTimeMillis());

            if (decimalVersionData == 10 || decimalVersionData == 0) {
                decimalVersionData = 16690;
            }

            String binaryVersionData = Integer.toString(decimalVersionData, 2);


            ModiModule module = ModiModule.makeModule(typeCode, uuid, decimalVersionData, getSubVersion(binaryVersionData), state, time);
            mModuleMap.put(moduleKey, module);
            module.index = addMultiModule(module);
            jsonListForInterpreter.add(module.getJsonData());

            ModiLog.i(module.getString() + " Connected. os-version = " + decimalVersionData);

            removeDisableMapModule(moduleKey);

            if (mListener != null) {
                mListener.onConnectModule(this, module);
            }
        } else {
            updateModuleTime(moduleKey);
        }
    }

    private int getSubVersion(String data) {
        return convertBinaryToDecimal(getBinaryOfSubVersion(data));
    }

    private StringBuffer getBinaryOfSubVersion(String binaryString) {
        StringBuffer stringBuffer = new StringBuffer(modiDataFrameSize);

        for (int i = 0; i < modiDataFrameSize - binaryString.length(); i++) {
            stringBuffer.append("0");
        }

        for (int i = 0; i < binaryString.length(); i++) {
            stringBuffer.append(binaryString.charAt(i));
        }

        return stringBuffer;
    }

    private int convertBinaryToDecimal(StringBuffer frameOfSubVersion) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(Integer.parseInt(frameOfSubVersion.toString().substring(0, 3), 2));
        stringBuffer.append(Integer.parseInt(frameOfSubVersion.toString().substring(3, 8), 2));
        stringBuffer.append(Integer.parseInt(frameOfSubVersion.toString().substring(8, 16), 2));
        return Integer.parseInt(stringBuffer.toString());
    }

    private void updateModule(int moduleKey, ModiModule module) {
        if (!mModuleMap.containsKey(moduleKey)) {
            mModuleMap.put(moduleKey, module);
            ModiLog.i(module.getString() + " Connected");
            module.index = addMultiModule(module);
            jsonListForInterpreter.add(module.getJsonData());

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

            Log.v("Greg", "ModiModuleManager -> updateModuleState -> version : " + version);

            ModiModule module = ModiModule.makeModule(typeCode, uuid, version, version, state, time);
            mModuleMap.put(id, module);
            module.index = addMultiModule(module);
            jsonListForInterpreter.add(module.getJsonData());

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
            removeMultiModule(module);
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

        for (Map.Entry<Integer, ArrayList<ModiModule>> entry : multiModuleMap.entrySet()) {
            Objects.requireNonNull(multiModuleMap.get(entry.getKey())).clear();
        }
    }

    private boolean isRootModule(int moduleKey) {

        if (mRootmodule == null)
            return false;

        int rootModuleKey = mRootmodule.uuid & 0xFFF;
        if (rootModuleKey == moduleKey)
            return true;

        return false;
    }

    public ArrayList<String> getJsonListForInterpreter() {
        return jsonListForInterpreter;
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
