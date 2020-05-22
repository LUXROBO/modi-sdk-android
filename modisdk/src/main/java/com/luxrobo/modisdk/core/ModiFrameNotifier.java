package com.luxrobo.modisdk.core;

import com.luxrobo.modisdk.client.ModiFrameObserver;
import java.util.ArrayList;

public abstract class ModiFrameNotifier {

    private ArrayList<ModiFrameObserver> mObservers = new ArrayList<>();

    public void attach(ModiFrameObserver observer) {
        for (ModiFrameObserver v : mObservers) {
            if (v == observer) {
                return;
            }
        }

        mObservers.add(observer);
    }

    public void detach(ModiFrameObserver observer) {

        ModiFrameObserver detachone = null;
        for (ModiFrameObserver v : mObservers) {
            if (v == observer) {
                detachone = v;
                break;
            }
        }

        if (detachone != null) {
            mObservers.remove(detachone);
        }
    }

    public void notifyModiFrame(ModiFrame frame) {
        for (ModiFrameObserver observer : mObservers) {
            observer.onModiFrame(frame);
        }
    }
}
