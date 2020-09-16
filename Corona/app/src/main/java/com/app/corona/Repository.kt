package com.app.corona

import android.util.Log
import androidx.lifecycle.MutableLiveData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

object Repository {

    private const val baseUrl = "https://api.covid19api.com"
    private val retrofit = Retrofit.Builder().baseUrl(baseUrl).addConverterFactory(GsonConverterFactory.create()).build()
    private val coronaDataService = retrofit.create(CoronaDataService::class.java)

    fun getCoronaResults(country: String, status: String, from: String, to: String, coronaResult: MutableLiveData<Int>) {
        val call: Call<List<CoronaObject>> = coronaDataService.geCoronaData(country, status, from, to)
        call.enqueue(object : Callback<List<CoronaObject>> {
            override fun onResponse(call: Call<List<CoronaObject>>, response: Response<List<CoronaObject>>) {
                var result = 0
                val coronaObjectList = response.body()
                if (coronaObjectList != null) {
                    // the cumulative value of the last day minus the cumulative value of the first day give us the required value
                    result = coronaObjectList.last().Cases - coronaObjectList.first().Cases
                }
                // update the result using live data
                coronaResult.value = result
            }

            override fun onFailure(call: Call<List<CoronaObject>>, t: Throwable) {
                Log.e("CoronaApp", "Failed to get corona data")
            }
        })
    }
}

interface CoronaDataService {
    @GET("/country/{countryName}/status/{stat}")
    fun geCoronaData(
        @Path("countryName") country: String,
        @Path("stat") status: String,
        @Query("from") from: String,
        @Query("to") to: String
    ): Call<List<CoronaObject>>
}

data class CoronaObject(val Country: String, val Cases: Int,val Status: String, val Date: String)
