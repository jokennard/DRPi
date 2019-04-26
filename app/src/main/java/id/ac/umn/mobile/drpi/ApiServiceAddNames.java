package id.ac.umn.mobile.drpi;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface ApiServiceAddNames {
    @POST("/api/inquire")
    @FormUrlEncoded
    Call<POST> readJson();
}
