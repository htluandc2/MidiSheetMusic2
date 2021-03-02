package com.midisheetmusic.onset_frames_transcription;

public interface Transciption {

    public void setOnsetsFramesTranscriptionRealtimeListener(
            TranscriptionRealtimeListener listener);

    public void startRecording();
    public void stopRecording();
    public void waitForRecordingStopped();

    public void startRecognition();
    public void stopRecognition();
    public void waitForRecognitionStopped();
}
