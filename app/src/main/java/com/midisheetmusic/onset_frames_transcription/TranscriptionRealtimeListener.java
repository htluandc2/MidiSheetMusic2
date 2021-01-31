package com.midisheetmusic.onset_frames_transcription;

public interface TranscriptionRealtimeListener {

    public void onGetPianoRolls(float[][] pianoRolls, int initFrame);
//    public void onGetLastInferenceTime(long lastProcessingTimeMs);
}
