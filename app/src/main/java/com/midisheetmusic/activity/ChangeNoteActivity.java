package com.midisheetmusic.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import com.midisheetmusic.R;

public class ChangeNoteActivity extends AppCompatActivity {

    private int currentTime;
    private String noteStr;

    private EditText etChangeNotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_note);

        Intent i = getIntent();
        etChangeNotes = findViewById(R.id.etChangeNotes);
        currentTime = i.getIntExtra(TestActivity.ChangeNoteTime, -1);
        noteStr = i.getStringExtra(TestActivity.ChangeNoteName).trim();

        etChangeNotes.setText(noteStr);
    }



}