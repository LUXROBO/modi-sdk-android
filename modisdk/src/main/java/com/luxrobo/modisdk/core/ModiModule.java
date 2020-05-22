package com.luxrobo.modisdk.core;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;

public class ModiModule {

    public static final String TYPE_NETWORK = "Network";
    public static final String TYPE_ENVIRONMENT = "Environment";
    public static final String TYPE_GYRO = "Gyro";
    public static final String TYPE_MIC = "Mic";
    public static final String TYPE_BUTTON = "Button";
    public static final String TYPE_DIAL = "Dial";
    public static final String TYPE_ULTRASONIC = "Ultrasonic";
    public static final String TYPE_IR = "Ir";
    public static final String TYPE_DISPLAY = "Display";
    public static final String TYPE_MOTOR = "Motor";
    public static final String TYPE_LED = "Led";
    public static final String TYPE_SPEAKER = "Speaker";

    public int version;
    public int typeCode;
    public String type = "null";
    public int uuid;
    public int state;
    public Timestamp lastUpdate;

    public static ModiModule makeModule(int type, int uuid, int version, int state, Timestamp time) {
        ModiModule module = new ModiModule();
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
            case 0x0000: return TYPE_NETWORK;
            case 0x2000: return TYPE_ENVIRONMENT;
            case 0x2010: return TYPE_GYRO;
            case 0x2020: return TYPE_MIC;
            case 0x2030: return TYPE_BUTTON;
            case 0x2040: return TYPE_DIAL;
            case 0x2050: return TYPE_ULTRASONIC;
            case 0x2060: return TYPE_IR;
            case 0x4000: return TYPE_DISPLAY;
            case 0x4010: return TYPE_MOTOR;
            case 0x4020: return TYPE_LED;
            case 0x4030: return TYPE_SPEAKER;
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

    public String getString() {

        String name = type.toLowerCase()+"0";
        return String.format("%s %s(0x%04X%08X);\n", type, name, typeCode, uuid);
//        return String.format("%s (%04X%8X)", type, typeCode, uuid);
    }

    public JSONObject getData() {

        String name = type.toLowerCase() + "0";
        String data = String.format("%s(0x%04X%08X)", name, typeCode, uuid);

        JSONObject result = new JSONObject();

        try {

            result.put(type, data);

        } catch (JSONException e) {

        }

        return result;
    }
}
