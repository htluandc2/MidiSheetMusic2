package com.midisheetmusic;

import android.app.Activity;
import android.graphics.Color;
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
    // All notes from all tracks
    private List<MidiNote> notes;
    private List<Integer> prevWrongNotes;
    private int currentNoteIndex;

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
        prevWrongNotes = new ArrayList<Integer>();

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
        currentNoteIndex = 0;
    }

    /**
     * Người dùng nhập 1 chuỗi các notes. Các note nhập sai sẽ sáng màu đỏ.
     * @param notes
     * @param pressed
     */
    public void OnMidiMultipleNotes(int notes[], boolean pressed) {
        if(!pressed) return;
        for(int i = 0; i < prevWrongNotes.size(); i++) {
            piano.UnShadeOneNote(prevWrongNotes.get(i));
        }
        prevWrongNotes.clear();

        List<MidiNote> currentNotes = getCurrentNotes();
        boolean currentNotesBool[] = new boolean[200];
        for(MidiNote n: currentNotes) {
            currentNotesBool[n.getNumber()] = true;
        }

        for(int n: notes) {
            if(!currentNotesBool[n]) {
                piano.ShadeOneNote(n, Color.RED);
            }
        }
    }

    public List<MidiNote> getCurrentNotes() {
        int minStartTime = notes.get(currentNoteIndex).getStartTime();
        List<MidiNote> currentNotes = new ArrayList<MidiNote>();
        for(int i = currentNoteIndex; i < notes.size(); i++) {
            if (notes.get(i).getStartTime() == minStartTime) {
                currentNotes.add(notes.get(i));
            }
        }
        return currentNotes;
    }
}
