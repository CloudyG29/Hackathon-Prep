package com.example.hackathonprep;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Signup {

    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    public void signup(String email,String Password,String name){
        mAuth.createUserWithEmailAndPassword(email,Password).addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    String uid = user.getUid();
                    Map<String,Object> userData = new HashMap<>();
                    userData.put("name",name);
                    userData.put("email",email);
                    db.collection("ussers").document(uid).set(userData).addOnSuccessListener(aVoid ->
                                    Log.d("Signup","User data saved successfully"))
                    .addOnFailureListener(e -> Log.w("Signup","Error saving user data",e));
                    }
                else{
                        Log.w("Signup","User creation failed",task.getException());
                    }
                }

            });


    }

}
