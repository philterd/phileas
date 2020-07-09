package com.mtnfog.phileas.service.ai;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

import java.util.List;

public interface PyTorchRestService {

    // curl -X POST http://localhost:18080/process -H "Content-Type: application/json" -d 'John went to Paris.'

    @POST("process")
    @Headers({"Content-Type: text/plain", "Accept: application/json"})
    Call<List<PhileasSpan>> process(@Query("c") String context, @Query("d") String documentId, @Query("p") int piece, @Body String text);

}
