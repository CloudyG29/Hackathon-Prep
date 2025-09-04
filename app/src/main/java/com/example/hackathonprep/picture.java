package com.example.hackathonprep;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class picture {
        private FirebaseStorage storage = FirebaseStorage.getInstance();
        private FirebaseFirestore db = FirebaseFirestore.getInstance();
        private FirebaseAuth auth = FirebaseAuth.getInstance();

        public void uploadProfilePicture(Uri imageUri) {
            String uid = auth.getCurrentUser().getUid();
            StorageReference profileRef = storage.getReference()
                    .child("users/" + uid + "/profile.jpg");

            profileRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot ->
                            profileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                // Save download URL to Firestore
                                db.collection("users").document(uid)
                                        .update("profilePic", uri.toString())
                                        .addOnSuccessListener(aVoid ->
                                                Log.d("ProfileUploader", "Profile pic URL saved"))
                                        .addOnFailureListener(e ->
                                                Log.e("ProfileUploader", "Error saving URL", e));
                            }))
                    .addOnFailureListener(e ->
                            Log.e("ProfileUploader", "Upload failed", e));
        }
    }


