package com.jacobobryant.scripturemastery;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DBHandler extends SQLiteOpenHelper {
    private final int[] BOOK_IDS = {R.raw.old_testament,
            R.raw.new_testament, R.raw.book_of_mormon,
            R.raw.doctrine_and_covenants, R.raw.lists,
            R.raw.articles_of_faith};
    public static final String DB_NAME = "scriptures.db";
    public static final String BOOKS = "books";
    private static final int VERSION = 7;
    private Context context;

    public DBHandler(Context context) {
        super(context, DB_NAME, null, VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTableBooks(db);
        for (int i = 0; i < BOOK_IDS.length; i++) {
            addBook(db, readBook(BOOK_IDS[i]), true);
        }
    }

    private void createTableBooks(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + BOOKS + " (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title STRING, " +
                "routine STRING, " +
                "preloaded INTEGER DEFAULT 0);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion,
                          int newVersion) {
        if (oldVersion == 3) {
            upgrade3to4(db);
            oldVersion++;
        }
        if (oldVersion == 4) {
            upgrade4to5(db);
            oldVersion++;
        }
        if (oldVersion == 5) {
            upgrade5to6(db);
            oldVersion++;
        }
        if (oldVersion == 6) {
            upgrade6to7(db);
            oldVersion++;
        }
    }

    private void upgrade3to4(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + BOOKS + " ADD COLUMN " +
                "is_scripture INTEGER DEFAULT 0");
        db.execSQL("UPDATE " + BOOKS + " SET is_scripture = 1 " + 
                "WHERE _id <= " + BOOK_IDS.length);
    }

    private void upgrade4to5(SQLiteDatabase db) {
        final int SCRIPTURE_COUNT = 25;
        String table;
        Cursor bookCursor = db.rawQuery("SELECT _id, is_scripture FROM "
                + BOOKS + " ORDER BY _id ASC", null);
        int isScripture;
        int scripIndex = 0;
        Book book;
        Scripture scrip;

        bookCursor.moveToFirst();
        while (! bookCursor.isAfterLast()) {
            table = getTable(bookCursor.getInt(0));
            isScripture = bookCursor.getInt(1);
            db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + 
                    "keywords STRING");
            if (isScripture == 1) {
                book = readBook(BOOK_IDS[scripIndex++]);
                for (int i = 0; i < SCRIPTURE_COUNT; i++) {
                    scrip = book.getScripture(i);
                    db.execSQL("UPDATE " + table + " SET keywords=\"" +
                            scrip.getKeywords() + "\" WHERE _id=" +
                            (i + 1));
                }
            }
            bookCursor.moveToNext();
        }
    }

    private void upgrade5to6(SQLiteDatabase db) {
        Cursor cur = db.rawQuery("SELECT _id, title, routine, " +
                "is_scripture FROM " + BOOKS + " ORDER BY _id", null);
        int position = 0;
        List<Book> books = new ArrayList<Book>();
        int id;
        String title;
        String routine;
        boolean preloaded;

        cur.moveToFirst();
        while (!cur.isAfterLast()) {
            position++;
            if (position == 5) {
                books.add(readBook(BOOK_IDS[4]));
                books.add(readBook(BOOK_IDS[5]));
                position += 2;
            }
            id = cur.getInt(0);
            title = cur.getString(1);
            routine = cur.getString(2);
            preloaded = (cur.getInt(3) == 1) ? true : false;
            books.add(new Book(title, new Scripture[0], routine, id,
                               preloaded));
            if (id != position) {
                db.execSQL("ALTER TABLE " + getTable(id) + " RENAME TO " +
                        getTable(position));
            }
            cur.moveToNext();
        }
        cur.close();
        db.execSQL("DROP TABLE " + BOOKS);
        createTableBooks(db);
        position = 0;
        for (Book book : books) {
            position++;
            if (position == 5 || position == 6) {
                addBook(db, book, true);
                continue;
            }
            routine = book.getRoutine().toString();
            routine = (routine == null) ?
                    "null" : "\"" + routine + "\"";
            db.execSQL(String.format("INSERT INTO %s (title, routine, " +
                    "preloaded) VALUES (\"%s\", %s, %d)", BOOKS,
                    book.getTitle(), routine,
                    ((book.wasPreloaded()) ? 1 : 0)));
        }
    }

    private void upgrade6to7(SQLiteDatabase db) {
        final short LIST_POSITION = 4;
        Book listBook = readBook(BOOK_IDS[LIST_POSITION]);
        String table = getTable(LIST_POSITION + 1);

        for (Scripture scrip : listBook.getScriptures()) {
            db.execSQL("UPDATE " + table + " SET verses = \"" +
                    scrip.getVerses() + "\" WHERE reference = \"" +
                    scrip.getReference() + "\"");
        }
    }

    private void addBook(SQLiteDatabase db, Book book,
            boolean preloaded) {
        String table;
        Cursor cursor;
        int preloadedInt = (preloaded) ? 1 : 0;

        db.execSQL("INSERT INTO books (title, preloaded) VALUES (\"" +
                book.getTitle() + "\", " + preloadedInt + ")");
        cursor = db.rawQuery("SELECT _id FROM " + BOOKS +
                " ORDER BY _id DESC LIMIT 1", null);
        cursor.moveToFirst();
        table = "book_" + cursor.getString(0);
        cursor.close();
        db.execSQL("CREATE TABLE " + table + " (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " + 
                "reference STRING, " +
                "keywords STRING, " +
                "verses STRING, " +
                "status INTEGER DEFAULT " + Scripture.NOT_STARTED +
                ", finishedStreak INTEGER DEFAULT 0);");
        for (Scripture scripture : book.getScriptures()) {
            addScripture(db, table, scripture);
        }
    }

    public void addBook(SQLiteDatabase db, Book book) {
        addBook(db, book, false);
    }

    public void addScripture(SQLiteDatabase db, String table,
            Scripture scripture) {
        db.execSQL("INSERT INTO " + table +
                "(reference, keywords, verses) VALUES (\"" +
                scripture.getReference() + "\", \"" +
                scripture.getKeywords() + "\", \"" +
                scripture.getVerses() + "\");");
    }

    private Book readBook(int id) {
        String title = "";
        List<Scripture> scriptures = new ArrayList<Scripture>();
        String reference;
        String keywords;
        StringBuilder verses;
        String verse;
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getResources().openRawResource(id)));

        try {
            title = reader.readLine();
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
                scriptures.add(new Scripture(reference, keywords,
                        verses.toString()));
            }
            reader.close();
        } catch (IOException ioe) {
            Log.e("tag", "There was an input error");
        }
        return new Book(title, scriptures);
    }

    public static String getTable(int id) {
        return "book_" + id;
    }
}
