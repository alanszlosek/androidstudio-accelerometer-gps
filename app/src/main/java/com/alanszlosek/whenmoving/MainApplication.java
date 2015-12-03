package com.alanszlosek.whenmoving;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by alanszlosek on 12/13/13.
 */
public class MainApplication extends Application {
    protected static MainApplication mInstance;
    public static MainService mServiceInstance;
    protected static SharedPreferences sharedPreferences;

    // Status and Preferences
    public static boolean trackingOn = false; // not running
    public static int prefInterval = 0;
    public static int prefTimeout = 0;
    public static double prefThreshold = 0.30;

    // Wake Locks
    protected static PowerManager mPowerManager;
    protected static PowerManager.WakeLock mWakeLock1;
    protected static PowerManager.WakeLock mWakeLock2;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        mServiceInstance = null;
        mPowerManager = (PowerManager)getApplicationContext().getSystemService(Context.POWER_SERVICE);
        // Save a handle to our SharedPreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Reset tracking state when application/activity is created
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("pref_onoff", false);
        editor.commit();
    }
    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    public static void Debug(String message) {
        Log.d("MainActivity", message);
    }

    public static MainApplication getInstance() {
        return mInstance;
    }

    public static SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public void startup() {
        wakeLock1(true);
        Debug("startup");
        Intent i = new Intent(this, MainService.class);
        startService(i);
    }

    // Accelerometer Wake Lock
    public static void wakeLock1(boolean up) {
        int locks;
        if (up) {
            // Certain phones will need a PowerManager.SCREEN_DIM_WAKE_LOCK wake lock
            // instead of the PARTIAL_WAKE_LOCK
            // My LG Optimus V was one, but my newer phone doesn't seem to need it.
            locks = PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP;
            mWakeLock1 = mPowerManager.newWakeLock(
                //
                locks,
                "WakeLock:Accelerometer"
            );
            mWakeLock1.acquire();
        } else {
            if (mWakeLock1 != null) {
                if (mWakeLock1.isHeld()) {
                    mWakeLock1.release();
                }
                mWakeLock1 = null;
            }
        }
    }
    // GPS Wake Lock
    public static void wakeLock2(boolean up) {
        if (up) {
            mWakeLock2 = mPowerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "WakeLock:GPS"
            );
            mWakeLock2.acquire();
        } else {
            if (mWakeLock2 != null) {
                if (mWakeLock2.isHeld()) {
                    mWakeLock2.release();
                }
                mWakeLock2 = null;
            }
        }
    }

    public static void onPreferenceChange() {
        //  Get latest settings, and update accordingly
        boolean newState = sharedPreferences.getBoolean("pref_onoff", false); // false is off/not-running

        prefInterval = Integer.parseInt( sharedPreferences.getString("pref_interval", "60") );
        prefTimeout = Integer.parseInt( sharedPreferences.getString("pref_timeout", "30") );
        prefThreshold = Double.parseDouble( sharedPreferences.getString("pref_threshold", "0.30") );

        // If we turned off the service, handle that change
        Debug(String.format("New state: %s", (newState == true ? "on" : "off")));
        if (MainApplication.trackingOn == true) {
            if (newState == false) {
                MainApplication.trackingOn = false;
                // Graceful shutdown in progress
                Toast.makeText(getInstance(), String.format("Gracefully stopping in %ds", MainApplication.prefInterval), Toast.LENGTH_SHORT).show();
            }
        } else {
            if (newState == true) {
                // Schedule an alarm
                MainApplication.trackingOn = true;
                MainApplication.getInstance().startup();
            }
        }
    }
}
