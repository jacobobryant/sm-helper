package com.jacobobryant.scripturemastery;

import com.orm.androrm.field.BlobField;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.ForeignKeyField;
import com.orm.androrm.field.IntegerField;
import com.orm.androrm.Filter;

import com.orm.androrm.migration.Migrator;
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
    public static final int NUM_LEVELS = 5;
    public static final int MAX_IN_PROGRESS = 3;
    protected CharField reference;
    protected CharField keywords;
    protected BlobField context;
    protected BlobField application;
    protected BlobField doctrine;
    protected BlobField verses;
    protected IntegerField status;
    protected IntegerField finishedStreak;
    protected IntegerField position;
    protected ForeignKeyField<Book> book;

    public Scripture() {
        position = new IntegerField();
        reference = new CharField();
        keywords = new CharField();
        context = new BlobField();
        application = new BlobField();
        doctrine = new BlobField();
        verses = new BlobField();
        status = new IntegerField(1);
        status.set(NOT_STARTED);
        finishedStreak = new IntegerField(2);
        book = new ForeignKeyField<Book>(Book.class);
    }

    public static final QuerySet<Scripture> objects(Context context) {
        return objects(context, Scripture.class);
    }

    public static Scripture object(Context context, int bookId,
            int index) {
        return objects(context, Scripture.class).filter(new Filter()
            .is("book__mId", bookId)).limit(index, 1).toList().get(0);
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

    public Integer getPosition() {
        return position.get();
    }

    public void setPosition(Integer position) {
        this.position.set(position);
    }

    public void setReference(String reference) {
        this.reference.set(reference);
    }

    public String getReference() {
        return reference.get();
    }

    public void setVerses(String verses) {
        this.verses.set(verses.getBytes());
    }

    public String getVerses() {
        return new String(verses.get());
    }

    public void setContext(String context) {
        this.context.set(context.getBytes());
    }

    public String getContext() {
        return ((context.get() != null) ? new String(context.get()) : "");
    }

    public void setDoctrine(String doctrine) {
        this.doctrine.set(doctrine.getBytes());
    }

    public String getDoctrine() {
        return ((doctrine.get() != null)
                ? new String(doctrine.get()) : "");
    }

    public void setApplication(String application) {
        this.application.set(application.getBytes());
    }

    public String getApplication() {
        return ((application.get() != null)
                ? new String(application.get()) : "");
    }

    public void setStatus(int status) {
        this.status.set(status);
    }

    public int getStatus() {
        return status.get();
    }

    public void setFinishedStreak(int streak) {
        finishedStreak.set(streak);
    }

    public int getFinishedStreak() {
        return finishedStreak.get();
    }

    public void setProgress(int progress) {
        switch (progress) {
            case MASTERED:
                if (finishedStreak.get() < NUM_LEVELS) {
                    finishedStreak.set(NUM_LEVELS);
                    status.set(FINISHED);
                }
                break;
            case MEMORIZED:
                finishedStreak.set(finishedStreak.get() + 1);
                status.set((finishedStreak.get() > NUM_LEVELS)
                        ? FINISHED : IN_PROGRESS);
                /*
                if (finishedStreak.get() > NUM_LEVELS ) {
                    status.set(FINISHED);
                } else {
                    status.set(IN_PROGRESS);
                }
                */
                break;
            case PARTIALLY_MEMORIZED:
                // decrement the starting level
                int streak = finishedStreak.get();
                if (streak > 0) {
                    finishedStreak.set((streak > NUM_LEVELS)
                            ? NUM_LEVELS - 1 : streak - 1);
                    /*
                    if (finishedStreak.get() > NUM_LEVELS) {
                        finishedStreak.set(NUM_LEVELS - 1);
                    } else {
                        finishedStreak.set(finishedStreak.get() - 1);
                    }
                    */
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
        int level = finishedStreak.get();
        if (level < 0) {
            level = 0;
        } else if (level > NUM_LEVELS) {
            level = NUM_LEVELS;
        }
        return level;
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

    public void setKeywords(String keywords) {
        this.keywords.set(keywords);
    }

    public String getKeywords() {
        return keywords.get();
    }

    @Override
    public String toString() {
        return "Scripture [reference=" + reference + ", position=" +
            position + "]";
        /*
        return "Scripture [reference=" + reference + ", keywords=" +
            keywords + ", verses=" + verses + ", status=" + status +
            ", finishedStreak=" + finishedStreak + ", book=" +
            book.get().getTitle() + "]";
            */
    }

    @Override
    public boolean delete(Context app) {
        Book book = this.book.get(app);
        if (book != null) {
            book.deleteScripture(this, app);
        }
        return super.delete(app);
    }

    @Override
    protected void migrate(Context context) {
        Migrator<Scripture> migrator =
                new Migrator<Scripture>(Scripture.class);
        migrator.addField("context", new CharField());
        migrator.addField("application", new CharField());
        migrator.addField("doctrine", new CharField());
        migrator.addField("position", new IntegerField());
        migrator.migrate(context);
    }
}
