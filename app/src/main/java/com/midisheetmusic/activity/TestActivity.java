package com.midisheetmusic.activity;

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
import com.midisheetmusic.MidiOptions;
import com.midisheetmusic.MidiPlayer;
import com.midisheetmusic.MidiPlayerListener;
import com.midisheetmusic.MidiPlayerTranscription;
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

import java.util.zip.CRC32;

public class TestActivity extends MidiHandlingActivity
        implements TranscriptionRealtimeListener, MidiPlayerListener {

    private static final int REQUEST_RECORD_AUDIO = 3;
    private static final String LOG_TAG = TestActivity.class.getSimpleName();

    public static final String MidiTitleID = "MidiTitleID";

    private MidiPlayerTranscription player;      // The play/stop/rewind toolbar
    private Piano piano;            // The piano at the top
    private SheetMusic sheet;       // The sheet music
    private LinearLayout layout;    // The layout
    private MidiFile midiFile;      // The midi file to play
    private MidiOptions options;    // The options for sheet music and sound
    private long midiCRC;           // CRC of the midi bytes

    // For transcription methods
    private Transciption transcription;
    private Button btnRecord;
    private Button btnStop;
    private Button btnPlay;

    int APP_STATE = 0;
    final int INIT_STATE = 0; // Not run anything
    final int PLAY_STATE = 1;
    final int PAUSE_STATE = 2;
    final int TRACKING_STATE = 3;

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
        MidiOptions savedOptions = MidiOptions.fromJson(json);

        createViews();

        // For transcription sound
        transcription = new TranscriptionRealtimeStack(this);
        transcription.setOnsetsFramesTranscriptionRealtimeListener(this);
        btnRecord = findViewById(R.id.btnRecord);
        btnStop   = findViewById(R.id.btnStop);
        btnPlay   = findViewById(R.id.btnPlay);

        printMidiFile();
    }

    private void printMidiFile() {

    }

    private void createViews() {
        layout = findViewById(R.id.content_layout_test);
        player = new MidiPlayerTranscription(this);

        piano  = new Piano(this);
        layout.addView(piano);
        player.SetPiano(piano);
        layout.requestLayout();

        player.setMidiPlayerListener(this);

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
        player.startTracking();

        int c4 = Librosa.notenameToMidi("C4");
        String results = "";
        results += c4;
        results.trim(); // Xóa space ở 2 đầu
        player.putEvents(results);

        int g3 = Librosa.notenameToMidi("G3");
        results = "";
        results += g3;
        player.putEvents(results);

        results = "";
        results += c4;
        player.putEvents(results);

        results = "";
        results += g3;
        player.putEvents(results);

    }

    protected void requestMicrophonePermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                    new String[] {Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
        }
    }

    @Override
    public void onGetPianoRolls(float[][] pianoRolls, int initFrame) {
        Utils.printTranscriptionNote(LOG_TAG, pianoRolls, 0.5f, initFrame);
        for(int i = 0; i < pianoRolls.length; i++) {
            String notes = "";
            for(int j = 0; j < pianoRolls[i].length; j++) {
                if(pianoRolls[i][j] == 1.0f) {
                    notes += (j + 21) + " ";
                }
            }
            notes.trim();
            player.putEvents(notes);
        }
    }

    public void onClickButtons(View view) {
        if(view.getId() == R.id.btnRecord) {
            APP_STATE = TRACKING_STATE;
            btnPlay.setEnabled(false);

            btnRecord.setEnabled(false);
            btnStop.setEnabled(true);
            requestMicrophonePermission();

            transcription.startRecording();
            transcription.startRecognition();
            player.startTracking();
        }
        if(view.getId() == R.id.btnStop) {
            APP_STATE = INIT_STATE;

            transcription.stopRecognition();
            transcription.stopRecording();
            player.stopTracking();

            btnRecord.setEnabled(true);
            btnStop.setEnabled(false);
            btnPlay.setEnabled(true);
        }
    }

    public void onClickPlayButton(View view) {
        if(APP_STATE != PLAY_STATE && APP_STATE != TRACKING_STATE) {
            APP_STATE = PLAY_STATE;
            btnPlay.setText("Pause");
            player.PlayDemo();
            btnRecord.setEnabled(false);
            return;
        }
        if(APP_STATE == PLAY_STATE) {
            APP_STATE = PAUSE_STATE;
            btnPlay.setText("DEMO");
            player.PauseDemo();
            return;
        }
    }

    @Override
    public void OnMidiPlayerChangeState(int state) {
        String tag = "MidiPlayerState";
        if(state == MidiPlayer.stopped) {
            Log.d(tag, "Current state: STOPPED");
            btnRecord.setEnabled(true);
            btnPlay.setText("DEMO");
            player.RemoveShading();
        }
        if(state == MidiPlayer.playing) {
            Log.d(tag, "Current state: PLAYING");
        }
        if(state == MidiPlayer.paused) {
            Log.d(tag, "Current state: PAUSED");
            btnPlay.setText("DEMO");
            APP_STATE = PAUSE_STATE;
        }
        if(state == MidiPlayer.initStop) {
            Log.d(tag, "Current state: INIT_STOP");
        }
        if(state == MidiPlayer.initPause) {
            Log.d(tag, "Current state: INIT_PAUSE");
        }
        if(state == MidiPlayer.midi) {
            Log.d(tag, "Current state: MIDI");
        }
    }
}