package com.opengrid.simplerelationshiptracker.ui.settings;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.opengrid.simplerelationshiptracker.MainActivity;
import com.opengrid.simplerelationshiptracker.R;
import com.opengrid.simplerelationshiptracker.data.PreferenceManager;
import com.opengrid.simplerelationshiptracker.databinding.FragmentSettingsTrackerBinding;
import com.opengrid.simplerelationshiptracker.util.TranslationManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SettingsFragment extends Fragment {

    private FragmentSettingsTrackerBinding binding;
    private PreferenceManager preferenceManager;
    private TranslationManager translationManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsTrackerBinding.inflate(inflater, container, false);
        preferenceManager = new PreferenceManager(requireContext());
        translationManager = new TranslationManager(requireContext());

        binding.btnSetDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(R.string.set_start_date)
                    .setSelection(preferenceManager.getStartDateMillis() != 0 ? preferenceManager.getStartDateMillis() : MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                preferenceManager.setStartDateMillis(selection);
            });

            datePicker.show(getParentFragmentManager(), "DATE_PICKER");
        });

        binding.btnSetLocation.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.nav_location);
        });

        binding.btnChangeLanguage.setOnClickListener(v -> showLanguageDialog());
        binding.btnEditApi.setOnClickListener(v -> showEditApiDialog());
        binding.btnReadLogs.setOnClickListener(v -> showLogsDialog());

        return binding.getRoot();
    }

    private void showLanguageDialog() {
        List<String> languages = new ArrayList<>();
        languages.add(getString(R.string.language_en));
        languages.add(getString(R.string.language_ru));
        
        List<String> codes = new ArrayList<>();
        codes.add("en");
        codes.add("ru");

        languages.add(getString(R.string.auto_translate));
        codes.add("auto");

        Set<String> customLangs = preferenceManager.getCustomLanguages();
        for (String code : customLangs) {
            Locale locale = new Locale(code);
            languages.add(locale.getDisplayName() + " (Auto)");
            codes.add(code);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.change_language)
                .setItems(languages.toArray(new String[0]), (dialog, which) -> {
                    String selectedLang = codes.get(which);
                    if (selectedLang.equals("auto")) {
                        showFullLanguageListPicker();
                    } else if (which >= 3 + 0 && !selectedLang.equals("en") && !selectedLang.equals("ru")) {
                         // This is a custom language, ask if delete or select
                         showCustomLanguageActions(selectedLang);
                    } else if (!selectedLang.equals(preferenceManager.getLanguage())) {
                        preferenceManager.setLanguage(selectedLang);
                        restartApp();
                    }
                })
                .show();
    }

    private void showCustomLanguageActions(String code) {
        String[] actions = {getString(R.string.save), getString(R.string.delete_translation)};
        new AlertDialog.Builder(requireContext())
                .setTitle(new Locale(code).getDisplayName())
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) { // Select
                        preferenceManager.setLanguage(code);
                        restartApp();
                    } else { // Delete
                        new AlertDialog.Builder(requireContext())
                                .setMessage(R.string.delete_confirm)
                                .setPositiveButton(android.R.string.yes, (d, w) -> {
                                    preferenceManager.deleteTranslations(code);
                                    if (preferenceManager.getLanguage().equals(code)) {
                                        preferenceManager.setLanguage("en");
                                        restartApp();
                                    } else {
                                        Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .setNegativeButton(android.R.string.no, null)
                                .show();
                    }
                })
                .show();
    }

    private void showFullLanguageListPicker() {
        List<LanguageItem> allLanguages = new ArrayList<>();
        for (String langTag : TranslateLanguage.getAllLanguages()) {
            Locale locale = new Locale(langTag);
            allLanguages.add(new LanguageItem(locale.getDisplayName(), langTag));
        }
        Collections.sort(allLanguages, (a, b) -> a.name.compareToIgnoreCase(b.name));

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_language_search, null);
        EditText searchEdit = dialogView.findViewById(R.id.edit_search_language);
        RecyclerView recyclerView = dialogView.findViewById(R.id.recycler_languages);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        final LanguageAdapter adapter = new LanguageAdapter(allLanguages, item -> {
            startAutoTranslation(item.code);
        });
        recyclerView.setAdapter(adapter);

        final AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.select_target_language)
                .setView(dialogView)
                .show();

        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        adapter.setOnItemClickListener(item -> {
            dialog.dismiss();
            startAutoTranslation(item.code);
        });
    }

    private void showEditApiDialog() {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_api, null);
        EditText editMapUrl = view.findViewById(R.id.edit_map_url);
        EditText editMapKey = view.findViewById(R.id.edit_map_key);
        EditText editTimeUrl = view.findViewById(R.id.edit_time_url);
        EditText editTimeKey = view.findViewById(R.id.edit_time_key);

        editMapUrl.setText(preferenceManager.getMapApiUrl());
        editMapKey.setText(preferenceManager.getMapApiKey());
        editTimeUrl.setText(preferenceManager.getTimeApiUrl());
        editTimeKey.setText(preferenceManager.getTimeApiKey());

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.edit_api)
                .setView(view)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    preferenceManager.setMapApiUrl(editMapUrl.getText().toString());
                    preferenceManager.setMapApiKey(editMapKey.getText().toString());
                    preferenceManager.setTimeApiUrl(editTimeUrl.getText().toString());
                    preferenceManager.setTimeApiKey(editTimeKey.getText().toString());
                    Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showLogsDialog() {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_logs, null);
        TextView textLogs = view.findViewById(R.id.text_logs);
        
        StringBuilder logs = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                logs.append(line).append("\n");
            }
        } catch (IOException e) {
            logs.append("Error reading logs: ").append(e.getMessage());
        }
        textLogs.setText(logs.toString());

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.read_logs)
                .setView(view)
                .show();

        view.findViewById(R.id.btn_copy_logs).setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("App Logs", logs.toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(requireContext(), R.string.logs_copied, Toast.LENGTH_SHORT).show();
        });
    }

    private void startAutoTranslation(String targetCode) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_translation_progress, null);
        TextView statusText = dialogView.findViewById(R.id.text_status);
        TextView progressText = dialogView.findViewById(R.id.text_progress);
        LinearProgressIndicator progressBar = dialogView.findViewById(R.id.progress_bar);

        AlertDialog progressDialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .show();

        Map<String, String> sourceStrings = getAppStrings();

        translationManager.translateApp(targetCode, sourceStrings, new TranslationManager.TranslationCallback() {
            @Override
            public void onProgress(int current, int total) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    statusText.setText(R.string.translating);
                    progressBar.setIndeterminate(false);
                    progressBar.setProgress(current * 100 / total);
                    progressText.setText(current + "/" + total);
                });
            }

            @Override
            public void onComplete(Map<String, String> translatedStrings) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    preferenceManager.saveTranslations(targetCode, translatedStrings);
                    preferenceManager.setLanguage(targetCode);
                    progressDialog.dismiss();
                    restartApp();
                });
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private Map<String, String> getAppStrings() {
        Map<String, String> strings = new HashMap<>();
        strings.put("nav_home", getString(R.string.nav_home));
        strings.put("nav_calendar", getString(R.string.nav_calendar));
        strings.put("nav_settings", getString(R.string.nav_settings));
        strings.put("set_start_date", getString(R.string.set_start_date));
        strings.put("select_location", getString(R.string.select_location));
        strings.put("change_language", getString(R.string.change_language));
        strings.put("years_label", "years");
        strings.put("months_label", "months");
        strings.put("days_full", "days");
        strings.put("upcoming_events", getString(R.string.upcoming_events));
        strings.put("sync_button", getString(R.string.sync_button));
        return strings;
    }

    private void restartApp() {
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class LanguageItem {
        String name, code;
        LanguageItem(String name, String code) {
            this.name = name;
            this.code = code;
        }
    }

    private static class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.ViewHolder> {
        private final List<LanguageItem> originalList;
        private List<LanguageItem> filteredList;
        private OnItemClickListener listener;

        interface OnItemClickListener {
            void onItemClick(LanguageItem item);
        }

        LanguageAdapter(List<LanguageItem> list, OnItemClickListener listener) {
            this.originalList = list;
            this.filteredList = new ArrayList<>(list);
            this.listener = listener;
        }

        void setOnItemClickListener(OnItemClickListener listener) {
            this.listener = listener;
        }

        void filter(String query) {
            filteredList = new ArrayList<>();
            for (LanguageItem item : originalList) {
                if (item.name.toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(item);
                }
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            tv.setPadding(32, 32, 32, 32);
            tv.setTextSize(16);
            return new ViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final LanguageItem item = filteredList.get(position);
            ((TextView) holder.itemView).setText(item.name);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item);
            });
        }

        @Override
        public int getItemCount() {
            return filteredList.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ViewHolder(View v) { super(v); }
        }
    }
}