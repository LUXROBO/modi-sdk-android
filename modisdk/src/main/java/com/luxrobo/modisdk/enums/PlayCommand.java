package com.luxrobo.modisdk.enums;

public enum PlayCommand {
    RECEIVE_DATA(0x00),
    SEND_DATA(0x02),
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

    //MODI1
    JOYSTICK_MODI_1(0x0003),
    DIAL_MODI_1(0x0004),
    LEFT_SLIDER_MODI_1(0x0005),
    RIGHT_SLIDER_MODI_1(0x0006),
    TIMER_MODI_1(0x0007),
    BUTTON_CLICK_MODI_1(0x0102),
    BUTTON_DOUBLE_CLICK_MODI_1(0x0103),
    TOGGLE(0x0104),
    ;
    public int value;

    PlayCommand(int value) {

        this.value = (value & 0xFF);
    }
}
