package com.alanszlosek.whenmoving;


import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.util.Log;


public class DaysActivity extends ListActivity implements AbsListView.OnScrollListener {

    protected int loading = 0;
    protected int lastPosition = 0;
    SQLiteOpenHelper dbHelper;
    SQLiteDatabase db;
    ListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        // We'll define a custom screen layout here (the one shown above), but
        // typically, you could just use the standard ListActivity layout.
        //setContentView(R.layout.days_list_item);

        dbHelper = new DatabaseHelper(this);
        db = dbHelper.getReadableDatabase();
        // Get each day
        Cursor c = db.rawQuery("select day as _id, count(milliseconds) as points from gps_locations group by day order by day desc", null);
        startManagingCursor(c);

        // Now create a new list adapter bound to the cursor.
        // SimpleListAdapter is designed for binding to a Cursor.
        adapter = new SimpleCursorAdapter(
                this,
                R.layout.days_list_item,
                c,
                // Array of cursor columns to bind to.
                new String[] {
                        "_id",
                        "points"
                },
                // corresponding fields in template
                new int[] {R.id.text1, R.id.text2}
        );
        setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick (ListView l, View v, int position, long id) {
        Cursor c;
        Intent intent = new Intent();
        intent.putExtra("id", id);
        setResult(ListActivity.RESULT_OK, intent);
        finish();
    }

    // Scroll callbacks
    public void onScroll(AbsListView v, int firstVisible, int visibleCount, int total) {
        // If list isn't empty, and we've scrolled to bottom
        if (total > 0 && loading == 0 && (firstVisible + visibleCount) >= total) {
            loading = 2;
            lastPosition = firstVisible;
            // Get next day
        }
    }
    public void onScrollStateChanged(AbsListView v, int scrollState) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null) db.close();
    }
}
