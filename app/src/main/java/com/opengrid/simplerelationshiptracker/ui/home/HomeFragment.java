package com.opengrid.simplerelationshiptracker.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.opengrid.simplerelationshiptracker.R;
import com.opengrid.simplerelationshiptracker.api.TimeApiService;
import com.opengrid.simplerelationshiptracker.api.TimeResponse;
import com.opengrid.simplerelationshiptracker.data.PreferenceManager;
import com.opengrid.simplerelationshiptracker.databinding.FragmentHomeBinding;
import com.opengrid.simplerelationshiptracker.model.Event;
import com.opengrid.simplerelationshiptracker.util.RelationshipCalculator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private PreferenceManager preferenceManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateCounter();
            handler.postDelayed(this, 1000);
        }
    };

    private TimeApiService timeApiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        preferenceManager = new PreferenceManager(requireContext());

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://www.timeapi.io/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        timeApiService = retrofit.create(TimeApiService.class);

        binding.fabSync.setOnClickListener(v -> syncTime());

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.post(ticker);
        updateSyncInfo();
        updateEvents();
        updateStaticLabels();
    }

    private void updateStaticLabels() {
        String lang = preferenceManager.getLanguage();
        binding.fabSync.setText(preferenceManager.getTranslation(lang, "sync_button", getString(R.string.sync_button)));
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
    }

    private void updateCounter() {
        if (binding == null) return;
        long startDate = preferenceManager.getStartDateMillis();
        if (startDate == 0) {
            binding.textYears.setText(R.string.set_start_date_hint);
            binding.textMonths.setText("");
            binding.textDays.setText("");
            binding.textTime.setText("");
            return;
        }

        long currentTime = System.currentTimeMillis() + preferenceManager.getServerOffset();
        String tzId = preferenceManager.getTimeZoneId();
        String lang = preferenceManager.getLanguage();

        RelationshipCalculator.Duration duration = RelationshipCalculator.calculateDuration(startDate, currentTime, tzId);

        String yearsT = preferenceManager.getTranslation(lang, "years_label", getString(R.string.years_label));
        if (yearsT.contains("%d")) {
            binding.textYears.setText(String.format(yearsT, (int)duration.years));
        } else {
            binding.textYears.setText(duration.years + " " + yearsT);
        }

        String monthsT = preferenceManager.getTranslation(lang, "months_label", getString(R.string.months_label));
        if (monthsT.contains("%d")) {
            binding.textMonths.setText(String.format(monthsT, (int)duration.months));
        } else {
            binding.textMonths.setText(duration.months + " " + monthsT);
        }

        String daysT = preferenceManager.getTranslation(lang, "days_full", getString(R.string.days_full));
        if (daysT.contains("%d")) {
            binding.textDays.setText(String.format(daysT, (int)duration.days));
        } else {
            binding.textDays.setText(duration.days + " " + daysT);
        }

        binding.textTime.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", duration.hours, duration.minutes, duration.seconds));
        
        String tzLabel = preferenceManager.getTranslation(lang, "timezone_label", getString(R.string.timezone_label));
        binding.textTimezone.setText(String.format(tzLabel, tzId));
    }

    private void updateSyncInfo() {
        if (binding == null) return;
        long lastSync = preferenceManager.getLastSyncTime();
        String lang = preferenceManager.getLanguage();
        if (lastSync == 0) {
            binding.textLastSync.setText(preferenceManager.getTranslation(lang, "last_sync_never", getString(R.string.last_sync_never)));
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            String syncLabel = preferenceManager.getTranslation(lang, "last_sync_label", getString(R.string.last_sync_label));
            binding.textLastSync.setText(String.format(syncLabel, sdf.format(new Date(lastSync))));
        }
    }

    private void updateEvents() {
        if (binding == null) return;
        binding.layoutEventsList.removeAllViews();
        
        String json = preferenceManager.getEventsJson();
        List<Event> allEvents = new Gson().fromJson(json, new TypeToken<List<Event>>(){}.getType());
        if (allEvents == null) allEvents = new ArrayList<>();

        long now = System.currentTimeMillis();
        long fiveDaysMillis = TimeUnit.DAYS.toMillis(5);
        boolean hasEvents = false;

        for (Event event : allEvents) {
            long diff = event.timestamp - now;
            if (diff > 0 && diff <= fiveDaysMillis) {
                hasEvents = true;
                addEventView(event, diff);
            }
        }

        binding.cardEvents.setVisibility(hasEvents ? View.VISIBLE : View.GONE);
    }

    private void addEventView(Event event, long diffMillis) {
        View view = getLayoutInflater().inflate(R.layout.item_home_event, binding.layoutEventsList, false);
        TextView nameText = view.findViewById(R.id.text_event_name);
        TextView countdownText = view.findViewById(R.id.text_event_countdown);

        nameText.setText(event.name);

        long days = TimeUnit.MILLISECONDS.toDays(diffMillis);
        long hours = TimeUnit.MILLISECONDS.toHours(diffMillis) % 24;
        String lang = preferenceManager.getLanguage();

        if (days > 0) {
            String daysLeft = preferenceManager.getTranslation(lang, "days_left", getString(R.string.days_left));
            countdownText.setText(String.format(daysLeft, days));
        } else if (hours > 0) {
            String hoursLeft = preferenceManager.getTranslation(lang, "hours_left", getString(R.string.hours_left));
            countdownText.setText(String.format(hoursLeft, hours));
        } else {
            countdownText.setText(preferenceManager.getTranslation(lang, "starts_soon", getString(R.string.starts_soon)));
        }

        binding.layoutEventsList.addView(view);
    }

    private void syncTime() {
        binding.fabSync.setEnabled(false);
        String tzId = preferenceManager.getTimeZoneId();
        
        timeApiService.getCurrentTimeByZone(tzId).enqueue(new Callback<TimeResponse>() {
            @Override
            public void onResponse(Call<TimeResponse> call, Response<TimeResponse> response) {
                if (binding == null) return;
                binding.fabSync.setEnabled(true);
                String lang = preferenceManager.getLanguage();
                if (response.isSuccessful() && response.body() != null) {
                    preferenceManager.setLastSyncTime(System.currentTimeMillis());
                    updateSyncInfo();
                    Toast.makeText(requireContext(), preferenceManager.getTranslation(lang, "synced_successfully", getString(R.string.synced_successfully)), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), preferenceManager.getTranslation(lang, "sync_failed", getString(R.string.sync_failed)), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TimeResponse> call, Throwable t) {
                if (binding == null) return;
                binding.fabSync.setEnabled(true);
                String lang = preferenceManager.getLanguage();
                Toast.makeText(requireContext(), preferenceManager.getTranslation(lang, "network_error", getString(R.string.network_error)), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}