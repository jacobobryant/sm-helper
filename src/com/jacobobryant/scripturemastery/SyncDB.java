package com.jacobobryant.scripturemastery;

import com.orm.androrm.DatabaseAdapter;
import com.orm.androrm.Model;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.*;
import java.util.*;

public class SyncDB {

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

    	models.add(Book.class);
    	models.add(Scripture.class);
    	DatabaseAdapter.setDatabaseName(DB_NAME);
        DatabaseAdapter.getInstance(app)
                .setModels(models);
        if (Book.objects(app).count() == 0) {
            if (app.getDatabasePath(DBHandler.DB_NAME).exists()) {
                //try {
                    upgradeDB(app);
                /*} catch (SQLiteException e) {
                    Log.e(MainActivity.TAG,
                        "Couldn't upgrade old database", e);
                    populate(app);
                }*/
            } else {
                populate(app);
            }
        }
    }

    private static void upgradeDB(Context app) {
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
            book.setRoutine(bookRecord);
            book.save(app);
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
        final int[] BOOK_IDS = {R.raw.old_testament,
            R.raw.new_testament, R.raw.book_of_mormon,
            R.raw.doctrine_and_covenants, R.raw.lists,
            R.raw.articles_of_faith};
        BufferedReader reader;
        Book book;
        Scripture scrip;
        String reference;
        String keywords;
        StringBuilder verses;
        String verse;
        DatabaseAdapter adapter = DatabaseAdapter.getInstance(app);

        adapter.beginTransaction();
        for (int id : BOOK_IDS) {
            book = new Book();
            book.setPreloaded(true);
            reader = new BufferedReader(new InputStreamReader(
                app.getResources().openRawResource(id)));
            try {
                book.setTitle(reader.readLine());
                while ((reference = reader.readLine()) != null) {
                    keywords = reader.readLine();
                    verses = new StringBuilder();
                    while ((verse = reader.readLine()) != null &&
                            verse.length() != 0) {
                        if (verses.length() > 0) {
                            verses.append("\n");
                        }
                        verses.append(verse);
                    }
                    scrip = new Scripture(reference, keywords,
                        verses.toString());
                    book.addScripture(scrip);
                    scrip.save(app);
                }
                reader.close();
            } catch (IOException ioe) {
                Log.e(MainActivity.TAG, "Couldn't read book data " +
                    "from file (id = " + id + ")");
            }
            book.save(app);
        }
        adapter.commitTransaction();
    }
}
