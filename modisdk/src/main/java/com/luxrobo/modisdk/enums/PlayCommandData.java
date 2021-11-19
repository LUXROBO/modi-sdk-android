package com.luxrobo.modisdk.enums;

public enum PlayCommandData {
    PRESSED(100),
    UNPRESSED(0),
    JOYSTICK_UNPRESSED(0),
    JOYSTICK_UP(20),
    JOYSTICK_DOWN(30),
    JOYSTICK_LEFT(40),
    JOYSTICK_RIGHT(50),
    ;
    public int value;

    PlayCommandData(int value) {
        this.value = (byte) (value & 0xFF);
    }
}
