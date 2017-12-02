/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (C) 2013-2016, OnePlus Technology Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;

import com.android.internal.os.BackgroundThread;

import com.oem.os.IOemExService;
import com.oneplus.ThreeKey;
import com.oneplus.ThreeKeyHw;

public final class OemExService extends IOemExService.Stub {
    private static final String TAG = "OemExService";

    // For message handler
    private static final int MSG_SYSTEM_READY = 1;

    private final Object mLock = new Object();
    private Context mContext;

    // held while there is a pending state change.
    private final WakeLock mWakeLock;

    private ThreeKeyHw threekeyhw;
    private ThreeKey threekey;

    private final Handler mHandler = new Handler(Looper.myLooper(), null, true) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_SYSTEM_READY) {
                threekeyhw = new ThreeKeyHw(mContext);
                threekeyhw.init();
                threekey = new ThreeKey(mContext, threekeyhw.getState());
                if (mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
            }
        }
    };

    public OemExService(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mContext = context;
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
    }

    public boolean isUnlocked() {
        return threekey.isUnlocked();
    }

    public void systemRunning() {
        synchronized (mLock) {
            // This wakelock will be released by the handler
            if (!mWakeLock.isHeld()) {
                mWakeLock.acquire();
            }

            // Use message to aovid blocking system server
            Message msg = mHandler.obtainMessage(MSG_SYSTEM_READY, 0, 0, null);
            mHandler.sendMessage(msg);
        }
    }
}
