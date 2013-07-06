package com.jacobobryant.scripturemastery;

import com.orm.androrm.Filter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.util.*;

public class ScriptureListActivity extends ListActivity {
    public static final String EXTRA_BOOK_ID =
            "com.jacobobryant.scripturemastery.BOOK_ID";
    public static final String EXTRA_SCRIP_ID =
            "com.jacobobryant.scripturemastery.SCRIP_ID";
    public static final String EXTRA_IN_ROUTINE =
            "com.jacobobryant.scripturemastery.IN_ROUTINE";
    private static final int LEARN_SCRIPTURE_REQUEST = 0;
    private static final int LEARN_KEYWORD_REQUEST = 1;
    private final int DELETE_DIALOG = 0;
    private int bookId;
    private int curScripId;
    private Scripture deleteScrip;
    private boolean inRoutine;
    
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        Log.d(SMApp.TAG, "ScriptureListActivity.onCreate()");
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        bookId = getIntent().getIntExtra(
                MainActivity.EXTRA_BOOK_ID, -1);
        if (bookId == -1) finish();
        setTitle(Book.objects(getApplication()).get(bookId).getTitle());
        buildList();
        try {
            curScripId = state.getInt(EXTRA_SCRIP_ID);
        } catch (NullPointerException e) {
            curScripId = -1;
        }
        try {
            inRoutine = state.getBoolean(EXTRA_IN_ROUTINE);
        } catch (NullPointerException e) {
            inRoutine = false;
        }
        registerForContextMenu(getListView());
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(SMApp.TAG, "ScriptureListActivity.onResume()");
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
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.scripture_list_activity_options, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Book book = Book.objects(getApplication()).get(bookId);
        menu.findItem(R.id.mnuContinueRoutine)
            .setVisible(book.getRoutineLength() != 0);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Book book = Book.objects(getApplication()).get(bookId);
        Intent intent;

        switch (item.getItemId()) {
            /*
            case android.R.id.home:
                finish();
                return true;
                */
            case R.id.mnuStartRoutine:
                book.createRoutine(getApplication());
                book.save(getApplication());
            case R.id.mnuContinueRoutine:
                Log.d(SMApp.TAG, book.getDebugRoutine(getApplication()));
                curScripId = book.current(getApplication()).getId();
                inRoutine = true;
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
                    case ScriptureActivity.RESULT_MEMORIZED:
                    case ScriptureActivity.RESULT_PARTIALLY_MEMORIZED:
                        commit(resultCode);
                        if (inRoutine) {
                            moveForward();
                        }
                        break;
                    default:
                        inRoutine = false;
                        curScripId = -1;
                }
                /*
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
                */
                break;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        state.putInt(EXTRA_SCRIP_ID, curScripId);
        state.putBoolean(EXTRA_IN_ROUTINE, inRoutine);
        super.onSaveInstanceState(state);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        Book book = Book.objects(getApplication()).get(bookId);

        if (!book.getPreloaded()) {
            inflater.inflate(R.menu.delete_menu, menu);
            menu.setHeaderTitle(book.getTitle());
        }
    }

    // the support library doesn't have a FragmentListActivity, so we have
    // to use the deprecated showDialog method
    @SuppressWarnings("deprecation")
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info =
            (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        deleteScrip = Scripture.object(getApplication(), bookId,
                info.position);

        switch (item.getItemId()) {
            case R.id.mnuDelete:
                showDialog(DELETE_DIALOG);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch (id) {
            case DELETE_DIALOG:
                builder.setMessage(String.format(getString(R.string.
                        dialog_delete), deleteScrip.getReference()))
                    .setPositiveButton(R.string.delete,
                            new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int id) {
                            deletePassage();
                            //deleteScrip = null;
                        }
                    });
                /*
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int id) {
                            deleteScrip = null;
                        }
                    });
                    */
                break;
        }
        return builder.create();
    }

    private void deletePassage() {
        deleteScrip.delete(getApplication());
        buildList();
        Toast.makeText(this, R.string.passage_deleted,
                Toast.LENGTH_SHORT).show();
        // reset routine here
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

    private void commit(int returnCode) {
        Context a = getApplication();
        Scripture curScripture = Scripture.objects(a).get(curScripId);
        Map<Integer, Integer> results = new HashMap<Integer, Integer>();
        results.put(ScriptureActivity.RESULT_MASTERED,
                Scripture.MASTERED);
        results.put(ScriptureActivity.RESULT_MEMORIZED,
                Scripture.MEMORIZED);
        results.put(ScriptureActivity.RESULT_PARTIALLY_MEMORIZED,
                Scripture.PARTIALLY_MEMORIZED);

        curScripture.setProgress(results.get(returnCode));
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
                inRoutine = false;
                // needed to update the status part
                buildList();
                // update option menu here?
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
