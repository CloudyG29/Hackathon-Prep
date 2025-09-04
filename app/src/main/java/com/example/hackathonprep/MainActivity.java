package com.example.hackathonprep;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import ai.picovoice.porcupine.Porcupine;
import ai.picovoice.porcupine.PorcupineException;

public class MainActivity extends AppCompatActivity {
    private Porcupine porcupine;
    public EditText emailEditText;
    public EditText passwordEditText;
    public EditText nameEditText;
    private static final int PICK_IMAGE_REQUEST = 71;

    private AudioRecord audioRecord;

    private Thread recordingThread;
    private Button dangerButton;
    private static final int REQUEST_RECORD_AUDIO = 1;
    private CountDownTimer timer;
    private Button doneButton;  // just declare here

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Button uploadBtn = findViewById(R.id.button);
        uploadBtn.setOnClickListener(v -> chooseImage());
        doneButton = findViewById(R.id.done);
        Button loginButton = findViewById(R.id.login);
        Button signupButton = findViewById(R.id.btn1);

        nameEditText = findViewById(R.id.name);
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);

        loginButton.setOnClickListener(v -> {
            emailEditText.setVisibility(View.VISIBLE);
            passwordEditText.setVisibility(View.VISIBLE);
            doneButton.setVisibility(View.VISIBLE);
            loginButton.setVisibility(View.GONE);

            doneButton.setOnClickListener(m -> {
                Login login = new Login();
                login.login(emailEditText.getText().toString(),
                        passwordEditText.getText().toString());
                Toast.makeText(MainActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
            });
        });

        signupButton.setOnClickListener(v -> {
            emailEditText.setVisibility(View.VISIBLE);
            passwordEditText.setVisibility(View.VISIBLE);
            doneButton.setVisibility(View.VISIBLE);
            signupButton.setVisibility(View.GONE);

            Signup sign = new Signup();
            doneButton.setOnClickListener(m -> {
                sign.signup(emailEditText.getText().toString(),
                        passwordEditText.getText().toString(),
                        nameEditText.getText().toString());
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












