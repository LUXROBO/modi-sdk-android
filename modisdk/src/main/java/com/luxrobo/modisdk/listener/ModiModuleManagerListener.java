package com.luxrobo.modisdk.listener;

import com.luxrobo.modisdk.core.ModiModule;
import com.luxrobo.modisdk.core.ModiModuleManager;

public interface ModiModuleManagerListener {
    void onConnectModule(ModiModuleManager manager, ModiModule module);
    void onExpiredModule(ModiModuleManager manager, ModiModule module);
    void onUpdateModule(ModiModuleManager manager, ModiModule module);
}
