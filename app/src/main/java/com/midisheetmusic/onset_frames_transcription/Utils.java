package com.midisheetmusic.onset_frames_transcription;

import android.util.Log;

import com.midisheetmusic.onset_frames_transcription.librosa.Librosa;

import java.io.File;

public class Utils {

    private static final String LOG_TAG = Utils.class.getSimpleName();

    public static void printTranscriptionNote(
            String LOG_TAG,
            float[][] pianoRolls,
            float threshold,
            int initFrame,
            boolean isSeparate) {
        int minNote = 15;   // C2
        int maxNote = 51;   // C5

        String results = String.format("Current frame: %d\n", initFrame);
        for(int i = 0; i < pianoRolls.length; i++) {
            int sr = TranscriptionRealtimeStack.SAMPLE_RATE;
            int hopSize = TranscriptionRealtimeStack.HOP_SIZE;
            String frameStr = Librosa.frameToTimestamp(initFrame + i, sr, hopSize);
            frameStr += "-";

            float[] frame = pianoRolls[i];
            for(int j = minNote; j <= maxNote; j++) {
                float note = frame[j];
                String noteStr = "";
                if(note > threshold) {
                    noteStr = notename(j, false);
                    if(noteStr.length() == 2) {
                        noteStr += " ";
                    }
                }
                else {
                    noteStr = "|  ";
                }
                frameStr += noteStr;
            }
            results += frameStr;
            results += "\n";
        }
        Log.d(LOG_TAG, results);
    }

    public static void printTranscriptionNoteToFile(File file,
                                                    float[][] pianoRolls,
                                                    float threshold,
                                                    int initFrame,
                                                    boolean isSeparate) {

    }

    public static String notename(int n, boolean space) {
        if(space) {
            String spaces[] = new String[12];
            for(int i = 0; i < spaces.length; i++) {
                spaces[i] = " ";
            }
            return spaces[n % 12];
        }
        String[] note = {"A", "A#", "B", "C",
                "C#", "D", "D#", "E",
                "F", "F#", "G", "G#"};
        int octave = (n - 3) / 12 + 1;
        if(n < 3)
            octave = 0;
        return note[n % 12] + octave;
    }
}
