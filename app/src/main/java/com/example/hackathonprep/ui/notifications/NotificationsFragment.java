package com.example.hackathonprep.ui.notifications;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.example.hackathonprep.R;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private RecyclerView rvAlerts;
    private Spinner spinnerFilters;
    private AlertsAdapter adapter;
    private List<Alert> allAlerts;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        rvAlerts = view.findViewById(R.id.rvAlerts);
        spinnerFilters = view.findViewById(R.id.spinnerFilters);

        // Setup RecyclerView
        rvAlerts.setLayoutManager(new LinearLayoutManager(getContext()));
        allAlerts = getSampleAlerts(); // Replace with real data later
        adapter = new AlertsAdapter(allAlerts);
        rvAlerts.setAdapter(adapter);

        // Setup Spinner
        String[] filters = {"All", "Crime", "GBV", "Harassment"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, filters);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilters.setAdapter(spinnerAdapter);

        spinnerFilters.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedFilter = filters[position];
                filterAlerts(selectedFilter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        return view;
    }

    private void filterAlerts(String type) {
        List<Alert> filteredList = new ArrayList<>();
        for (Alert alert : allAlerts) {
            if (type.equals("All") || alert.getType().equals(type)) {
                filteredList.add(alert);
            }
        }
        adapter.updateList(filteredList);
    }

    // Sample data
    private List<Alert> getSampleAlerts() {
        List<Alert> list = new ArrayList<>();
        list.add(new Alert("GBV", "Hillbrow, Johannesburg", "10 minutes ago"));
        list.add(new Alert("Crime", "Soweto, Johannesburg", "20 minutes ago"));
        list.add(new Alert("Harassment", "Sandton, Johannesburg", "5 minutes ago"));
        return list;
    }
}