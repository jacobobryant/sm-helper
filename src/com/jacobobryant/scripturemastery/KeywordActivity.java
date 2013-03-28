package com.jacobobryant.scripturemastery;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import java.util.*;

public class KeywordActivity extends Activity
        implements View.OnClickListener {
    private final String CHOICES_KEY = "references";
    private final String SELECTED_KEY = "selected";
    private RadioGroup radioGroup;
    private String correctChoice;
    private TextView radSelected;
    private TextView btnChoose;
    private List<String> choices;
    Mode mode;

    private enum Mode {
        GUESS_REFERENCE(R.string.guess_reference),
        GUESS_KEYWORD(R.string.guess_keyword);

        private int id;

        private Mode(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.keyword_activity);
        RadioButton btn;
        String selectedRef;
        Intent intent = getIntent();
        int scripId = intent.getIntExtra(
                MainActivity.EXTRA_SCRIP_ID, -1);
        Scripture scrip = Scripture.objects(getApplication())
                .get(scripId);

        mode = (new Random().nextInt(2) == 0) ?
                Mode.GUESS_REFERENCE : Mode.GUESS_KEYWORD;
        radioGroup = (RadioGroup) findViewById(R.id.radio_group);
        btnChoose = (TextView) findViewById(R.id.choose_button);
        setTitle(getString(mode.getId()));
        ((TextView) findViewById(R.id.hint)).setText(
                (mode == Mode.GUESS_REFERENCE) ?
                scrip.getKeywords() : scrip.getReference());

        correctChoice = (mode == Mode.GUESS_REFERENCE) ?
                scrip.getReference() : scrip.getKeywords();
        // there appears to be a bug in the Bundle.get*() methods. They
        // shouldn't throw NullPointerExceptions, but they do.
        try {
            choices = state.getStringArrayList(CHOICES_KEY);
        } catch (NullPointerException e) {
            choices = null;
        }
        try {
            selectedRef = state.getString(SELECTED_KEY);
        } catch (NullPointerException e) {
            selectedRef = null;
        }
        if (choices == null) {
            choices = getChoices(scrip);
        }
        for (String choice : choices) {
            btn = new RadioButton(this);
            btn.setText(choice);
            btn.setOnClickListener(this);
            radioGroup.addView(btn);
            if (choice.equals(selectedRef)) {
                btn.toggle();
                btnChoose.setEnabled(true);
                radSelected = btn;
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        state.putStringArrayList(CHOICES_KEY,
                (ArrayList<String>) choices);
        if (radSelected != null) {
            state.putString(SELECTED_KEY, radSelected.getText()
                    .toString());
        }
        super.onSaveInstanceState(state);
    }

    public List<String> getChoices(Scripture scrip) {
        final int NUMCHOICES = 6;
        List<String> pool = new ArrayList<String>();
        Random rand = new Random();
        Context a = getApplication();

        for (Scripture scripture :
                scrip.getBook(a).getScriptures(a).all()) {
            if (scripture.getStatus() != Scripture.NOT_STARTED &&
                    ! scripture.getReference()
                    .equals(scrip.getReference())) {
                pool.add((mode == Mode.GUESS_REFERENCE) ?
                        scripture.getReference() : 
                        scripture.getKeywords());
            }
        }
        while (pool.size() >= NUMCHOICES) {
            pool.remove(rand.nextInt(pool.size()));
        }
        pool.add((mode == Mode.GUESS_REFERENCE) ?
                scrip.getReference() : scrip.getKeywords());
        Collections.shuffle(pool);
        return pool;
    }

    public void onClick(View v) {
        radSelected = (TextView) v;
        if (!btnChoose.isEnabled()) {
            btnChoose.setEnabled(true);
        }
    }

    public void btnChooseClick(View btnChoose) {
        if ((radSelected.getText().equals(correctChoice))) {
            Toast.makeText(this, R.string.right_reference,
                    Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            choices.remove(radSelected.getText());
            radioGroup.removeView(radSelected);
            this.btnChoose.setEnabled(false);
            Toast.makeText(this, R.string.wrong_reference,
                    Toast.LENGTH_SHORT).show();
        }
    }
}
