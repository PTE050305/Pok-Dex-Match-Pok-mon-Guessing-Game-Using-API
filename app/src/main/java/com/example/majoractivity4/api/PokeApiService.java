package com.example.majoractivity4.api;

import com.example.majoractivity4.model.PokemonResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface PokeApiService {

    @GET("pokemon/{id}")
    Call<PokemonResponse> getPokemonById(@Path("id") int id);
}
