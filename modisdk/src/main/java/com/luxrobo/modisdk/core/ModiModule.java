package com.luxrobo.modisdk.core;

import android.util.Log;

import com.luxrobo.modisdk.data.ModiVersion;
import com.luxrobo.modisdk.enums.ModiType;
import com.luxrobo.modisdk.utils.ModiStringUtil;
import com.luxrobo.modisdk.utils.TextUtilsKt;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;

public class ModiModule {

    public ModiVersion version;
    public int typeCode;
    public String type = "null";
    public int index = 0;
    public int motoridx = 0;
    public int uuid;
    public int state;
    public Timestamp lastUpdate;

    public static ModiModule makeModule(int type, int uuid, int osVersion, int appVersion, int state, Timestamp time) {
        ModiModule module = new ModiModule();
        ModiVersion version = new ModiVersion();

        version.setOSVersion(osVersion);
        version.setAppVersion(appVersion);

        module.typeCode = type;
        module.type = ModiModule.typeCodeToString(type);
        module.uuid = uuid;
        module.version = version;
        module.state = state;
        module.lastUpdate = time;
        return module;
    }

    public static String typeCodeToString(int typeCode) {
        switch (typeCode) {
            case 0x0000: return ModiType.TYPE_NETWORK.getType();
            case 0x0010: return ModiType.TYPE_BATTERY.getType();
            case 0x2000: return ModiType.TYPE_ENVIRONMENT.getType();
            case 0x2010: return ModiType.TYPE_IMU.getType();
            case 0x2020: return ModiType.TYPE_MIC.getType();
            case 0x2030: return ModiType.TYPE_BUTTON.getType();
            case 0x2040: return ModiType.TYPE_DIAL.getType();
            case 0x2050: return ModiType.TYPE_ULTRASONIC.getType();
            case 0x2060: return ModiType.TYPE_IR.getType();
            case 0x2070: return ModiType.TYPE_JOYSTICK.getType();
            case 0x2080: return ModiType.TYPE_TOF.getType();
            case 0x4000: return ModiType.TYPE_DISPLAY.getType();
            case 0x4010: return ModiType.TYPE_MOTOR.getType();
            case 0x4011: return ModiType.TYPE_MOTOR_B.getType();
            case 0x4020: return ModiType.TYPE_LED.getType();
            case 0x4030: return ModiType.TYPE_SPEAKER.getType();

            default:
                break;
        }
        return "Unknown";
    }

    public ModiModule clone() {
        ModiModule module = new ModiModule();
        module.uuid = uuid;
        module.type = type;
        module.typeCode = typeCode;
        module.version = version;
        module.state = state;
        module.lastUpdate = lastUpdate;
        return module;
    }

    public String getAppVersionToString() {

        String binaryVersionDataApp = Integer.toString(version.getAppVersion(), 2);
        int version = ModiStringUtil.getVersionFromBinary(binaryVersionDataApp);
        StringBuilder versionStr = new StringBuilder(String.valueOf(version));

        if(versionStr.length() < 3) {

            while (versionStr.length() < 3) {
                versionStr.append("0");
            }
        }

        Log.v("Greg","ModiModule getAppVersion : "+ versionStr + " appVersion " + this.version.getAppVersion());

        return TextUtilsKt.addSeparator(versionStr.toString());
    }
    public String getOSVersionToString() {
        String binaryVersionDataOS= Integer.toString(version.getOSVersion(), 2);
        int version = ModiStringUtil.getVersionFromBinary(binaryVersionDataOS);
        StringBuilder versionStr = new StringBuilder(String.valueOf(version));

        if(versionStr.length() < 3) {

            while (versionStr.length() < 3) {
                versionStr.append("0");
            }
        }
        Log.v("Greg","ModiModule getOSVersion : "+version + " getOSVersion " + this.version.getOSVersion());

        return TextUtilsKt.addSeparator(versionStr.toString());
    }

    public String getString() {
        String name = type.toLowerCase()+index;
        return String.format("this.%s = %s(0x%04X%08X);\n", name, type, typeCode, uuid);
    }

    public int getUUID() {
        return uuid;
    }
    
    public String getJsonData() {

        String haxUUID = String.format("0x%04X%08X", typeCode, uuid);

        JSONObject result = new JSONObject();

        try {
            result.put("index", String.valueOf(index));
            result.put("type", type.toLowerCase());
            result.put("uuid", haxUUID);
            result.put("osVersion", getOSVersionToString());
            result.put("appVersion", getAppVersionToString());
            result.put("motoridx", motoridx);
        } catch (JSONException e) {

        }

        Log.v("Greg","ModiModule jsonObject : "+result);

        return result.toString();
    }
}

