package com.example.hackathonprep;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class Login {
    public static int loginCheck = 0;

    FirebaseAuth mAuth=FirebaseAuth.getInstance();
    FirebaseFirestore db=FirebaseFirestore.getInstance();
    public void login(String email , String password){

        mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(task-> {
            if (task.isSuccessful()) {
                FirebaseUser user=mAuth.getCurrentUser();
                Log.d("Login","Login Successful:"+"Welcome"+user.getDisplayName());
                db.collection("users").document(user.getUid()).get().addOnSuccessListener(document->{
                    if(document.exists()) {
                    String name=document.getString("name");
                    //String phone=document.getString("phone");
                    Log.d("Login","Name:"+name);
                    loginCheck += 1;
                    }
                    });
            }
            else{
                Log.d("Login","Login Failed");
                loginCheck = 0;
            }
            });

    }
}
