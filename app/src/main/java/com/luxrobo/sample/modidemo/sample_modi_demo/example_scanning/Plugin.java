package com.luxrobo.sample.modidemo.sample_modi_demo.example_scanning;

import android.app.Activity;

import com.luxrobo.modisdk.client.ModiClient;
import com.luxrobo.modisdk.core.ModiManager;
import com.polidea.rxandroidble2.scan.ScanResult;

import java.util.List;

public class Plugin {

    private List scanData;
    private List scanResultData;

    private ModiClient mModiClient;
    public ModiManager mModiManager;

    public void init() {
        setModiClient();
        setModiManager();
    }

    private void setModiClient() {
        mModiClient = new ModiClient() {

            @Override
            public void onFoundDevice(ScanResult bleScanResult) {

            }

            @Override
            public void onDiscoveredService() {

            }

            @Override
            public void onDiscoverServiceFailure() {

            }

            @Override
            public void onConnecting() {

            }

            @Override
            public void onConnected() {

            }

            @Override
            public void onConnectionFailure(Throwable e) {

            }

            @Override
            public void onDisconnected() {

            }

            @Override
            public void onScan() {

            }

            @Override
            public void stopScan() {

            }

            @Override
            public void onScanFailure() {

            }

            @Override
            public void onReceivedData(String data) {

            }

            @Override
            public void onReceivedData(byte[] data) {

            }

            @Override
            public void onOffEvent() {

            }

            @Override
            public void disconnectedByModulePowerOff() {

            }
        };
    }

    private void setModiManager() {
        mModiManager = new ModiManager().init(new Activity().getApplicationContext(), mModiClient);
    }
}
