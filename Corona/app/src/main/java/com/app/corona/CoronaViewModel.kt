package com.app.corona

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider


class CoronaViewModel(application: Application) : AndroidViewModel(application) {

    // live data properties that the activity observe to get the results from the network
    val confirmedLiveData = MutableLiveData<Int>()
    val recoveredLiveData = MutableLiveData<Int>()
    val deathsLiveData = MutableLiveData<Int>()

    // add known mac address of infected to this array
    val infectedMac = arrayOf("5D:EA:22:DC:57:45", "EC:FA:5C:80:CF:B1")

    fun getCoronaConfirmed(country: String, from: String, to: String) =
        Repository.getCoronaResults(country, "confirmed", from, to, confirmedLiveData)

    fun getCoronaRecovered(country: String, from: String, to: String) =
        Repository.getCoronaResults(country, "recovered", from, to, recoveredLiveData)

    fun getCoronaDeaths(country: String, from: String, to: String) =
        Repository.getCoronaResults(country, "deaths", from, to, deathsLiveData)
}

class CoronaViewModelFactory(private val application: Application) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CoronaViewModel(application) as T
    }
}