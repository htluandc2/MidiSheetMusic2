package com.midisheetmusic.onset_frames_transcription;

import android.os.Process;
import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.midisheetmusic.onset_frames_transcription.file.PCMUtils;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class TranscriptionRealtimeStack implements Transciption {

    public static final int N_FRAMES = 32;

    public static final int SAMPLE_RATE = 16_000;
    public static final int RECORDING_LENGTH      = (N_FRAMES + 3) * 512;
    public static final float DETECTION_THRESHOLD = 0.9f;
    public static final float SILENT_THRESHOLD    = 0.2f; // Threshold to remove silent frame
    public static final long MINIMUM_TIME_BETWEEN_SAMPLES_MS = 20;

    public static final int HOP_SIZE         = 512;
    public static final int INPUT_WAV_LENGTH = (N_FRAMES + 3) * 512;
    public static final int WINDOW_LENGTH    = 2048;

    // Queue for Multi-threads.
    private static final int QUEUE_SIZE = 256 * 512;
    private static BlockingQueue<Short> queue = new ArrayBlockingQueue<Short>(QUEUE_SIZE);

    // Queue for Recognition Thread.
    private static final int OVERLAP_TIMESTEPS  = 4; // overlap chuẩn là 4.
    private static final int OVERLAP_WAV_LENGTH = HOP_SIZE * OVERLAP_TIMESTEPS + WINDOW_LENGTH;

    private static final String STACK_MODEL_FILENAME = String.format("file:///android_asset/stack-32.tflite");

    private static final String LOG_TAG = TranscriptionRealtimeStack.class.getSimpleName();

    boolean shouldContinue = false;
    private Thread recordingThread;
    boolean shouldContinueRecognition = true;
    private Thread recognitionThread;

    private Interpreter tfLiteModel;

    private long lastProcessingTimeMs;

    private Activity activity;
    private TranscriptionRealtimeListener listener;

    /**
     * TODO: Load model from runtime...
     * @param activity
     */
    public TranscriptionRealtimeStack(Activity activity) {
        this.activity = activity;
        String actualModelFilename = STACK_MODEL_FILENAME.split("file:///android_asset/", -1)[1];

        AssetManager assetManager = activity.getAssets();
        try {
            tfLiteModel = new Interpreter(loadModelFile(assetManager, actualModelFilename));
            tfLiteModel.allocateTensors();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Load TF-Lite file failed.");
            e.printStackTrace();
        }
    }

    public void setOnsetsFramesTranscriptionRealtimeListener(
            TranscriptionRealtimeListener listener) {
        this.listener = listener;
    }


    /** Memory-map the model file in Assets.*/
    private static MappedByteBuffer loadModelFile(
            AssetManager assets, String modelFilename) throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public synchronized void startRecording() {
        if(recordingThread != null) {
            return;
        }
        queue.clear();
        shouldContinue = true;
        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                record();
            }
        });
        recordingThread.start();
    }

    public synchronized void stopRecording() {
        if(recordingThread == null) {
            return;
        }
        shouldContinue = false;
        recordingThread = null;
    }

    private void record() {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

        // Estimate the buffer size we 'll need for this device.
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if(bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }

        short[] audioBuffer = new short[bufferSize / 2];

        AudioRecord record = new AudioRecord(
                MediaRecorder.AudioSource.DEFAULT, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        if(record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "Audio Record can't initialize!");
            return;
        }

        record.startRecording();

        Log.v(LOG_TAG, "Start recording...");
        // PCM file for TEMP_FILE...
        PCMUtils pcm = new PCMUtils();
        // Loop, gathering audio data and copying it to a round-robin buffer.
        while (shouldContinue) {
            int numberRead = record.read(audioBuffer, 0, audioBuffer.length);
            for(int i = 0; i < audioBuffer.length; i++) {
                queue.add(audioBuffer[i]);
            }
            // Write to TEMP_FILE
            pcm.writeAudioDataToFile(audioBuffer);
        }

        pcm.closePCMFIle();
        pcm.convertToWavFile();
        Log.d(LOG_TAG, "Saved temp_file.wav");

        record.stop();
        record.release();

        queue.clear();
    }

    public synchronized void startRecognition() {
        if(recognitionThread != null) {
            return;
        }

        shouldContinueRecognition = true;
        recognitionThread = new Thread(new Runnable() {
            @Override
            public void run() {
                recognize();
            }
        });
        recognitionThread.start();
    }

    public synchronized void stopRecognition() {
        if(recognitionThread == null) {
            return;
        }
        shouldContinueRecognition = false;
        recognitionThread = null;
    }


    private static float[][] resultRolls = new float[N_FRAMES-OVERLAP_TIMESTEPS-1][88];
    private void recognize() {
        Log.v(LOG_TAG, "Start recognition...");

        // Input of Models
        short[] inputBuffer      = new short[RECORDING_LENGTH];
        float[][] floatInputBuffer = new float[1][RECORDING_LENGTH];
        // Output of Models
        float[][][] pianoRolls = new float[1][N_FRAMES][88];
        int initFrame = 0;

        // Loop, grabbing recorded data and running the recognition model on it.
        int currentPoint = 0;
        while(shouldContinueRecognition) {
            if(queue.size() < OVERLAP_WAV_LENGTH) {
                try {
                    Thread.sleep(MINIMUM_TIME_BETWEEN_SAMPLES_MS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            while(currentPoint < INPUT_WAV_LENGTH) {
                try {
                    inputBuffer[currentPoint] = queue.take();
                    currentPoint++;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if(currentPoint < RECORDING_LENGTH) {
                continue;
            }
            // We need to feed in float values between -1.0f and 1.0f, so divide the
            // signed 16-bit inputs.
            for(int i = 0; i < RECORDING_LENGTH; i++) {
                floatInputBuffer[0][i] = inputBuffer[i] / 32767.0f;
            }

            if(energy(floatInputBuffer[0]) < SILENT_THRESHOLD) {
                // This is a silent frame.
                removeAllPianoRolls(pianoRolls[0]);
            }
            else {
                long startTime = new Date().getTime();
                tfLiteModel.run(floatInputBuffer, pianoRolls);
                lastProcessingTimeMs = new Date().getTime() - startTime;
                Log.d(LOG_TAG, String.format("Time processing %d ms", lastProcessingTimeMs));

                threshPianoRolls(pianoRolls[0]);
                removeDuplicateRolls(pianoRolls[0]);
            }
            copyPianoRollsToResult(pianoRolls[0]);

            // Draw results on UI
            int finalInitFrame = initFrame;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listener.onGetPianoRolls(resultRolls, finalInitFrame);
                }
            });
            System.arraycopy(inputBuffer, INPUT_WAV_LENGTH - OVERLAP_WAV_LENGTH,
                    inputBuffer, 0, OVERLAP_WAV_LENGTH);
            currentPoint = OVERLAP_WAV_LENGTH;

            initFrame += resultRolls.length;
        }
    }

    private void copyPianoRollsToResult(float[][] pianoRolls) {
        for(int i = 0; i < resultRolls.length; i++) {
            for(int j = 0; j < pianoRolls[0].length; j++) {
                resultRolls[i][j] = pianoRolls[i][j];
            }
        }
    }

    private void threshPianoRolls(float[][] pianoRolls) {
        for(int i = 0; i < pianoRolls.length; i++) {
            for(int j = 0; j < pianoRolls[0].length; j++) {
                if(pianoRolls[i][j] > DETECTION_THRESHOLD) {
                    pianoRolls[i][j] = 1.0f;
                }
                else {
                    pianoRolls[i][j] = 0.0f;
                }
            }
        }
    }

    private void removeAllPianoRolls(float[][] pianoRolls) {
        for(int i = 0; i < pianoRolls.length; i++) {
            for(int j = 0; j < pianoRolls[0].length; j++) {
                pianoRolls[i][j] = 0.0f;
            }
        }
    }

    private void removeDuplicateRolls(float[][] pianoRolls) {
        // TODO: Remove duplicate in resultRolls
        for(int i = 0; i < pianoRolls.length; i++) {
            for(int j = 0; j < pianoRolls[0].length; j++) {
                if(i - 2 < 0) {
                    continue;
                }
                if(i - 1 < 0) {
                    continue;
                }
                if((pianoRolls[i - 1][j] == 1.0f) && (pianoRolls[i][j] == 1.0f)) {
                    pianoRolls[i][j] = 0.0f;
                }
                if((pianoRolls[i -2][j] == 1.0f) && (pianoRolls[i][j] == 1.0f)) {
                    pianoRolls[i][j] = 0.0f;
                }
            }
        }
    }

    private float energy(float[] inputBuffer) {
        float x = 0.0f;
        for(int i = 0; i < inputBuffer.length; i++) {
            x += inputBuffer[i] * inputBuffer[i];
        }
        return (float) Math.sqrt(x);
    }
}
