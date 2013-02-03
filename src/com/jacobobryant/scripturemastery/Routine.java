package com.jacobobryant.scripturemastery;

import java.util.*;

public class Routine {
    private LinkedList<Integer> routine;
    private Scripture[] scriptures;

    public Routine(Scripture[] scriptures, String strRoutine) {
        int index;
        this.scriptures = scriptures;
        routine = new LinkedList<Integer>();
        for (String element : strRoutine.split(",")) {
            index = Integer.parseInt(element);
            if (index < 0 || index >= scriptures.length) {
                throw new IndexOutOfBoundsException();
            }
            routine.add(index);
        }
    }

    public Routine(Scripture[] scriptures) {
        this.scriptures = scriptures;
        routine = new LinkedList<Integer>();
    }

    public void newRoutine() {
        final float REVIEW_PERCENT = 2.0f / 3;
        List<Integer> notStarted = getIndices(Scripture.NOT_STARTED);
        List<Integer> inProgress = getIndices(Scripture.IN_PROGRESS);
        List<Integer> finished = getIndices(Scripture.FINISHED);
        int index;
        int reviewCount = Math.round(REVIEW_PERCENT * finished.size());
        Random rand = new Random();

        routine.clear();
        routine.addAll(inProgress);
        if (notStarted.size() > 0 &&
                inProgress.size() < Scripture.MAX_IN_PROGRESS) {
            routine.add(notStarted.get(0));
        }
        for (int i = 0; i < reviewCount; i++) {
            index = rand.nextInt(finished.size());
            routine.add(finished.remove(index));
        }
    }

    private List<Integer> getIndices(int status) {
        List<Integer> indices = new LinkedList<Integer>();
        for (int i = 0; i < scriptures.length; i++) {
            if (scriptures[i].getStatus() == status) {
                indices.add(i);
            }
        }
        return indices;
    }

    public String toString(boolean humanReadable) {
        Iterator<Integer> iter = routine.iterator();
        StringBuilder ret = new StringBuilder();

        if (routine.size() == 0) {
            return null;
        }
        if (humanReadable) {
            while (iter.hasNext()) {
                if (ret.length() > 0) {
                    ret.append("\n");
                }
                ret.append(scriptures[iter.next().intValue()]
                        .getReference());
            }
        } else {
            while (iter.hasNext()) {
                if (ret.length() > 0) {
                    ret.append(",");
                }
                ret.append(iter.next().intValue());
            }
        }
        return ret.toString();
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public int length() {
        return routine.size();
    }

    public Scripture current() {
        return scriptures[routine.element()];
    }

    public void moveToNext() {
        routine.remove();
    }
}
