package com.jacobobryant.scripturemastery;

import com.orm.androrm.field.BooleanField;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.OneToManyField;
import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;

import android.content.Context;

public class Book extends Model {
    protected CharField title;
    protected OneToManyField<Book, Scripture> scriptures;
    protected CharField strRoutine;
    protected BooleanField preloaded;
    private Routine routine;

	public static final QuerySet<Book> objects(Context context) {
		return objects(context, Book.class);
	}

	public Book() {
		super();
		title = new CharField();
        preloaded = new BooleanField();
        strRoutine = new CharField();
		scriptures = new OneToManyField<Book, Scripture>(
                Book.class, Scripture.class);
	}

	public String getTitle() {
		return title.get();
	}

    public void setTitle(String title) {
        this.title.set(title);
    }
	
	public QuerySet<Scripture> getScriptures(Context context) {
		return scriptures.get(context, this);
	}

	public Scripture getScripture(Context context, int index) {
		return scriptures.get(context, this).toList().get(index);
	}

    public void addScripture(Scripture scrip) {
        scriptures.add(scrip);
    }
	
    public Routine getRoutine(Context context) {
        if (routine == null) {
            routine = new Routine(context, this);
        }
        return routine;
    }

    public void setRoutine(Context context, String routine) {
        strRoutine.set(routine);
        this.routine = new Routine(context, this, routine);
    }

    public boolean getPreloaded() {
        return preloaded.get();
    }

    public void setPreloaded(boolean preloaded) {
        this.preloaded.set(preloaded);
    }

    public boolean hasKeywords(Context context) {
        if (scriptures.get(context, this).isEmpty()) {
            throw new UnsupportedOperationException("this book is empty");
        }
        return (scriptures.get(context, this).all().limit(1).toList()
                .get(0).getKeywords().length() > 0);
    }

    @Override
    public boolean save(Context context) {
        strRoutine.set(routine.toString());
        return super.save(context);
    }

    @Override
    public boolean save(Context context, int id) {
        strRoutine.set(routine.toString());
        return super.save(context, id);
    }
}
