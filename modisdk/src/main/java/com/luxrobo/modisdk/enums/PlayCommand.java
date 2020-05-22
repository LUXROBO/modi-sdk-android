package com.luxrobo.modisdk.enums;

public enum PlayCommand {
    RECEIVE_DATA(0x0002),
    BUTTON_PRESS_STATUS(0x0003),
    JOYSTICK(0x0003),
    DIAL(0x0004),
    LEFT_SLIDER(0x0005),
    RIGHT_SLIDER(0x0006),
    TIMER(0x0007),
    BUTTON_CLICK(0x0102),
    BUTTON_DOUBLE_CLICK(0x0103),
    TOGGLE(0x0104),
    ;
    public int value;

    PlayCommand(int value) {

        this.value = (value & 0xFFFF);
    }
}
