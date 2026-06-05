package com.opengrid.simplerelationshiptracker;

import android.app.Application;
import com.google.android.material.color.DynamicColors;

public class RelationshipTrackerApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}