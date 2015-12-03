package com.alanszlosek.whenmoving;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.util.Log;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by alanszlosek on 12/13/13.
 */
public class MainService  extends Service implements SensorEventListener, LocationListener {
    // NOTIFICATION RELATED

    protected static final int NOTIFICATION_ID = 8383939;
    protected long fSince = 0;
    protected Notification mNotification;
    protected Notification.Builder mNotificationBuilder;

    // INTERNAL STATE
    private boolean movingState = false;

    // Accelerometer-related
    private int iAccelReadings, iAccelSignificantReadings;
    private long iAccelTimestamp;
    private SensorManager mSensorManager;

    // GPS-related
    public static Location currentBestLocation;
    private long lGPSTimestamp;
    private LocationManager mLocationManager = null;
    private PendingIntent pi;
    private boolean cellOnly = false;
    private boolean gotNetwork = false;

    // ACCELEROMETER METHODS
    public void startAccelerometer() {
        iAccelReadings = 0;
        iAccelSignificantReadings = 0;
        iAccelTimestamp = System.currentTimeMillis();
        // should probably store handles to these earlier, when service is created
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    public void stopAccelerometer() {
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        double accel, x, y, z;
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }

        iAccelReadings++;
        x = event.values[0];
        y = event.values[1];
        z = event.values[2];

        accel = Math.abs(
                Math.sqrt(
                        Math.pow(x,2)
                                +
                                Math.pow(y,2)
                                +
                                Math.pow(z,2)
                )
        );
        // Was 0.6. Lowered to 0.3 (plus gravity) to account for smooth motion from Portland Streetcar
        if (
                accel > (9.8 + MainApplication.prefThreshold)
                ||
                accel < (9.8 - MainApplication.prefThreshold)
        ) {
            iAccelSignificantReadings++;
        }

        //Debug(String.format("event: %f %f %f %f %f", x, y, z, accel, 0.600));

        // Get readings for 1 second
        // Maybe we should sample for longer given that I've lowered the threshold
        if ( (System.currentTimeMillis() - iAccelTimestamp) < 2000) return;

        stopAccelerometer();

        Debug(String.format("Accelerometer readings: %d Significant: %d", iAccelReadings, iAccelSignificantReadings));

        // Appeared to be movingState 30% of the time?
        // If the bar is this low, why not report motion at the first significant reading and be done with it?
        if (((1.0*iAccelSignificantReadings) / iAccelReadings) > 0.30) {
            setMovingState(true);
            Debug("Moving");

            // Get new lock for GPS so we can turn off screen
            MainApplication.wakeLock2(true);
            MainApplication.wakeLock1(false);

            // Start GPS
            startGPS();

        } else {
            setMovingState(false);
            Debug("Stationary");
            sleep();
            MainApplication.wakeLock1(false);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // can be safely ignored
    }


    // GPS METHODS
    public void startGPS() {
        // Set timeout for 30 seconds
        AlarmManager mgr = null;
        Intent i = null;
        GregorianCalendar cal = null;
        int iProviders = 0;
        currentBestLocation = null;
        gotNetwork = false;

        // Make sure at least one provider is available
        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, this);
            iProviders++;
        }
        if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500, 0, this);
            iProviders++;
            if (iProviders == 1) {
                cellOnly = true;
            }
        }

        if (iProviders == 0) {
            // Should probably trigger a toast, so the user knows
            Debug("No providers available");
            sleep();
            MainApplication.wakeLock2(false);
            return;
        }

        lGPSTimestamp = System.currentTimeMillis();
        mgr = (AlarmManager)getSystemService(ALARM_SERVICE);
        cal = new GregorianCalendar();
        i = new Intent(this, TimeoutReceiver.class);
        this.pi = PendingIntent.getBroadcast(this, 0, i, 0);
        cal.add(Calendar.SECOND, MainApplication.prefTimeout);
        mgr.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), this.pi);
    }

    @Override
    public void onLocationChanged(Location location) {
        Debug(String.format(
                "lat: %f lon: %f acc: %f provider: %s",
                location.getLatitude(),
                location.getLongitude(),
                location.getAccuracy(),
                location.getProvider()
        ));

        /*
        So always store network/cell location, but only the first since accuracy will be quite low
        */
        if (location.getProvider().equals("network") && gotNetwork == false) {
            saveLocation(location);
            gotNetwork = true;
            // Don't return here, because if GPS is unavailable, we'll fall-through and stop the polling
        }

        if (cellOnly) {
            // fall through to stop GPS

        } else {
            // If we are expecting GPS updates, and we just got one ...
            if (location.getProvider().equals("gps")) {
                // Let's make this simple ... keep the location with the greatest accuracy
                if (currentBestLocation == null || location.getAccuracy() < currentBestLocation.getAccuracy()) {
                    currentBestLocation = location;
                }


                // What's our accuracy cutoff?

                // Keep polling if our accuracy is worse than 1 meter
		        // This should be configurable
                if (location.getAccuracy() > 1) {
                    return;
                }
                // Stop GPS if we're getting much worse accuracy?

            } else {
                // We just got a network location
                return;
            }
        }
        stopGPS();

        // THINGS I'D LIKE TO LOG
        // Compared to current millis, how old is this location?
        // How long does it take to get location


        // Don't like this being hardcoded here ... need a better scheme for handling this
        // Would rather wait a minute between GPS attempts
        sleep();
        MainApplication.wakeLock2(false);
    }

    protected void saveLocation(Location l) {
        ContentValues data;
        SQLiteOpenHelper dbHelper;
        SQLiteDatabase db;
        GregorianCalendar cal;
        if (l == null) {
            return;
        }

        dbHelper = new DatabaseHelper(MainService.this);
        db = dbHelper.getWritableDatabase();

        cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"), Locale.US);
        cal.setTimeInMillis( l.getTime() );

        data = new ContentValues();
        data.put("day", DateFormat.format("yyyyMMdd", cal).toString() );
        data.put("hour", DateFormat.format("hh", cal).toString() );
        data.put("milliseconds", l.getTime());
        data.put("longitude", l.getLongitude());
        data.put("latitude", l.getLatitude());
        data.put("altitude", l.getAltitude());
        data.put("gpsStart", lGPSTimestamp);
        data.put("accuracy", l.getAccuracy());
        data.put("bearing", l.getBearing());
        data.put("speed", l.getSpeed());
        db.insert(l.getProvider() + "_locations", null, data);
        db.close();
        dbHelper.close();
    }

    public void stopGPS() {
        saveLocation(currentBestLocation);
        lGPSTimestamp = 0;
        if (this.pi != null) {
            AlarmManager mgr = (AlarmManager)getSystemService(ALARM_SERVICE);
            mgr.cancel(this.pi);
            this.pi = null;
        }
        mLocationManager.removeUpdates(this);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Debug(String.format("onStatusChanged: %s status: %d", provider, status));
    }

    @Override
    public void onProviderEnabled(String provider) {
        Debug(String.format("onProviderEnabled: %s", provider));
        if (lGPSTimestamp == 0) {
            // Not currently interested
            return;
        }
        // If it's a provider we care about, and we're listening, listen!
        if (provider == LocationManager.GPS_PROVIDER || provider == LocationManager.NETWORK_PROVIDER) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, this);
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        Debug(String.format("onProviderDisabled: %s", provider));

        if (lGPSTimestamp == 0) {
            // Not currently interested
            return;
        }
        // If it's a provider we care about, and we're listening, listen!
        if (provider == LocationManager.GPS_PROVIDER && mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500, 0, this);
        } else if (provider == LocationManager.NETWORK_PROVIDER && mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, this);
        }
        // Need to update the cellOnly flag
        // Think we can simplify the above logic by simply checking if each provider exists anymore,
        // and requesting updates for those that do
    }


    // OTHER
    public void sleep() {
        // Check desired state
        if (MainApplication.trackingOn == false) {
            Debug("Tracking has been toggled off. Not scheduling any more wakeup alarms");
            stopSelf();
            // Tracking has been turned off, don't schedule any new alarms
            return;
        }
        AlarmManager mgr = (AlarmManager)getSystemService(ALARM_SERVICE);
        Intent i = new Intent(this, MainReceiver.class);
        Calendar cal = new GregorianCalendar();

        Debug(String.format("Waiting %d seconds", MainApplication.prefInterval));
        cal.add(Calendar.SECOND, MainApplication.prefInterval);

        this.pi = PendingIntent.getBroadcast(this, 0, i, 0);
        mgr.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), this.pi);
    }

    public void gpsTimeout() {
        Debug("GPS timeout");
        stopGPS();
        sleep();
        MainApplication.wakeLock2(false);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        MainApplication.mServiceInstance = this;
        Debug("Service.onCreate");

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        setMovingState(false);
        startForeground(NOTIFICATION_ID, mNotification);

		/*
		//HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
		HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
		thread.setDaemon(true);
		thread.start();

		// Get the HandlerThread's Looper and use it for our Handler
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);
		*/
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Debug("Service.onStartCommand");
        if (MainApplication.trackingOn == false) {

            Debug("Tracking has been toggled off. Not scheduling any more wakeup alarms");
            stopSelf();
            MainApplication.wakeLock1(false);
            // Tracking has been turned off, don't schedule any new alarms
            return START_NOT_STICKY;

        }
        startAccelerometer();
        return START_REDELIVER_INTENT;
        //return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Debug("Service.onDestroy");
        MainApplication.mServiceInstance = null;
    }


    public static void Debug(String message) {
        Log.d("WhenMoving.Service", message);
    }

    protected void setMovingState(boolean newMovingState) {
        String s;
        NotificationManager nm;
        PendingIntent mPendingIntent;

        if (movingState != newMovingState) {
            // State has changed. Log the time.
            fSince = System.currentTimeMillis();
        }

        if (newMovingState == true) {
            if (currentBestLocation != null) {
                s = String.format("At " + currentBestLocation.getSpeed() + " m/s");
            } else {
                s = String.format("Awaiting speed reading");
            }
        } else {
            if (currentBestLocation != null) {
                s = String.format("Previous speed: " + currentBestLocation.getSpeed() + " m/s");
            } else {
                s = String.format(". . .");
            }
        }

        // Could detect long periods of motion here and give a badge or say something funny

        if (mNotificationBuilder == null) {
            // The PendingIntent to launch our activity if the user selects this notification
            mPendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    new Intent(this, MainActivity.class),
                    0
            );
            mNotificationBuilder = new Notification.Builder(this).setContentIntent(mPendingIntent);
        }
        mNotificationBuilder.setContentTitle( (newMovingState ? "Moving" : "Stationary") )
            .setContentText(s)
            .setSmallIcon( (newMovingState ? R.drawable.moving : R.drawable.stationary) );
        mNotificationBuilder.setWhen(fSince);
        mNotification = mNotificationBuilder.build();

        nm = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, mNotification);

        movingState = newMovingState;
    }

}
