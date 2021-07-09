package com.example.instagram.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.instagram.helpers.BitmapScaler;
import com.example.instagram.models.Post;
import com.example.instagram.adapters.ProfilePostsAdapter;
import com.example.instagram.R;
import com.example.instagram.helpers.EndlessRecyclerViewScrollListener;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ProfileFragment extends Fragment {

    public static final String TAG = "ProfileFragment"; // TAG for log messages
    public static final String USER_KEY = "user"; // key to receive and send users from messaging objects

    // Codes for GET_CONTENT action
    public final static int PICK_PHOTO_CODE = 1046;

    // User object
    ParseUser profileUser;

    // Views
    ImageView ivProfile;
    TextView tvUsername;
    Button btnUpload, btnLogout;
    RecyclerView rvPosts; // View group to display user's posts

    // HELPERS
    SwipeRefreshLayout swipeContainer; // handles refresh action
    EndlessRecyclerViewScrollListener scrollListener; // handles endless scrolling (adds new posts to the RV)

    Date oldestDate; // Member variable to store the date of the oldest post (this is used in the query for new posts when the user scrolls)

    // Posts variables
    List<Post> allPosts; // model to save posts
    ProfilePostsAdapter adapter; // class to bind data with views

    GridLayoutManager gridLayoutManager;  // manager for RV (also required for scrollListener)

    // Required empty public constructor
    public ProfileFragment() {}

    // REQUIRED METHODS

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Get post's author from bundle
        Bundle bundle = this.getArguments();
        if(bundle != null) {
            profileUser = bundle.getParcelable("user");
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // initialize the array that will hold posts and create a ProfilePostsAdapter
        allPosts = new ArrayList<>();
        adapter = new ProfilePostsAdapter(getContext(), allPosts);

        // Set views from specified layout
        setViews(view);
        // Bind profile and post
        populateViews();
        // Set listeners for upload and logout buttons
        setClickListeners();
        // Check if profileUser is current user
        verifyUser();

        // Setup RecyclerView
        rvPosts.setAdapter(adapter);
        // Set the layout manager on the recycler view
        gridLayoutManager = new GridLayoutManager(getContext(), 3); // use 3 columns for grid
        rvPosts.setLayoutManager(gridLayoutManager);
        // query posts from database
        queryPosts();

        // Enable refresh feature
        setRefreshFeature(view);
        // Enable endless scrolling
        setEndlessScrollingFeature();
    }

    // VIEWS METHODS

    private void setViews(View view) {
        // User info
        ivProfile = view.findViewById(R.id.ivProfile);
        tvUsername = view.findViewById(R.id.tvUsername);
        // Interactions
        btnUpload = view.findViewById(R.id.btnUpload);
        btnLogout = view.findViewById(R.id.btnLogout);
        // Recycler view
        rvPosts = view.findViewById(R.id.rvPosts);
    }

    private void populateViews() {
        ParseFile profileImage = (ParseFile) profileUser.get("profileImage");
        Glide.with(getContext())
                .load(profileImage.getUrl())
                .circleCrop()
                .into(ivProfile);
        tvUsername.setText(profileUser.getUsername());
    }

    // Checks if profileUser is the same as profile user (if it is not, hides logout and change photo buttons)
    private void verifyUser() {
        // Get profile user's id
        String userId = profileUser.getObjectId();
        // Get current user's id
        String currentUserId = ParseUser.getCurrentUser().getObjectId();
        // Compare users through their ids
        if (!userId.equals(currentUserId)){
            // If they are not the same, change buttons visibility
            btnLogout.setVisibility(View.GONE);
            btnUpload.setVisibility(View.GONE);
        }
    }

    // LISTENERS AND FEATURES

    // Set listeners for each buttons (these are only available if profileUser is the same as current user)
    private void setClickListeners() {

        // Calls onPickPhoto to access media storage and select a photo
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPickPhoto(v);
            }
        });

        // Logout current user and close the app
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
                getActivity().finish();
            }
        });
    }

    // Lets the user refresh the profile posts swiping down on the RV
    private void setRefreshFeature(View view) {
        // Get view from layout
        swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Make query to get newest posts
                queryPosts();
            }
        });
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
    }


    // Enables queries for most posts while the user is scrolling
    private void setEndlessScrollingFeature() {
        // Create new instance of scroll listener
        scrollListener = new EndlessRecyclerViewScrollListener(gridLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                // Triggered only when new data needs to be appended to the list
                queryMorePosts();
            }
        };
        // Adds the scroll listener to RecyclerView
        rvPosts.addOnScrollListener(scrollListener);
    }


    // QUERY METHODS

    // Get first n posts from database
    private void queryPosts() {
        // specify what type of data we want to query - Post.class
        ParseQuery<Post> query = ParseQuery.getQuery(Post.class);
        // include data referred by user key
        query.include(Post.KEY_USER);
        // get posts that correspond to the profile user
        query.whereEqualTo(Post.KEY_USER, profileUser);
        // limit query to latest 20 items
        query.setLimit(20);
        // order posts by creation date (newest first)
        query.addDescendingOrder("createdAt");
        // start an asynchronous call for posts
        query.findInBackground(new FindCallback<Post>() {
            @Override
            public void done(List<Post> posts, ParseException e) {
                // check for errors
                if (e != null) {
                    Log.e(TAG, "Issue with getting posts", e);
                    return;
                }

                // for debugging purposes let's print every post description to logcat
                for (Post post : posts) {
                    Log.i(TAG, "Post: " + post.getDescription() + ", username: " + post.getUser().getUsername());
                }
                // Clear the list of all posts
                allPosts.clear();
                // Save received posts to list and notify adapter of new data
                allPosts.addAll(posts);
                adapter.notifyDataSetChanged();
                // Hide refreshing icon
                swipeContainer.setRefreshing(false);
                // Set new oldest date
                setOldestDate();
            }
        });
    }

    private void queryMorePosts() {
        // specify what type of data we want to query - Post.class
        ParseQuery<Post> query = ParseQuery.getQuery(Post.class);
        // include data referred by user key
        query.include(Post.KEY_USER);
        // get posts that correspond to the profile user
        query.whereEqualTo(Post.KEY_USER, profileUser);
        // limit query to 5 items
        query.setLimit(5);
        // get only posts that are older than the current oldest post (refer to oldestDate)
        query.whereLessThan("createdAt", oldestDate);
        // order posts by creation date (newest first)
        query.addDescendingOrder("createdAt");
        // start an asynchronous call for posts
        query.findInBackground(new FindCallback<Post>() {
            @Override
            public void done(List<Post> posts, ParseException e) {
                // check for errors
                if (e != null) {
                    Log.e(TAG, "Issue with getting more posts", e);
                    return;
                }
                // Save received posts to list and notify adapter of new data
                allPosts.addAll(posts);
                adapter.notifyDataSetChanged();
                // Set new oldest date
                setOldestDate();
            }
        });
    }

    // MEDIA METHODS

    // Trigger gallery selection for a photo
    public void onPickPhoto(View view) {
        // Create intent for picking a photo from the gallery
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        // If you call startActivityForResult() using an intent that no app can handle, your app will crash.
        // So as long as the result is not null, it's safe to use the intent.
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            // Bring up gallery to select a photo
            startActivityForResult(intent, PICK_PHOTO_CODE);
        }
    }

    public Bitmap loadFromUri(Uri photoUri) {
        Bitmap image = null;
        try {
            // check version of Android on device
            if(Build.VERSION.SDK_INT > 27){
                // on newer versions of Android, use the new decodeBitmap method
                ImageDecoder.Source source = ImageDecoder.createSource(getActivity().getContentResolver(), photoUri);
                image = ImageDecoder.decodeBitmap(source);
            } else {
                // support older versions of Android by using getBitmap
                image = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), photoUri);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

    // RESULT METHOD

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null && requestCode == PICK_PHOTO_CODE) {
            // Get image from intent
            Uri photoUri = data.getData();
            // Load the image located at photoUri into selectedImage
            Bitmap selectedImage = loadFromUri(photoUri);
            Bitmap resizedBitmap = BitmapScaler.scaleToFitWidth(selectedImage, 500); // resize image using helper
            // Configure byte output stream
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            // Transform to byte array
            byte[] byteArray = stream.toByteArray();

            // Create new parseFile to save image
            ParseFile newProfileImage = new ParseFile("profile.png", byteArray);
            // Set new image on profileUser object
            profileUser.put("profileImage", newProfileImage);
            // Save profile user in background thread
            profileUser.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    // Check for errors
                    if (e != null) {
                        Log.e(TAG, "Profile image wasn't saved", e);
                        return;
                    }
                    Toast.makeText(getContext(), "Profile image changed!", Toast.LENGTH_SHORT).show(); // display success message
                }
            });
            // Change image on view
            ivProfile.setImageBitmap(resizedBitmap);
        }
    }

    // OTHER METHODS

    // Calls logout method from ParseUser to forget current credentials
    public void logout() {
        ParseUser.logOut();
    }

    // Gets the date of the oldest post (last one on list) and saves value for it to be used in queryMorePosts
    protected void setOldestDate() {
        // Get index for last item
        int lastIdx = adapter.getItemCount() - 1;
        // Check that index is greater than zero
        if(lastIdx > 0) {
            // save oldest date
            oldestDate = allPosts.get(lastIdx).getCreatedAt();
        }
    }
}
