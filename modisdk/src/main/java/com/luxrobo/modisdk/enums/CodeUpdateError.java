package com.luxrobo.modisdk.enums;

public enum CodeUpdateError {
    SUCCESS(0),
    MODULE_TIMEOUT(1),
    CONNECTION_ERROR(2),
    CODE_NOW_UPDATING(3),
    ;

    public int value;

    CodeUpdateError(int code) {
        this.value = code;
    }
}
