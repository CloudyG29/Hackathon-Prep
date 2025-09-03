package com.example.hackathonprep;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.hackathonprep.BuildConfig;
import ai.picovoice.porcupine.Porcupine;
import ai.picovoice.porcupine.PorcupineException;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private Porcupine porcupine;
    private AudioRecord audioRecord;
    private boolean isListening = false;
    private Thread recordingThread;
    private Button dangerButton;
    private TextView ans;
    private static final int REQUEST_RECORD_AUDIO = 1;
    private CountDownTimer timer;

    // AI Components
    private GenerativeModel generativeModel;
    private GenerativeModelFutures model;

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

        // Initialize AI model
        initializeAIModel();

        dangerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "Safeword detected! Triggering danger action!", Toast.LENGTH_LONG).show();
                generateAIResponse();
            }
        });

        // Request microphone permission for wake word detection only
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

    private void initializeAIModel() {
        // Initialize the Generative AI model
        generativeModel = new GenerativeModel(
                "gemini-1.5-flash",
                BuildConfig.GEMINI_API_KEY
        );
        model = GenerativeModelFutures.from(generativeModel);
    }

    private void generateAIResponse() {
        runOnUiThread(() -> {
            ans.setText("🔄 Generating AI response...");
        });

        new Thread(() -> {
            try {
                // CUSTOMIZABLE PROMPT - Modify this based on your needs
                String prompt = "can you access my location because i want you to give me the coordinates of the places in my 50 km radius where there has been a crime reported";

                Content content = new Content.Builder()
                        .addText(prompt)
                        .build();

                GenerateContentResponse response = model.generateContent(content).get();
                String aiResponse = response.getText();

                runOnUiThread(() -> {
                    ans.setText(aiResponse);
                    Toast.makeText(MainActivity.this, "AI response generated!", Toast.LENGTH_SHORT).show();

                    // Save to Firebase
                    saveIncident(aiResponse, "ai_emergency_response");
                });

            } catch (ExecutionException | InterruptedException e) {
                Log.e("AI_RESPONSE", "Error generating AI response", e);
                runOnUiThread(() -> {
                    ans.setText("Error: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "AI response failed", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void startWakeWordDetection() {
        int bufferSize = AudioRecord.getMinBufferSize(
                porcupine.getSampleRate(),
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
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
        builder.setMessage("Emergency response will be triggered in 5 seconds.\nTap CANCEL to stop.");
        builder.setCancelable(false);

        builder.setNegativeButton("CANCEL", (dialog, which) -> {
            if (timer != null) {
                timer.cancel();
            }
            dialog.dismiss();
            Toast.makeText(this, "Alert canceled.", Toast.LENGTH_SHORT).show();
        });

        AlertDialog alertDialog = builder.create();

        timer = new CountDownTimer(countdownMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                alertDialog.setMessage(
                        "Emergency response will be triggered in " + (millisUntilFinished / 1000) + " seconds.\nTap CANCEL to stop."
                );
            }

            @Override
            public void onFinish() {
                alertDialog.dismiss();
                generateAIResponse();
            }
        };

        alertDialog.show();
        timer.start();
    }

    private void saveIncident(String response, String category) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> incident = new HashMap<>();
        incident.put("response", response);
        incident.put("category", category);
        incident.put("timestamp", new Date());
        incident.put("userId", FirebaseAuth.getInstance().getUid());

        db.collection("incidents").add(incident)
                .addOnSuccessListener(doc -> Log.d("FIREBASE", "Saved incident"))
                .addOnFailureListener(e -> Log.e("FIREBASE", "Error", e));
    }
}