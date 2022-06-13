package com.midisheetmusic.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;

import com.midisheetmusic.R;
import com.midisheetmusic.adapter.HomeScreenAdapter;
import com.midisheetmusic.fragment.FavouriteFragment;
import com.midisheetmusic.fragment.HomeFragment;
import com.midisheetmusic.fragment.LibraryFragment;
import com.midisheetmusic.fragment.RecentFragment;

public class HomeScreenActivity extends AppCompatActivity {

    private ViewPager viewPagerHomeScreen;
    private HomeScreenAdapter homeScreenAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        viewPagerHomeScreen = findViewById(R.id.viewPagerHomeScreen);
        homeScreenAdapter = new HomeScreenAdapter(getSupportFragmentManager());
        homeScreenAdapter.addFragment(new HomeFragment());
        homeScreenAdapter.addFragment(new RecentFragment());
        homeScreenAdapter.addFragment(new FavouriteFragment());
        homeScreenAdapter.addFragment(new LibraryFragment());
        viewPagerHomeScreen.setAdapter(homeScreenAdapter);
    }
}