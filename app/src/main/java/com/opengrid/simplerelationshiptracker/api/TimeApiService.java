package com.opengrid.simplerelationshiptracker.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface TimeApiService {
    @GET("api/Time/current/coordinate")
    Call<TimeResponse> getCurrentTimeByCoordinate(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude
    );

    @GET("api/Time/current/zone")
    Call<TimeResponse> getCurrentTimeByZone(
            @Query("timeZone") String timeZone
    );
}