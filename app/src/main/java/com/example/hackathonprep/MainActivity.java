package com.example.hackathonprep;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import com.example.hackathonprep.BuildConfig;
import ai.picovoice.porcupine.Porcupine;
import ai.picovoice.porcupine.PorcupineException;


import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;



public class MainActivity extends AppCompatActivity {
    private Porcupine porcupine;
    private AudioRecord audioRecord;
    private boolean isListening = false;
    private Thread recordingThread;
    private Button dangerButton;
    private TextView ans;
    private static final int REQUEST_RECORD_AUDIO = 1;
    private CountDownTimer timer;

    private MediaRecorder recorder;
    private File audioFile;
    String geminiApiKey = BuildConfig.GEMINI_API_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dangerButton = findViewById(R.id.dangerButton);
        ans = findViewById(R.id.ans);


        dangerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                //TODO: alert the community or something i donno
                Toast.makeText(MainActivity.this, "Safeword detected! Triggering danger action!", Toast.LENGTH_LONG).show();
                startRecording();
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

    private void startRecording() {
        try {
            // Create the directory if it doesn't exist
            File externalDir = getExternalFilesDir(null);
            if (externalDir != null && !externalDir.exists()) {
                externalDir.mkdirs();
            }

            audioFile = new File(externalDir, "distress_audio.wav");

            // Delete any existing file with the same name
            if (audioFile.exists()) {
                audioFile.delete();
            }

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setOutputFile(audioFile.getAbsolutePath());
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            recorder.prepare();
            recorder.start();

            // Record for 10 sec then stop
            new Handler().postDelayed(this::stopRecording, 10000);

        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                ans.setText("Recording failed: " + e.getMessage());
            });
        }
    }

    private void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;

        //call Gemini
        transcribeAudioFile(audioFile);
    }

    @SuppressLint("SetTextI18n")
    private void transcribeAudioFile(File audioFile) {
        runOnUiThread(() -> {
            ans.setText("Checking file...");
        });

        // Debug: Check if file exists and is readable
        if (!audioFile.exists()) {
            runOnUiThread(() -> {
                ans.setText("Error: File doesn't exist: " + audioFile.getAbsolutePath());
            });
            return;
        }

        if (!audioFile.canRead()) {
            runOnUiThread(() -> {
                ans.setText("Error: Cannot read file: " + audioFile.getAbsolutePath());
            });
            return;
        }
        runOnUiThread(() -> {
            ans.setText("Transcribing...");
            //transcribeButton.setEnabled(false);
        });

        new Thread(() -> {
            try {
                // Convert File to Uri - use FileProvider for better security
                Uri audioUri = FileProvider.getUriForFile(
                        MainActivity.this,
                        getPackageName() + ".provider",
                        audioFile
                );

                String transcription = AudioTranscriber.transcribeAudio(audioUri);

                runOnUiThread(() -> {
                    ans.setText(transcription);
                    //transcribeButton.setEnabled(true);
                    Toast.makeText(MainActivity.this, "Transcription completed!", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    ans.setText("Error: " + e.getMessage());
                   // transcribeButton.setEnabled(true);
                    Toast.makeText(MainActivity.this, "Transcription failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
            }
        }).start();
    }



    private void saveIncident(String transcript, String category) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> incident = new HashMap<>();
        incident.put("transcript", transcript);
        incident.put("category", category);
        incident.put("timestamp", new Date());
        incident.put("userId", FirebaseAuth.getInstance().getUid());

        db.collection("incidents").add(incident)
                .addOnSuccessListener(doc -> Log.d("FIREBASE", "Saved incident"))
                .addOnFailureListener(e -> Log.e("FIREBASE", "Error", e));
    }

}