package com.midisheetmusic.onset_frames_transcription.file;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class PCMUtils {

    private static final String LOG_TAG = PCMUtils.class.getSimpleName();

    private static final String pcmTempFile = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + "/temp_file.pcm";
    public static final String wavTempFile = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + "/temp_file.wav";

    private FileOutputStream os = null;

    public PCMUtils() {
        Log.d(LOG_TAG, "PCM temp file path: " + pcmTempFile);
        Log.d(LOG_TAG, "WAV temp file path: " + wavTempFile);

        openPCMFile();
    }

    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }

    public void openPCMFile() {
        try {
            os = new FileOutputStream(pcmTempFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void closePCMFIle() {
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeAudioDataToFile(short[] sData) {
        byte bData[] = short2byte(sData);
        try {
            os.write(bData, 0, bData.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void convertToWavFile() {
        File pcmFile = new File(pcmTempFile);
        File wavFile = new File(wavTempFile);

        try {
            WavUtils.PCMToWAV(pcmFile, wavFile,
                    1, 16000, 16);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
