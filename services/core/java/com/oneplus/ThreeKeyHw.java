package com.oneplus;

import static com.threekey.Actions.TRI_STATE_KEY_INTENT;
import static com.threekey.Actions.TRI_STATE_KEY_INTENT_EXTRA;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.util.Slog;

public class ThreeKeyHw {
    private static final String TAG = "ThreeKeyHw";
    private static final boolean debug = true;

    private static final String UDEV_NAME_THREEKEY = "tri-state-key";
    private UEventInfo mThreeKeyUEventInfo = new UEventInfo(UDEV_NAME_THREEKEY);
    private OemUEventObserver mOemUEventObserver = new OemUEventObserver();
    private Context mContext;

    public ThreeKeyHw(Context context) {
        mContext = context;
    }

    public int getState() {
        try {
            char[] buffer = new char[1024];
            FileReader file = new FileReader(mThreeKeyUEventInfo.getSwitchStatePath());
            int len = file.read(buffer, 0, 1024);
            file.close();
            return Integer.valueOf(new String(buffer, 0, len).trim());
        } catch (Exception e) {
            Slog.e(TAG, "getState failed", e);
            return -1;
        }
    }

    public void init() {
        mOemUEventObserver.startMonitor();
    }

    private final class UEventInfo {
        private final String mDevName;

        public UEventInfo(String devName) {
            mDevName = devName;
        }

        public String getDevName() {
            return mDevName;
        }

        public String getDevPath() {
            return String.format(Locale.US, "/devices/virtual/switch/%s", mDevName);
        }

        public String getSwitchStatePath() {
            return String.format(Locale.US, "/sys/class/switch/%s/state", mDevName);
        }

        public boolean checkSwitchExists() {
            return new File(getSwitchStatePath()).exists();
        }
    }


    class OemUEventObserver extends UEventObserver {
        void startMonitor() {
             this.startObserving("DEVPATH=" + mThreeKeyUEventInfo.getDevPath());
        }

        @Override
        public void onUEvent(UEventObserver.UEvent event) {
            if (debug) {
                Slog.d(TAG, "OEM UEVENT: " + event.toString());
            }

            try {
                String devPath = event.get("DEVPATH");
                String name = event.get("SWITCH_NAME");
                int state = Integer.parseInt(event.get("SWITCH_STATE"));
                sendBroadcastForZenModeChanged(state);
            } catch (NumberFormatException e) {
                Slog.e(TAG, "Could not parse switch state from event " + event);
            }
        }
    }

    private void sendBroadcastForZenModeChanged(int state) {
        Intent intentZenMode = new Intent(TRI_STATE_KEY_INTENT);
        intentZenMode.putExtra(TRI_STATE_KEY_INTENT_EXTRA, state);
        mContext.sendBroadcastAsUser(intentZenMode, UserHandle.ALL);
    }
}
