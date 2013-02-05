package com.jacobobryant.scripturemastery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.*;

public class KeywordActivity extends Activity
        implements View.OnClickListener {
    private final String REF_KEY = "references";
    private final String SELECTED_KEY = "selected";
    private RadioGroup radioGroup;
    private String correctReference;
    private TextView selected;
    private TextView choose;
    private List<String> references;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.keyword_activity);
        Scripture scrip = MainActivity.getScripture();
        RadioButton btn;
        String selectedRef;

        radioGroup = (RadioGroup) findViewById(R.id.radio_group);
        choose = (TextView) findViewById(R.id.choose_button);
        setTitle(R.string.keyword_activity_title);
        ((TextView) findViewById(R.id.keyword))
                .setText(scrip.getKeywords());

        correctReference = scrip.getReference();
        // there appears to be a bug in the Bundle.get*() methods. They
        // shouldn't throw NullPointerExceptions, but they do.
        try {
            references = state.getStringArrayList(REF_KEY);
        } catch (NullPointerException e) {
            references = null;
        }
        try {
            selectedRef = state.getString(SELECTED_KEY);
        } catch (NullPointerException e) {
            selectedRef = null;
        }
        if (references == null) {
            references = chooseReferences(scrip);
        }
        for (String ref : references) {
            btn = new RadioButton(this);
            btn.setText(ref);
            btn.setOnClickListener(this);
            radioGroup.addView(btn);
            if (ref.equals(selectedRef)) {
                btn.toggle();
                choose.setEnabled(true);
                selected = btn;
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        state.putStringArrayList(REF_KEY,
                (ArrayList<String>) references);
        if (selected != null) {
            state.putString(SELECTED_KEY, selected.getText().toString());
        }
        super.onSaveInstanceState(state);
    }

    public List<String> chooseReferences(Scripture scrip) {
        final int COUNT = 6;
        List<String> pool = new ArrayList<String>();
        Random rand = new Random();

        for (Scripture scripture : scrip.getParent().getScriptures()) {
            if (scripture.getStatus() != Scripture.NOT_STARTED &&
                    ! scripture.getReference()
                    .equals(scrip.getReference())) {
                pool.add(scripture.getReference());
            }
        }
        Collections.shuffle(pool);
        while (pool.size() >= COUNT) {
            pool.remove(rand.nextInt(pool.size()));
        }
        pool.add(rand.nextInt(pool.size() + 1), scrip.getReference());
        return pool;
    }

    public void onClick(View v) {
        selected = (TextView) v;
        if (!choose.isEnabled()) {
            choose.setEnabled(true);
        }
    }

    public void btnChooseClick(View btnChoose) {
        if ((selected.getText().equals(correctReference))) {
            Toast.makeText(this, R.string.right_reference,
                    Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            references.remove(selected.getText());
            radioGroup.removeView(selected);
            choose.setEnabled(false);
            Toast.makeText(this, R.string.wrong_reference,
                    Toast.LENGTH_SHORT).show();
        }
        Log.d(MainActivity.TAG, "" + references.size());
    }
}
