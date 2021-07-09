package com.example.instagram.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.instagram.R;
import com.example.instagram.activities.DetailsActivity;
import com.example.instagram.models.Post;
import com.parse.ParseFile;

import org.parceler.Parcels;

import java.util.List;

// Binds data to post that appear on a ProfileFragment (Just the image appears)
public class ProfilePostsAdapter extends RecyclerView.Adapter<ProfilePostsAdapter.ViewHolder>{

    // FIELDS
    private Context context;
    private List<Post> posts;

    public ProfilePostsAdapter(Context context, List<Post> posts){
        this.context = context;
        this.posts = posts;
    }

    // MANDATORY METHODS

    @NonNull
    @Override
    public ProfilePostsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.profile_post, parent, false); // user profile_post to inflate each row
        return new ProfilePostsAdapter.ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ProfilePostsAdapter.ViewHolder holder, int position) {
        Post post = posts.get(position);
        holder.bind(post);
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    // CUSTOM METHODS

    public void clear() {
        posts.clear();
        notifyDataSetChanged();
    }

    // Add a list of items -- change to type used
    public void addAll(List<Post> list) {
        posts.addAll(list);
        notifyDataSetChanged();
    }

    // VIEWHOLDER (implements an interface to handle clicks)
    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        // VIEWS
        private ImageView ivImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Get image view from layout
            ivImage = itemView.findViewById(R.id.ivImage);
            // Create a listener that applies to the whole viewholder
            itemView.setOnClickListener(this);
        }

        // Connect post data to the view
        public void bind(Post post) {
            // Get post image
            ParseFile postImage = post.getImage();
            // Check that image is not null
            if (postImage != null) {
                // Use glide to embed image
                Glide.with(context).load(postImage.getUrl()).into(ivImage);
            }
        }

        // If the post is clicked, a detail view is launched in which the user can see specific details for this post
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
