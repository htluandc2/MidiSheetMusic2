package com.midisheetmusic.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.midisheetmusic.R;
import com.midisheetmusic.adapter.MidiSongAdapter;
import com.midisheetmusic.model.MidiSong;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView midiSongRecyclerViewInFragmentHome;
    private MidiSongAdapter midiSongAdapter;
    private List<MidiSong> midiSongList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        midiSongList = new ArrayList<MidiSong>();
        for(int i = 0; i < 10; i++) {
            midiSongList.add(new MidiSong());
        }
        midiSongAdapter = new MidiSongAdapter();
        midiSongAdapter.setMidiSongList(midiSongList);

        midiSongRecyclerViewInFragmentHome = view.findViewById(R.id.midiSongRecyclerViewInFragmentHome);
        midiSongRecyclerViewInFragmentHome.setAdapter(midiSongAdapter);
        midiSongRecyclerViewInFragmentHome.setLayoutManager(new LinearLayoutManager(view.getContext()));
        return view;
    }


}
