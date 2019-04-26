package id.ac.umn.mobile.drpi;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiServiceNames {
    @GET("/api/names")
    Call<JsonObject> readJson();
}
