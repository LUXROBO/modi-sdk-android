/*
 * Developement Part, Luxrobo INC., SEOUL, KOREA
 * Copyright(c) 2018 by Luxrobo Inc.
 *
 * All rights reserved. No part of this work may be reproduced, stored in a
 * retrieval system, or transmitted by any means without prior written
 * Permission of Luxrobo Inc.
 */

package com.luxrobo.modisdk.client;

import android.bluetooth.BluetoothDevice;

import com.polidea.rxandroidble2.scan.ScanResult;


public interface ModiClient {

    void onFoundDevice(ScanResult bleScanResult);
    void onDiscoveredService();
    void onDiscoverServiceFailure();

    void onConnecting();
    void onConnected();
    void onConnectionFailure(Throwable e);
    void onDisconnected();

    void onScan();
    void stopScan();
    void onScanFailure();

    void onReceivedData(String data);
    void onReceivedData(byte[] data);

    void onOffEvent();
    void disconnectedByModulePowerOff();
}
