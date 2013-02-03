package com.jacobobryant.scripturemastery;

import java.util.List;

public class Book {
    private int id;
    private String title;
    private Scripture[] scriptures;
    private boolean isScripture;
    private Routine routine;

	public Book(String title, Scripture[] scriptures, String strRoutine,
            int id, boolean isScripture) {
		this.title = title;
		this.scriptures = scriptures;
        for (Scripture scripture : scriptures) {
            scripture.setParent(this);
        }
        this.routine = (strRoutine == null) ?
                new Routine(scriptures) :
                new Routine(scriptures, strRoutine);
        this.id = id;
        this.isScripture = isScripture;
	}

	public Book(String title, List<Scripture> scriptures, String routine,
            int id, boolean isScripture) {
        this(title, scriptures.toArray(new Scripture[scriptures.size()]),
                routine, id, isScripture);
    }

    public Book(String title, List<Scripture> scriptures) {
        this(title, scriptures, null, 0, false);
    }

    public Book(String title, Scripture[] scriptures) {
        this(title, scriptures, null, 0, false);
    }

    public Book(String title, Scripture scripture) {
        this(title, new Scripture[] { scripture });
    }

    public int getId() {
        return id;
    }

	public String getTitle() {
		return title;
	}
	
    public Scripture[] getScriptures() {
        return scriptures;
    }

    public Scripture getScripture(int index) {
        return scriptures[index];
    }

	@Override
	public String toString() {
		return "Book [title=" + title + ", scriptures.length="
				+ scriptures.length + "]";
	}

    public void createRoutine() {
        routine.newRoutine();
    }

    public Routine getRoutine() {
        return routine;
    }

    public boolean isScriptureMastery() {
        return isScripture;
    }
}
