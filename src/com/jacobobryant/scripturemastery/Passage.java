package com.jacobobryant.scripturemastery;

import android.graphics.Paint;
import android.os.Bundle;
import android.util.FloatMath;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Passage {
    public static final String HIDE_ORDER = "hideOrder";
    public static final String LEVEL = "level";
    private final int HIDDEN_STATES = 2; // HIDDEN and MOSTLY_HIDDEN
    private Paragraph[] paragraphs;
    private Paint textPaint;
    private boolean hintActive;
    private int level;
    private int[] hideOrder;
    private int position;
    private int positionIncrement;

    private class Paragraph {
        private Word[] words;
        private String prefix;
        public final int length;

        public class Word {
            public static final int VISIBLE = 0;
            public static final int MOSTLY_HIDDEN = 1;
            public static final int HIDDEN = 2;
            private int visible;
            private String text;

            public Word(String text) {
                this.text = text;
                visible = VISIBLE;
            }

             void setVisible(int visible) {
                switch (visible) {
                    case VISIBLE:
                    case MOSTLY_HIDDEN:
                    case HIDDEN:
                        this.visible = visible;
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            }

            @Override
            public String toString() {
                StringBuilder word = new StringBuilder();
                float width;
                float originalWidth = textPaint.measureText(text);
                float halfIncrementWidth =
                        textPaint.measureText("_") / 2;

                if (hintActive == true || visible == VISIBLE) {
                    return text;
                } else if (visible == HIDDEN) {
                    // Ensures that we don't return an empty string.
                    word.append("_");
                } else {            // state == MOSTLY_HIDDEN
                    // get the first character of the word plus any
                    // preceding non-letter characters.
                    int position = 0;
                    String character;
                    do {
                        if (position == text.length()) {
                            break;
                        }
                        character = text.substring(position, ++position);
                        word.append(character);
                    } while (!character.matches("\\w"));
                }
                do {
                    width = textPaint.measureText(word.toString());
                    if (width + halfIncrementWidth > originalWidth) {
                        break;
                    }
                    word.append("_");
                } while (true);
                return word.toString();
            }

            public void hideMore() {
                setVisible(visible + 1);
            }
        }

        public Paragraph(String text, String prefix) {
            String[] words = text.split(" ");
            this.words = new Word[words.length];
            for (int i = 0; i < words.length; i++) {
                this.words[i] = new Word(words[i]);
            }
            this.prefix = prefix;
            length = this.words.length;
        }

        public Word getWord(int index) {
            return words[index];
        }

        @Override
        public String toString() {
            StringBuilder text = new StringBuilder(prefix);
            if (words.length > 0) {
                text.append(words[0]);
                for (int i = 1; i < words.length; i++) {
                    text.append(" ");
                    text.append(words[i]);
                }
            }
            return text.toString();
        }
    }

    public Passage(Scripture scripture, Paint textPaint, Bundle bundle) {
        this(scripture, textPaint,
                bundle.getIntArray(HIDE_ORDER),
                bundle.getInt(LEVEL));
    }

    public Passage(Scripture scripture, Paint textPaint) {
        this(scripture, textPaint, calcHideOrder(scripture),
                scripture.getStartLevel());
    }

    private Passage(Scripture scripture, Paint textPaint, int[] hideOrder,
            int startLevel) {
        String[] lines = scripture.getVerses().split("\n");
        int wordCount = 0;
        String prefix = "";

        this.paragraphs = new Paragraph[lines.length];
        for (int i = 0; i < lines.length; i++) {
            if (scripture.isNumbered()) {
                prefix = (scripture.getFirstVerse() + i) + ". ";
            }
            this.paragraphs[i] = new Paragraph(lines[i], prefix);
            wordCount += this.paragraphs[i].length;
        }
        this.positionIncrement = (int) FloatMath.ceil(
                (wordCount * HIDDEN_STATES) / (float) Scripture.NUM_LEVELS);
        this.textPaint = textPaint;
        this.hideOrder = hideOrder;
        this.position = 0;
        this.level = 0;
        this.hintActive = false;
        for (int i = 0; i < startLevel; i++) {
            increaseLevel();
        }
    }

    private static int[] calcHideOrder(Scripture scripture) {
        int[] hideOrder;
        int wordCount = 0;
        List<Integer> indices;
        Random rand = new Random();
        int index;

        for (String line : scripture.getVerses().split("\n")) {
            wordCount += line.split(" ").length;
        }
        hideOrder = new int[wordCount];
        indices = new ArrayList<Integer>(wordCount);
        for (int i = 0; i < wordCount; i++) {
            indices.add(i);
        }
        for (int i = 0; i < hideOrder.length; i++) {
            // indices.size() == wordCount - i
            index = rand.nextInt(wordCount - i);
            hideOrder[i] = indices.remove(index);
        }
        return hideOrder;
    }

    public Bundle getBundle() {
        Bundle bundle = new Bundle();
        bundle.putIntArray(HIDE_ORDER, hideOrder);
        bundle.putInt(LEVEL, level);
        return bundle;
    }

    public String[] getParagraphs() {
        String[] ret = new String[paragraphs.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = paragraphs[i].toString();
        }
        return ret;
    }

    public void setHintActive(boolean hintActive) {
        this.hintActive = hintActive;
    }

    public boolean hintActive() {
        return hintActive;
    }

    public void increaseLevel() {
        int nextPosition = position + positionIncrement;
        int maxPosition = hideOrder.length * HIDDEN_STATES;
        if (! hasMoreLevels()) {
            throw new UnsupportedOperationException();
        }
        if (nextPosition > maxPosition) {
            nextPosition = maxPosition;
        }
        for (int i = position; i < nextPosition; i++) {
            getWord(i).hideMore();
        }
        position = nextPosition;
        level++;
    }

    private Paragraph.Word getWord(int hideOrderIndex) {
        int wordsCounted = 0;
        // make hideOrderIndex wrap, since we want to iterate through
        // hideOrder more than once (words aren't fully hidden after
        // the first pass).
        int wordIndex = hideOrder[hideOrderIndex % hideOrder.length];
        int paraIndex;

        for (paraIndex = 0; paraIndex < paragraphs.length;
                paraIndex++) {
            if (wordsCounted + paragraphs[paraIndex].length >
                    wordIndex) {
                wordIndex -= wordsCounted;
                break;
            }
            wordsCounted += paragraphs[paraIndex].length;
        }
        return paragraphs[paraIndex].getWord(wordIndex);
    }

    public boolean hasMoreLevels() {
        return (level < Scripture.NUM_LEVELS);
    }
}
