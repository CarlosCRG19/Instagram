package com.example.instagram.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.example.instagram.adapters.CommentsAdapter;
import com.example.instagram.models.Comment;
import com.example.instagram.models.Post;
import com.example.instagram.adapters.PostsAdapter;
import com.example.instagram.R;
import com.example.instagram.helpers.EndlessRecyclerViewScrollListener;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PostsFragment extends Fragment {

    // MEMBER VARIABLES

    public static final String TAG = "PostsFragment"; // TAG for log messages

    RecyclerView rvPosts; // Viewgroup to populate with Posts

    // HELPERS
    SwipeRefreshLayout swipeContainer; // handles refresh action
    EndlessRecyclerViewScrollListener scrollListener; // handles endless scrolling (adds new posts to the RV)

    Date oldestDate; // Member variable to store the date of the oldest post (this is used in the query for new posts when the user scrolls)

    // Posts Code
    List<Post> allPosts; // model to save posts
    PostsAdapter adapter; // class to bind the data with views
    LinearLayoutManager linearLayoutManager; // manager for RV (also required for scrollListener)

    // Required empty public constructor
    public PostsFragment() {}

    // REQUIRED METHODS

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_posts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // initialize the array that will hold posts and create a PostsAdapter
        allPosts = new ArrayList<>();
        adapter = new PostsAdapter(getContext(), allPosts);

        // Setup comments recycler view
        rvPosts = view.findViewById(R.id.rvPosts);
        // set the adapter on the recycler view
        rvPosts.setAdapter(adapter);
        // Set layout manager on the RV
        linearLayoutManager = new LinearLayoutManager(getContext());
        rvPosts.setLayoutManager(linearLayoutManager);
        // Add divider between rows
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvPosts.getContext(),
                linearLayoutManager.getOrientation());
        rvPosts.addItemDecoration(dividerItemDecoration);
        // query posts from database
        queryPosts();

        // Enable refresh feature
        setRefreshFeature(view);
        // Enable endless scrolling
        setEndlessScrollingFeature();
    }

    // FEATURES METHODS

    // Lets the user refresh the main feed swiping down on the RV
    protected void setRefreshFeature(View view) {
        // Get view from layout
        swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Make query to get newest posts
                queryPosts();
                scrollListener.resetState(); // NOTE: This line solved a really weird problem in which, after refreshing, the recycler view lost the scroll listener. Thanks Rey for your help :)
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
        scrollListener = new EndlessRecyclerViewScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                // Triggered only when new data needs to be appended to the list
                Log.i(TAG, "Scrolling...");
                queryMorePosts();
            }
        };
        // Adds the scroll listener to RecyclerView
        rvPosts.addOnScrollListener(scrollListener);
    }

    // QUERY METHODS

    // Gets first n posts from database
    protected void queryPosts() {
        // specify what type of data we want to query - Post.class
        ParseQuery<Post> query = ParseQuery.getQuery(Post.class);
        // include data referred by user key
        query.include(Post.KEY_USER);
        // limit query to latest 5 items
        query.setLimit(5);
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
                adapter.clear();
                // Save received posts to list and notify adapter of new data
                allPosts.addAll(posts);
                adapter.notifyDataSetChanged();
                // Hide refreshing icon
                swipeContainer.setRefreshing(false);
                // Set new oldest date
                setOldestDate();
                Log.i(TAG, oldestDate.toString());
                // rvPosts.addOnScrollListener(scrollListener);
            }
        });
    }

    // Gets n more posts older than current oldest post
    protected void queryMorePosts() {
        // specify what type of data we want to query - Post.class
        ParseQuery<Post> query = ParseQuery.getQuery(Post.class);
        // include data referred by user key
        query.include(Post.KEY_USER);
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

                // for debugging purposes let's print every post description to logcat
                for (Post post : posts) {
                    Log.i(TAG, "Post: " + post.getDescription() + ", username: " + post.getUser().getUsername());
                }

                // Save received posts to list and notify adapter of new data
                allPosts.addAll(posts);
                adapter.notifyDataSetChanged();
                // Set new oldest date
                setOldestDate();
            }
        });
    }

    // OTHER METHODS

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