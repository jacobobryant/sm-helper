package com.jacobobryant.scripturemastery;

import com.orm.androrm.DatabaseAdapter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.text.util.Linkify;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.*;

public class MainActivity extends ListActivity {
    public static final String EXTRA_BOOK_ID =
            "com.jacobobryant.scripturemastery.BOOK_ID";
    private static final int NEW_PASSAGE_REQUEST = 0;
    private final int DELETE_DIALOG = 0;
    private final int ABOUT_DIALOG = 1;
    private Book deleteBook;
    
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        Log.d(SMApp.TAG, "Entering SM Helper");
        ChangeLog cl = new ChangeLog(this);
        //cl.dontuseSetLastVersion("0.6");
        if (cl.firstRun()) {
            cl.getLogDialog().show();
        }
        SyncDB.syncDB(getApplication());
        buildList();
        registerForContextMenu(getListView());
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

    @SuppressWarnings("deprecation")
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
            case R.id.mnu_about:
                showDialog(ABOUT_DIALOG);
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

    // the support library doesn't have a FragmentListActivity, so we have
    // to use the deprecated showDialog method
    @SuppressWarnings("deprecation")
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info =
            (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        deleteBook = Book.object(getApplication(), info.position);

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
                builder.setMessage(String.format(getString(
                        R.string.dialog_delete), deleteBook.getTitle()))
                    .setPositiveButton(R.string.delete,
                            new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int id) {
                            deleteGroup();
                            deleteBook = null;
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int id) {
                            deleteBook = null;
                        }
                    });
                break;
            case ABOUT_DIALOG:
                builder.setTitle(R.string.title_activity_main)
                    .setMessage(getAboutText())
                    .setPositiveButton(android.R.string.ok, null);
                break;
        }
        return builder.create();
    }

    private Spannable getAboutText() {
        String version;
        BufferedReader reader;
        String line;
        StringBuilder text = new StringBuilder();
        Spannable content;

        try {
            version = getPackageManager()
                .getPackageInfo(getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            return null;
        }
        reader = new BufferedReader(new InputStreamReader(
            getApplication().getResources()
            .openRawResource(R.raw.about)));
        try {
            while ((line = reader.readLine()) != null) {
                text.append(line);
            }
            reader.close();
        } catch (IOException ioe) {
            Log.e(SMApp.TAG, "Couldn't read about text");
        }
        content = (Spannable) Html.fromHtml(String.format(text.toString(), version));
        Linkify.addLinks(content,
                Linkify.EMAIL_ADDRESSES|Linkify.WEB_URLS);
        return content;
    }

    private void deleteGroup() {
        Context app = getApplication();
        DatabaseAdapter adapter = DatabaseAdapter.getInstance(app);
        adapter.beginTransaction();
        for (Scripture scrip : deleteBook.getScriptures(app).all()) {
            scrip.delete(app);
        }
        deleteBook.delete(app);
        adapter.commitTransaction();
        buildList();
        Toast.makeText(this, R.string.group_deleted,
                Toast.LENGTH_SHORT).show();
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
