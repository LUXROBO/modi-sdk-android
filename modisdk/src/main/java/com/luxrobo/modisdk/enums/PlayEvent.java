package com.luxrobo.modisdk.enums;

public enum PlayEvent {
    INVALID(0x0000),
    DATA(0x0002),
    BUZZER(0x100),
    CAMERA(0x101),
    ;
    public int value;

    PlayEvent(int value) {
        this.value = (byte) (value & 0xFF);
    }

    public static PlayEvent fromInteger(int x) {
        switch (x) {
            case 0x0002: return DATA;
            case 0x0100: return BUZZER;
            case 0x0101: return  CAMERA;
            default:
                return INVALID;
        }
    }
}
