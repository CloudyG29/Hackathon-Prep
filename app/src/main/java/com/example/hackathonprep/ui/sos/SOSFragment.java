package com.example.hackathonprep.ui.sos;

import static com.example.hackathonprep.MainActivity.ACCESS_KEY;
import static com.example.hackathonprep.MainActivity.REQUEST_RECORD_AUDIO;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.example.hackathonprep.BuildConfig;
import com.example.hackathonprep.R;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import ai.picovoice.porcupine.Porcupine;
import ai.picovoice.porcupine.PorcupineException;


public class SOSFragment extends Fragment {

    private static final String TAG = "SOS";

    // Wake-word (Porcupine) mic
    private Porcupine porcupine;
    private AudioRecord hotwordRecord;
    private Thread hotwordThread;
    private volatile boolean isListening = false;

    // Recorder mic
    private AudioRecord micRecord;
    private Thread recordingThread;
    private volatile boolean isRecording = false;

    private CountDownTimer timer;
    private Handler mainHandler;

    // In-memory buffer for PCM while recording
    private ByteArrayOutputStream pcmBuffer;

    // Audio config
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    // AI (kept as you had it)
    private GenerativeModel generativeModel;
    private GenerativeModelFutures model;

    private Button SOSbtn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainHandler = new Handler(Looper.getMainLooper());
        initializeAIModel();

        ActivityCompat.requestPermissions(requireActivity(),
                new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_RECORD_AUDIO);

