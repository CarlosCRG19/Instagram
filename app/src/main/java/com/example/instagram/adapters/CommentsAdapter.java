package com.example.instagram.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.instagram.R;
import com.example.instagram.activities.DetailsActivity;
import com.example.instagram.models.Comment;
import com.example.instagram.models.Post;
import com.parse.ParseFile;

import org.parceler.Parcels;

import java.util.List;

// Binds data for comments
public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.ViewHolder>{

    // FIELDS
    private Context context;
    private List<Comment> comments;

    public CommentsAdapter(Context context, List<Comment> comments){
        this.context = context;
        this.comments = comments;
    }

    // MANDATORY METHODS

    @NonNull
    @Override
    public CommentsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.comment_layout, parent, false);
        return new CommentsAdapter.ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull CommentsAdapter.ViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.bind(comment);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    // CUSTOM METHODS

    // Clears list and notifies changes to adapter
    public void clear() {
        comments.clear();
        notifyDataSetChanged();
    }

    // Adds a complete list of items
    public void addAll(List<Comment> list) {
        comments.addAll(list);
        notifyDataSetChanged();
    }

    // VIEWHOLDER
    class ViewHolder extends RecyclerView.ViewHolder{

        // VIEWS
        private ImageView ivProfile;
        private TextView tvUsername, tvBody;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Get views from layout
            ivProfile = itemView.findViewById(R.id.ivProfile);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvBody = itemView.findViewById(R.id.tvBody);
        }

        // Connect comment data to the views
        public void bind(Comment comment) {
            // Bind the comment data to the view elements
            ParseFile profileImage = (ParseFile) comment.getUser().get("profileImage");
            if (profileImage != null) {
                Glide.with(context).load(profileImage.getUrl()).into(ivProfile); // Use glide to embed profile image into view
            }
            tvUsername.setText(comment.getUser().getUsername());
            tvBody.setText(comment.getBody());
        }

    }

}
