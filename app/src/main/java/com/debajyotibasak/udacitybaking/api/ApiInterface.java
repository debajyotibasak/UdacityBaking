package com.debajyotibasak.udacitybaking.api;

import com.debajyotibasak.udacitybaking.api.model.Recipe;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiInterface {

    @GET("baking.json")
    Call<List<Recipe>> getRecipes();
}