        try {
            porcupine = new Porcupine.Builder()
                    .setAccessKey(ACCESS_KEY)
                    .setKeywordPath(getAssetPath("GuardianHelp.ppn"))
                    .setSensitivity(1f)
                    .build(getContext());

            startWakeWordDetection();
        } catch (PorcupineException e) {
            Log.e(TAG, "Porcupine init failed", e);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_sos, container, false);
        SOSbtn = root.findViewById(R.id.btnSOS);
        SOSbtn.setOnClickListener(v -> onWakeWordDetected());
        return root;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startWakeWordDetection();
            } else {
                Toast.makeText(getContext(), "Microphone permission is required for SOS", Toast.LENGTH_LONG).show();
            }
        }
    }

    // ================= Wake Word =================
    private void startWakeWordDetection() {
        if (porcupine == null) return;
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        int bufferSize = AudioRecord.getMinBufferSize(
                porcupine.getSampleRate(),
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );

        if (bufferSize <= 0) bufferSize = porcupine.getFrameLength() * 2; // fallback

        hotwordRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                porcupine.getSampleRate(),
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
        );

        try {
            hotwordRecord.startRecording();
        } catch (IllegalStateException ise) {
            Log.e(TAG, "Hotword startRecording failed", ise);
            return;
        }

        isListening = true;

        hotwordThread = new Thread(() -> {
            short[] buf = new short[porcupine.getFrameLength()];
            while (isListening && hotwordRecord != null) {
                int read = hotwordRecord.read(buf, 0, buf.length);
                if (read > 0) {
                    try {
                        int keywordIndex = porcupine.process(buf);
                        if (keywordIndex >= 0) {
                            mainHandler.post(this::onWakeWordDetected);
                        }
                    } catch (PorcupineException e) {
                        Log.e(TAG, "Porcupine process error", e);
                    }
                }
            }
        }, "hotword-thread");
        hotwordThread.setPriority(Thread.MAX_PRIORITY);
        hotwordThread.start();
    }

    // ================= Confirm dialog =================
    private void onWakeWordDetected() {
        long countdownMillis = 5000;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle("Distress Alert")
                .setMessage("Emergency recording will start in 5 seconds. Tap CANCEL to stop.")
                .setCancelable(false)
                .setNegativeButton("CANCEL", (dialog, which) -> {
                    if (timer != null) timer.cancel();
                    dialog.dismiss();
                    // Ensure no recording starts and stop any active recording
                    stopRecordingIfActive();
                    Toast.makeText(getContext(), "Alert canceled.", Toast.LENGTH_SHORT).show();
                });

        AlertDialog alertDialog = builder.create();

        timer = new CountDownTimer(countdownMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                alertDialog.setMessage("Emergency recording will start in " + (millisUntilFinished / 1000) + " seconds.\nTap CANCEL to stop.");
            }

            @Override
            public void onFinish() {
                if (!alertDialog.isShowing()) return; // safety
                alertDialog.dismiss();
                startWavRecording();
            }
        };

        alertDialog.show();
        timer.start();
    }

    // ================= Recording to memory =================
    private void startWavRecording() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Record Audio permission not granted");
            return;
        }
        if (isRecording) return; // already running

        int minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        if (minBuf <= 0) minBuf = SAMPLE_RATE; // fallback

        micRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                minBuf
        );

        try {
            micRecord.startRecording();
        } catch (IllegalStateException ise) {
            Log.e(TAG, "Recorder startRecording failed", ise);
            return;
        }

        isRecording = true;
        pcmBuffer = new ByteArrayOutputStream();

        final int readSize = minBuf;
        recordingThread = new Thread(() -> {
            byte[] temp = new byte[readSize];
            while (isRecording && micRecord != null) {
                int read = micRecord.read(temp, 0, temp.length);
                if (read > 0) {
                    pcmBuffer.write(temp, 0, read);
                }
            }
        }, "pcm-recorder");
        recordingThread.start();

        // Auto-stop after 10s
        mainHandler.postDelayed(this::stopWavRecording, 10_000);
    }

    private void stopWavRecording() {
        if (!isRecording) return;
        isRecording = false;

        if (recordingThread != null) {
            try { recordingThread.join(500); } catch (InterruptedException ignored) {}
            recordingThread = null;
        }

        if (micRecord != null) {
            try { micRecord.stop(); } catch (Exception ignored) {}
            try { micRecord.release(); } catch (Exception ignored) {}
            micRecord = null;
        }

        // Build WAV in memory and upload
        if (pcmBuffer != null) {
            byte[] pcm = pcmBuffer.toByteArray();
            pcmBuffer = null;
            byte[] wav = buildWavBytes(pcm, SAMPLE_RATE, 1, 16);
            uploadWavToFirebase(wav);
        } else {
            Toast.makeText(getContext(), "No audio captured", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecordingIfActive() {
        // cancel auto-stop callback if queued
        mainHandler.removeCallbacks(this::stopWavRecording);
        stopWavRecording();
    }

    // ================= WAV utils =================
    private byte[] buildWavBytes(byte[] pcmData, int sampleRate, int channels, int bitsPerSample) {
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int totalDataLen = pcmData.length + 36;

        ByteArrayOutputStream out = new ByteArrayOutputStream(44 + pcmData.length);
        try {
            // RIFF header
            out.write(new byte[]{'R','I','F','F'});
            writeIntLE(out, totalDataLen);
            out.write(new byte[]{'W','A','V','E'});

            // fmt chunk
            out.write(new byte[]{'f','m','t',' '});
            writeIntLE(out, 16);              // Subchunk1Size for PCM
            writeShortLE(out, (short)1);      // AudioFormat PCM
            writeShortLE(out, (short)channels);
            writeIntLE(out, sampleRate);
            writeIntLE(out, byteRate);
            writeShortLE(out, (short)(channels * bitsPerSample / 8));
            writeShortLE(out, (short)bitsPerSample);

            // data chunk
            out.write(new byte[]{'d','a','t','a'});
            writeIntLE(out, pcmData.length);
            out.write(pcmData);
        } catch (IOException e) {
            Log.e(TAG, "WAV build error", e);
        }
        return out.toByteArray();
    }

    private void writeIntLE(ByteArrayOutputStream out, int value) throws IOException {
        out.write(value & 0xff);
        out.write((value >> 8) & 0xff);
        out.write((value >> 16) & 0xff);
        out.write((value >> 24) & 0xff);
    }

    private void writeShortLE(ByteArrayOutputStream out, short value) throws IOException {
        out.write(value & 0xff);
        out.write((value >> 8) & 0xff);
    }

    // ================= Firebase =================
    private void uploadWavToFirebase(byte[] wavBytes) {
        if (wavBytes == null || wavBytes.length == 0) {
            Toast.makeText(getContext(), "Empty audio", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference rootRef = storage.getReference();
        String fileName = "recordings/emergency_" + System.currentTimeMillis() + ".wav";
        StorageReference audioRef = rootRef.child(fileName);

        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("audio/wav")
                .build();

        UploadTask uploadTask = audioRef.putBytes(wavBytes, metadata);

        uploadTask
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Upload failed", e);
                    Toast.makeText(getContext(), "❌ Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                })
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "Upload success, bytes= " + taskSnapshot.getTotalByteCount());
                    // Retrieve download URL after successful upload
                    audioRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String downloadUrl = uri.toString();
                                Log.d(TAG, "Download URL: " + downloadUrl);
                                saveIncident(downloadUrl, "audio_recording");
                                Toast.makeText(getContext(), "✅ Uploaded: " + downloadUrl, Toast.LENGTH_LONG).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "getDownloadUrl failed", e);
                                Toast.makeText(getContext(), "Uploaded, but URL access denied (check rules): " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
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
                .addOnSuccessListener(doc -> Log.d(TAG, "Saved incident"))
                .addOnFailureListener(e -> Log.e(TAG, "Firestore error", e));
    }

    // ================= AI (unchanged behavior) =================
    private void initializeAIModel() {
        generativeModel = new GenerativeModel(
                "gemini-1.5-flash",
                BuildConfig.GEMINI_API_KEY
        );
        model = GenerativeModelFutures.from(generativeModel);
    }

    private void classifyTranscript(String transcript) {
        new Thread(() -> {
            try {
                String prompt = "Classify the following transcript as 'Fire, GBV, Medical, House break in':\n" + transcript;
                Content content = new Content.Builder().addText(prompt).build();
                GenerateContentResponse response = model.generateContent(content).get();
                String aiResponse = response.getText();
                if (aiResponse != null) {
                    saveIncident(aiResponse, "distress_classification");
                }
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "AI classify error", e);
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    // ================= Helpers =================
    private String getAssetPath(String assetFileName) {
        try {
            java.io.File file = new java.io.File(requireContext().getFilesDir(), assetFileName);
            if (!file.exists()) {
                try (InputStream is = requireContext().getAssets().open(assetFileName);
                     java.io.FileOutputStream os = new java.io.FileOutputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = is.read(buffer)) > 0) {
                        os.write(buffer, 0, length);
                    }
                }
            }
            return file.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Asset copy failed", e);
            return null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cancel any pending auto-stop
        if (mainHandler != null) mainHandler.removeCallbacks(this::stopWavRecording);
        if (timer != null) timer.cancel();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop wake-word
        isListening = false;
        if (hotwordThread != null) {
            try { hotwordThread.join(300); } catch (InterruptedException ignored) {}
            hotwordThread = null;
        }
        if (hotwordRecord != null) {
            try { hotwordRecord.stop(); } catch (Exception ignored) {}
            try { hotwordRecord.release(); } catch (Exception ignored) {}
            hotwordRecord = null;
        }

        // Stop any active recording
        stopRecordingIfActive();

        if (porcupine != null) {
            porcupine.delete();
            porcupine = null;
        }
    }
}
