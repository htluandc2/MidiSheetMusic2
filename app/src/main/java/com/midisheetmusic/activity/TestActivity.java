package com.midisheetmusic.activity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
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
import com.midisheetmusic.MidiPlayerListener;
import com.midisheetmusic.MidiPlayerTranscription;
import com.midisheetmusic.MidiTrack;
import com.midisheetmusic.Piano;
import com.midisheetmusic.R;
import com.midisheetmusic.SettingsActivity;
import com.midisheetmusic.SheetMusic;
import com.midisheetmusic.TimeSigSymbol;
import com.midisheetmusic.onset_frames_transcription.Transciption;
import com.midisheetmusic.onset_frames_transcription.TranscriptionRealtimeListener;
import com.midisheetmusic.onset_frames_transcription.TranscriptionRealtimeStack;
import com.midisheetmusic.onset_frames_transcription.Utils;
import com.midisheetmusic.onset_frames_transcription.file.PCMUtils;
import com.midisheetmusic.onset_frames_transcription.librosa.Librosa;
import com.midisheetmusic.sheets.ClefSymbol;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.CRC32;

public class TestActivity extends MidiHandlingActivity
        implements TranscriptionRealtimeListener, MidiPlayerListener {

    public static final String MidiTitleID = "MidiTitleID";
    public static final String ChangeNoteName = "ChangeNoteName";
    public static final String ChangeNoteTime = "ChangeNoteTime";
    public static final int settingsRequestCode   = 1;
    public static final int changeNoteRequestCode = 2;

    private static final int REQUEST_RECORD_AUDIO = 3;
    private static final String LOG_TAG = TestActivity.class.getSimpleName();

    // Use a custom class for tracking notes
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
    private Button btnChange;
    private boolean isStopRecording, isStopReconizing, isChangingNote;

    int APP_STATE = 0;
    final int INIT_STATE = 0; // Not run anything
    final int PLAY_STATE = 1;
    final int PAUSE_STATE = 2;
    final int TRACKING_STATE = 3;

    // For playback recording file
    private Button btnPlayback;
    private MediaPlayer playBackPlayer;
    private static List<String> pianoRollsForPlayback = new ArrayList<>(100_000);

    /**
     * Create this SheetMusicActivity.
     * The Intent should have 2 parameters:
     * - data: The uri of the midi file to open.
     * - MidiTitleID: The title of the song (String)
     *
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
        if (uri == null) {
            this.finish();
            return;
        }
        String title = this.getIntent().getStringExtra(MidiTitleID);
        if (title == null) {
            title = uri.getLastPathSegment();
        }
        Log.d(LOG_TAG, "Music song:" + title);
        FileUri file = new FileUri(uri, title);
        byte[] data;
        data = file.getData(this);
        midiFile = new MidiFile(data, title);
        CRC32 crc = new CRC32();
        crc.update(data);
        midiCRC = crc.getValue();

        makeDefaultOptions();
        createViews();

        // For transcription sound
        transcription = new TranscriptionRealtimeStack(this);
        transcription.setOnsetsFramesTranscriptionRealtimeListener(this);
        btnRecord = findViewById(R.id.btnRecord);
        btnStop = findViewById(R.id.btnStop);

        isStopReconizing = false;
        isStopRecording = false;
        isChangingNote = false;

        btnPlay = findViewById(R.id.btnPlay);

        // For playback sound
        btnPlayback = findViewById(R.id.btnPlayback);
        playBackPlayer = new MediaPlayer();
    }

    /**
     * Make default options from saved options and default options
     * TODO: Hiện tại còn thiếu các settings:
     * - Save as Images.
     * - Select Tracks to Play.
     * - Select Instruments for each Track.
     */
    private void makeDefaultOptions() {
        // MidiOptions: init the settings.
        // If previous settings have been saved, use thos
        options = new MidiOptions(midiFile);
        SharedPreferences settings = getPreferences(0);
        options.scrollVert = settings.getBoolean("scrollVert", false);
        options.playMeasuresInLoop = settings.getBoolean("playMeasuresInLoop", false);
        options.showLyrics = settings.getBoolean("showLyrics", true);
        options.showNoteLetters = settings.getInt("showNoteLetters", MidiOptions.NoteNameLetter);
        options.transpose = settings.getInt("transpose", 0);
        options.midiShift = settings.getInt("midiShift", 0);
        int orange = Color.rgb(255, 165, 0);
        int blue   = Color.BLUE;
        options.shade1Color = settings.getInt("shade1Color", orange);
        options.shade2Color = settings.getInt("shade2Color", blue);
        options.showPiano = settings.getBoolean("showPiano", true);
        options.useColors  = settings.getBoolean("useColors", true);

        String json = settings.getString("" + midiCRC, null);
        MidiOptions savedOptions = MidiOptions.fromJson(json);
        if(savedOptions != null) {
            options.merge(savedOptions);
        }
    }

    private void createViews() {
        layout = findViewById(R.id.content_layout_test);
        player = new MidiPlayerTranscription(this);

        piano = new Piano(this);
        layout.addView(piano);
        player.SetPiano(piano);
        layout.requestLayout();

        player.setMidiPlayerListener(this);

        createSheetMusic(options);
    }

    private void createSheetMusic(MidiOptions options) {
        if (sheet != null) {
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

    // For testing "track" function in MidiPlayerTranscription
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
        }
    }

    @Override
    public void onGetPianoRolls(float[][] pianoRolls, int initFrame) {
        Utils.printTranscriptionNote(LOG_TAG, pianoRolls, 0.5f, initFrame);
        for (int i = 0; i < pianoRolls.length; i++) {
            String notes = "";
            for (int j = 0; j < pianoRolls[i].length; j++) {
                if (pianoRolls[i][j] == 1.0f) {
                    notes += (j + 21) + " ";
                }
            }
            notes.trim();
            player.putEvents(notes);
            pianoRollsForPlayback.add(notes);
        }
    }

    @Override
    public void onStopRecording() {
    }

    @Override
    public void onStopRecognizing() {
    }

    public void onClickRecord(View view) {
        if (view.getId() == R.id.btnRecord) {
            requestMicrophonePermission();
            pianoRollsForPlayback.clear();

            APP_STATE = TRACKING_STATE;
            btnPlay.setEnabled(false);
            btnRecord.setEnabled(false);
            btnStop.setEnabled(true);
            btnPlayback.setEnabled(false);

            transcription.startRecording();
            transcription.startRecognition();
            player.startTracking();
        }
        if (view.getId() == R.id.btnStop) {
            APP_STATE = INIT_STATE;

            transcription.stopRecognition();
            transcription.stopRecording();
            player.stopTracking();

            transcription.waitForRecognitionStopped();
            transcription.waitForRecordingStopped();
            player.waitForTrackingStopped();

            btnRecord.setEnabled(true);
            btnStop.setEnabled(false);
            btnPlay.setEnabled(true);
            btnPlayback.setEnabled(true);
        }
    }

    public void onClickPlayBack(View view) {
        btnRecord.setEnabled(false);
        btnPlay.setEnabled(false);
        btnPlayback.setEnabled(false);
        PlaySound();
    }

    private void PlaySound() {
        player.RemoveShading();
        if (playBackPlayer == null) {
            return;
        }
        try {
            FileInputStream input = new FileInputStream(PCMUtils.wavTempFile);
            playBackPlayer.reset();
            playBackPlayer.setDataSource(input.getFD());
            input.close();
            playBackPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    btnRecord.setEnabled(true);
                    btnPlay.setEnabled(true);
                    btnPlayback.setEnabled(true);
                }
            });
            playBackPlayer.prepare();
            playBackPlayer.start();
            player.startTracking();
            for (int i = 0; i < pianoRollsForPlayback.size(); i++) {
                String notes = pianoRollsForPlayback.get(i);
                player.putEvents(notes);
            }
        } catch (IOException e) {
            Log.e("Playback", e.getMessage());
        }

    }

    public void onClickPlay(View view) {
        // On pause state
        if (APP_STATE != PLAY_STATE && APP_STATE != TRACKING_STATE) {
            APP_STATE = PLAY_STATE;
            btnPlay.setText("Pause");
            player.PlayDemo();
            btnRecord.setEnabled(false);
            btnStop.setEnabled(false);
            btnPlayback.setEnabled(false);
            return;
        }
        if (APP_STATE == PLAY_STATE) {
            APP_STATE = PAUSE_STATE;
            btnPlay.setText("Demo");
            player.PauseDemo();
            return;
        }
    }

    @Override
    public void OnMidiPlayerChangeState(int state) {
        String tag = "MidiPlayerState";
        if (state == MidiPlayer.stopped) {
            Log.d(tag, "Current state: STOPPED");
            btnRecord.setEnabled(true);
            btnPlay.setText("DEMO");
        }
        if (state == MidiPlayer.playing) {
            Log.d(tag, "Current state: PLAYING");
        }
        if (state == MidiPlayer.paused) {
            Log.d(tag, "Current state: PAUSED");
            btnPlay.setText("DEMO");
            APP_STATE = PAUSE_STATE;
        }
        if (state == MidiPlayer.initStop) {
            Log.d(tag, "Current state: INIT_STOP");
        }
        if (state == MidiPlayer.initPause) {
            Log.d(tag, "Current state: INIT_PAUSE");
        }
        if (state == MidiPlayer.midi) {
            Log.d(tag, "Current state: MIDI");
        }
    }


    public void onClickSettings(View view) {
        changeSettings();
    }

    private void changeSettings() {
        MidiOptions defaultOptions = new MidiOptions(midiFile);
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra(SettingsActivity.settingsID, options);
        intent.putExtra(SettingsActivity.defaultSettingsID, defaultOptions);
        startActivityForResult(intent, settingsRequestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == settingsRequestCode) {
            options = (MidiOptions)
                    intent.getSerializableExtra(SettingsActivity.settingsID);

            // Check whether the default instruments have changed.
            for (int i = 0; i < options.instruments.length; i++) {
                if (options.instruments[i] !=
                        midiFile.getTracks().get(i).getInstrument()) {
                    options.useDefaultInstruments = false;
                }
            }

            saveOptions();

            // Recreate the sheet music with the new options
            createSheetMusic(options);
        }
        if(requestCode == changeNoteRequestCode) {
            String noteStr = intent.getStringExtra(ChangeNoteActivity.ChangeNoteName);
            int currentTime= intent.getIntExtra(ChangeNoteActivity.ChangeNoteTime, -1);
            String newNotes[] = noteStr.split("\\s+");
            if(newNotes.length == 0 || currentTime < 0) {
                return;
            }

            int currentNewNote = 0;
            for(MidiTrack track: midiFile.getTracks()) {
                ArrayList<MidiNote> notes = track.getNotes();
                for(MidiNote note: notes) {
                    if(note.getStartTime() == currentTime) {
                        int newNoteNumber = Librosa.notenameToMidi(newNotes[currentNewNote]);
                        note.setNumber(newNoteNumber);
                        currentNewNote++;
                    }
                }
            }
            createSheetMusic(options);
        }
    }

    private void saveOptions() {
        SharedPreferences.Editor editor = getPreferences(0).edit();
        editor.putBoolean("scrollVert", options.scrollVert);
        editor.putInt("shade1Color", options.shade1Color);
        editor.putInt("shade2Color", options.shade2Color);
        editor.putBoolean("showPiano", options.showPiano);
        for (int i = 0; i < options.noteColors.length; i++) {
            editor.putInt("noteColor" + i, options.noteColors[i]);
        }
        String json = options.toJson();
        if (json != null) {
            editor.putString("" + midiCRC, json);
        }
        editor.apply();
    }

    @Override
    protected void onStop() {
        super.onStop();

        player.stopTracking();
        transcription.stopRecognition();
        transcription.stopRecording();

        player.waitForTrackingStopped();
        transcription.waitForRecognitionStopped();
        transcription.waitForRecordingStopped();
    }

    public void onClickChange(View view) {
        Intent intent = new Intent(this, ChangeNoteActivity.class);
        String note_str = "";
        int n_track = midiFile.getTracks().size();
        for(int track = 0; track < midiFile.getTracks().size(); track++) {
            ArrayList<MidiNote> notes = midiFile.getTracks().get(track).getNotes();
            for(MidiNote note: notes) {
                if(note.getStartTime() == sheet.getCurrentTime()) {
                    note_str += Librosa.midiToNoteName(note.getNumber()) + " ";
                }
            }
        }
        intent.putExtra(ChangeNoteName, note_str);
        intent.putExtra(ChangeNoteTime, sheet.getCurrentTime());
        Log.d("SheetMusic.java", note_str);
        startActivityForResult(intent, changeNoteRequestCode);
    }
}