package com.jacobobryant.scripturemastery;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.orm.androrm.DatabaseAdapter;
import com.orm.androrm.Model;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class SyncDB {

    private static class BookRecord {
        public String title;
        public String routine;
        public boolean preloaded;
        public List<ScripRecord> scriptures;

        public BookRecord() {
            scriptures = new LinkedList<ScripRecord>();
        }
    }

    private static class ScripRecord {
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
        List<BookRecord> books;
        Book book;
        Scripture scrip;

    	models.add(Book.class);
    	models.add(Scripture.class);
    	DatabaseAdapter.setDatabaseName(DB_NAME);
        DatabaseAdapter.getInstance(app)
                .setModels(models);
        if (app.getDatabasePath(DBHandler.DB_NAME).exists()) {
            books = getBooks(app);
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
                book.setRoutine(app, bookRecord.routine);
                book.save(app);
            }
            app.deleteDatabase(DBHandler.DB_NAME);
        }
    }

    private static List<BookRecord> getBooks(Context app) {
        String table;
        Cursor bookCursor;
        Cursor scriptureCursor;
        List<BookRecord> books = new LinkedList<BookRecord>();
        DBHandler handler = new DBHandler(app);
        SQLiteDatabase db = handler.getReadableDatabase();
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
            scriptureCursor = db.rawQuery("SELECT reference, " +
                    "keywords, verses, status, finishedStreak FROM " +
                    table, null);
            scriptureCursor.moveToFirst();
            while (! scriptureCursor.isAfterLast()) {
                scrip = new ScripRecord();
                scrip.ref = scriptureCursor.getString(0);
                scrip.keywords = scriptureCursor.getString(1);
                scrip.verses = scriptureCursor.getString(2);
                scrip.status = scriptureCursor.getInt(3);
                scrip.finishedStreak = scriptureCursor.getInt(4);
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
}
