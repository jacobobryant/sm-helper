package com.jacobobryant.scripturemastery;

import android.app.ExpandableListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.*;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

import java.util.*;

public class MainActivity extends ExpandableListActivity {
    public static final String TAG = "scripturemastery";
    private static final int LEARN_SCRIPTURE_REQUEST = 0;
    private static final int NEW_PASSAGE_REQUEST = 1;
    private DataSource data;
    private static Book[] books;
    private static Scripture curScripture;
    private boolean inRoutine;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        data = new DataSource(this);
        buildExpandableList(true);
        curScripture = null;
        inRoutine = false;
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
        Intent intent = new Intent(this, ScriptureActivity.class);
        curScripture = books[groupPosition].getScripture(childPosition);
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
        switch (item.getItemId()) {
            case R.id.mnuNewPassage:
                Intent intent =
                        new Intent(this, NewPassageActivity.class);
                startActivityForResult(intent, NEW_PASSAGE_REQUEST);
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
                startScripture(book);
                return true;
            case R.id.mnuContinueRoutine:
                startScripture(book);
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

    private void startScripture(Book book) {
        Intent intent = new Intent(this, ScriptureActivity.class);
        curScripture = book.getRoutine().current();
        inRoutine = true;
        startActivityForResult(intent, LEARN_SCRIPTURE_REQUEST);
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
                startScripture(book);
            } else {
                buildExpandableList(false);
            }
        } else {
            curScripture = null;
            buildExpandableList(false);
        }
    }

    public static Scripture getScripture() {
        return curScripture;
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
