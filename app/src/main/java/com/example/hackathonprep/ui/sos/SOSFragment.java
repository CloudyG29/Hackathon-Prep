package com.example.hackathonprep.ui.sos;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.hackathonprep.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class SOSFragment extends Fragment {

    private Button btnSOS;
    private TextView tvSOSStatus;
    private FusedLocationProviderClient fusedLocationClient;

    // Put emergency contacts here
    private final String[] emergencyNumbers = {"+27681558983", "+27694548748"};

    private static final int PERMISSION_REQUEST_CODE = 101;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sos, container, false);

        btnSOS = view.findViewById(R.id.btnSOS);
        tvSOSStatus = view.findViewById(R.id.tvSOSStatus);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        btnSOS.setOnClickListener(v -> sendSOS());

        return view;
    }

    private void sendSOS() {
        // Check permissions
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
            return;
        }

        // Get location
        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    String message = "🚨 SOS! I need help. My location: " +
                            "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();

                    for (String number : emergencyNumbers) {
                        try {
                            SmsManager smsManager = SmsManager.getDefault();
                            smsManager.sendTextMessage(number, null, message, null, null);
                        } catch (Exception e) {
                            Toast.makeText(requireContext(), "Failed to send to " + number, Toast.LENGTH_SHORT).show();
                        }
                    }

                    tvSOSStatus.setText("SOS sent successfully!");
                } else {
                    tvSOSStatus.setText("Unable to fetch location.");
                }
            }
        });
    }

    // Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendSOS();
            } else {
                Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
