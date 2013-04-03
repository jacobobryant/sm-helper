package com.jacobobryant.scripturemastery;

import com.actionbarsherlock.app.SherlockListActivity;
import com.orm.androrm.DatabaseAdapter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.*;

public class ScriptureListActivity extends SherlockListActivity {
    public static final String EXTRA_BOOK_ID =
            "com.jacobobryant.scripturemastery.BOOK_ID";
    public static final String EXTRA_SCRIP_ID =
            "com.jacobobryant.scripturemastery.SCRIP_ID";
    public static final String EXTRA_IN_ROUTINE =
            "com.jacobobryant.scripturemastery.IN_ROUTINE";
    private static final int LEARN_SCRIPTURE_REQUEST = 0;
    private static final int LEARN_KEYWORD_REQUEST = 1;
    private int bookId;
    private int curScripId;
    
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        bookId = getIntent().getIntExtra(
                MainActivity.EXTRA_BOOK_ID, -1);
        if (bookId == -1) finish();
        buildList();
        try {
            curScripId = state.getInt(EXTRA_SCRIP_ID);
        } catch (NullPointerException e) {
            curScripId = -1;
        }
        registerForContextMenu(getListView());
    }

    @Override
    public void onListItemClick(ListView listView, View v,
            int index, long id) {
        Context app = getApplication();
        Book book = Book.objects(app).get(bookId);
        int scripId = book.getScripture(app, index).getId();
        Intent intent = new Intent(this, ScriptureActivity.class);
        intent.putExtra(EXTRA_SCRIP_ID, scripId);
        startActivityForResult(intent, LEARN_SCRIPTURE_REQUEST);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.scripture_list_activity_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.mnu_settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode) {
            case LEARN_KEYWORD_REQUEST:
                if (resultCode == RESULT_OK) {
                    Intent scriptureIntent =
                            new Intent(this, ScriptureActivity.class);
                    scriptureIntent.putExtra(EXTRA_SCRIP_ID, curScripId);
                    scriptureIntent.putExtra(EXTRA_IN_ROUTINE, true);
                    startActivityForResult(scriptureIntent,
                            LEARN_SCRIPTURE_REQUEST);
                }
                break;
            case LEARN_SCRIPTURE_REQUEST:
                switch (resultCode) {
                    case ScriptureActivity.RESULT_MASTERED:
                        moveForward(Scripture.MASTERED);
                        break;
                    case ScriptureActivity.RESULT_MEMORIZED:
                        moveForward(Scripture.MEMORIZED);
                        break;
                    case ScriptureActivity.RESULT_PARTIALLY_MEMORIZED:
                        moveForward(Scripture.PARTIALLY_MEMORIZED);
                        break;
                    default:
                        curScripId = -1;
                }
                break;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        state.putInt(EXTRA_SCRIP_ID, curScripId);
        super.onSaveInstanceState(state);
    }

    private void buildList() {
        final String NAME = "NAME";
        final String STATUS = "STATUS";
        List<Map<String, String>> data =
                new ArrayList<Map<String, String>>();
        Map<String, String> map;
        Context app = getApplication();

        for (Scripture scrip : Book.objects(app).get(bookId)
                .getScriptures(app).all()) {
            map = new HashMap<String, String>();
            map.put(NAME, scrip.getReference());
            map.put(STATUS, getStatusString(scrip.getStatus()));
            data.add(map);
        }
        setListAdapter(new SimpleAdapter(
                this,
                data,
                R.layout.scripture_list_item,
                new String[] {NAME, STATUS},
                new int[] {R.id.txtReference, R.id.txtStatus}));
    }

    private String getStatusString(int status) {
        switch (status) {
            case Scripture.NOT_STARTED:
                return "";
            case Scripture.IN_PROGRESS:
                return "in progress";
            case Scripture.FINISHED:
                return "finished";
            default:
                throw new IllegalArgumentException();
        }
    }

    private void moveForward(int status) {
        Context a = getApplication();
        Book book = Book.objects(a).get(bookId);
        Scripture curScripture = Scripture.objects(a).get(curScripId);
        DatabaseAdapter adapter = DatabaseAdapter.getInstance(a);

        adapter.beginTransaction();
        curScripture.setProgress(status);
        curScripture.save(a);
        book.moveToNext();
        book.save(a);
        adapter.commitTransaction();
        if (book.getRoutineLength() > 0) {
            curScripId = book.current(a).getId();
            startScripture();
        } else {
            curScripId = -1;
            // needed to update the status part
            buildList();
        }
    }

    private void startScripture() {
        Intent intent = new Intent();
        int count = 0;
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(this);
        boolean practiceKeywords =
                prefs.getBoolean(SettingsActivity.KEYWORDS, true);
        Context app = getApplication();
        Book book = Book.objects(app).get(bookId);

        intent.putExtra(EXTRA_SCRIP_ID, curScripId);
        intent.putExtra(EXTRA_IN_ROUTINE, true);
        if (practiceKeywords && book.hasKeywords(app)) {
            for (Scripture scrip : book.getScriptures(app).all()) {
                if (scrip.getStatus() != Scripture.NOT_STARTED &&
                        ++count > 1) {
                    intent.setClass(this, KeywordActivity.class);
                    startActivityForResult(intent,
                            LEARN_KEYWORD_REQUEST);
                    return;
                }
            }
        }
        intent.setClass(this, ScriptureActivity.class);
        startActivityForResult(intent, LEARN_SCRIPTURE_REQUEST);
    }
}
