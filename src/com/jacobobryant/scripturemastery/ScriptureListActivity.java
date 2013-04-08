package com.jacobobryant.scripturemastery;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.orm.androrm.Filter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
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
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
        Log.d(SMApp.TAG, "index = " + index);
        curScripId = Scripture.objects(getApplication()).all().filter(
            new Filter().is("book__mId", bookId)).limit(index, 1)
            .toList().get(0).getId();
        Intent intent = new Intent(this, ScriptureActivity.class);
        intent.putExtra(EXTRA_SCRIP_ID, curScripId);
        startActivityForResult(intent, LEARN_SCRIPTURE_REQUEST);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.scripture_list_activity_options, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Book book = Book.objects(getApplication()).get(bookId);
        if (book.getRoutineLength() != 0) {
            menu.findItem(R.id.mnuContinueRoutine).setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Book book = Book.objects(getApplication()).get(bookId);
        Intent intent;

        switch (item.getItemId()) {
            case android.R.id.home:
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            case R.id.mnuStartRoutine:
                book.createRoutine(getApplication());
                book.save(getApplication());
            case R.id.mnuContinueRoutine:
                curScripId = book.current(getApplication()).getId();
                startScripture();
                return true;
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
                        commit(Scripture.MASTERED);
                        moveForward();
                        break;
                    case ScriptureActivity.RESULT_MEMORIZED:
                        commit(Scripture.MEMORIZED);
                        moveForward();
                        break;
                    case ScriptureActivity.RESULT_PARTIALLY_MEMORIZED:
                        commit(Scripture.PARTIALLY_MEMORIZED);
                        moveForward();
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

        for (Scripture scrip : Scripture.objects(app).filter(
                new Filter().is("book__mId", bookId))) {
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

    private void commit(int status) {
        Context a = getApplication();
        Scripture curScripture = Scripture.objects(a).get(curScripId);
        curScripture.setProgress(status);
        curScripture.save(a);
    }

    private void moveForward() {
        Context a = getApplication();
        Book book = Book.objects(a).get(bookId);
        if (book.getRoutineLength() > 0) {
            book.moveToNext();
            book.save(a);
            if (book.getRoutineLength() > 0) {
                curScripId = book.current(a).getId();
                startScripture();
            } else {
                curScripId = -1;
                // needed to update the status part
                buildList();
            }
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
