package com.example.instagram.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.instagram.R;
import com.example.instagram.fragments.ComposeFragment;
import com.example.instagram.fragments.PostsFragment;
import com.example.instagram.fragments.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.jetbrains.annotations.NotNull;
import org.parceler.Parcels;

// This activity is only going to serve as a navigation screen. It will host the different Fragments
public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity"; // Tag for log messages
    public static final String LAUNCH_PF_KEY = "ProfileFragment"; // Key to launch ProfileFragment (this is used when an authors username or profile pic is clicked)
    public static final String USER_KEY = "user"; // key to receive and send users from messaging objects

    // Object responsible of adding, removing or replacing Fragments in the stack
    final FragmentManager fragmentManager = getSupportFragmentManager();

    // View that allows movement between primary destinations in app
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set custom toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get current user
        ParseUser currentUser = ParseUser.getCurrentUser();

        // Check if this activity was called by DetailsActivity : this happens when user clicks on author post username or photo
        // since MainActivity hosts ProfileFragment
        if (getIntent().getBooleanExtra(LAUNCH_PF_KEY, false)) { // checking bool value
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

        // Assign bottom navigation bar from layout
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        // Create listener for bottomNavigationView items
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull @NotNull MenuItem item) {
                // Create new fragment (each different fragment extends this class)
                Fragment fragment;
                // Check which item was selected using ids
                switch (item.getItemId()) {
                    case R.id.action_home: // Launch main feed fragment
                        fragment = new PostsFragment();
                        break;
                    case R.id.action_compose: // Launch compose fragment to create a post
                        fragment = new ComposeFragment();
                        break;
                    case R.id.action_profile: // Launch profile fragment (for current user)
                        // Setup bundle to pass currentUser
                        Bundle bundle = new Bundle();
                        bundle.putParcelable(USER_KEY, currentUser);
                        // Assign fragment with new args
                        fragment = new ProfileFragment();
                        fragment.setArguments(bundle);
                        break;
                    default: // By default, go to main feed
                        fragment = new PostsFragment();
                        break;
                }
                // Change to selected fragment using manager
                fragmentManager.beginTransaction().replace(R.id.flContainer, fragment).commit();
                return true;
            }
        });
        // Set home option as default
        bottomNavigationView.setSelectedItemId(R.id.action_home);

    }

}