package com.example.instagram.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.instagram.adapters.CommentsAdapter;
import com.example.instagram.models.Comment;
import com.example.instagram.models.Like;
import com.example.instagram.models.Post;
import com.example.instagram.R;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

public class DetailsActivity extends AppCompatActivity {

    // MEMBER VARIABLES

    public static final String TAG = "DetailsActivity"; // TAG for log messages
    public static final String LAUNCH_PF_KEY = "ProfileFragment"; // Key to launch ProfileFragment for the user post

    // Post
    Post post;

    // User Verification
    ParseUser postUser; // the author of the post
    ParseUser currentUser; // the user that is seeing the post (can be the same as postUser)

    // General Views
    ImageView ivProfile, ivImage;
    EditText etComment;
    LinearLayout llProfile; // ivProfile and tvUsername are both inside this LL, this way, a clickListener to change to ProfileFragment can be added
    RecyclerView rvComments; // View group to populate with comments
    Button btnComment;
    ImageButton btnLike;
    TextView tvUsername, tvDescription, tvCreatedAt, tvLikesCount;

    // Likes Info
    int likesCount; //
    Like userLike; // ParseObject representing the user's like

    // Comments Code
    List<Comment> allComments; // model to save comments
    CommentsAdapter adapter;  // class to bind the data with views

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Assign views
        setViews();

        // Get post from intent
        post = (Post) Parcels.unwrap(getIntent().getParcelableExtra(Post.class.getSimpleName()));

        // Set users
        postUser = post.getUser();
        currentUser = ParseUser.getCurrentUser();

        // Get number of likes
        likesCount = post.getLikesCount();

        // Populate views with info from post
        populateViews();
        // Set click listeners for different views
        setClickListeners();

        // Check if currentUser has liked the post
        verifyUserLiked(post, currentUser);

        // Setup comments recycler view
        allComments = new ArrayList<>(); // starts with empty list
        adapter = new CommentsAdapter(DetailsActivity.this, allComments);
        rvComments.setAdapter(adapter);
        rvComments.setLayoutManager(new LinearLayoutManager(DetailsActivity.this));

