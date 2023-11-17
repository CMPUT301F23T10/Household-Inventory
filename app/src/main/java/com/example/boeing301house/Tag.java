package com.example.boeing301house;


import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

/**
 * Model class for tag.
 * Contains all current tags associated with user + tag control b/w app and db
 */
public class Tag {
    /**
     * tag for logs
     */
    private static final String TAG = "TAGS";

    /**
     * DB reference to user
     */
    private DocumentReference user;
    /**
     * Tags belonging to current user
     */
    private ArrayList<String> tags;

    /**
     * Constructor for tags
     * @param connection connection to db
     */
    public Tag(DBConnection connection) {
        tags = new ArrayList<>();
        user = connection.getUserRef();
        initTag();
    }

    /**
     * initializes tags
     */
    private void initTag() {
        user.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                tags = (ArrayList<String>) task.getResult().get("Tags");
            }
        })
        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "FAILED TO GET FROM FIRESTORE");
            }
        });
    }

    /**
     * Add tag to list + db
     * @param tag tag to be added
     * @param listener optional {@link com.example.boeing301house.Itemlist.OnCompleteListener} for error handling
     */
    public void addTag(String tag, @Nullable com.example.boeing301house.Itemlist.OnCompleteListener<String> listener) {
        if (tag == null) {
            listener.onComplete(null, false); // TODO: snackbar "No tag"
            return;
        }
        if (StringUtils.isBlank(tag)) {
            listener.onComplete(null, false); // TODO: snackbar "No tag"
            return;
        }
        if (tags.contains(tag)) {
            listener.onComplete(tag, false); // TODO: snackbar "Tag already exists"
            return;
        }
        // add tags
        tags.add(tag);
        user.update("Tags", tags).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "FAILED TO UPDATE FIRESTORE : " + e);
            }
        });
    }

    /**
     * Remove tag from tags
     * @param tag tag to be removed
     * @param listener optional {@link com.example.boeing301house.Itemlist.OnCompleteListener} for error handling
     */
    public void removeTag(String tag, @Nullable com.example.boeing301house.Itemlist.OnCompleteListener<String> listener) {
        if (tag == null) {
            listener.onComplete(null, false); // TODO: snackbar "No tag"
            return;
        }
        if (StringUtils.isBlank(tag)) {
            listener.onComplete(null, false); // TODO: snackbar "No tag"
            return;
        }
        if (!tags.contains(tag)) {
            listener.onComplete(tag, false); // TODO: snackbar "Tag does not exist"
            return;
        }
        // remove tag
        tags.remove(tag);
        user.update("Tags", tags).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "FAILED TO UPDATE FIRESTORE : " + e);
            }
        });

    }

    /**
     * Getter for tags
     * @return list of tags
     */
    public ArrayList<String> getTags() {
        return tags;
    }
}