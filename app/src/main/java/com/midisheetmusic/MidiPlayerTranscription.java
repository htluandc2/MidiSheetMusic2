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
    private List<MidiNote> allNotes;
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
        allNotes = new ArrayList<MidiNote>();
        prevWrongNotes = new ArrayList<Integer>();

        for(MidiTrack track: midifile.getTracks()) {
            for(MidiNote note: track.getNotes()) {
                allNotes.add(note);
            }
        }
        Collections.sort(allNotes, new MidiNote(0, 0, 0, 0));
        String results = "Onset Events:\n";
        int currentStartTime = allNotes.get(0).getStartTime();
        for(int i = 0; i < allNotes.size(); i++) {
            MidiNote currentNote = allNotes.get(i);
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
        if(currentNotes.size() == 0) {
            Log.d(LOG_TAG, "Done a song!!!");
        }

        boolean isWrong[] = new boolean[notes.length];
        for(int i = 0; i < notes.length; i++) {
            boolean inCurrentNotes = false;
            for(int j = 0; j < currentNotes.size(); j++) {
                if(notes[i] == currentNotes.get(j).getNumber()) {
                    inCurrentNotes = true;
                }
            }
            isWrong[i] = !inCurrentNotes;
            if(isWrong[i]) {
                piano.ShadeOneNote(notes[i], Color.RED);
                prevWrongNotes.add(notes[i]);
            }
        }

        boolean isPressed[] = new boolean[currentNotes.size()];
        boolean isCompleted = true;  // Đã gõ tất cả các note cùng onsets
        for(int i = 0; i < currentNotes.size(); i++) {
            for(int j = 0; j < notes.length; j++) {
                if(currentNotes.get(i).getNumber() == notes[j]) {
                    isPressed[i] = true;
                }
                else {
                    isCompleted = false;
                }
            }
        }
        if(isCompleted) {
            sheet.ShadeNotes((int) currentPulseTime, (int) prevPulseTime, SheetMusic.ImmediateScroll);

            currentNoteIndex = getNextNoteIndex();
            prevPulseTime = currentNotes.get(0).getStartTime();
            currentPulseTime = allNotes.get(currentNoteIndex).getStartTime();
        }
    }

    public List<MidiNote> getCurrentNotes() {
        int minStartTime = allNotes.get(currentNoteIndex).getStartTime();
        List<MidiNote> currentNotes = new ArrayList<MidiNote>();
        for(int i = currentNoteIndex; i < allNotes.size(); i++) {
            if (allNotes.get(i).getStartTime() == minStartTime) {
                currentNotes.add(allNotes.get(i));
            }
        }
        return currentNotes;
    }

    public int getNextNoteIndex() {
        int minStartTime = allNotes.get(currentNoteIndex).getStartTime();
        List<MidiNote> currentNotes = new ArrayList<MidiNote>();
        for(int i = currentNoteIndex; i < allNotes.size(); i++) {
            if (allNotes.get(i).getStartTime() == minStartTime) {
                currentNotes.add(allNotes.get(i));
            }
        }
        return currentNoteIndex + currentNotes.size();
    }
}
