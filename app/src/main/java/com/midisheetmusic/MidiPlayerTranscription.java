package com.midisheetmusic;

import android.app.Activity;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;

import com.midisheetmusic.onset_frames_transcription.TranscriptionRealtimeStack;
import com.midisheetmusic.onset_frames_transcription.librosa.Librosa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class MidiPlayerTranscription extends MidiPlayer {

    private static final String LOG_TAG = MidiPlayerTranscription.class.getSimpleName();
    private static final long MININUM_TIME_BETWEEN_FRAME_MS = 30;

    // Queue to tracking piano events
    private static final int QUEUE_SIZE = 1024 * 8;
    private static BlockingQueue<String> queue = new ArrayBlockingQueue<String>(QUEUE_SIZE);

    private Handler trackingHandler;

    // All notes from all tracks
    private List<MidiNote> allNotes;
    private List<Integer> prevWrongNotes;
    private List<Integer> prevCorrectNotes;
    private int currentNoteIndex;
    private int currentNoteSize;

    boolean isTracking = false;
    private Thread trackingThread;
    private static int currentFrame = 0;

    private List<Integer> currentPressedNotes;

    private static final int CORRECT_COLOR = Color.rgb(102, 255, 102);
    private static final int WRONG_COLOR   = Color.RED;

    public MidiPlayerTranscription(Activity activity) {
        super(activity);

        currentNoteIndex = 0;
        currentNoteSize  = 0;

        trackingHandler = new Handler();
    }



    public void putEvents(String notes) {
        queue.add(notes);
    }

    public synchronized void startTracking() {
        ScrollToStart();
        updateCurrentNoteIndex(0);
        if(trackingThread != null) {
            return;
        }
        queue.clear();
        isTracking = true;
        trackingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                tracking();
            }});
        trackingThread.start();
    }

    public synchronized void stopTracking() {
        if(trackingThread == null) {
            return;
        }
        isTracking = false;
        trackingThread = null;
    }

    public synchronized void waitForTrackingStopped() {
        if(trackingThread == null) {
            return;
        }
        try {
            trackingThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void tracking() {
        while (isTracking) {
            try {
                Thread.sleep(MININUM_TIME_BETWEEN_FRAME_MS);
                if(queue.size() == 0) {
                    continue;
                }
                String notes = queue.take();
                trackingHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        OnMidiMultipleNotes(notes, true, currentFrame);
                        currentFrame++;
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        queue.clear();
    }


    // Remember: This function need + opt.transpose
    @Override
    public void SetMidiFile(MidiFile file, MidiOptions opt, SheetMusic s) {
        super.SetMidiFile(file, opt, s);
        allNotes = new ArrayList<MidiNote>();
        prevWrongNotes = new ArrayList<Integer>();
        prevCorrectNotes = new ArrayList<Integer>();

        currentPressedNotes = new ArrayList<Integer>();

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
            int notenumber = currentNote.getNumber() + options.transpose;
            String notename = Librosa.midiToNoteName(notenumber);
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
        updateCurrentNoteIndex(0);
    }

    private void updateCurrentNoteIndex(int currentNoteIndex) {
        this.currentNoteIndex = currentNoteIndex;
        this.currentNoteSize  = 1;
        int currentTime = allNotes.get(currentNoteIndex).getStartTime();
        for(int i = currentNoteIndex + 1; i < allNotes.size(); i++) {
            int noteTime = allNotes.get(i).getStartTime();
            if(noteTime == currentTime) {
                this.currentNoteSize++;
            }
        }
    }

    // Remember: This function need + opt.transpose
    /**
     * TODO: OnMidiMultipleNotes
     * Người dùng nhập 1 chuỗi các notes. Các note nhập sai sẽ sáng màu đỏ.
     * @param notes 1 string các note đã gõ dưới dạng midi index, ngăn cách với dấu space (Ví dụ "60 64 67").
     * @param pressed
     */
    public void OnMidiMultipleNotes(String notes, boolean pressed, int frame) {
        if(!pressed) return;
        currentPressedNotes.clear();

        // Parsing pressed notes
        try {
            for(String n: notes.split(" ")) {
                currentPressedNotes.add(Integer.parseInt(n));
            }
        } catch (Exception e) {
            // not do anything...
        }
        if(currentNoteIndex == allNotes.size()) {
            Log.d(LOG_TAG, "Done this song!!!");
            return;
        }
        if(currentPressedNotes.size() > 0) {
            unShadePrevWrongNotes();
        }

        boolean clearPrevCorrectNotes = true;
        int numberOfCorrectNotes = 0;
        for(int i = 0; i < currentPressedNotes.size(); i++) {
            boolean inCurrentNotes = false;
            for(int j = this.currentNoteIndex; j < this.currentNoteSize + this.currentNoteIndex; j++) {
                int notenumber = allNotes.get(j).getNumber() + options.transpose;
                if(currentPressedNotes.get(i) == notenumber) {
                    inCurrentNotes = true;
                    break;
                }
            }
            if(!inCurrentNotes) {
                piano.ShadeOneNote(currentPressedNotes.get(i), WRONG_COLOR);
                prevWrongNotes.add(currentPressedNotes.get(i));
            }
            if(inCurrentNotes) {
                if(clearPrevCorrectNotes) {
                    unShadePrevCorrectNotes();
                    clearPrevCorrectNotes = false;
                }
                piano.ShadeOneNote(currentPressedNotes.get(i), CORRECT_COLOR);
                prevCorrectNotes.add(currentPressedNotes.get(i));
                numberOfCorrectNotes++;
            }
        }

        boolean isCompleted = true;  // Đã gõ tất cả các note cùng onsets
        if(numberOfCorrectNotes == this.currentNoteSize) {
            isCompleted = true;
        }
        else {
            isCompleted = false;
        }

        printCurrentPressedNote(frame);
        if(isCompleted) {
            sheet.ShadeNotes((int) currentPulseTime, (int) prevPulseTime, SheetMusic.ImmediateScroll);
            prevPulseTime    = allNotes.get(currentNoteIndex).getStartTime();
            updateCurrentNoteIndex(this.currentNoteIndex + this.currentNoteSize);
            if(currentNoteIndex == allNotes.size()) {
                return;
            }
            currentPulseTime = allNotes.get(currentNoteIndex).getStartTime();
        }
    }

    private void unShadePrevCorrectNotes() {
        if(prevCorrectNotes == null) {
            return;
        }
        for(int i = 0; i < prevCorrectNotes.size(); i++) {
            piano.UnShadeOneNote(prevCorrectNotes.get(i));
        }
        prevCorrectNotes.clear();
    }

    private void unShadePrevWrongNotes() {
        if(prevWrongNotes == null) {
            return;
        }
        for(int i = 0; i < prevWrongNotes.size(); i++) {
            piano.UnShadeOneNote(prevWrongNotes.get(i));
        }
        prevWrongNotes.clear();
    }

    // Remember: This function need + opt.transpose
    private void printCurrentPressedNote(int frame) {
        String frameTime = Librosa.frameToTimestamp(frame,
                TranscriptionRealtimeStack.SAMPLE_RATE,
                TranscriptionRealtimeStack.HOP_SIZE);
        String results = "On frame: "  + frame + ", Current time: " + frameTime;
        results += "\nCurrent notes:";
        for(int j = currentNoteIndex; j < currentNoteSize + currentNoteIndex; j++) {
            int n = allNotes.get(j).getNumber() + options.transpose;
            results += Librosa.midiToNoteName(n) + " ";
        }
        results += "\nPressed notes:";
        for(int n: currentPressedNotes) {
            results += Librosa.midiToNoteName(n) + " ";
        }
        results +=  "\nWrong notes: ";
        for(int n: prevWrongNotes) {
            results += Librosa.midiToNoteName(n)+ " ";
        }
        results += "\nCorrect notes: ";
        for(int n: prevCorrectNotes) {
            results += Librosa.midiToNoteName(n) + " ";
        }
        results += "\n";
        Log.d(LOG_TAG, results);
    }

    public void PlayDemo() {
        RemoveShading();
        this.Play();
    }

    public void PauseDemo() {
        this.Pause();
    }

    @Override
    public void MoveToClicked(int x, int y) {
        if(isTracking) {
            return;
        }
        super.MoveToClicked(x, y);
    }

    @Override
    void ScrollToStart() {
        super.ScrollToStart();
        trackingHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                RemoveShading();
            }
        }, 200);

    }

    @Override
    public void RemoveShading() {
        super.RemoveShading();
        unShadePrevCorrectNotes();
        unShadePrevWrongNotes();
    }
}
