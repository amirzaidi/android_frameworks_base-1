package com.threekey;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;

import com.oem.os.IOemExService;

public class ThreeKeyManager {
    private static final String TAG = "ThreeKeyManager";
    private static IOemExService sService;

    public ThreeKeyManager(Context context) {
    }

    public boolean isUnlocked() {
        android.util.Log.d(TAG, "isUnlocked()");
        try {
            return getService().isUnlocked();
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "service is unavailable");
        }
        return true;
    }

    public static IOemExService getService() {
        if (sService == null) {
            IBinder b = ServiceManager.getService("OEMExService");
            sService = IOemExService.Stub.asInterface(b);
        }
        return sService;
    }
}
