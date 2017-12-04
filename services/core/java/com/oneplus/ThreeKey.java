package com.oneplus;

import android.app.NotificationManager;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.os.UserHandle;
import android.provider.Settings;

import com.threekey.Actions;

public class ThreeKey extends ContentObserver {
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

    private int mThreeKeyMode;

    private boolean mAcceptZenChange = false;
    private boolean mUserChange = false;

    public ThreeKey(Context context, int switchState) {
        super(new Handler());

        mContext = context;
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        setKeyMode(switchState);

        context.getContentResolver().registerContentObserver(Settings.Global.getUriFor(Settings.Global.ZEN_MODE), false, this);
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor(Settings.System.THREEKEY_UP_ALLOW_ALARMS), false, new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);
                if (isMuted()) {
                    setUp();
                }
            }
        }, UserHandle.USER_ALL);

        context.registerReceiverAsUser(new BroadcastReceiver() {
            //3Key has moved
            @Override
            public void onReceive(Context context, Intent intent) {
                mUserChange = true;
                setKeyMode(intent.getIntExtra(ACTION_THREE_KEY_EXTRA, -1));
            }
        }, UserHandle.ALL, new IntentFilter(ACTION_THREE_KEY), null, null);
    }

    public boolean isMuted() {
        return mThreeKeyMode == SWITCH_STATE_UP;
    }

    public boolean isUnlocked()  {
        return mThreeKeyMode == SWITCH_STATE_MIDDLE;
    }

    public boolean isLoud() {
        return mThreeKeyMode == SWITCH_STATE_DOWN;
    }

    private void setKeyMode(int threeKeyMode) {
        mThreeKeyMode = threeKeyMode;
        if (isMuted()) {
            setUp();
        } else if (isUnlocked()) {
            setMiddle();
        } else if (isLoud()) {
            setBottom();
        }
    }

    private void setUp() {
        boolean upAllowAlarms = 0 != Settings.System.getIntForUser(
            mContext.getContentResolver(),
            Settings.System.THREEKEY_UP_ALLOW_ALARMS,
            0,
            UserHandle.USER_CURRENT);

        setZenMode(upAllowAlarms ?
            Settings.Global.ZEN_MODE_ALARMS :
            Settings.Global.ZEN_MODE_NO_INTERRUPTIONS);
    }

    private void setMiddle() {
        boolean middleStartPriority = 0 != Settings.System.getIntForUser(
            mContext.getContentResolver(),
            Settings.System.THREEKEY_MIDDLE_START_PRIORITY,
            0,
            UserHandle.USER_CURRENT);

        setZenMode(middleStartPriority ?
            Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS :
            Settings.Global.ZEN_MODE_OFF);

        //Switch to automatic rules here
    }

    private void setBottom() {
        setZenMode(Settings.Global.ZEN_MODE_OFF);
    }

    private void setZenMode(int mode) {
        if (Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.ZEN_MODE, -1) == mode) {
            setRingMode();
        } else {
            mAcceptZenChange = true;        
            mNotificationManager.setZenMode(mode, null, TAG);
            Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ZEN_MODE, mode);
        }
    }

    //On zen changed
    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
        if (mAcceptZenChange) {
            mAcceptZenChange = false;
            setRingMode(); //Use as async callback
        } else if (isMuted()) {
            setUp();
        } else if (isLoud()) {
            setBottom();
        }
    }

    //Always gets called at the end of a zen change
    private void setRingMode() {
        if (mUserChange) { //Only do this when the 3Key has moved
            mUserChange = false;

            if (isUnlocked()) {
                boolean mMiddleRing = 0 == Settings.System.getIntForUser(
                    mContext.getContentResolver(),
                    Settings.System.THREEKEY_MIDDLE_START_VIBRATE,
                    0,
                    UserHandle.USER_CURRENT);

                if (mMiddleRing) {
                    mAudioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0);
                } else {
                    mAudioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
                }
            } else if (isLoud()) {
                mAudioManager.setStreamVolume(AudioManager.STREAM_RING, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING), 0);
            }

            mVibrator.vibrate(VibrationEffect.createOneShot(50, 255));
        }
    }
}
