package com.midisheetmusic.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

import com.midisheetmusic.FileUri;
import com.midisheetmusic.MidiFile;
import com.midisheetmusic.MidiHandlingActivity;
import com.midisheetmusic.MidiNote;
import com.midisheetmusic.MidiOptions;
import com.midisheetmusic.MidiPlayer;
import com.midisheetmusic.MidiPlayerTranscription;
import com.midisheetmusic.MidiTrack;
import com.midisheetmusic.Piano;
import com.midisheetmusic.R;
import com.midisheetmusic.SheetMusic;
import com.midisheetmusic.TimeSigSymbol;
import com.midisheetmusic.onset_frames_transcription.Transciption;
import com.midisheetmusic.onset_frames_transcription.TranscriptionRealtimeListener;
import com.midisheetmusic.onset_frames_transcription.TranscriptionRealtimeStack;
import com.midisheetmusic.onset_frames_transcription.Utils;
import com.midisheetmusic.onset_frames_transcription.librosa.Librosa;
import com.midisheetmusic.sheets.ClefSymbol;
import com.mikepenz.materialdrawer.Drawer;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

public class TestActivity extends MidiHandlingActivity implements TranscriptionRealtimeListener {

    private static final int REQUEST_RECORD_AUDIO = 3;
    private static final String LOG_TAG = TestActivity.class.getSimpleName();

    public static final String MidiTitleID = "MidiTitleID";
    public static final int settingsRequestCode = 1;
    public static final int ID_LOOP_ENABLE = 10;
    public static final int ID_LOOP_START  = 11;
    public static final int ID_LOOP_END    = 12;

    private MidiPlayerTranscription player;      // The play/stop/rewind toolbar
    private Piano piano;            // The piano at the top
    private SheetMusic sheet;       // The sheet music
    private LinearLayout layout;    // The layout
    private MidiFile midiFile;      // The midi file to play
    private MidiOptions options;    // The options for sheet music and sound
    private long midiCRC;           // CRC of the midi bytes
    private Drawer drawer;

    // For transcription methods
    private Transciption transcription;
    private Button btnRecord;
    private Button btnStop;

    /**
     * Create this SheetMusicActivity.
     * The Intent should have 2 parameters:
     * - data: The uri of the midi file to open.
     * - MidiTitleID: The title of the song (String)
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hide the navigation bar before the views are laid out
        hideSystemUI();
        setContentView(R.layout.activity_test);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ClefSymbol.LoadImages(this);
        TimeSigSymbol.LoadImages(this);

        // Parse the MidiFile from the raw bytes
        Uri uri = this.getIntent().getData();
        if(uri == null) {
            this.finish();
            return;
        }
        String title = this.getIntent().getStringExtra(MidiTitleID);
        if(title == null) {
            title = uri.getLastPathSegment();
        }
        Log.d(LOG_TAG, "Music song:" + title);
        FileUri file = new FileUri(uri, title);
        byte[] data;
        data = file.getData(this);
        midiFile = new MidiFile(data, title);

        // MidiOptions: init the settings.
        // If previous settings have been saved, use those
        options = new MidiOptions(midiFile);
        CRC32 crc = new CRC32();
        crc.update(data);
        midiCRC = crc.getValue();
        SharedPreferences settings = getPreferences(0);
        options.scrollVert = settings.getBoolean("scrollVert", false);
        options.shade1Color= settings.getInt("shade1Color", options.shade1Color);
        options.shade2Color= settings.getInt("shade2Color", options.shade2Color);
        options.showPiano  = settings.getBoolean("showPiano", true);
        String json = settings.getString(""+midiCRC, null);
        Log.d(LOG_TAG, "Settings: " + json);
        MidiOptions savedOptions = MidiOptions.fromJson(json);

        createViews();

        // For transcription sound
        transcription = new TranscriptionRealtimeStack(this);
        transcription.setOnsetsFramesTranscriptionRealtimeListener(this);
        btnRecord = findViewById(R.id.btnRecord);
        btnStop   = findViewById(R.id.btnStop);

//        printMidiFile();
    }

    private void createViews() {
        layout = findViewById(R.id.content_layout_test);
        player = new MidiPlayerTranscription(this);

        piano  = new Piano(this);
        layout.addView(piano);
        player.SetPiano(piano);
        layout.requestLayout();

        createSheetMusic(options);
    }

    private void createSheetMusic(MidiOptions options) {
        if(sheet != null) {
            layout.removeView(sheet);
        }
        piano.setVisibility(options.showPiano ? View.VISIBLE : View.GONE);
        sheet = new SheetMusic(this);
        sheet.init(midiFile, options);
        sheet.setPlayer(player);
        layout.addView(sheet);
        piano.SetMidiFile(midiFile, options, player);
        piano.SetShadeColors(options.shade1Color, options.shade2Color);

        player.SetMidiFile(midiFile, options, sheet);
        layout.requestLayout();
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    public void OnMidiDeviceStatus(boolean connected) {

    }

    @Override
    public void OnMidiNote(int note, boolean pressed) {
    }

    public void onClickSimulation(View view) {
        int notes[] = new int[]{60, 64, 67};
        player.OnMidiMultipleNotes(notes, true, 0);
    }

    private void printMidiFile() {
        Log.d(LOG_TAG, "Midi Track: " + midiFile.getTracks().size());
        for(int i = 0; i < midiFile.getTracks().size(); i++) {
            MidiTrack track = midiFile.getTracks().get(i);
            String results = "Note in Track:\n";
            for(MidiNote note: track.getNotes()) {
                results += "\t";
                results += "Note in list: " + i + ", ";
                results += "Note number: " + note.getNumber() + ", ";
                results += "Note name: " + Librosa.midiToNoteName(note.getNumber()) + ", ";
                results += "Start: " + note.getStartTime() + "\t";
                results += "Duration: " + note.getDuration();
                results += "\n";
            }
            Log.d(LOG_TAG, results);
        }
    }

    protected void requestMicrophonePermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                    new String[] {Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
        }
    }

    @Override
    public void onGetPianoRolls(float[][] pianoRolls, int initFrame) {
        for(int i = 0; i < pianoRolls.length; i++) {
            List<Integer> noteList = new ArrayList<Integer>();
            for(int j = 0; j < pianoRolls[i].length; j++) {
                if(pianoRolls[i][j] > 0.5f) {
                    noteList.add(j + 21);
                }
            }
            if(noteList.size() == 0) {
                return;
            }
            int notes[] = new int[noteList.size()];
            for(int j = 0; j < noteList.size(); j++) {
                notes[j] = noteList.get(j);
            }
            player.OnMidiMultipleNotes(notes, true, initFrame+i);
        }
    }

    public void onClickButtons(View view) {
        if(view.getId() == R.id.btnRecord) {
            btnRecord.setEnabled(false);
            btnStop.setEnabled(true);
            requestMicrophonePermission();

            transcription.startRecording();
            transcription.startRecognition();
        }
        if(view.getId() == R.id.btnStop) {
            transcription.stopRecognition();
            transcription.stopRecording();

            btnRecord.setEnabled(true);
            btnStop.setEnabled(false);
        }
    }
}