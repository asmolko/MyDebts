package com.dao.mydebts;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import static android.provider.ContactsContract.Contacts;
import static android.provider.ContactsContract.Data;

public class GroupsListActivity extends AppCompatActivity {

    @SuppressLint("InlinedApi")
    private static final String[] PROJECTION = {Data._ID, Data.DISPLAY_NAME_PRIMARY,
            Data.LOOKUP_KEY, Contacts.PHOTO_THUMBNAIL_URI};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups_list);
        final ListView list = (ListView) findViewById(R.id.list);

        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(Contacts.CONTENT_URI, PROJECTION, null, null, null);

        list.setAdapter(new SimpleCursorAdapter(this, R.layout.list_item, cursor,
                new String[] {Contacts.DISPLAY_NAME, Contacts.PHOTO_THUMBNAIL_URI},
                new int[]{R.id.item_text, R.id.quickcontact},
                SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_groups_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
