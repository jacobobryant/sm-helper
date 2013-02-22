package com.jacobobryant.scripturemastery;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;

import android.view.View;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;

public class NewPassageActivity extends Activity {
    private String newGroup;
    private Book[] books;

    private class GroupSelectionListener
            implements OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, 
                int pos, long id) {
            final View txtNewGroup = findViewById(R.id.txtNewGroup);
            if (parent.getItemAtPosition(pos).toString()
                    .equals(newGroup)) {
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

        newGroup = getString(R.string.newGroup);
        books = MainActivity.getBooks();
        for (Book book : books) {
            if (!book.wasPreloaded()) {
                titles.add(book.getTitle());
            }
        }
        if (titles.size() > 0) {
            ArrayAdapter<String> spnGroupAdapter =
                    new ArrayAdapter<String>(this,
                    android.R.layout.simple_spinner_item, titles);
            spnGroupAdapter.add(newGroup);
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
        Scripture passage;
        DataSource data;
        String group;
        int position;
        
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

        data = new DataSource(this);
        data.open();
        passage = new Scripture(txtTitle.getText().toString(), "",
                txtPassage.getText().toString());
        if (createGroup) {
            group = txtGroup.getText().toString();
            data.addGroup(new Book(group, passage));
        } else {
            position = spnGroup.getSelectedItemPosition();
            for (int i = 0; i <= position; i++) {
                if (books[i].wasPreloaded()) {
                    position++;
                }
            }
            passage.setParent(MainActivity.getBooks()[position]);
            data.addPassage(passage);
        }
        data.close();
        setResult(RESULT_OK);
        finish();
    }

    private boolean isEmpty(TextView tv) {
        return (tv.getText().toString().trim().length() == 0);
    }
}
