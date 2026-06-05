package com.opengrid.simplerelationshiptracker.ui.calendar;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.opengrid.simplerelationshiptracker.R;
import com.opengrid.simplerelationshiptracker.data.PreferenceManager;
import com.opengrid.simplerelationshiptracker.databinding.FragmentCalendarBinding;
import com.opengrid.simplerelationshiptracker.model.Event;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class CalendarFragment extends Fragment {

    private FragmentCalendarBinding binding;
    private PreferenceManager preferenceManager;
    private List<Event> allEvents;
    private List<Event> filteredEvents;
    private EventAdapter adapter;
    private final Calendar selectedCalendar = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        preferenceManager = new PreferenceManager(requireContext());
        
        loadEvents();
        
        binding.recyclerEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        filteredEvents = new ArrayList<>();
        adapter = new EventAdapter();
        binding.recyclerEvents.setAdapter(adapter);

        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedCalendar.set(year, month, dayOfMonth);
            filterEvents(year, month, dayOfMonth);
        });

        binding.fabAddEvent.setOnClickListener(v -> showAddEventDialog());

        // Initial filter for today
        Calendar today = Calendar.getInstance();
        filterEvents(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));

        return binding.getRoot();
    }

    private void loadEvents() {
        String json = preferenceManager.getEventsJson();
        allEvents = new Gson().fromJson(json, new TypeToken<List<Event>>(){}.getType());
        if (allEvents == null) allEvents = new ArrayList<>();
    }

    private void saveEvents() {
        String json = new Gson().toJson(allEvents);
        preferenceManager.setEventsJson(json);
    }

    private void filterEvents(int year, int month, int day) {
        filteredEvents.clear();
        Calendar cal = Calendar.getInstance();
        for (Event e : allEvents) {
            cal.setTimeInMillis(e.timestamp);
            if (cal.get(Calendar.YEAR) == year && 
                cal.get(Calendar.MONTH) == month && 
                cal.get(Calendar.DAY_OF_MONTH) == day) {
                filteredEvents.add(e);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showAddEventDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_event, null);
        EditText editName = dialogView.findViewById(R.id.edit_event_name);
        TextView textDateTime = dialogView.findViewById(R.id.text_selected_date_time);
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        textDateTime.setText(sdf.format(selectedCalendar.getTime()));

        textDateTime.setOnClickListener(v -> {
            new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                selectedCalendar.set(Calendar.YEAR, year);
                selectedCalendar.set(Calendar.MONTH, month);
                selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                
                new TimePickerDialog(requireContext(), (view1, hourOfDay, minute) -> {
                    selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedCalendar.set(Calendar.MINUTE, minute);
                    textDateTime.setText(sdf.format(selectedCalendar.getTime()));
                }, selectedCalendar.get(Calendar.HOUR_OF_DAY), selectedCalendar.get(Calendar.MINUTE), true).show();
                
            }, selectedCalendar.get(Calendar.YEAR), selectedCalendar.get(Calendar.MONTH), selectedCalendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.add_event)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String name = editName.getText().toString();
                    if (!name.isEmpty()) {
                        Event newEvent = new Event(UUID.randomUUID().toString(), name, selectedCalendar.getTimeInMillis());
                        allEvents.add(newEvent);
                        saveEvents();
                        filterEvents(selectedCalendar.get(Calendar.YEAR), selectedCalendar.get(Calendar.MONTH), selectedCalendar.get(Calendar.DAY_OF_MONTH));
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Event event = filteredEvents.get(position);
            holder.name.setText(event.name);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            holder.time.setText(sdf.format(new Date(event.timestamp)));
            holder.delete.setOnClickListener(v -> {
                allEvents.remove(event);
                saveEvents();
                filterEvents(selectedCalendar.get(Calendar.YEAR), selectedCalendar.get(Calendar.MONTH), selectedCalendar.get(Calendar.DAY_OF_MONTH));
            });
        }

        @Override
        public int getItemCount() {
            return filteredEvents.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, time;
            ImageButton delete;
            ViewHolder(View v) {
                super(v);
                name = v.findViewById(R.id.text_event_name);
                time = v.findViewById(R.id.text_event_time);
                delete = v.findViewById(R.id.btn_delete_event);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}