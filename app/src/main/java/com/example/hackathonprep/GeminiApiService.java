package com.example.hackathonprep;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface GeminiApiService {
    @POST("v1beta/models/gemini-pro:generateContent")
    Call<GeminiResponse> generateContent(
            @Header("Content-Type") String contentType,
            @Header("Authorization") String authorization,
            @Body GeminiRequest request
    );
}
