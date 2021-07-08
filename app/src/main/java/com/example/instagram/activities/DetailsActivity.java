package com.example.instagram.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DetailsActivity extends AppCompatActivity {

    public static final String TAG = "DetailsActivity";
    Like userLike;
    private LinearLayout llProfile;
    private TextView tvUsername;
    private ImageView ivImage;
    private TextView tvDescription, tvCreatedAt, tvLikesCount;
    private EditText etComment;
    private Button btnComment, btnLike;
    private List<Comment> allComments;
    private RecyclerView rvComments;
    private int likesCount;

    CommentsAdapter adapter;

    Post post;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        allComments = new ArrayList<>();
        adapter = new CommentsAdapter(DetailsActivity.this, allComments);

        llProfile = findViewById(R.id.llProfile);
        tvUsername = findViewById(R.id.tvUsername);
        ivImage = findViewById(R.id.ivImage);
        tvDescription = findViewById(R.id.tvDescription);
        tvLikesCount = findViewById(R.id.tvLikesCount);
        tvCreatedAt = findViewById(R.id.tvCreatedAt);
        etComment = findViewById(R.id.etComment);

        // CHECK THIS OUT!!!!
        post = (Post) Parcels.unwrap(getIntent().getParcelableExtra(Post.class.getSimpleName()));

        Date createdAt = post.getCreatedAt();
        String timeAgo = Post.calculateTimeAgo(createdAt);
        tvCreatedAt.setText(timeAgo);

        likesCount = post.getLikesCount();
        tvLikesCount.setText(String.valueOf(likesCount));

        btnComment = findViewById(R.id.btnComment);
        btnComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String body = etComment.getText().toString();
                if(body.isEmpty()){
                    Toast.makeText(DetailsActivity.this, "Body cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                ParseUser currentUser = ParseUser.getCurrentUser();
                saveComment(post, body, currentUser);
            }
        });

        tvDescription.setText(post.getDescription());
        tvUsername.setText(post.getUser().getUsername());
        ParseFile image = post.getImage();
        if (image != null) {
            Glide.with(this).load(image.getUrl()).into(ivImage);
        }

        llProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(DetailsActivity.this, MainActivity.class);
                i.putExtra("user", post.getUser());
                i.putExtra("ProfileFragment", true);
                startActivity(i);
            }
        });

        rvComments = findViewById(R.id.rvComments);
        rvComments.setAdapter(adapter);
        rvComments.setLayoutManager(new LinearLayoutManager(DetailsActivity.this));

        queryComments();

        btnLike = findViewById(R.id.btnLike);

        // INCLUDE CURRENT USER
        isUserLiked(post, ParseUser.getCurrentUser());

        btnLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(userLike != null) {
                        userLike.deleteInBackground(new DeleteCallback() {
                            @Override
                            public void done(ParseException e) {
                                if(e != null) {
                                    Log.e(TAG, "Error while saving", e);
                                    Toast.makeText(DetailsActivity.this, "Error while unliking!", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                post.decrementLikesCount();
                                post.saveInBackground();
                                likesCount -= 1;
                                tvLikesCount.setText(String.valueOf(likesCount));
                                userLike = null;
                                btnLike.setBackgroundResource(R.drawable.ufi_heart);
                                Toast.makeText(DetailsActivity.this, "Post unliked!" , Toast.LENGTH_SHORT).show();
                            }
                        });

                } else {
                    ParseUser currentUser = ParseUser.getCurrentUser();
                    saveLike(post, currentUser);
                    btnLike.setBackgroundResource(R.drawable.ufi_heart_active);
                }

            }
        });

    }

    private void isUserLiked(Post post, ParseUser currentUser) {
        ParseQuery<Like> query = ParseQuery.getQuery(Like.class);
        query.whereEqualTo("post", post);
        query.whereEqualTo("user", currentUser);
        query.getFirstInBackground(new GetCallback<Like>() {
            @Override
            public void done(Like foundLike, ParseException e) {
                if(e != null) {
                    btnLike.setBackgroundResource(R.drawable.ufi_heart);
                    return;
                }
                btnLike.setBackgroundResource(R.drawable.ufi_heart_active);
                userLike = foundLike;
            }
        });
    }

    private void saveLike(Post post, ParseUser currentUser) {
        Like like = new Like();
        like.setPost(post);
        like.setUser(currentUser);
        like.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e != null) {
                    Log.e(TAG, "Error while saving", e);
                    Toast.makeText(DetailsActivity.this, "Error while saving!", Toast.LENGTH_SHORT).show();
                    return;
                }
                userLike = like;
                post.incrementLikesCount();
                post.saveInBackground();
                likesCount += 1;
                tvLikesCount.setText(String.valueOf(likesCount));
                Toast.makeText(DetailsActivity.this, "Post liked!" , Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveComment(Post post, String body, ParseUser currentUser) {
        Comment comment = new Comment();
        comment.setBody(body);
        comment.setUser(currentUser);
        comment.setPost(post);

        comment.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e != null) {
                    Log.e(TAG, "Error while saving", e);
                    Toast.makeText(DetailsActivity.this, "Error while saving!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.i(TAG, "Comment save was successfully!");
                queryComments();
                Toast.makeText(DetailsActivity.this, "Comment submitted!" + comment.getUser().getUsername(), Toast.LENGTH_SHORT).show();
                etComment.setText("");
            }
        });
    }

    protected void queryComments() {
        // specify what type of data we want to query - Post.class
        ParseQuery<Comment> query = ParseQuery.getQuery(Comment.class);
        // limit query to latest 20 items
        query.include(Comment.KEY_USER);
        query.setLimit(20);
        // order posts by creation date (newest first)
        query.whereEqualTo(Comment.KEY_POST, post);
        // start an asynchronous call for posts
        query.findInBackground(new FindCallback<Comment>() {
            @Override
            public void done(List<Comment> comments, ParseException e) {
                // check for errors
                if (e != null) {
                    Log.e(TAG, "Issue with getting comments", e);
                    return;
                }

                // for debugging purposes let's print every post description to logcat
                for (Comment comment: comments) {
                    Log.i(TAG, "Comment: " + comment.getBody());
                }

                // save received posts to list and notify adapter of new data
                allComments.clear();
                allComments.addAll(comments);
                adapter.notifyDataSetChanged();
            }
        });
    }

}
