package com.midisheetmusic.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.midisheetmusic.MidiFile;
import com.midisheetmusic.MidiNote;
import com.midisheetmusic.MidiOptions;
import com.midisheetmusic.MidiPlayer;
import com.midisheetmusic.MidiTrack;
import com.midisheetmusic.Piano;
import com.midisheetmusic.R;
import com.midisheetmusic.SheetMusic;
import com.midisheetmusic.onset_frames_transcription.Transciption;
import com.midisheetmusic.onset_frames_transcription.TranscriptionRealtimeListener;
import com.midisheetmusic.onset_frames_transcription.TranscriptionRealtimeStack;
import com.midisheetmusic.onset_frames_transcription.librosa.Librosa;


public class MainActivity extends AppCompatActivity implements TranscriptionRealtimeListener {

    private static final int REQUEST_RECORD_AUDIO = 3;
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private Button btnRecord;
    private Button btnShade;
    private Button btnStop;

    private MidiPlayer midiPlayer;
    private SheetMusic sheet;
    private Piano piano;
    private LinearLayout layout;
    private MidiFile midiFile;
    private MidiOptions options;
    private long midiCRC;

    private final String asset = "file:///android_asset/";
    private String midiPath = asset + "midi/4 - mozart_eine_kleine_easy.mid";

    private Transciption transciption;

    // Dùng để đánh dấu event tiếp theo cần phải tracking...
    private int nextNoteInTrack;
    private int prevPulseTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onGetPianoRolls(float[][] pianoRolls, int initFrame) {
        MidiTrack track = midiFile.getTracks().get(0);

        // Init min note
        MidiNote nextNote = track.getNotes().get(nextNoteInTrack);
        int nextTime = nextNote.getStartTime();

        for(int frame = 0; frame < pianoRolls.length; frame++) {
            int nNote = 0;
            int nNoteTyped = 0;
            for(int noteId = nextNoteInTrack; noteId < track.getNotes().size(); noteId++) {
                MidiNote note = track.getNotes().get(noteId);
                if(note.getStartTime() == nextTime) {
                    nNote += 1;
                }
            }
            Log.d(LOG_TAG, String.format("You have to type %d notes", nNote));
            for(int noteId = nextNoteInTrack; noteId < nextNoteInTrack + nNote; noteId++) {
                MidiNote note = track.getNotes().get(noteId);
                int noteIdx = Librosa.midiToNoteIndex(note.getNumber());
                if(pianoRolls[frame][noteIdx] > TranscriptionRealtimeStack.DETECTION_THRESHOLD) {
                    nNoteTyped += 1;
                }
            }
            Log.d(LOG_TAG, String.format("You typed %d notes", nNoteTyped));

            if(nNoteTyped == nNote) {
                shadeNote(nextNote);
                // Tìm next note
                nextNoteInTrack = nextNoteInTrack + nNote;
                if(nextNoteInTrack >= track.getNotes().size()) {
                    break;
                }
                else {
                    nextNote = track.getNotes().get(nextNoteInTrack);
                    nextTime = nextNote.getStartTime();
                }
            }
        }
    }

    @Override
    public void onStopRecording() {

    }

    @Override
    public void onStopRecognizing() {

    }

    private void hideSystemUI() {
        // Enables sticky immersive mode.
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    public void onClickShade(View view) {
        MidiTrack track = midiFile.getTracks().get(0);
        MidiNote note = track.getNotes().get(nextNoteInTrack);
        sheet.ShadeNotes(note.getStartTime(), prevPulseTime, SheetMusic.ImmediateScroll);
    }

    public void shadeNote(MidiNote note) {
        sheet.ShadeNotes(note.getStartTime(), prevPulseTime, SheetMusic.ImmediateScroll);
        prevPulseTime = note.getStartTime();
    }


    protected void requestMicrophonePermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                    new String[] {Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
        }
    }

    public void reset() {
        nextNoteInTrack = 0;
        prevPulseTime   = -10;
    }

    public void simulateShadeSheetMusic() {
        MidiTrack track = midiFile.getTracks().get(0);
        float[][] pianoRolls = new float[1][88];
        for(int i = 0; i < track.getNotes().size(); i++) {
            MidiNote note = track.getNotes().get(i);
            int noteIdx = Librosa.midiToNoteIndex(note.getNumber());
            pianoRolls[0][noteIdx] = 1.0f;

            onGetPianoRolls(pianoRolls, i);
            resetPianoRolls(pianoRolls);
        }
    }

    public void resetPianoRolls(float[][] pianoRolls) {
        for(int i = 0; i < pianoRolls.length; i++) {
            for(int j = 0; j < pianoRolls[0].length; j++) {
                pianoRolls[i][j] = 0.0f;
            }
        }
    }

    public void onClickSimulation(View view) {
        reset();
        simulateShadeSheetMusic();
    }

    public void onClickButtons(View view) {
        if(view.getId() == R.id.btnRecord) {
            btnRecord.setEnabled(false);
            btnStop.setEnabled(true);
            requestMicrophonePermission();

            transciption.startRecording();
            transciption.startRecognition();
        }
        if(view.getId() == R.id.btnStop) {
            transciption.stopRecognition();
            transciption.stopRecording();

            btnRecord.setEnabled(true);
            btnStop.setEnabled(false);
        }
    }

    public void onClickPlayButton(View view) {

    }
}