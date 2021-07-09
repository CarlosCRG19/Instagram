package com.example.instagram.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.instagram.FeedActivity;
import com.example.instagram.models.Post;
import com.example.instagram.R;
import com.example.instagram.helpers.BitmapScaler;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.app.Activity.RESULT_OK;

public class ComposeFragment extends Fragment {

    // MEMBER VARIABLES

    public static final String TAG = "ComposeFragment"; // TAG for log messages

    // CODES TO CALL MEDIA ACTIONS (TAKE PHOTO AND GET FROM CONTENT)
    public static final int PICK_PHOTO_CODE = 1046;
    public static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 42;

    // VIEWS
    ImageView ivPostImage;
    EditText etDescription;
    Button btnCaptureImage, btnMedia, btnSubmit;
    ProgressBar pbSubmit; // ProgressBar shown when post is being submitted

    // MEDIA FILE VARIABLES
    File file;
    ParseFile photoFile;
    String photoFileName = "photo.jpg";

    // Required empty public constructor
    public ComposeFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_compose, container, false);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set views from specified layout
        setViews(view);
        // Set listeners for different buttons
        setClickListeners();
    }

    // VIEWS METHODS

    // Gets views from layout
    private void  setViews(View view) {
        // Post info
        ivPostImage = view.findViewById(R.id.ivPostImage);
        etDescription = view.findViewById(R.id.etDescription);

        // Media action
        btnMedia = view.findViewById(R.id.btnMedia);
        btnCaptureImage = view.findViewById(R.id.btnCapture);

        // Submit action
        btnSubmit = view.findViewById(R.id.btnSubmit);
        pbSubmit = view.findViewById(R.id.pbSubmit);

    }

    // Sets listeners for each button
    private void setClickListeners() {
        // Calls onPickPhoto to access media storage and pick a photo
        btnMedia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPickPhoto(v);
            }
        });
        // Calls launchCamera to open camera and take a picture
        btnCaptureImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchCamera();
            }
        });
        // Checks that all the required fields aren't empty and saves the post in database
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if there is an image loaded
                if(photoFile == null || ivPostImage.getDrawable() == null) {
                    Toast.makeText(getContext(), "There is no image", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Get description from EditText and check that context is not empty
                String description = etDescription.getText().toString();
                if(description.isEmpty()){
                    Toast.makeText(getContext(), "Description cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Get current user to assign it to this post
                ParseUser currentUser = ParseUser.getCurrentUser();
                // Save post in database
                savePost(description, currentUser, photoFile);
            }
        });
    }

    // SAVE (POST) METHODS

    // Creates a new post and saves it into database, displays a progress bar while the post is being saved
    private void savePost(String description, ParseUser currentUser, ParseFile photoFile) {
        // Display progress bar
        pbSubmit.setVisibility(ProgressBar.VISIBLE);
        // Create new post instance
        Post post = new Post();
        // Assign attributes
        post.setDescription(description);
        post.setImage(photoFile);
        post.setUser(currentUser);
        // Save into database using background thread
        post.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                // Check for errors
                if(e != null) {
                    Log.e(TAG, "Error while saving", e);
                    Toast.makeText(getContext(), "Error while saving post!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Notify user using a toast
                Toast.makeText(getContext(), "Post submitted!", Toast.LENGTH_SHORT).show();

                // Empty views for post
                etDescription.setText("");
                ivPostImage.setImageResource(0);

                // Hide progress bar
                pbSubmit.setVisibility(ProgressBar.INVISIBLE);
            }
        });
    }

    // MEDIA METHODS (provided by CodePath)

    private void launchCamera() {
        // create Intent to take a picture and return control to the calling application
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Create a File reference for future access
        file = getPhotoFileUri(photoFileName);
        // Set value for photoFile so it can be saved into database
        photoFile = new ParseFile(file);
        // wrap File object into a content provider
        // required for API >= 24
        // See https://guides.codepath.com/android/Sharing-Content-with-Intents#sharing-files-with-api-24-or-higher
        Uri fileProvider = FileProvider.getUriForFile(getContext(), "com.codepath.fileprovider", file);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider);

        // If you call startActivityForResult() using an intent that no app can handle, your app will crash.
        // So as long as the result is not null, it's safe to use the intent.
        // Start the image capture intent to take photo
        // TODO: intent.resolveActivity is null
        if(intent.resolveActivity(getContext().getPackageManager()) != null) {
            startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
        }
    }

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

    // Returns the File for a photo stored on disk given the fileName
    public File getPhotoFileUri(String fileName) {
        // Get safe storage directory for photos
        // Use `getExternalFilesDir` on Context to access package-specific directories.
        // This way, we don't need to request external read/write runtime permissions.
        File mediaStorageDir = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), TAG);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()){
            Log.d(TAG, "failed to create directory");
        }

        // Return the file target for the photo based on filename
        return new File(mediaStorageDir.getPath() + File.separator + fileName);
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

    // Handles the result of both media actions (Pick from media or take photo)
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) { // Handle Take Photo
            if (resultCode == RESULT_OK) {
                // By this point we have the camera photo on disk
                Bitmap takenImage = BitmapFactory.decodeFile(file.getAbsolutePath());
                Bitmap resizedBitmap = BitmapScaler.scaleToFitWidth(takenImage, 500); // resize image using helper
                // Configure byte output stream
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                // Compress the image further
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                // Create a new file for the resized bitmap (`getPhotoFileUri` defined above)
                File resizedFile = getPhotoFileUri(photoFileName + "_resized");
                try {
                    resizedFile.createNewFile();
                    FileOutputStream fos = new FileOutputStream(resizedFile);
                    // Write the bytes of the bitmap to file
                    fos.write(bytes.toByteArray());
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // Load the taken image into a preview
                ivPostImage.setImageBitmap(resizedBitmap);
            } else { // Result was a failure
                Toast.makeText(getContext(), "Picture wasn't taken!", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PICK_PHOTO_CODE) { // Handle Pick Photo
            if (data != null) {
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
                // Create new ParseFile (this will be saved by savePost)
                photoFile = new ParseFile("profile.png", byteArray);
                // Load selected image into a preview
                ivPostImage.setImageBitmap(resizedBitmap);

            } else {
                Toast.makeText(getContext(), "Picture wasn't selected!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}