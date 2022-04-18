package com.luxrobo.modisdk.enums;

public enum PlayCommand {
    RECEIVE_DATA(0x00),
    BUTTON_PRESS_STATUS(0x00),
    BUTTON_CLICK(0x02),
    BUTTON_DOUBLE_CLICK(0x04),
    SWITCH(0x00),
    DIAL_POSITION(0x00),
    JOYSTICK_DIRECTION(0x00),
    SLIDER_POSITION(0x00),
    TIMER(0x00),
    IMU_ANGLE_ROLL(0x00),
    IMU_ANGLE_PITCH(0x02),
    IMU_ANGLE_YAW(0x04),
    IMU_DIRECTION(0x00),
    IMU_ROTATION(0x00),
    ;
    public int value;

    PlayCommand(int value) {

        this.value = (value & 0xFF);
    }
}
