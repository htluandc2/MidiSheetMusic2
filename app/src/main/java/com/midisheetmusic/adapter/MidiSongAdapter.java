package com.midisheetmusic.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.midisheetmusic.R;
import com.midisheetmusic.model.MidiSong;

import java.util.List;

public class MidiSongAdapter extends RecyclerView.Adapter<MidiSongAdapter.MidiSongViewHolder> {

    private List<MidiSong> midiSongList;

    public MidiSongAdapter() {

    }


    public void setMidiSongList(List<MidiSong> midiSongList) {
        this.midiSongList = midiSongList;
    }

    @NonNull
    @Override
    public MidiSongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_midi_song, parent, false);
        return new MidiSongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MidiSongViewHolder holder, int position) {
        holder.idTextView.setText(String.valueOf(position + 1));
        if(position % 2 == 0) {
            holder.favouriteImageView.setImageResource(R.drawable.ic_favourite_star);
        }
        else {
            holder.favouriteImageView.setImageResource(R.drawable.ic_favourite_star_marked);
        }
    }

    @Override
    public int getItemCount() {
        return midiSongList.size();
    }

    public class MidiSongViewHolder extends RecyclerView.ViewHolder {

        public TextView idTextView;
        public ImageView favouriteImageView;
        public TextView midiSongTextView;
        public ImageView moreImageView;

        public MidiSongViewHolder(@NonNull View itemView) {
            super(itemView);

            idTextView = itemView.findViewById(R.id.idTextView);
            favouriteImageView = itemView.findViewById(R.id.favouriteImageView);
            midiSongTextView = itemView.findViewById(R.id.midiSongTextView);
            moreImageView = itemView.findViewById(R.id.moreImageView);
        }


    }

}
