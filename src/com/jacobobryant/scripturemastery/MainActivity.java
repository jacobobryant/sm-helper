package com.jacobobryant.scripturemastery;

import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import android.util.Log;

import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Toast;

import java.util.*;

public class MainActivity extends ListActivity {
    public static final String TAG = "scripturemastery";
    public static final String EXTRA_BOOK_ID =
            "com.jacobobryant.scripturemastery.BOOK_ID";
    public static final String EXTRA_SCRIP_ID =
            "com.jacobobryant.scripturemastery.SCRIP_ID";
    public static final String EXTRA_IN_ROUTINE =
            "com.jacobobryant.scripturemastery.IN_ROUTINE";
    private static final String ROUTINE_REF = "in_routine";
    private static final int LEARN_SCRIPTURE_REQUEST = 0;
    private static final int NEW_PASSAGE_REQUEST = 1;
    private static final int LEARN_KEYWORD_REQUEST = 2;
    private Scripture curScripture;
    private boolean inRoutine;
    
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        int scripId;

        Log.d(TAG, "Entering SM Helper");
        SyncDB.syncDB(getApplication());
        buildList();
        try {
            scripId = state.getInt(EXTRA_SCRIP_ID);
            curScripture = Scripture.objects(getApplication())
                    .get(scripId);
        } catch (NullPointerException e) {
            curScripture = null;
        }
        try {
            inRoutine = state.getBoolean(ROUTINE_REF);
        } catch (NullPointerException e) {
            inRoutine = false;
        }
        registerForContextMenu(getListView());
    }

    @Override
    public boolean onChildClick(ListView parent, View v,
            int position, long id) {
        Book book = Book.objects(getApplication()).limit(position, 1)
                .toList().get(0);
        Intent intent = new Intent(this, ScriptureListActivity.class);

        intent.putExtra(EXTRA_BOOK_ID, book.getId());
        startActivity(intent);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) menuInfo;
        MenuInflater inflater = getMenuInflater();
        int position = ExpandableListView
                .getPackedPositionGroup(info.packedPosition);
        Book book = Book.objects(getApplication()).all()
            .toList().get(groupPos);

        inflater.inflate(R.menu.book_menu, menu);
        menu.setHeaderTitle(book.getTitle());
        if (book.getRoutineLength() != 0) {
            menu.findItem(R.id.mnuContinueRoutine).setVisible(true);
        }
        if (!book.getPreloaded()) {
            menu.findItem(R.id.mnuDeleteGroup).setVisible(true);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ExpandableListContextMenuInfo info =
                (ExpandableListContextMenuInfo) item.getMenuInfo();
        int groupPos = ExpandableListView
                .getPackedPositionGroup(info.packedPosition);
        int childPos = ExpandableListView
                .getPackedPositionChild(info.packedPosition);
        Book book = Book.objects(getApplicationContext()).all().toList()
                .get(groupPos);

        switch (item.getItemId()) {
            case R.id.mnuStartRoutine:
                book.createRoutine(getApplication());
                book.save(getApplication());
            case R.id.mnuContinueRoutine:
                inRoutine = true;
                curScripture = book.current(getApplication());
                startScripture();
                return true;
            case R.id.mnuDeleteGroup:
                book.delete(getApplicationContext());
                buildExpandableList();
                toast(R.string.groupDeleted, true);
                return true;
            case R.id.mnuDeletePassage:
                book.getScripture(getApplicationContext(), childPos)
                        .delete(getApplicationContext());
                buildExpandableList();
                toast(R.string.passageDeleted, true);
                return true;
            default:
                return super.onContextItemSelected(item);
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
                    scriptureIntent.putExtra(EXTRA_SCRIP_ID,
                            curScripture.getId());
                    scriptureIntent.putExtra(EXTRA_IN_ROUTINE, true);
                    startActivityForResult(scriptureIntent,
                            LEARN_SCRIPTURE_REQUEST);
                }
                break;
            case LEARN_SCRIPTURE_REQUEST:
                switch (resultCode) {
                    case ScriptureActivity.RESULT_MASTERED:
                        curScripture.setProgress(Scripture.MASTERED);
                        commit();
                        break;
                    case ScriptureActivity.RESULT_MEMORIZED:
                        curScripture.setProgress(Scripture.MEMORIZED);
                        commit();
                        break;
                    case ScriptureActivity.RESULT_PARTIALLY_MEMORIZED:
                        curScripture.setProgress(
                                Scripture.PARTIALLY_MEMORIZED);
                        commit();
                        break;
                    default:
                        inRoutine = false;
                        curScripture = null;
                }
                break;
            case NEW_PASSAGE_REQUEST:
                switch (resultCode) {
                    case RESULT_OK:
                        buildExpandableList();
                        toast(R.string.passageAdded, true);
                }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        if (curScripture != null) {
            state.putInt(EXTRA_SCRIP_ID, curScripture.getId());
        }
        state.putBoolean(ROUTINE_REF, inRoutine);
        super.onSaveInstanceState(state);
    }

    private void buildExpandableList() {
        final String NAME = "NAME";
        final String STATUS = "STATUS";
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

    private void commit() {
        Book book;
        Context a = getApplication();

        curScripture.save(a);
        if (inRoutine) {
            book = curScripture.getBook(a);
            book.moveToNext();
            book.save(a);
            if (book.getRoutineLength() > 0) {
                curScripture = book.current(a);
                startScripture();
            } else {
                buildExpandableList();
            }
        } else {
            curScripture = null;
            buildExpandableList();
        }
    }

    private void startScripture() {
        Intent intent = new Intent();
        int count = 0;
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(this);
        boolean practiceKeywords =
                prefs.getBoolean(SettingsActivity.KEYWORDS, true);
        Book book = curScripture.getBook(getApplication());

        intent.putExtra(EXTRA_SCRIP_ID, curScripture.getId());
        intent.putExtra(EXTRA_IN_ROUTINE, true);
        if (practiceKeywords && book.hasKeywords(getApplication())) {
            for (Scripture scrip : book.getScriptures(
                    getApplication()).all()) {
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

    private void toast(int id, boolean lengthShort) {
        int length = (lengthShort) ? Toast.LENGTH_SHORT
                : Toast.LENGTH_LONG;
        Toast.makeText(this, getString(id), length).show();
    }
}
