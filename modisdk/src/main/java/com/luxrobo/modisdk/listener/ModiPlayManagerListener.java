package com.luxrobo.modisdk.listener;

public interface ModiPlayManagerListener {
    void onEventData(byte[] data);
    void onEventBuzzer(boolean enable);
    void onEventCamera(boolean enable);
}
