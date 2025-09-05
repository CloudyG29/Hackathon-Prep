package com.example.hackathonprep.ui.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.hackathonprep.R;
import com.example.hackathonprep.Signup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class SignupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        TextView loginbtn = findViewById(R.id.tvLoginRedirect);
        loginbtn.setOnClickListener(view -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        TextInputEditText Email, Phone, Password, Name;
        Button signup;

        Email = findViewById(R.id.etEmail);
        Phone = findViewById(R.id.etPhone);
        Password = findViewById(R.id.etPassword);
        Name = findViewById(R.id.etName);

        signup = findViewById(R.id.btnSignup);

        signup.setOnClickListener(view -> {
            Signup.signup(Objects.requireNonNull(Email.getText()).toString(), Objects.requireNonNull(Password.getText()).toString(), Objects.requireNonNull(Name.getText()).toString());
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });



    }
}