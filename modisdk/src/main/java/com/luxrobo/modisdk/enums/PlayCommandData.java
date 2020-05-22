package com.luxrobo.modisdk.enums;

public enum PlayCommandData {
    PRESSED(100),
    UNPRESSED(0),
    JOYSTICK_UNPRESSED(0),
    JOYSTICK_UP(0),
    JOYSTICK_DOWN(0),
    JOYSTICK_LEFT(0),
    JOYSTICK_RIGHT(0),
    ;
    public int value;

    PlayCommandData(int value) {
        this.value = (byte) (value & 0xFF);
    }
}
