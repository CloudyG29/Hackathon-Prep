package com.example.hackathonprep;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ai.picovoice.porcupine.Porcupine;
import ai.picovoice.porcupine.PorcupineException;

public class MainActivity extends AppCompatActivity {

    private Porcupine porcupine;
    private AudioRecord audioRecord;
    private boolean isListening = false;
    private Thread recordingThread;
    private Button dangerButton;
    private TextView ans;
    private static final int REQUEST_RECORD_AUDIO = 1;
    private CountDownTimer timer;

    private boolean isRecording = false;
    private File wavFile;

    // AI Components
    private GenerativeModel generativeModel;
    private GenerativeModelFutures model;
    private static final String ACCESS_KEY = "oUD1Hs9X9838Yhni2sjw+n8aaWXSZBzB11dC/xXLZkXB7VK+GaA0LQ==";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

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
                startWavRecording();
            }
        });

        // Request microphone permission
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_RECORD_AUDIO);

        try {
            // Initialize Porcupine with your safeword model
            porcupine = new Porcupine.Builder()
                    .setAccessKey(ACCESS_KEY)
                    .setKeywordPath(getAssetPath("GuardianHelp.ppn"))
                    .setSensitivity(1f)
                    .build(this);

            startWakeWordDetection();

        } catch (PorcupineException e) {
            e.printStackTrace();
        }
    }

    // ------------------- WAV RECORDING -------------------

    private void startWavRecording() {
        int sampleRate = 16000;
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
        );

        wavFile = new File(getExternalFilesDir(null),
                "emergency_recording_" + System.currentTimeMillis() + ".wav");

        audioRecord.startRecording();
        isRecording = true;

        new Thread(() -> writeAudioDataToWavFile(wavFile, bufferSize, sampleRate)).start();

        runOnUiThread(() -> ans.setText("🔴 Recording emergency audio..."));

        new Handler().postDelayed(this::stopWavRecording, 10000);
    }

    private void stopWavRecording() {
        if (audioRecord != null && isRecording) {
            isRecording = false;
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;

            runOnUiThread(() -> ans.setText("✅ Recording completed. Transcribing..."));
            uploadAudioToFirebase(wavFile);
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        LatLng campus = new LatLng(-25.7545, 28.2314); // Example: Pretoria campus
        mMap.addMarker(new MarkerOptions()
                .position(campus)
                .title("My Campus")
                .snippet("Tap me for more info"));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(campus, 15));


        // Options: NORMAL, SATELLITE, TERRAIN, HYBRID
        mMap.addCircle(new CircleOptions()
                .center(campus)
                .radius(200) // in meters
                .strokeColor(Color.BLUE)
                .fillColor(0x220000FF));


        // Example: put a marker on Johannesburg
        LatLng joburg = new LatLng(-26.2041, 28.0473);
        mMap.addMarker(new MarkerOptions().position(joburg).title("Marker in Joburg"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(joburg, 12));
    }
}


    private void writeAudioDataToWavFile(File file, int bufferSize, int sampleRate) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[bufferSize];
            int totalAudioLen = 0;

            writeWavHeader(fos, sampleRate, 1, 16, 0);

            while (isRecording) {
                int read = audioRecord.read(buffer, 0, buffer.length);
                if (read > 0) {
                    fos.write(buffer, 0, read);
                    totalAudioLen += read;
                }
            }

            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            writeWavHeader(raf, sampleRate, 1, 16, totalAudioLen);
            raf.close();

        } catch (IOException e) {
            Log.e("AUDIO", "Recording failed", e);
        }
    }

    private void writeWavHeader(FileOutputStream out, int sampleRate, int channels,
                                int bitsPerSample, long totalAudioLen) throws IOException {
        long byteRate = sampleRate * channels * bitsPerSample / 8;
        long totalDataLen = totalAudioLen + 36;

        byte[] header = new byte[44];
        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';
        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0;
        header[20] = 1; header[21] = 0;
        header[22] = (byte) channels; header[23] = 0;
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (channels * bitsPerSample / 8); header[33] = 0;
        header[34] = (byte) bitsPerSample; header[35] = 0;
        header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    private void writeWavHeader(RandomAccessFile raf, int sampleRate, int channels,
                                int bitsPerSample, long totalAudioLen) throws IOException {
        raf.seek(0);
        writeWavHeader(new FileOutputStream(raf.getFD()), sampleRate, channels, bitsPerSample, totalAudioLen);
    }


    // ------------------- CLASSIFICATION (GEMINI) -------------------
    private void initializeAIModel() {
        generativeModel = new GenerativeModel(
                "gemini-1.5-flash",
                BuildConfig.GEMINI_API_KEY
        );
        model = GenerativeModelFutures.from(generativeModel);
    }

    private void classifyTranscript(String transcript) {
        runOnUiThread(() -> ans.setText("🔄 Classifying distress..."));

        new Thread(() -> {
            try {
                String prompt = "Classify the following transcript as 'Fire, GBV, Medical, House break in':\n" + transcript;

                Content content = new Content.Builder()
                        .addText(prompt)
                        .build();

                GenerateContentResponse response = model.generateContent(content).get();
                String aiResponse = response.getText();

                if (aiResponse == null) {
                    throw new IllegalStateException("AI returned null response");
                }

                runOnUiThread(() -> ans.setText("Classification: " + aiResponse));
                saveIncident(aiResponse, "distress_classification");

            } catch (ExecutionException e) {
                runOnUiThread(() -> ans.setText("Classification failed: " + e.getCause().getMessage()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                runOnUiThread(() -> ans.setText("Classification interrupted"));
            }
        }).start();
    }

    // ------------------- PORCUPINE (WAKE WORD) -------------------
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
                            runOnUiThread(this::triggerDangerButton);
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
        long countdownMillis = 5000;

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
                startWavRecording();
            }
        };

        alertDialog.show();
        timer.start();
    }

    // ------------------- FIREBASE SAVE -------------------

    private void uploadAudioToFirebase(File audioFile) {
        if (audioFile == null || !audioFile.exists()) {
            runOnUiThread(() -> Toast.makeText(this, "No audio file found", Toast.LENGTH_SHORT).show());
            return;
        }

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        // Save in folder "recordings" with unique filename
        Uri fileUri = Uri.fromFile(audioFile);
        StorageReference audioRef = storageRef.child("recordings/" + audioFile.getName());

        audioRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    audioRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();

                        // Save metadata + download URL to Firestore
                        saveIncident(downloadUrl, "audio_recording");

                        runOnUiThread(() -> ans.setText("✅ Audio uploaded successfully! @ " + downloadUrl));
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE", "Upload failed", e);
                    runOnUiThread(() -> ans.setText("❌ Upload failed: " + e.getMessage()));
                });
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
