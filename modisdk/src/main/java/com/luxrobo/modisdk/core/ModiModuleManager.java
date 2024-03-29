package com.luxrobo.modisdk.core;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.luxrobo.modisdk.client.ModiFrameObserver;
import com.luxrobo.modisdk.data.ModiVersion;
import com.luxrobo.modisdk.enums.ModiType;
import com.luxrobo.modisdk.listener.ModiModuleManagerListener;
import com.luxrobo.modisdk.utils.ModiLog;
import com.luxrobo.modisdk.utils.ModiStringUtil;

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

    private ConcurrentHashMap<Integer, ModiModule> mModuleMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, ModiModule> mDisabledModuleMap = new ConcurrentHashMap<>();
    private HashMap<String, ArrayList<ModiModule>> multiModuleMap = new HashMap<>();
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
        ArrayList<ModiModule> motorAList = new ArrayList<>();
        ArrayList<ModiModule> motorBList = new ArrayList<>();
        ArrayList<ModiModule> ledList =new ArrayList<>();
        ArrayList<ModiModule> speakerList = new ArrayList<>();

        multiModuleMap.put(ModiType.TYPE_NETWORK.getType(), networkList);
        multiModuleMap.put(ModiType.TYPE_BATTERY.getType(), batteryList);
        multiModuleMap.put(ModiType.TYPE_ENVIRONMENT.getType(), environmentList);
        multiModuleMap.put(ModiType.TYPE_IMU.getType(), imuList);
        multiModuleMap.put(ModiType.TYPE_MIC.getType(), micList);
        multiModuleMap.put(ModiType.TYPE_BUTTON.getType(), buttonList);
        multiModuleMap.put(ModiType.TYPE_DIAL.getType(), dialList);
        multiModuleMap.put(ModiType.TYPE_JOYSTICK.getType(), joystickList);
        multiModuleMap.put(ModiType.TYPE_TOF.getType(), tofList);
        multiModuleMap.put(ModiType.TYPE_DISPLAY.getType(), displayList);
        multiModuleMap.put(ModiType.TYPE_MOTOR.getType(), motorList);
        multiModuleMap.put("MotorA", motorAList);
        multiModuleMap.put("MotorB", motorBList);
        multiModuleMap.put(ModiType.TYPE_LED.getType(), ledList);
        multiModuleMap.put(ModiType.TYPE_SPEAKER.getType(), speakerList);
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
        ArrayList<ModiModule> moduleList = multiModuleMap.get(module.type);

        for (int i = 0; i < moduleList.size(); i++) {

            if(moduleList.get(i).index != i) {
                emptyArray.add(i);
            }
        }

        if(emptyArray.isEmpty()) {
            moduleList.add(module);
        }

        else {
            moduleList.add(emptyArray.get(0),module);
        }

        return moduleList.indexOf(module);
    }

    private int addMultiModuleForMotor(ModiModule module) {

        String type = "";

        if(module.typeCode == 0x4010) {
            type = "MotorA";
            Log.v("Greg", "addMultiModuleForMotor -> MotorA");
        }

        else if (module.typeCode == 0x4011) {
            type = "MotorB";
            Log.v("Greg", "addMultiModuleForMotor -> MotorB");
        }

        ArrayList<Integer> emptyArray = new ArrayList<>();
        ArrayList<ModiModule> moduleList = multiModuleMap.get(type);

        for (int i = 0; i < moduleList.size(); i++) {

            if(moduleList.get(i).motoridx != i) {
                ModiLog.i("addMultiModuleForMotor moduleList.get(i).index " + moduleList.get(i).index  + " i = " + i);
                emptyArray.add(i);
            }
        }
        ModiLog.i("addMultiModuleForMotor moduleList size " + moduleList.size() + " type = " + type);
        ModiLog.i("addMultiModuleForMotor emptyArray size " + emptyArray.size());

        if(emptyArray.isEmpty()) {
            moduleList.add(module);
            ModiLog.i("addMultiModuleForMotor moduleList.add(module);");
        }

        else {
            moduleList.add(emptyArray.get(0),module);
            ModiLog.i("addMultiModuleForMotor emptyArray.get(0) : "  + emptyArray.get(0));
        }

        ModiLog.i(module.getString() + " addMultiModuleForMotor size " + moduleList.size() + " index : " + moduleList.indexOf(module));

        return moduleList.indexOf(module);
    }

    private void removeMultiModule(ModiModule module) {

        ArrayList<ModiModule> moduleList = multiModuleMap.get(module.type);

        if(Objects.requireNonNull(moduleList).isEmpty()) return;

        moduleList.remove(module);

            if(module.typeCode == 0x4010) {
                ArrayList<ModiModule> moduleListA = multiModuleMap.get("MotorA");
                moduleListA.remove(module);
            }

            else if(module.typeCode == 0x4011) {
                ArrayList<ModiModule> moduleListB = multiModuleMap.get("MotorB");
                moduleListB.remove(module);
            }

        jsonListForInterpreter.remove(module.getJsonData());
    }

    public void setRootModule(int uuid) {
        if (mRootmodule != null) {
            if (uuid == mRootmodule.uuid) {
                return;
            }

            ModiLog.i("root module uuid chaged" + mRootmodule + "to " + uuid);
            resetAllModules();
        }

        ModiModule rootModule = ModiModule.makeModule(0x0000, uuid, 0, 0, 0, new Timestamp(System.currentTimeMillis()));
        ModiLog.i("set Root Module " + rootModule);

        mRootmodule = rootModule;

        updateModule(uuid & 0xFFF, rootModule);

        mHandler.postDelayed(this, MODULE_CHECK_PERIOD);
    }

    public void resetAllModules() {

        ModiLog.i("resetAllModules : " + mModuleMap.size());
        expireAllModules();
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

            ModiLog.d(m.toString() + "version : " + m.version.getAppVersion());

            return m.version.getAppVersion();
        } else if (mDisabledModuleMap.containsKey(key)) {
            ModiModule m = mDisabledModuleMap.get(key);

            ModiLog.d(m.toString() + "last version : " + m.version.getAppVersion());
            return m.version.getAppVersion();
        }

        ModiLog.d(uuid + " can't find version");
        return 16690;
    }

    public void setModuleVersion(int uuid, ModiVersion version) {

        int key = uuid & 0xFFF;

        if(mModuleMap.containsKey(key)) {
            ModiModule m = mModuleMap.get(key);

            jsonListForInterpreter.remove(m.getJsonData());

            m.version = version;
            jsonListForInterpreter.add(m.getJsonData());
        }
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
                if(frame.len() < 10) {
                    ModiLog.d(" 0x05 frame.len() < 10)");
                    return;
                }
                updateModule(frame.sid(), frame.data());
                break;
            }
            case 0x0A: {
                if(frame.len() < 10) {
                    ModiLog.d(" 0x0A frame.len() < 10)");
                    return;
                }
                updateModuleState(frame.sid(), frame.data());
                break;
            }
            default:
                break;
        }
    }

    private void updateModule(int moduleKey, byte[] moduleData) {

        ModiLog.d(" updateModule " + mModuleMap.size());

        if (!mModuleMap.containsKey(moduleKey)) {

            int uuid = ByteBuffer.wrap(Arrays.copyOfRange(moduleData, 0, 4)).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
            int typeCode = ByteBuffer.wrap(Arrays.copyOfRange(moduleData, 4, 6)).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();
            int moduleOSVersion = ByteBuffer.wrap(Arrays.copyOfRange(moduleData, 6, 8)).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();
            int moduleAppVersion = ByteBuffer.wrap(Arrays.copyOfRange(moduleData, 8, 10)).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();

            int state = getModuleState(moduleKey);
            Timestamp time = new Timestamp(System.currentTimeMillis());

            ModiLog.i(" Connected. os-version binaryVersionDataOS = " + moduleOSVersion);
            ModiLog.i(" Connected. os-version binaryVersionDataApp = " + moduleAppVersion);

            ModiModule module = ModiModule.makeModule(typeCode, uuid, moduleOSVersion, moduleAppVersion, state, time);
            mModuleMap.put(moduleKey, module);
            module.index = addMultiModule(module);

            if(typeCode == 0x4010 || typeCode == 0x4011) {
                module.motoridx = addMultiModuleForMotor(module);
//                Log.v("Greg", "ModiModuleManager -> updateModule -> module : " + module.getString() + " module.motoridx :" + module.motoridx);
            }

            else if (typeCode == 0x0000) {
                mRootmodule = module;
                mHandler.postDelayed(this, MODULE_CHECK_PERIOD);
            }

            jsonListForInterpreter.add(module.getJsonData());

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
            module.index = addMultiModule(module);
            jsonListForInterpreter.add(module.getJsonData());

            if(module.typeCode == 0x4010 || module.typeCode == 0x4011) {
                module.motoridx = addMultiModuleForMotor(module);
            }

            if (mListener != null) {
                mListener.onConnectModule(this, module);
            }
        }
    }

    private void updateModuleState(int id, byte[] moduleData) {

        if (!mModuleMap.containsKey(id)) {
            int uuid = ByteBuffer.wrap(Arrays.copyOfRange(moduleData, 0, 4)).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
            int typeCode = ByteBuffer.wrap(Arrays.copyOfRange(moduleData, 4, 6)).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();
            int moduleOSVersion = ByteBuffer.wrap(Arrays.copyOfRange(moduleData, 6, 8)).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();
            int moduleAppVersion = ByteBuffer.wrap(Arrays.copyOfRange(moduleData, 8, 10)).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();
            int state = moduleData[6];
            Timestamp time = new Timestamp(System.currentTimeMillis());

            String binaryVersionDataOS = Integer.toString(moduleOSVersion, 2);
            String binaryVersionDataApp = Integer.toString(moduleAppVersion, 2);

            ModiModule module = ModiModule.makeModule(typeCode, uuid, moduleOSVersion, moduleAppVersion, state, time);

            mModuleMap.put(id, module);
            module.index = addMultiModule(module);
            jsonListForInterpreter.add(module.getJsonData());

            if(module.typeCode == 0x4010 || module.typeCode == 0x4011) {
                module.motoridx = addMultiModuleForMotor(module);
            }

            ModiLog.i(module.getString() + " Connected. os-version-cached = " + ModiStringUtil.getVersionFromBinary(binaryVersionDataApp));

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

        for (Map.Entry<String, ArrayList<ModiModule>> entry : multiModuleMap.entrySet()) {
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

//        ModiLog.d(" steave run : mRootmodule " + mRootmodule.getJsonData());

        if (mRootmodule != null) {

            Timestamp currentTime = new Timestamp(System.currentTimeMillis());

            ArrayList<Integer> expireList = new ArrayList<>();

            for (ConcurrentHashMap.Entry<Integer, ModiModule> entry : mModuleMap.entrySet()) {

                if (isRootModule(entry.getKey()))
                    continue;

                ModiModule module = entry.getValue();
                long duration = currentTime.getTime() - module.lastUpdate.getTime();
//                ModiLog.d(" steave run : " + module.getJsonData());
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
