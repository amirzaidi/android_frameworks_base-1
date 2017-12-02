package com.oneplus;

import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.UserHandle;
import android.util.Slog;
import android.content.Context;
import android.os.Vibrator;
import android.media.AudioManager;
import android.app.NotificationManager;
import android.provider.Settings;

import com.threekey.Actions;

public class ThreeKey {
    private static final String TAG = "ThreeKey";

    private static final String ACTION_THREE_KEY = Actions.TRI_STATE_KEY_INTENT;
    private static final String ACTION_THREE_KEY_EXTRA = Actions.TRI_STATE_KEY_INTENT_EXTRA;

    public static final int SWITCH_STATE_UP = 1;
    public static final int SWITCH_STATE_MIDDLE = 2;
    public static final int SWITCH_STATE_DOWN = 3;

    private final Context mContext;
    private Vibrator mVibrator;
    private NotificationManager mNotificationManager;
    private AudioManager mAudioManager;

    private boolean mUpAllowAlarms = false;
    private boolean mMiddleStartPriority = true;
    private boolean mMiddleStartVibrate = false;

    private int mThreeKeyMode;

    public ThreeKey(Context context, int switchState) {
        mContext = context;
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        mThreeKeyMode = switchState;
        setSwitchState();

        IntentFilter intentFilter = new IntentFilter(ACTION_THREE_KEY);
        context.registerReceiverAsUser(new BroadcastReceiver() {
            @Override
            public synchronized void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(ACTION_THREE_KEY)) {
                    mThreeKeyMode = intent.getIntExtra(ACTION_THREE_KEY_EXTRA, -1);
                    setSwitchState();
                }
            }
        }, UserHandle.ALL, intentFilter, null, null);
    }

    private void setSwitchState() {
        switch (mThreeKeyMode) {
            case SWITCH_STATE_UP:
                setUp();
                break;
            case SWITCH_STATE_MIDDLE:
                setMiddle();
                break;
            case SWITCH_STATE_DOWN:
                setDown();
                break;
            default:
                Slog.e(TAG, "Invalid switchState");
                return;
        }

        mVibrator.vibrate(100);
    }

    public boolean isUnlocked()  {
        return mThreeKeyMode == SWITCH_STATE_MIDDLE;
    }

    private void setUp() {
        android.util.Log.w(TAG, "State switch up");

        //Lock into silence
        int mode = mUpAllowAlarms ?
            Settings.Global.ZEN_MODE_ALARMS :
            Settings.Global.ZEN_MODE_NO_INTERRUPTIONS;

        setZenMode(mode);
    }

    private void setMiddle() {
        android.util.Log.w(TAG, "State switch middle");

        //Go to vibrate or ring depending on the setting
        if (mMiddleStartVibrate) {
            mAudioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
        } else {
            unmuteFromVibrate();
        }

        //Set to the default from settings, unlocked
        int mode = mMiddleStartPriority ?
            Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS :
            Settings.Global.ZEN_MODE_OFF;

        setZenMode(mode);

        //switch to automatic rules here
    }

    private void setDown() {
        android.util.Log.w(TAG, "State switch down");

        //Lock inro ring
        setZenMode(Settings.Global.ZEN_MODE_OFF);

        //Max out ring volume
        unmuteFromVibrate();
    }

    private void setZenMode(int mode) {
        mNotificationManager.setZenMode(mode, null, TAG);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ZEN_MODE, mode);
    }

    private void unmuteFromVibrate() {
        mAudioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0);
    }
}
