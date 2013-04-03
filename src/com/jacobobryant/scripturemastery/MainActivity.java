package com.jacobobryant.scripturemastery;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.util.*;

public class MainActivity extends SherlockListActivity {
    public static final String EXTRA_BOOK_ID =
            "com.jacobobryant.scripturemastery.BOOK_ID";
    private static final int NEW_PASSAGE_REQUEST = 0;
    
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        Log.d(SMApp.TAG, "Entering SM Helper");
        SyncDB.syncDB(getApplication());
        buildList();
    }

    @Override
    public void onListItemClick(ListView parent, View v,
            int position, long id) {
        int bookId = Book.object(getApplication(), position).getId();
        Intent intent = new Intent(this, ScriptureListActivity.class);

        intent.putExtra(EXTRA_BOOK_ID, bookId);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.main_activity_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.mnuNewPassage:
                intent = new Intent(this, NewPassageActivity.class);
                startActivityForResult(intent, NEW_PASSAGE_REQUEST);
                return true;
            case R.id.mnu_settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // todo: refactor so that buildList doesn't have to be called.
    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == NEW_PASSAGE_REQUEST &&
                resultCode == RESULT_OK) {
            buildList();
            Toast.makeText(this, getString(R.string.passageAdded),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void buildList() {
        final String NAME = "NAME";
        List<Map<String, String>> bookData =
                new ArrayList<Map<String, String>>();
        Map<String, String> map;

        for (Book book : Book.objects(getApplication()).all()) {
            map = new HashMap<String, String>();
            map.put(NAME, book.getTitle());
            bookData.add(map);
        }
        setListAdapter(new SimpleAdapter(
                this,
                bookData,
                android.R.layout.simple_list_item_1,
                new String[] {NAME},
                new int[] {android.R.id.text1}));
    }
}
