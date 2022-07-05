package com.midisheetmusic.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import com.midisheetmusic.R;
import com.midisheetmusic.onset_frames_transcription.librosa.Librosa;

import java.text.NumberFormat;

public class ChangeNoteActivity extends AppCompatActivity
        implements TextWatcher {

    private int currentTime;
    private String noteStr;
    private int nNotes;
    private boolean valid;

    private EditText etChangeNotes;
    private TextView tvMessage;

    public static final String ChangeNoteName = "ChangeNoteName";
    public static final String ChangeNoteTime = "ChangeNoteTime";

    private final static String LOG_TAG = "ChangeNoteActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_note);

        etChangeNotes = findViewById(R.id.etChangeNotes);
        tvMessage = findViewById(R.id.tvMessage);

        Intent i = getIntent();

        currentTime = i.getIntExtra(TestActivity.ChangeNoteTime, -1);
        noteStr = i.getStringExtra(TestActivity.ChangeNoteName).trim();
        nNotes = noteStr.split("\\s+").length;
        valid = true;

        etChangeNotes.setText(noteStr);
        etChangeNotes.addTextChangedListener(this);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        if(!valid) {
            intent.putExtra(ChangeNoteName, "");
        }
        else {
            intent.putExtra(ChangeNoteName, etChangeNotes.getText().toString().trim());
        }
        intent.putExtra(ChangeNoteTime, currentTime);
        setResult(Activity.RESULT_OK, intent);
        super.onBackPressed();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String newNoteStr = etChangeNotes.getText().toString().trim();
        String notes[] = newNoteStr.split("\\s+");
        if(notes.length != nNotes) {
            tvMessage.setText("Invalid number of notes.");
            valid = false;
            return;
        }
        for(String n: notes) {
            int noteId = Librosa.notenameToNoteIndex(n);
            if(noteId == -1) {
                tvMessage.setText("Invalid note: " + n);
                valid = false;
                return;
            }
        }
        valid = true;
        tvMessage.setText(" ");
    }

    @Override
    public void afterTextChanged(Editable s) {

    }
}