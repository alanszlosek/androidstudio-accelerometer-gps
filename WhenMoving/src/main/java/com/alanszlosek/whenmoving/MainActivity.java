package com.alanszlosek.whenmoving;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    public void Debug(String message) {
        Log.d("WhenMoving", message);
    }

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    protected void onStart() {
        super.onStart();

        Debug("Activity started");
    }

    @Override
    protected void onResume() {
        super.onResume();

        MainApplication.onPreferenceChange();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.when_moving, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Intent intent;
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                showLatest();
                return true;
            case R.id.menu_settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.show_days:
                intent = new Intent(this, DaysActivity.class);
                startActivityForResult(intent, 1);
                return true;
            case R.id.menu_calibrate:
                intent = new Intent(this, CalibrationActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
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

}
