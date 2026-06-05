package com.opengrid.simplerelationshiptracker.ui.location;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.opengrid.simplerelationshiptracker.api.TimeApiService;
import com.opengrid.simplerelationshiptracker.api.TimeResponse;
import com.opengrid.simplerelationshiptracker.data.PreferenceManager;
import com.opengrid.simplerelationshiptracker.databinding.FragmentLocationBinding;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LocationFragment extends Fragment {

    private FragmentLocationBinding binding;
    private PreferenceManager preferenceManager;
    private TimeApiService timeApiService;
    private MyLocationNewOverlay myLocationOverlay;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                // We mainly need location for "My Location" feature, 
                // and Storage for tile caching (on older Android versions).
                if (Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))) {
                    enableMyLocation();
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Configuration is needed for osmdroid
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLocationBinding.inflate(inflater, container, false);
        preferenceManager = new PreferenceManager(requireContext());

        setupRetrofit();
        setupMap();

        binding.btnConfirmLocation.setOnClickListener(v -> confirmLocation());

        return binding.getRoot();
    }

    private void setupMap() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK);
        binding.mapView.setMultiTouchControls(true);

        IMapController mapController = binding.mapView.getController();
        mapController.setZoom(12.0);

        // Initialize position
        double lat = preferenceManager.getLatitude();
        double lon = preferenceManager.getLongitude();
        if (lat != 0 || lon != 0) {
            GeoPoint startPoint = new GeoPoint(lat, lon);
            mapController.setCenter(startPoint);
            binding.editLatLon.setText(String.format(Locale.US, "%.4f, %.4f", lat, lon));
        } else {
            // Default center if no saved location (e.g., London)
            mapController.setCenter(new GeoPoint(51.5074, -0.1278));
        }

        // Listener for map movements to update coordinate text
        binding.mapView.addMapListener(new DelayedMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                updateCoordinatesFromMap();
                return true;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                updateCoordinatesFromMap();
                return true;
            }
        }, 100)); // 100ms delay to avoid too many updates

        checkPermissions();
    }

    private void updateCoordinatesFromMap() {
        if (binding == null) return;
        GeoPoint center = (GeoPoint) binding.mapView.getMapCenter();
        binding.editLatLon.setText(String.format(Locale.US, "%.4f, %.4f", center.getLatitude(), center.getLongitude()));
    }

    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        boolean allGranted = true;
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            enableMyLocation();
        } else {
            requestPermissionLauncher.launch(permissions);
        }
    }

    private void enableMyLocation() {
        if (binding == null) return;
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), binding.mapView);
        myLocationOverlay.enableMyLocation();
        binding.mapView.getOverlays().add(myLocationOverlay);
    }

    private void setupRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://www.timeapi.io/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        timeApiService = retrofit.create(TimeApiService.class);
    }

    private void confirmLocation() {
        String input = binding.editLatLon.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a location", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String[] parts = input.split(",");
            double lat = Double.parseDouble(parts[0].trim());
            double lon = Double.parseDouble(parts[1].trim());
            saveLocationAndSyncTimeZone(lat, lon);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Invalid format. Use: lat, lon", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveLocationAndSyncTimeZone(double lat, double lon) {
        setLoading(true);
        timeApiService.getCurrentTimeByCoordinate(lat, lon).enqueue(new Callback<TimeResponse>() {
            @Override
            public void onResponse(@NonNull Call<TimeResponse> call, @NonNull Response<TimeResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    preferenceManager.setLocation(lat, lon);
                    preferenceManager.setTimeZoneId(response.body().timeZone);
                    Toast.makeText(requireContext(), "Saved: " + response.body().timeZone, Toast.LENGTH_LONG).show();
                    getParentFragmentManager().popBackStack();
                } else {
                    Toast.makeText(requireContext(), "Error: Check coordinates or API", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<TimeResponse> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        if (binding == null) return;
        binding.loadingOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnConfirmLocation.setEnabled(!isLoading);
    }

    @Override
    public void onResume() {
        super.onResume();
        Configuration.getInstance().load(requireContext(), 
            android.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()));
        binding.mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        Configuration.getInstance().save(requireContext(), 
            android.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()));
        binding.mapView.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}