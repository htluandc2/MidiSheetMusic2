package com.midisheetmusic.onset_frames_transcription.librosa;

import com.midisheetmusic.onset_frames_transcription.TranscriptionRealtimeStack;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Librosa {

    public static String notename(int n) {
        boolean space = false;
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



    public static String midiToNoteName(int pitch) {
        int noteIndex = pitch - 21;
        return notename(noteIndex);
    }

    public static int midiToNoteIndex(int pitch) {
        return pitch - 21;
    }

    public static int notenameToNoteIndex(String notenameStr) {
        for(int i = 0; i < 88; i++) {
            String notenameStrFromNoteIndex = notename(i);
            if(notenameStr.equals(notenameStrFromNoteIndex)) {
                return i;
            }
        }
        return -1;
    }

    public static int notenameToMidi(String notenameStr) {
        return notenameToNoteIndex(notenameStr) + 21;
    }

    public static int timeToFrame(int time, int SAMPLE_RATE) {
        return 0;
    }

    /**
     * Nhập vào frame index, trả về time tương ứng.
     * Ví dụ: cứ 32 miliseconds sẽ là 1 frame nếu
     * @param frameIndex
     * @return time (in miliseconds)
     */
    public static int frameToTime(int frameIndex, int sr, int hopSize) {
        int samplesize  = hopSize * frameIndex;
        int miliseconds = (samplesize * 1000 / sr) ;
        return miliseconds;
    }

    public static String frameToTimestamp(int frameIndex, int sr, int hopSize) {
        int samplesize  = hopSize * frameIndex;
        int miliseconds = (samplesize * 1000 / sr) ;
        Date date = new Date(miliseconds);
        return (new SimpleDateFormat("mm:ss:SSS")).format(date);
    }
}
