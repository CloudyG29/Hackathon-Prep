package com.example.hackathonprep;

import android.util.Log;

import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class Login {
    FirebaseAuth mAuth=FirebaseAuth.getInstance();
    FirebaseFirestore db=FirebaseFirestore.getInstance();
    public void login(String email ,String password){
        mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(task-> {
            if (task.isSuccessful()) {
                FirebaseUser user=mAuth.getCurrentUser();
                Log.d("Login","Login Successful:"+"Welcome"+user.getDisplayName());
                db.collection("users").document(user.getUid()).get().addOnSuccessListener(document->{
                    if(document.exists()) {
                    String name=document.getString("name");
                    String phone=document.getString("phone");
                    Log.d("Login","Name:"+name);
                    }
                    });

            }
            else{
                Log.d("Login","Login Failed");
            }
            });
    }
}
