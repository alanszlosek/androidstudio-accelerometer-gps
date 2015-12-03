package com.alanszlosek.whenmoving;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends Activity {

    public void Debug(String message) {
        Log.d("WhenMoving", message);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Switch toggle = (Switch) findViewById(R.id.onoff_switch);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences sp = MainApplication.getSharedPreferences();
                SharedPreferences.Editor editor = sp.edit();
                if (isChecked) {
                    editor.putBoolean("pref_onoff", true);
                } else {
                    editor.putBoolean("pref_onoff", false);
                }
                editor.commit();
                MainApplication.onPreferenceChange();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        Debug("Activity started");
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences sp = MainApplication.getSharedPreferences();
        Switch toggle = (Switch) findViewById(R.id.onoff_switch);
        toggle.setChecked( sp.getBoolean("pref_onoff", false) );

        MainApplication.onPreferenceChange();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // show latest according to intent
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                long id = data.getLongExtra("id", 0);
                //showMarkers( String.format("day = %d", id));
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        /*
        if (myLocation != null) {
            myLocation.disableMyLocation();
            myLocation = null;
        }
        */
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void startGPSing() {
        MainApplication.trackingOn = true;

        MainApplication.getInstance().startup();
    }

    protected void showLatest() {
        GregorianCalendar cal;
        String q, dateStart, dateEnd;
        long millisStart, millisEnd;

        cal = new GregorianCalendar(); // local
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        millisStart = cal.getTimeInMillis();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        millisEnd = cal.getTimeInMillis();

        // Now get year, month and day for start and stop in GMT
        cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"), Locale.US); // GMT,
        cal.setTimeInMillis(millisStart);
        dateStart = DateFormat.format("yyyyMMdd", cal).toString();
        cal.setTimeInMillis(millisEnd);
        dateEnd = DateFormat.format("yyyyMMdd", cal).toString();


        q = String.format(
                "day between %s and %s and milliseconds between %d and %d and provider='gps' and accuracy < 40.00",
                dateStart,
                dateEnd,
                millisStart,
                millisEnd
        );
        //showMarkers(q);
    }

    /*
    protected void showMarkers(String where) {
        SQLiteOpenHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        // Clear layer/markers
        List<Overlay> myOverlays = myMapView.getOverlays();


        // Get 10 newest locations, all from GPS, and with better than 40 meters accuracy
        Cursor c = db.query(
                "locations",
                null,
                where,
                null,
                null,
                null,
                "milliseconds DESC",
                "1000"
        );
        int iMilliseconds = c.getColumnIndex("milliseconds");
        int iLongitude = c.getColumnIndex("longitude");
        int iLatitude = c.getColumnIndex("latitude");

        GeoPoint gpFirst = null;
        GeoPoint gpPrevious = null;

		//myLocation = new MyLocationOverlay(this, myMapView);
		//myLocation.enableMyLocation();

        MyOverlay.projection = myMapView.getProjection();
        myOverlays.clear();

        while (c.moveToNext()) {
            Double lo, la;
            la = new Double(c.getFloat(iLatitude) * 1E6);
            lo = new Double(c.getFloat(iLongitude) * 1E6);
            GeoPoint gp = new GeoPoint(
                    la.intValue(),
                    lo.intValue()
            );
            if (gpFirst == null) gpFirst = gp;
            Date d = new Date( c.getLong(iMilliseconds) );

            myOverlays.add( new MyOverlay(gpPrevious, gp) );
            gpPrevious = gp;
        }
        c.close();
        dbHelper.close();

        if (gpFirst != null) myMapView.getController().setCenter(gpFirst);
		// Redraw these GeoPoints on the map, with path lines, and pretty colors
		//mapController.setZoom(21);
    }
    */


    public void onSettingsButton(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
    public void onCalibrateButton(View view) {
        //Intent intent = new Intent(this, CalibrationActivity.class);
        //startActivity(intent);
    }

}
