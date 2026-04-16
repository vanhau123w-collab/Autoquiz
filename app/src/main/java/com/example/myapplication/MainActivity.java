package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

public class MainActivity extends AppCompatActivity {

    private LinearLayout customBottomNav;
    private LinearLayout navHome, navCreate, navLibrary, navProfile;
    private ImageView iconHome, iconCreate, iconLibrary, iconProfile;
    private TextView textHome, textCreate, textLibrary, textProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        boolean darkModeEnabled = false;
        try {
            darkModeEnabled = sharedPref.getBoolean("DarkMode", false);
        } catch (Exception e) {
            sharedPref.edit().remove("DarkMode").apply();
        }
        
        if (darkModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind views
        customBottomNav = findViewById(R.id.custom_bottom_nav);
        navHome = findViewById(R.id.nav_home);
        navCreate = findViewById(R.id.nav_create);
        navLibrary = findViewById(R.id.nav_library);
        navProfile = findViewById(R.id.nav_profile);

        iconHome = findViewById(R.id.nav_home_icon);
        textHome = findViewById(R.id.nav_home_text);
        iconCreate = findViewById(R.id.nav_create_icon);
        textCreate = findViewById(R.id.nav_create_text);
        iconLibrary = findViewById(R.id.nav_library_icon);
        textLibrary = findViewById(R.id.nav_library_text);
        iconProfile = findViewById(R.id.nav_profile_icon);
        textProfile = findViewById(R.id.nav_profile_text);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
            updateNavState(navHome, iconHome, textHome);
        }

        navHome.setOnClickListener(v -> {
            loadFragment(new HomeFragment());
            updateNavState(navHome, iconHome, textHome);
        });

        navCreate.setOnClickListener(v -> {
            loadFragment(new CreateFragment());
            updateNavState(navCreate, iconCreate, textCreate);
        });

        navLibrary.setOnClickListener(v -> {
            loadFragment(new LibraryFragment());
            updateNavState(navLibrary, iconLibrary, textLibrary);
        });

        navProfile.setOnClickListener(v -> {
            switchToTab(3);
        });
    }

    public void switchToTab(int tabIndex) {
        if (tabIndex == 0) {
            loadFragment(new HomeFragment());
            updateNavState(navHome, iconHome, textHome);
        } else if (tabIndex == 1) {
            loadFragment(new CreateFragment());
            updateNavState(navCreate, iconCreate, textCreate);
        } else if (tabIndex == 2) {
            loadFragment(new LibraryFragment());
            updateNavState(navLibrary, iconLibrary, textLibrary);
        } else if (tabIndex == 3) {
            loadFragment(new SettingsFragment());
            updateNavState(navProfile, iconProfile, textProfile);
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void updateNavState(LinearLayout selectedLayout, ImageView selectedIcon, TextView selectedText) {
        // Begin smooth transition animation without fade
        androidx.transition.ChangeBounds transition = new androidx.transition.ChangeBounds();
        transition.setDuration(200);
        transition.setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator());
        TransitionManager.beginDelayedTransition(customBottomNav, transition);

        // Reset all to default
        resetNav(navHome, iconHome, textHome);
        resetNav(navCreate, iconCreate, textCreate);
        resetNav(navLibrary, iconLibrary, textLibrary);
        resetNav(navProfile, iconProfile, textProfile);

        // Set selected
        selectedLayout.setBackgroundResource(R.drawable.bg_nav_active);
        selectedIcon.setColorFilter(Color.WHITE);
        selectedText.setVisibility(View.VISIBLE);
        
        // Add padding change for smooth look
        selectedLayout.setPadding((int)(16 * getResources().getDisplayMetrics().density), 
                                  selectedLayout.getPaddingTop(), 
                                  (int)(16 * getResources().getDisplayMetrics().density), 
                                  selectedLayout.getPaddingBottom());
    }

    private void resetNav(LinearLayout layout, ImageView icon, TextView text) {
        layout.setBackgroundColor(Color.TRANSPARENT);
        icon.setColorFilter(Color.parseColor("#9CA3AF")); // secondary color
        text.setVisibility(View.GONE);
        layout.setPadding((int)(8 * getResources().getDisplayMetrics().density), 
                          layout.getPaddingTop(), 
                          (int)(8 * getResources().getDisplayMetrics().density), 
                          layout.getPaddingBottom());
    }
}