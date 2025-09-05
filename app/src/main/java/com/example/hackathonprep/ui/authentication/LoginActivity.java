package com.example.hackathonprep.ui.authentication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.hackathonprep.Login;
import com.example.hackathonprep.MainActivity;
import com.example.hackathonprep.R;
import com.example.hackathonprep.Signup;
import com.example.hackathonprep.picture;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 71;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //Button uploadBtn = findViewById(R.id.button);
        //uploadBtn.setOnClickListener(v -> chooseImage());
       // doneButton = findViewById(R.id.done);
        Button loginButton = findViewById(R.id.btnLogin);
        //Button signupButton = findViewById(R.id.btn1);

        TextInputEditText  emailEditText;
        TextInputEditText  passwordEditText;
        //nameEditText = findViewById(R.id.name);
        emailEditText = findViewById(R.id.etLoginEmail);
        passwordEditText = findViewById(R.id.etLoginPassword);

        loginButton.setOnClickListener(v -> {
            emailEditText.setVisibility(View.VISIBLE);
            passwordEditText.setVisibility(View.VISIBLE);
            //doneButton.setVisibility(View.VISIBLE);
            //loginButton.setVisibility(View.GONE);

            loginButton.setOnClickListener(m -> {
                Login login = new Login();
                login.login(Objects.requireNonNull(emailEditText.getText()).toString(), Objects.requireNonNull(passwordEditText.getText()).toString());

                Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();

            });
        });



    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            Uri filePath = data.getData();
            new picture().uploadProfilePicture(filePath);
        }
    }
}