package com.midisheetmusic.model;

public class MidiSong {

    private int id;
    private String midiFile;
    private boolean isFavourite;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMidiFile() {
        return midiFile;
    }

    public void setMidiFile(String midiFile) {
        this.midiFile = midiFile;
    }

    public boolean isFavourite() {
        return isFavourite;
    }

    public void setFavourite(boolean favourite) {
        isFavourite = favourite;
    }
}