        // Query comments to fill the RV
        queryComments();
    }

    // VIEWS METHODS

    // Finds each view in layout and assigns them to member variables
    private void setViews() {
        // Author information
        llProfile = findViewById(R.id.llProfile);
        ivProfile = findViewById(R.id.ivProfile);
        tvUsername = findViewById(R.id.tvUsername);

        // Post data
        ivImage = findViewById(R.id.ivImage);
        tvDescription = findViewById(R.id.tvDescription);
        tvCreatedAt = findViewById(R.id.tvCreatedAt);
        tvLikesCount = findViewById(R.id.tvLikesCount);

        // Interaction views
        btnLike = findViewById(R.id.btnLike);
        btnComment = findViewById(R.id.btnComment);
        etComment = findViewById(R.id.etComment);

        rvComments = findViewById(R.id.rvComments); // Comments RV
    }

    private void setClickListeners() {

        // If the user clicks on the username or postUser photo, they will be redirected to a ProfileFragment for that user
        llProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create intent to pass info between Details Activity and MainActivity
                Intent i = new Intent(DetailsActivity.this, MainActivity.class); // ProfileFragment is hosted on MainActivity, so we need to launch that activity
                // Put user as an extra and a boolean to indicate the the fragment launch
                i.putExtra(Post.KEY_USER, post.getUser());
                i.putExtra(LAUNCH_PF_KEY, true);
                // Start MainActivity
                startActivity(i);
                finish();
            }
        });

        // Button to like a post, uses userLike object to verify if user has liked current post
        btnLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if post has been liked
                if(userLike != null) {
                    // Use unlike interaction
                    saveUnlike(post, currentUser);
                } else {
                    // User like interaction
                    saveLike(post, currentUser);
                }

            }
        });

        // Button to post a comment, verifies that the content of edit text is not null (whether comment body is empty)
        btnComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String body = etComment.getText().toString();
                if(body.isEmpty()){
                    Toast.makeText(DetailsActivity.this, "Body cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                saveComment(post, body, currentUser);
            }
        });
    }

    // Binds the post and users data into the views
    private void populateViews() {
        // Author information
        ParseFile profileImage = (ParseFile) post.getUser().get("profileImage");
        Glide.with(DetailsActivity.this)
                .load(profileImage.getUrl())
                .circleCrop()
                .into(ivProfile);
        tvUsername.setText(postUser.getUsername());

        // Post data
        ParseFile postImage = post.getImage(); // Get and set image from ParseObject
        Glide.with(this).load(postImage.getUrl()).into(ivImage);

        // Add username at the beginning of description
        String sourceString = "<b>" + post.getUser().getUsername() + "</b>  " + post.getDescription();
        tvDescription.setText(Html.fromHtml(sourceString, 42)); // Fill description text
        tvCreatedAt.setText(Post.calculateTimeAgo(post.getCreatedAt())); // uses static method to format the date
        tvLikesCount.setText(String.valueOf(likesCount));
    }

    // QUERY METHODS

    // Checks whether the current user has liked the post. If that is the case, it changes the button background and userLike stays as null
    private void verifyUserLiked(Post post, ParseUser currentUser) {
        // Create query
        ParseQuery<Like> query = ParseQuery.getQuery(Like.class);
        // Define attributes to look for (like is on this post and by this user)
        query.whereEqualTo("post", post);
        query.whereEqualTo("user", currentUser);
        // Get the like object
        query.getFirstInBackground(new GetCallback<Like>() { // getFirstInBackground ends the query when it has found the first object that matches the attributes (instead of going through every object)
            @Override
            public void done(Like foundLike, ParseException e) {
                if(e != null) { // e == null when no matching object has been found
                    btnLike.setBackgroundResource(R.drawable.heart_icon_stroke); // set button icon to just the stroke
                    return;
                }
                btnLike.setBackgroundResource(R.drawable.heart_icon); // change icon to filled heart
                userLike = foundLike;
            }
        });
    }

    // Gets all comments for this specific post (one-to-many relationship) and notifies the adapter so that it can populate the RV
    protected void queryComments() {
        // Specify what type of data we want to query - Comment.class
        ParseQuery<Comment> query = ParseQuery.getQuery(Comment.class);
        // Limit query to latest 20 items and include the user
        query.setLimit(20);
        query.include(Comment.KEY_USER);
        // Limit query to only those comments that belong to this post
        query.whereEqualTo(Comment.KEY_POST, post);
        // Start async call for comments
        query.findInBackground(new FindCallback<Comment>() {
            @Override
            public void done(List<Comment> comments, ParseException e) {
                // Check for errors
                if (e != null) {
                    Log.e(TAG, "Issue with getting comments", e);
                    return;
                }
                // Clear the list of all comments
                allComments.clear();
                // Save received comments to list and notify adapter of new data
                allComments.addAll(comments);
                adapter.notifyDataSetChanged();
            }
        });
    }

    // SAVE (POST) METHODS

    // Posts a like, changes the button background and changes the count on databes
    private void saveLike(Post post, ParseUser currentUser) {
        // Create new like
        Like like = new Like();
        // Set fields
        like.setPost(post);
        like.setUser(currentUser);
        // Save like in database using background thread
        like.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                // Check for errors
                if(e != null) {
                    Log.e(TAG, "Error while saving", e);
                    Toast.makeText(DetailsActivity.this, "Error liking post!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(DetailsActivity.this, "Post liked!" , Toast.LENGTH_SHORT).show(); // displays a success message

                // Increment like count in post object
                post.incrementLikesCount();
                // Save modified post
                post.saveInBackground();

                // Change local likes count and set new value to TextView
                likesCount += 1;
                tvLikesCount.setText(String.valueOf(likesCount));

                // Change button background
                btnLike.setBackgroundResource(R.drawable.heart_icon);

                // Change userLike (now it won't be null)
                userLike = like;
            }
        });
    }

    // Takes current userLike object and deletes it from database
    private void saveUnlike(Post post, ParseUser currentUser) {
        // Delete current like from database
        userLike.deleteInBackground(new DeleteCallback() {
            @Override
            public void done(ParseException e) {
                // Check for errors
                if(e != null) {
                    Log.e(TAG, "Error while saving", e);
                    Toast.makeText(DetailsActivity.this, "Error unliking post!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(DetailsActivity.this, "Post unliked!" , Toast.LENGTH_SHORT).show();

                // Decrement database like count
                post.decrementLikesCount();
                // Save modifications
                post.saveInBackground();

                // Change local likes count and change TV text
                likesCount -= 1;
                tvLikesCount.setText(String.valueOf(likesCount));

                // Change button background
                btnLike.setBackgroundResource(R.drawable.heart_icon_stroke);

                userLike = null; // Even though we delete the object on the database, that does not mean that our local variable has been deleted

            }
        });
    }

    // Creates a new comment object and saves it into the database with its respective post and user
    private void saveComment(Post post, String body, ParseUser currentUser) {
        // Create new comment instance
        Comment comment = new Comment();
        // Set fields values
        comment.setBody(body);
        comment.setUser(currentUser);
        comment.setPost(post);
        // Save comment into database using background thread
        comment.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                // Check if posting was successful
                if(e != null) {
                    Log.e(TAG, "Error while saving", e);
                    Toast.makeText(DetailsActivity.this, "Error while saving!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(DetailsActivity.this, "Comment submitted!", Toast.LENGTH_SHORT).show();

                // Empty edit text content
                etComment.setText("");

                // queryComments to get newer comment
                queryComments();

            }
        });
    }
}
