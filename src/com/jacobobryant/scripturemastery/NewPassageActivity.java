package com.jacobobryant.scripturemastery;

import android.app.Activity;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;

import java.util.ArrayList;
import java.util.List;

public class NewPassageActivity extends Activity {
    private List<Integer> bookIds;

    private class GroupSelectionListener
            implements OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, 
                int pos, long id) {
            final View txtNewGroup = findViewById(R.id.txtNewGroup);
            if (pos == parent.getCount() - 1) {
                txtNewGroup.setVisibility(View.VISIBLE);
                txtNewGroup.requestFocus();
            } else {
                txtNewGroup.setVisibility(View.GONE);
            }
        }

        public void onNothingSelected(AdapterView<?> parent) { }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_passage_activity);
        final Spinner spnGroup = (Spinner) findViewById(R.id.spnGroup);
        final View txtNewGroup = findViewById(R.id.txtNewGroup);
        List<String> titles = new ArrayList<String>();
        bookIds = new ArrayList<Integer>();

        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        for (Book book : Book.objects(getApplication()).all()) {
            if (!book.getPreloaded()) {
                bookIds.add(book.getId());
                titles.add(book.getTitle());
            }
        }
        if (titles.size() > 0) {
            ArrayAdapter<String> spnGroupAdapter =
                    new ArrayAdapter<String>(this,
                    android.R.layout.simple_spinner_item, titles);
            spnGroupAdapter.add(getString(R.string.new_group));
            spnGroupAdapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item);
            spnGroup.setAdapter(spnGroupAdapter);
            spnGroup.setOnItemSelectedListener(
                    new GroupSelectionListener());
        } else {
            spnGroup.setVisibility(View.GONE);
            txtNewGroup.setVisibility(View.VISIBLE);
        }
    }

    public void btnSaveClick(View v) {
        final Spinner spnGroup = (Spinner) findViewById(R.id.spnGroup);
        final EditText txtGroup =
                (EditText) findViewById(R.id.txtNewGroup);
        final EditText txtTitle = (EditText) findViewById(R.id.txtTitle);
        final EditText txtPassage =
                (EditText) findViewById(R.id.txtPassage);
        boolean errors = false;
        boolean createGroup = txtGroup.getVisibility() == View.VISIBLE;
        Book group;
        Scripture passage;
        int bookPosition;
        int scripPosition;
        
        if (createGroup && isEmpty(txtGroup)) {
            txtGroup.setError(getString(R.string.noGroupError));
            errors = true;
        }
        if (isEmpty(txtTitle)) {
            txtTitle.setError(getString(R.string.noTitleError));
            errors = true;
        }
        if (isEmpty(txtPassage)) {
            txtPassage.setError(getString(R.string.noPassageError));
            errors = true;
        }
        if (errors) {
            return;
        }

        passage = new Scripture(txtTitle.getText().toString(), null,
                txtPassage.getText().toString());
        if (createGroup) {
            group = new Book();
            group.setTitle(txtGroup.getText().toString());
            bookPosition = Book.objects(getApplication()).count();
            group.setPosition(bookPosition);
            group.save(getApplication());
        } else {
            bookPosition = spnGroup.getSelectedItemPosition();
            group = Book.objects(getApplication())
                    .get(bookIds.get(bookPosition));
        }
        scripPosition = group.getScriptures(getApplication()).count();
        passage.setBook(group);
        passage.setPosition(scripPosition);
        passage.save(getApplication());
        setResult(RESULT_OK);
        finish();
    }

    private boolean isEmpty(TextView tv) {
        return (tv.getText().toString().trim().length() == 0);
    }
}
