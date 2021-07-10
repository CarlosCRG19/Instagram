package com.example.instagram.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.instagram.R;
import com.example.instagram.activities.DetailsActivity;
import com.example.instagram.activities.MainActivity;
import com.example.instagram.fragments.ProfileFragment;
import com.example.instagram.models.Post;
import com.parse.ParseFile;

import org.parceler.Parcels;

import java.util.List;

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.ViewHolder> {

    private Context context;
    private List<Post> posts;

    public PostsAdapter(Context context, List<Post> posts){
        this.context = context;
        this.posts = posts;
    }

    // MANDATORY METHODS

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Post post = posts.get(position);
        holder.bind(post);
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    // CUSTOM METHODS

    // Clears list and notifies changes to adapter
    public void clear() {
        posts.clear();
        notifyDataSetChanged();
    }

    // Add a complete list of items
    public void addAll(List<Post> list) {
        posts.addAll(list);
        notifyDataSetChanged();
    }

    // VIEW HOLDER CLASS (uses interface so that every row in the RV can listen to clicks and change to DetailsActivity)
    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public static final String USER_KEY = "user"; // key to receive and send users from messaging objects

        // VIEWS
        private ImageView ivImage, ivProfile;
        private TextView tvUsername, tvDescription, tvCreatedAt;
        private LinearLayout llProfile; // Layout that contains author's profile pic and username (when this is clicked, ProfileFragment is launched)

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Get views from layout
            llProfile = itemView.findViewById(R.id.llProfile);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            tvUsername = itemView.findViewById(R.id.tvUsername);

            ivImage = itemView.findViewById(R.id.ivImage);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);

            // Set click listener
            itemView.setOnClickListener(this);
        }

        // Connects data to views
        public void bind(Post post) {
            // Bind the post data to the view elements

            // User data
            ParseFile profileImage = (ParseFile) post.getUser().get("profileImage");
            Glide.with(context)
                    .load(profileImage.getUrl())
                    .circleCrop()
                    .into(ivProfile);
            tvUsername.setText(post.getUser().getUsername());

            // Post data
            ParseFile postImage = post.getImage();
            // Verify that post has an image
            if (postImage != null) {
                Glide.with(context).load(postImage.getUrl()).into(ivImage);
            }

            // Add username at the beginning of description
            String sourceString = "<b>" + post.getUser().getUsername() + "</b>  " + post.getDescription();
            tvDescription.setText(Html.fromHtml(sourceString, 42));
            tvCreatedAt.setText(Post.calculateTimeAgo(post.getCreatedAt()));

            // Create listener to lauch ProfileFragment if username or profile pic is clicked
            llProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Setup bundle to pass user
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(USER_KEY, post.getUser());
                    // Create new fragment and set args
                    ProfileFragment profileFragment = new ProfileFragment();
                    profileFragment.setArguments(bundle);
                    // Change to ProfileFragment from MainActivity
                    ((MainActivity) context).getSupportFragmentManager().beginTransaction()
                            .replace(R.id.flContainer, profileFragment, "Posts")
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        // When a row is clicked (not on authors username or pp), a detail view for that specific post is launched
        @Override
        public void onClick(View v) {
            // Get position of adapter
            int position = getAdapterPosition();
            // Check if position exists on RV
            if(position != RecyclerView.NO_POSITION){
                // Get post from model
                Post currentPost = posts.get(position);
                // Create an intent to start DetailsActivity and pass info
                Intent i = new Intent(context, DetailsActivity.class);
                // User Parcel to wrap and pass data into the intent as an extra
                i.putExtra(Post.class.getSimpleName(), Parcels.wrap(currentPost));
                // Start new activity
                context.startActivity(i);
            }
        }

    }
}