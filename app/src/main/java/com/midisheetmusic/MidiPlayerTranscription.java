package com.midisheetmusic;

import android.app.Activity;
import android.os.SystemClock;
import android.util.Log;

import com.midisheetmusic.onset_frames_transcription.librosa.Librosa;
import com.midisheetmusic.sheets.ChordSymbol;
import com.midisheetmusic.sheets.MusicSymbol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MidiPlayerTranscription extends MidiPlayer {

    private static final String LOG_TAG = MidiPlayerTranscription.class.getSimpleName();
    private int numWrongNotes = 0;
    // All notes from all tracks
    private List<MidiNote> notes;

    public MidiPlayerTranscription(Activity activity) {
        super(activity);
        startTime = SystemClock.uptimeMillis();
        startPulseTime   = 0;
        currentPulseTime = 0;
        prevPulseTime    = -10;
    }

    @Override
    public void SetMidiFile(MidiFile file, MidiOptions opt, SheetMusic s) {
        super.SetMidiFile(file, opt, s);
        notes = new ArrayList<MidiNote>();
        for(MidiTrack track: midifile.getTracks()) {
            for(MidiNote note: track.getNotes()) {
                notes.add(note);
            }
        }
        Collections.sort(notes, new MidiNote(0, 0, 0, 0));
        String results = "Onset Events:\n";
        int currentStartTime = notes.get(0).getStartTime();
        for(int i = 0; i < notes.size(); i++) {
            MidiNote currentNote = notes.get(i);
            String notename = Librosa.midiToNoteName(currentNote.getNumber());
            if(currentNote.getStartTime() > currentStartTime) {
                results += "\n";
                results += String.format("%s(start=%d, dur=%d)\t\t", notename,
                        currentNote.getStartTime(), currentNote.getDuration());
                currentStartTime = currentNote.getStartTime();
            }
            else {
                results += String.format("%s(start=%d, dur=%d)\t\t", notename,
                        currentNote.getStartTime(), currentNote.getDuration());
            }
        }
        Log.d(LOG_TAG, results);
    }

    public void OnMidiMultipleNotes(int notes[], boolean pressed) {
        if(!pressed) return;

    }
}
