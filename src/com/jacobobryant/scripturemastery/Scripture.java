package com.jacobobryant.scripturemastery;

public class Scripture {
    public static final int NOT_STARTED = 0;
    public static final int IN_PROGRESS = 1;
    public static final int FINISHED = 2;
    public static final int MASTERED = 0;
    public static final int MEMORIZED = 1;
    public static final int PARTIALLY_MEMORIZED = 2;
    public static final int MAX_LEVEL = 6;
    public static final int WAIT = 1;
    public static final int MAX_IN_PROGRESS = WAIT + MAX_LEVEL + 1;
    private String reference;
    private String keywords;
    private String verses;
    private int status;
    private int finishedStreak;
    private int id;
    private Book parent;

    public Scripture(int id, String reference, String keywords,
            String verses, int status, int finishedStreak) {
        this.id = id;
        this.reference = reference;
        this.keywords = keywords;
        this.verses = verses;
        this.status = status;
        this.finishedStreak = finishedStreak;
    }

    public Scripture(String reference, String keywords, String verses) {
        this(0, reference, keywords, verses, NOT_STARTED, 0);
    }

	public String getReference() {
        return reference;
    }

    public String getVerses() {
        return verses;
    }

	public int getStatus() {
		return status;
	}

	public int getFinishedStreak() {
		return finishedStreak;
	}

    public void setProgress(int progress) {
        switch (progress) {
            case MASTERED:
                if (finishedStreak < WAIT + MAX_LEVEL) {
                    finishedStreak = WAIT + MAX_LEVEL;
                    status = FINISHED;
                }
                break;
            case MEMORIZED:
                finishedStreak++;
                if (finishedStreak > WAIT + MAX_LEVEL ) {
                    status = FINISHED;
                } else {
                    status = IN_PROGRESS;
                }
                break;
            case PARTIALLY_MEMORIZED:
                // decrement the starting level
                if (finishedStreak > 0) {
                    if (finishedStreak > WAIT + MAX_LEVEL) {
                        finishedStreak = WAIT + MAX_LEVEL - 1;
                    } else {
                        finishedStreak--;
                    }
                }
                status = IN_PROGRESS;
        }
    }

    public int getId() {
        return id;
    }

    public Book getParent() {
        return parent;
    }

    public void setParent(Book parent) {
        if (this.parent != null) {
            throw new UnsupportedOperationException();
        }
        this.parent = parent;
    }

    public int getStartLevel() {
        int level = finishedStreak - WAIT;
        if (level < 0) {
            level = 0;
        } else if (level > MAX_LEVEL) {
            level = MAX_LEVEL;
        }
        return level;
    }

    public boolean isNumbered() {
        return reference.matches(".*:\\d+.*");
    }

    public int getFirstVerse() {
        // ex: returns 4 if reference is "foo bar 12:4-6"
        try {
            return Integer.parseInt(
                    reference.replaceFirst(".+:(\\d+).*", "$1"));
        } catch (NumberFormatException e) {
            throw new UnsupportedOperationException(
                    "not a numbered scripture");
        }
    }

    public String getKeywords() {
        return keywords;
    }

    @Override
	public String toString() {
		return "Scripture [reference=" + reference + ", verses=" + verses
				+ ", status=" + status + ", finishedStreak=" + finishedStreak
				+ "]";
	}
}
