package com.luxrobo.modisdk.callback;

import com.luxrobo.modisdk.enums.CodeUpdateError;

public interface ModiCodeUpdaterCallback {
    void onUpdateSuccess();
    void onUpdateFailed(CodeUpdateError error, String reason);
    void onUpdateProgress(int progressCount, int total);
}
