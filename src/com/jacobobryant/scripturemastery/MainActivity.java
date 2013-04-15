package com.jacobobryant.scripturemastery;

import com.orm.androrm.DatabaseAdapter;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;

import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.util.*;

public class MainActivity extends ListActivity {
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
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_options, menu);
        if (BuildConfig.DEBUG) {
            MenuItem mnuCrash = menu.findItem(R.id.mnu_crash);
            mnuCrash.setVisible(true);
        }
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
            case R.id.mnu_crash:
                return (1 / 0 == 0);
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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info =
            (AdapterView.AdapterContextMenuInfo) menuInfo;
        MenuInflater inflater = getMenuInflater();
        Book book = Book.object(getApplication(), info.position);

        if (!book.getPreloaded()) {
            inflater.inflate(R.menu.delete_menu, menu);
            menu.setHeaderTitle(book.getTitle());
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Context app = getApplication();
        AdapterView.AdapterContextMenuInfo info =
            (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Book book = Book.object(app, info.position);
        DatabaseAdapter adapter;

        switch (item.getItemId()) {
            case R.id.mnuDelete:
                adapter = DatabaseAdapter.getInstance(app);
                adapter.beginTransaction();
                for (Scripture scrip : book.getScriptures(app).all()) {
                    scrip.delete(app);
                }
                book.delete(app);
                adapter.commitTransaction();
                buildList();
                Toast.makeText(this, R.string.group_deleted,
                        Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onContextItemSelected(item);
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
