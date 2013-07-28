package com.jacobobryant.scripturemastery;

import com.orm.androrm.DatabaseAdapter;
import com.orm.androrm.Filter;
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

    public static class Holder {
        public Book book;
        public List<Scripture> scriptures;

        public Holder(Book book, List<Scripture> scriptures) {
            this.book = book;
            this.scriptures = scriptures;
        }
    }

    public static void syncDB(Context app) {
        final String DB_NAME = "sm.db";
        List<Class<? extends Model>> models =
                new ArrayList<Class<? extends Model>>();
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(app);
        //int dbVersion = prefs.getInt(PREF_VERSION, 0);
        int dbVersion = 0;
        Log.d(SMApp.TAG, "syncing DB, version " + dbVersion);

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
            upgrade(app, dbVersion);
            prefs.edit().putInt(PREF_VERSION, VERSION).apply();
        }
        Log.d(SMApp.TAG, "done syncing DB");
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
        DatabaseAdapter adapter = DatabaseAdapter.getInstance(app);
        List<Holder> holders = getBooks(app);
        List<Book> bookMatches;
        List<Scripture> scripMatches;
        Filter filter;

        filter = new Filter().contains("reference", "-");
        adapter.beginTransaction();
        for (Scripture scrip : Scripture.objects(app).filter(filter)) {
            scrip.setReference(scrip.getReference().replace('-', '–'));
            scrip.save(app);
        }
        adapter.commitTransaction();
        for (Holder holder : holders) {
            L.log("merging " + holder.book.getTitle());
            filter = new Filter().is("preloaded", 1)
                    .is("title", holder.book.getTitle());
            bookMatches = Book.objects(app).filter(filter).toList();
            if (bookMatches.size() > 0) {
                for (Scripture newScrip : holder.scriptures) {
                    filter = new Filter().is("reference",
                            newScrip.getReference());
                    scripMatches = bookMatches.get(0).getScriptures(app)
                            .filter(filter).toList();
                    if (scripMatches.size() > 0) {
                        updateScripture(scripMatches.get(0), newScrip);
                    }
                }
            }
        }
        L.log("deleting old scriptures");
        adapter.beginTransaction();
        for (Book book : Book.objects(app).all()) {
            book.delete(app);
        }
        for (Scripture scrip : Scripture.objects(app).all()) {
            scrip.delete(app);
        }
        save(app, holders);
        adapter.commitTransaction();
    }

    private static void updateScripture(Scripture oldScrip,
            Scripture newScrip) {
        final int OLD_NUM_LEVELS = 5;
        double ratio = (double) oldScrip.getFinishedStreak() /
                OLD_NUM_LEVELS;

        if (oldScrip.getFinishedStreak() >= OLD_NUM_LEVELS) {
            ratio = 1.0;
            if (oldScrip.getStatus() == Scripture.IN_PROGRESS) {
                newScrip.setStatus(Scripture.FINISHED);
            }
        } else {
            newScrip.setStatus(oldScrip.getStatus());
        }
        newScrip.setStartRatio(ratio);
    }

    private static void migrate(Context app) {
        List<BookRecord> books;
        Book book;
        Scripture scrip;
        DatabaseAdapter adapter = DatabaseAdapter.getInstance(app);

        books = getOldBooks(app);
        adapter.beginTransaction();
        for (BookRecord bookRecord : books) {
            book = new Book();
            Log.d(SMApp.TAG, "migrating " + bookRecord.title);
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
            book.save(app);
        }
        adapter.commitTransaction();
        app.deleteDatabase(DBHandler.DB_NAME);
    }

    private static List<BookRecord> getOldBooks(Context app) {
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

    private static List<Holder> getBooks(Context app) {
        Log.d(SMApp.TAG, "Reading scriptures from file");
        BufferedReader reader;
        Book book;
        Scripture scrip;
        StringBuilder verses;
        String bookName;
        String line;
        LinkedList<String> fields;
        int bookPosition = 0;
        int scripPosition;
        List<Scripture> scripList;
        List<Holder> holders = new LinkedList<Holder>();

        reader = new BufferedReader(new InputStreamReader(
                app.getResources().openRawResource(R.raw.scriptures)));
        try {
            while ((bookName = reader.readLine()) != null &&
                    bookName.length() != 0) {
                Log.d(SMApp.TAG, "reading " + bookName);
                book = new Book();
                book.setPosition(bookPosition++);
                book.setPreloaded(true);
                book.setTitle(bookName);
                scripList = new LinkedList<Scripture>();
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
                    scrip.setKeywords(fields.remove().trim());
                    scrip.setContext(fields.remove().trim());
                    scrip.setDoctrine(fields.remove().trim());
                    scrip.setApplication(fields.remove().trim());
                    verses = new StringBuilder();
                    for (String verse : fields) {
                        if (verses.length() != 0) {
                            verses.append("\n");
                        }
                        verses.append(verse);
                    }
                    scrip.setVerses(verses.toString());
                    scripList.add(scrip);
                }
                holders.add(new Holder(book, scripList));
            }
            reader.close();
        } catch (IOException ioe) {
            Log.e(SMApp.TAG,
                    "Couldn't read book data from scriptures.txt");
        }
        L.log("finished reading books");
        return holders;
    }

    private static void populate(Context app) {
        DatabaseAdapter adapter = DatabaseAdapter.getInstance(app);
        adapter.beginTransaction();
        save(app, getBooks(app));
        adapter.commitTransaction();
    }

    private static void save(Context app, List<Holder> holders) {
        L.log("saving");
        for (Holder holder : holders) {
            for (Scripture scrip : holder.scriptures) {
                holder.book.addScripture(scrip);
                scrip.save(app);
            }
            holder.book.save(app);
        }
        L.log("finished saving");
    }
}
