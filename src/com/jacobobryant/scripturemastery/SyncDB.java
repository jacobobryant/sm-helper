package com.jacobobryant.scripturemastery;

import com.orm.androrm.DatabaseAdapter;
import com.orm.androrm.Model;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.*;
import java.util.*;

public class SyncDB {
    public static final int VERSION = 1;
    public static final String PREF_VERSION = "pref_version";

    public static class BookRecord {
        public String title;
        public String routine;
        public boolean preloaded;
        public List<ScripRecord> scriptures;

        public BookRecord() {
            scriptures = new LinkedList<ScripRecord>();
        }
    }

    public static class ScripRecord {
        public int id;
        public String ref;
        public String keywords;
        public String verses;
        public int status;
        public int finishedStreak;
    }

    public static void syncDB(Context app) {
        final String DB_NAME = "sm.db";
        List<Class<? extends Model>> models =
                new ArrayList<Class<? extends Model>>();
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(app);
        int dbVersion = prefs.getInt(PREF_VERSION, 0);

        models.add(Book.class);
        models.add(Scripture.class);
        DatabaseAdapter.setDatabaseName(DB_NAME);
        DatabaseAdapter.getInstance(app)
                .setModels(models);
        if (Book.objects(app).count() == 0) {
            if (app.getDatabasePath(DBHandler.DB_NAME).exists()) {
                migrate(app);
            } else {
                populate(app);
                prefs.edit().putInt(PREF_VERSION, VERSION).apply();
                dbVersion = VERSION;
            }
        }
        if (dbVersion < VERSION) {
            //upgrade(app, dbVersion);
            //prefs.edit().putInt(PREF_VERSION, VERSION).apply();
        }
    }

    private static void upgrade(Context app, int oldVersion) {
        Log.d(SMApp.TAG, "upgrading DB from " + oldVersion + " to " +
                VERSION);
        if (oldVersion == 0) {
            upgrade0to1(app);
            oldVersion++;
        }
    }

    private static void upgrade0to1(Context app) {
        int bookPosition = 0;
        int scripPosition;
        DatabaseAdapter adapter = DatabaseAdapter.getInstance(app);

        adapter.beginTransaction();
        for (Book book : Book.objects(app).all()) {
            book.setPosition(bookPosition++);
            book.save(app);
            scripPosition = 0;
            for (Scripture scrip : book.getScriptures(app).all()) {
                scrip.setPosition(scripPosition++);
                scrip.save(app);
            }
        }
        adapter.commitTransaction();
    }

    private static void migrate(Context app) {
        List<BookRecord> books;
        Book book;
        Scripture scrip;
        DatabaseAdapter adapter = DatabaseAdapter.getInstance(app);

        books = getBooks(app);
        adapter.beginTransaction();
        for (BookRecord bookRecord : books) {
            book = new Book();
            book.setTitle(bookRecord.title);
            book.setPreloaded(bookRecord.preloaded);
            for (ScripRecord scripRecord : bookRecord.scriptures) {
                scrip = new Scripture(scripRecord.ref,
                    scripRecord.keywords, scripRecord.verses,
                    scripRecord.status, scripRecord.finishedStreak);
                book.addScripture(scrip);
                scrip.save(app);
            }
            book.setRoutine(bookRecord, app);
        }
        adapter.commitTransaction();
        app.deleteDatabase(DBHandler.DB_NAME);
    }

    private static List<BookRecord> getBooks(Context app) {
        String table;
        Cursor bookCursor;
        Cursor scriptureCursor;
        List<BookRecord> books = new LinkedList<BookRecord>();
        DBHandler handler = new DBHandler(app);
        // db must be writeable so it can be upgraded if needed.
        SQLiteDatabase db = handler.getWritableDatabase();
        BookRecord book;
        ScripRecord scrip;

        bookCursor = db.rawQuery("SELECT _id, title, routine, " +
                "preloaded FROM books", null);
        bookCursor.moveToFirst();
        while (! bookCursor.isAfterLast()) {
            book = new BookRecord();
            table = DBHandler.getTable(bookCursor.getInt(0));
            book.title = bookCursor.getString(1);
            book.routine = bookCursor.getString(2);
            book.preloaded = (bookCursor.getInt(3) == 1) ? true : false;
            scriptureCursor = db.rawQuery("SELECT _id, reference, " +
                    "keywords, verses, status, finishedStreak FROM " +
                    table, null);
            scriptureCursor.moveToFirst();
            while (! scriptureCursor.isAfterLast()) {
                scrip = new ScripRecord();
                scrip.id = scriptureCursor.getInt(0);
                scrip.ref = scriptureCursor.getString(1);
                scrip.keywords = scriptureCursor.getString(2);
                scrip.verses = scriptureCursor.getString(3);
                scrip.status = scriptureCursor.getInt(4);
                scrip.finishedStreak = scriptureCursor.getInt(5);
                book.scriptures.add(scrip);
                scriptureCursor.moveToNext();
            }
            scriptureCursor.close();
            books.add(book);
            bookCursor.moveToNext();
        }
        bookCursor.close();
        return books;
    }

    private static void populate(Context app) {
        BufferedReader reader;
        Book book;
        Scripture scrip;
        StringBuilder verses;
        String bookName;
        String line;
        LinkedList<String> fields;
        DatabaseAdapter adapter = DatabaseAdapter.getInstance(app);
        int bookPosition = 0;
        int scripPosition;

        adapter.beginTransaction();
        reader = new BufferedReader(new InputStreamReader(
                app.getResources().openRawResource(R.raw.scriptures)));
        try {
            while ((bookName = reader.readLine()) != null &&
                    bookName.length() != 0) {
                book = new Book();
                book.setPosition(bookPosition++);
                book.setPreloaded(true);
                book.setTitle(bookName);
                scripPosition = 0;
                while ((line = reader.readLine()) != null &&
                        line.length() != 0) {
                    scrip = new Scripture();
                    scrip.setPosition(scripPosition++);
                    fields = new LinkedList<String>(
                            Arrays.asList(line.split("\t")));
                    if (fields.size() < 6) {
                        Log.e(SMApp.TAG, "not enough fields in line: \"" +
                                line + "\"");
                        continue;
                    }
                    scrip.setReference(fields.remove());
                    scrip.setKeywords(fields.remove());
                    scrip.setContext(fields.remove());
                    scrip.setDoctrine(fields.remove());
                    scrip.setApplication(fields.remove());
                    verses = new StringBuilder();
                    for (String verse : fields) {
                        if (verses.length() != 0) {
                            verses.append("\n");
                        }
                        verses.append(verse);
                    }
                    scrip.setVerses(verses.toString());
                    book.addScripture(scrip);
                    scrip.save(app);
                }
                book.save(app);
            }
            reader.close();
        } catch (IOException ioe) {
            Log.e(SMApp.TAG,
                    "Couldn't read book data from scriptures.txt");
        }
        adapter.commitTransaction();
    }
}
