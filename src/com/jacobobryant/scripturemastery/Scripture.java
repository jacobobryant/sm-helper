package com.jacobobryant.scripturemastery;

import com.orm.androrm.field.BlobField;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.ForeignKeyField;
import com.orm.androrm.field.IntegerField;

import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;

import android.content.Context;

public class Scripture extends Model {
    public static final int NOT_STARTED = 0;
    public static final int IN_PROGRESS = 1;
    public static final int FINISHED = 2;
    public static final int MASTERED = 0;
    public static final int MEMORIZED = 1;
    public static final int PARTIALLY_MEMORIZED = 2;
    public static final int MAX_LEVEL = 5;
    public static final int WAIT = 1;
    public static final int MAX_IN_PROGRESS = 3;
    protected CharField reference;
    protected CharField keywords;
    protected BlobField verses;
    protected IntegerField status;
    protected IntegerField finishedStreak;
    protected ForeignKeyField<Book> book;

    public Scripture() {
        reference = new CharField();
        keywords = new CharField();
        verses = new BlobField();
        status = new IntegerField(1);
        status.set(NOT_STARTED);
        finishedStreak = new IntegerField(2);
        book = new ForeignKeyField<Book>(Book.class);
    }

    public static final QuerySet<Scripture> objects(Context context) {
        return objects(context, Scripture.class);
    }

    public Scripture(String reference, String keywords,
            String verses, int status, int finishedStreak) {
        this();
        this.reference.set(reference);
        this.keywords.set(keywords);
        this.verses.set(verses.getBytes());
        this.status.set(status);
        this.finishedStreak.set(finishedStreak);
    }

    public Scripture(String reference, String keywords, String verses) {
        this(reference, keywords, verses, NOT_STARTED, 0);
    }

	public String getReference() {
        return reference.get();
    }

    public String getVerses() {
        return new String(verses.get());
    }

	public int getStatus() {
		return status.get();
	}

	public int getFinishedStreak() {
		return finishedStreak.get();
	}

    public void setProgress(int progress) {
        switch (progress) {
            case MASTERED:
                if (finishedStreak.get() < WAIT + MAX_LEVEL) {
                    finishedStreak.set(WAIT + MAX_LEVEL);
                    status.set(FINISHED);
                }
                break;
            case MEMORIZED:
                finishedStreak.set(finishedStreak.get() + 1);
                if (finishedStreak.get() > WAIT + MAX_LEVEL ) {
                    status.set(FINISHED);
                } else {
                    status.set(IN_PROGRESS);
                }
                break;
            case PARTIALLY_MEMORIZED:
                // decrement the starting level
                if (finishedStreak.get() > 0) {
                    if (finishedStreak.get() > WAIT + MAX_LEVEL) {
                        finishedStreak.set(WAIT + MAX_LEVEL - 1);
                    } else {
                        finishedStreak.set(finishedStreak.get() - 1);
                    }
                }
                status.set(IN_PROGRESS);
                break;
        }
    }

    public Book getBook(Context context) {
        return book.get(context);
    }

    public void setBook(Book book) {
        this.book.set(book);
    }

    public int getStartLevel() {
        int level = finishedStreak.get() - WAIT;
        if (level < 0) {
            level = 0;
        } else if (level > MAX_LEVEL) {
            level = MAX_LEVEL;
        }
        return level;
    }

    public boolean isNumbered() {
        return new String(reference.get()).matches(".*:\\d+.*");
    }

    public int getFirstVerse() {
        // ex: returns 4 if reference is "foo bar 12:4-6"
        try {
            return Integer.parseInt(new String(reference.get())
                    .replaceFirst(".+:(\\d+).*", "$1"));
        } catch (NumberFormatException e) {
            throw new UnsupportedOperationException(
                    "not a numbered scripture");
        }
    }

    public String getKeywords() {
        return keywords.get();
    }
}
