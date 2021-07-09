package com.example.instagram.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;

import com.example.instagram.R;
import com.example.instagram.fragments.ComposeFragment;
import com.example.instagram.fragments.PostsFragment;
import com.example.instagram.fragments.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.parse.ParseUser;

import org.jetbrains.annotations.NotNull;

// This activity is only going to serve as a navigation screen. It will host the different Fragments
public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity"; // Tag for log messages
    public static final String LAUNCH_PF_KEY = "ProfileFragment"; // Key to launch ProfileFragment (this is used when an authors username or profile pic is clicked)
    public static final String USER_KEY = "user"; // key to receive and send users from messaging objects

    // Object responsible of adding, removing or replacing Fragments in the stack
    final FragmentManager fragmentManager = getSupportFragmentManager();

    // View that allows movement between primary destinations in app
    private BottomNavigationView bottomNavigationView;

    // Set day/night mode views
    RelativeLayout rlMode; // RelativeLayout that contains the switch component
    private SwitchCompat switchUIMode; // switch component to change UI mode
    private SharedPreferences sharedPreferences;

    // Create new fragment (each different fragment extends this class)
    Fragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set custom toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Get current user
        ParseUser currentUser = ParseUser.getCurrentUser();

        // Change night mode setup
        rlMode = findViewById(R.id.rlMode);
        switchUIMode = findViewById(R.id.switchMode);
        // Check preferences (settings) when the activity is created
        sharedPreferences = getSharedPreferences("night", 0);
        Boolean booleanValue = sharedPreferences.getBoolean("night_mode", true);
        // Change to night mode depending on boolean
        if (booleanValue) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            switchUIMode.setChecked(true);
        }

        // Create listener for the switch component
        switchUIMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // Change style to night mode
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    switchUIMode.setChecked(true);
                    // Edit preferences to save new style
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("night_mode", true);
                    editor.commit();
                } else {
                    // Change style to night mode
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    switchUIMode.setChecked(false);
                    // Edit preferences to save new style
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("night_mode", false);
                    editor.commit();
                }
            }
        });

        // Assign bottom navigation bar from layout
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        // Create listener for bottomNavigationView items
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull @NotNull MenuItem item) {
                // Check which item was selected using ids
                switch (item.getItemId()) {
                    case R.id.action_compose: // Launch compose fragment to create a post
                        fragment = new ComposeFragment();
                        rlMode.setVisibility(View.INVISIBLE); // visibility of relative layout (ui mode can only be changed from PostsFragment)
                        break;
                    case R.id.action_profile: // Launch profile fragment (for current user)
                        // Setup bundle to pass currentUser
                        Bundle bundle = new Bundle();
                        bundle.putParcelable(USER_KEY, currentUser);
                        // Assign fragment with new args
                        fragment = new ProfileFragment();
                        fragment.setArguments(bundle);
                        // Change rlMode visibility
                        rlMode.setVisibility(View.INVISIBLE);
                        break;
                    default: // By default, go to main feed
                        fragment = new PostsFragment();
                        rlMode.setVisibility(View.VISIBLE);
                        break;
                }
                // Change to selected fragment using manager
                fragmentManager.beginTransaction().replace(R.id.flContainer, fragment).commit();
                return true;
            }
        });
        // Set home option as default
        bottomNavigationView.setSelectedItemId(R.id.action_home);

        // Check if this activity was called by DetailsActivity : this happens when user clicks on author post username or photo
        // since MainActivity hosts ProfileFragment
        if (getIntent().getBooleanExtra(LAUNCH_PF_KEY, false)) { // checking bool value
            // Set change mode views visibility
            rlMode.setVisibility(View.INVISIBLE);
            // Get user from intent
            ParseUser user = (ParseUser) getIntent().getParcelableExtra(USER_KEY);
            // Setup bundle to pass user as arguments
            Bundle bundle = new Bundle();
            bundle.putParcelable(USER_KEY, user);
            // Create new fragment and save arguments (this is a way of passing info from activities to fragments)
            Fragment fragment = new ProfileFragment();
            fragment.setArguments(bundle);
            // Change to Profile fragment using manager
            fragmentManager.beginTransaction().replace(R.id.flContainer, fragment).commit();
        }
    }
}