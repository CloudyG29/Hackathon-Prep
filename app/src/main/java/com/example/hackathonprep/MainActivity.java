package com.example.hackathonprep;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
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

    private AudioRecord audioRecord;
    private boolean isListening = false;
    private Thread recordingThread;
    private Button dangerButton;
    private static final int REQUEST_RECORD_AUDIO = 1;
    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.btn2), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dangerButton = findViewById(R.id.dangerbutton);
        Button doneButton = findViewById(R.id.done);



        dangerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                //TODO: alert the community or something i donno
                Toast.makeText(MainActivity.this, "Safeword detected! Triggering danger action!", Toast.LENGTH_LONG).show();
            }
        });
        // Request microphone permission
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_RECORD_AUDIO);

        try {
            // Initialize Porcupine with your safeword model
            porcupine = new Porcupine.Builder()
                    .setAccessKey("oUD1Hs9X9838Yhni2sjw+n8aaWXSZBzB11dC/xXLZkXB7VK+GaA0LQ==")
                    .setKeywordPath(getAssetPath("GuardianHelp.ppn"))
                    .setSensitivity(1f)
                    .build(this);


            startWakeWordDetection();

        } catch (PorcupineException e) {
            e.printStackTrace();
        }
        Button loginButton = findViewById(R.id.login);
         nameEditText = findViewById(R.id.name);
         emailEditText = findViewById(R.id.email);
         passwordEditText = findViewById(R.id.password);
        loginButton.setOnClickListener(v ->  {
            emailEditText.setVisibility(View.VISIBLE);
            passwordEditText.setVisibility(View.VISIBLE);
            doneButton.setVisibility(View.VISIBLE);
            loginButton.setVisibility(View.GONE);
            doneButton.setOnClickListener(m -> {
                Login login=new Login();
                login.login(emailEditText.getText().toString(), passwordEditText.getText().toString());
            });

        });

        Button signupButton = findViewById(R.id.btn1);
          nameEditText = findViewById(R.id.name);
         emailEditText = findViewById(R.id.email);
         passwordEditText = findViewById(R.id.password);
        signupButton.setOnClickListener(v ->  {
            emailEditText.setVisibility(View.VISIBLE);
            passwordEditText.setVisibility(View.VISIBLE);
            doneButton.setVisibility(View.VISIBLE);
            Signup sign=new Signup();
            doneButton.setOnClickListener(m -> {
                        sign.signup(emailEditText.getText().toString(), passwordEditText.getText().toString(), nameEditText.getText().toString());
                    });
            signupButton.setVisibility(View.GONE);

        });




    }

    private void startWakeWordDetection() {
        int bufferSize = AudioRecord.getMinBufferSize(
                porcupine.getSampleRate(),
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                porcupine.getSampleRate(),
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        audioRecord.startRecording();
        isListening = true;

        recordingThread = new Thread(() -> {
            short[] buffer = new short[porcupine.getFrameLength()];
            while (isListening) {
                int result = audioRecord.read(buffer, 0, buffer.length);
                if (result > 0) {
                    try {
                        int keywordIndex = porcupine.process(buffer);
                        if (keywordIndex >= 0) {
                            runOnUiThread(() -> triggerDangerButton());
                        }
                    } catch (PorcupineException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        recordingThread.setPriority(Thread.MAX_PRIORITY);
        recordingThread.start();
    }
    private String getAssetPath(String assetFileName) {
        File file = new File(getFilesDir(), assetFileName);
        if (!file.exists()) {
            try (InputStream is = getAssets().open(assetFileName);
                 FileOutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file.getAbsolutePath();
    }


    private void triggerDangerButton() {
        //Toast.makeText(MainActivity.this, "Safeword detected! Triggering danger action!", Toast.LENGTH_LONG).show();
        dangerButton.performClick();
        onWakeWordDetected();
    }

    protected void onDestroy() {
        super.onDestroy();
        isListening = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
        }
        if (porcupine != null) {
            porcupine.delete();
        }
    }
    private void onWakeWordDetected() {
        long countdownMillis = 5000; // 5 seconds

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Distress Alert");
        builder.setMessage("Distress alert will be sent in 5 seconds.\nTap CANCEL to stop.");
        builder.setCancelable(false);

        builder.setNegativeButton("CANCEL", (dialog, which) -> {
            if (timer != null) {
                timer.cancel(); // stop the countdown
            }
            dialog.dismiss();
            Toast.makeText(MainActivity.this, "Alert canceled.", Toast.LENGTH_SHORT).show();
        });

        AlertDialog alertDialog = builder.create();

        timer = new CountDownTimer(countdownMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                alertDialog.setMessage(
                        "Distress alert will be sent in " + (millisUntilFinished / 1000) + " seconds.\nTap CANCEL to stop."
                );
            }

            @Override
            public void onFinish() {
                alertDialog.dismiss();
                //sendDistressAlert(message);
            }
        };

        alertDialog.show();
        timer.start();
    }


}