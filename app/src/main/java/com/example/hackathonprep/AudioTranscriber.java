package com.example.hackathonprep;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.Arrays;
import java.util.List;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;

public class AudioTranscriber {
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/";
    private static final String API_KEY = "AIzaSyAmAZMJLmmjdUolKqk_r1V_BW8T6TiP99M"; // Replace with your actual API key

    private Context context;
    private GeminiApiService geminiApiService;

    public AudioTranscriber(Context context) {
        this.context = context;

        // Create Retrofit instance
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(GEMINI_API_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        geminiApiService = retrofit.create(GeminiApiService.class);
    }

    public static String transcribeAudio(Uri audioUri) throws Exception {
        // Read audio file and convert to base64
        String audioBase64 = readAudioFileToBase64(audioUri);

        // Create prompt for Gemini
        String prompt = "Transcribe this audio file to text. The audio is encoded in base64: " + audioBase64;

        // Create request using the proper class structure
        GeminiRequest.Content.Part part = new GeminiRequest.Content.Part(prompt);
        List<GeminiRequest.Content.Part> parts = Arrays.asList(part);
        GeminiRequest.Content content = new GeminiRequest.Content(parts);
        List<GeminiRequest.Content> contents = Arrays.asList(content);
        GeminiRequest request = new GeminiRequest(contents);

        // Make API call
        String authHeader = "Bearer " + API_KEY;
        Response<GeminiResponse> response = geminiApiService
                .generateContent("application/json", authHeader, request)
                .execute();

        if (response.isSuccessful() && response.body() != null) {
            return response.body().getTranscription();
        } else {
            throw new Exception("API call failed: " + response.message());
        }
    }

    private String readAudioFileToBase64(Uri audioUri) throws Exception {
        try (InputStream inputStream = context.getContentResolver().openInputStream(audioUri);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            byte[] audioBytes = outputStream.toByteArray();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return Base64.getEncoder().encodeToString(audioBytes);
            }
        }
        return "";
    }
}
