package com.luxrobo.modisdk.enums;

public enum Property {
    DATA(0x02),
    BUTTON(0x03),
    SWITCH(0x04),
    DIAL(0x05),
    JOYSTICK(0x06),
    SLIDER(0x07),
    TIMER(0x08),
    IMU_ANGLE_ROLL(0x00),
    IMU_ANGLE_PITCH(0x02),
    IMU_ANGLE_YAW(0x04),
    IMU_DIRECTION(0x00),
    IMU_ROTATION(0x00),
    ;
    public int value;

    Property(int value) {

        this.value = (value & 0xFF);
    }
}
