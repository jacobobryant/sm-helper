package com.jacobobryant.scripturemastery;

import android.app.ExpandableListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.*;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import java.util.*;

public class MainActivity extends ExpandableListActivity {
    public static final String TAG = "scripturemastery";
    public static final String EXTRA_BOOK_ID =
            "com.jacobobryant.scripturemastery.BOOK_ID";
    public static final String EXTRA_SCRIP_ID =
            "com.jacobobryant.scripturemastery.SCRIP_ID";
    private static final String ROUTINE_REF = "in_routine";
    private static final int LEARN_SCRIPTURE_REQUEST = 0;
    private static final int NEW_PASSAGE_REQUEST = 1;
    private static final int LEARN_KEYWORD_REQUEST = 2;
    private DataSource data;
    private static Book[] books;
    private Scripture curScripture;
    private boolean inRoutine;
    
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        int bookId;
        int scripId;
        try {
            bookId = state.getInt(EXTRA_BOOK_ID);
            scripId = state.getInt(EXTRA_SCRIP_ID);
        } catch (NullPointerException e) {
            bookId = 0;
            scripId = 0;
        }

        data = new DataSource(this);
        buildExpandableList(true);
        if (bookId == 0) {
            curScripture = null;
        } else {
            curScripture = findBookById(bookId).findScriptureById(scripId);
        }
        try {
            inRoutine = state.getBoolean(ROUTINE_REF);
        } catch (NullPointerException e) {
            inRoutine = false;
        }
        registerForContextMenu(getExpandableListView());
    }

    @Override
    public void onPause() {
        super.onPause();
        data.close();
    }

    @Override
    public void onResume() {
        super.onResume();
        data.open();
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v,
            int groupPosition, int childPosition, long id) {
        Book book = books[groupPosition];
        Intent intent = new Intent(this, ScriptureActivity.class);

        curScripture = book.getScripture(childPosition);
        intent.putExtra(EXTRA_BOOK_ID, book.getId());
        intent.putExtra(EXTRA_SCRIP_ID, curScripture.getId());
        startActivityForResult(intent, LEARN_SCRIPTURE_REQUEST);
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
        ExpandableListView.ExpandableListContextMenuInfo info =
                (ExpandableListView.ExpandableListContextMenuInfo)
                menuInfo;
        MenuInflater inflater = getMenuInflater();
        int type = ExpandableListView
                .getPackedPositionType(info.packedPosition);
        int groupPos = ExpandableListView
                .getPackedPositionGroup(info.packedPosition);
        int childPos = ExpandableListView
                .getPackedPositionChild(info.packedPosition);

        if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            inflater.inflate(R.menu.book_menu, menu);
            menu.setHeaderTitle(books[groupPos].getTitle());
            if (books[groupPos].getRoutine().length() > 0) {
                menu.findItem(R.id.mnuContinueRoutine).setVisible(true);
            }
            if (!books[groupPos].isScriptureMastery()) {
                menu.findItem(R.id.mnuDeleteGroup).setVisible(true);
            }
        } else {
            if (!books[groupPos].isScriptureMastery()) {
                inflater.inflate(R.menu.passage_menu, menu);
                menu.setHeaderTitle(books[groupPos]
                        .getScripture(childPos).getReference());
            }
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
        Book book = books[groupPos];

        switch (item.getItemId()) {
            case R.id.mnuStartRoutine:
                book.createRoutine();
                data.commit(book);
            case R.id.mnuContinueRoutine:
                inRoutine = true;
                curScripture = book.getRoutine().current();
                startScripture();
                return true;
            case R.id.mnuDeleteGroup:
                data.deleteGroup(book);
                buildExpandableList(true);
                toast(R.string.groupDeleted, true);
                return true;
            case R.id.mnuDeletePassage:
                data.deletePassage(book.getScripture(childPos));
                buildExpandableList(true);
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
                    scriptureIntent.putExtra(EXTRA_BOOK_ID,
                            curScripture.getParent().getId());
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
                        data.open();
                        buildExpandableList(true);
                        toast(R.string.passageAdded, true);
                }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        if (curScripture != null) {
            state.putInt(EXTRA_BOOK_ID, curScripture.getParent().getId());
            state.putInt(EXTRA_SCRIP_ID, curScripture.getId());
        }
        state.putBoolean(ROUTINE_REF, inRoutine);
        super.onSaveInstanceState(state);
    }

    private Book findBookById(int id) {
        for (Book book : books) {
            if (book.getId() == id) {
                return book;
            }
        }
        throw new NoSuchElementException("no book with id = " + id);
    }

    private void buildExpandableList(boolean refreshFromDB) {
        final String NAME = "NAME";
        final String STATUS = "STATUS";
        List<Map<String, String>> bookData =
                new ArrayList<Map<String, String>>();
        List<List<Map<String, String>>> referenceData =
                new ArrayList<List<Map<String, String>>>();
        List<Map<String, String>> childReferenceData;
        Map<String, String> map;

        if (refreshFromDB) {
            books = data.getBooks();
        }
        for (Book book : books) {
            map = new HashMap<String, String>();
            map.put(NAME, book.getTitle());
            bookData.add(map);
            childReferenceData = new ArrayList<Map<String, String>>();
            for (Scripture scripture : book.getScriptures()) {
                map = new HashMap<String, String>();
                map.put(NAME, scripture.getReference());
                map.put(STATUS, getStatusString(scripture.getStatus()));
                childReferenceData.add(map);
            }
            referenceData.add(childReferenceData);
        }
        setListAdapter(new SimpleExpandableListAdapter(
                this,
                bookData,
                android.R.layout.simple_expandable_list_item_1,
                new String[] {NAME},
                new int[] {android.R.id.text1},
                referenceData,
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

    private void commit() {
        Book book;
        Routine routine;

        data.open();
        data.commit(curScripture);
        if (inRoutine) {
            book = curScripture.getParent();
            routine = book.getRoutine();
            routine.moveToNext();
            data.commit(book);
            if (routine.length() > 0) {
                curScripture = routine.current();
                startScripture();
            } else {
                buildExpandableList(false);
            }
        } else {
            curScripture = null;
            buildExpandableList(false);
        }
    }

    private void startScripture() {
        Intent intent = new Intent();
        int count = 0;
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(this);
        boolean practiceKeywords =
                prefs.getBoolean(SettingsActivity.KEYWORDS, true);
        Book book = curScripture.getParent();

        intent.putExtra(EXTRA_BOOK_ID, book.getId());
        intent.putExtra(EXTRA_SCRIP_ID, curScripture.getId());
        if (practiceKeywords && book.isScriptureMastery()) {
            for (Scripture scrip : book.getScriptures()) {
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

    public static Book[] getBooks() {
        return books;
    }

    private void toast(int id, boolean lengthShort) {
        int length = (lengthShort) ? Toast.LENGTH_SHORT
                : Toast.LENGTH_LONG;
        Toast.makeText(this, getString(id), length).show();
    }
}
