package com.app.corona

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.DatePicker
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var coronaViewModel: CoronaViewModel
    private var fromYear = 2020; private var fromMonth = 8; private var fromDay = 11 // init the from/to date to the 2 days 11.9.2020 - 13.9.2020
    private var toYear = 2020; private var toMonth = 8; private var toDay = 13
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLocation: Location? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 10
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        coronaViewModel = ViewModelProvider(this, CoronaViewModelFactory(application)).get(CoronaViewModel::class.java)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        findLastLocation()
        registerBlueToothReceiver()
        initSpinner()
        setDateRangeListener()
        setDateRangeTextView()
        setByCountryButtonListener()
        setMyCountryButtonListener()
        setNearByButtonListener()
        observeCoronaResults()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            // we have asked two permissions, for ACCESS_COARSE_LOCATION and ACCESS_FINE_LOCATION
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                findLastLocation()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // unregister the bluetooth ACTION_FOUND receiver.
        unregisterReceiver(bluetoothReceiver)
    }

    private val coronaNearbyAlertDialog = CoronaNearbyAlertDialogFragment()
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice object and its info from the Intent.
                    val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    //val deviceName = device.name
                    val macAddress = device.address // MAC address
                    if (!coronaNearbyAlertDialog.isAdded && coronaViewModel.infectedMac.contains(macAddress)) { // if infected person is nearby, show alert dialog
                        coronaNearbyAlertDialog.show(supportFragmentManager, null)
                    }
                }
            }
        }
    }

    private fun registerBlueToothReceiver() {
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(bluetoothReceiver, filter)
    }

    private fun initSpinner() {
        val countries = arrayOf("switzerland", "south-africa", "poland")
        val adapter: ArrayAdapter<*> = ArrayAdapter<Any?>(this, android.R.layout.simple_spinner_item, countries)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        countrySpinner.adapter = adapter
    }

    private fun setDateRangeListener() {
        fromButton.setOnClickListener {
            DatePickerFragment { year, month, day ->
                fromYear = year
                fromMonth = month
                fromDay = day
                setDateRangeTextView()
            }.show(supportFragmentManager, null)
        }

        toButton.setOnClickListener {
            DatePickerFragment{ year, month, day ->
                toYear = year
                toMonth = month
                toDay = day
                setDateRangeTextView()
            }.show(supportFragmentManager, null)
        }
    }

    private fun setByCountryButtonListener() = byCountryButton.setOnClickListener {
        val dateRangePair = getDateRangePair()
        val fromTime = dateRangePair.first
        val toTime = dateRangePair.second
        val country = countrySpinner.selectedItem as String

        coronaViewModel.getCoronaConfirmed(country, fromTime, toTime)
        coronaViewModel.getCoronaRecovered(country, fromTime, toTime)
        coronaViewModel.getCoronaDeaths(country, fromTime, toTime)
        countryResultTextView.text = country
    }

    private fun setMyCountryButtonListener() = myCountryButton.setOnClickListener {
        findLastLocation() // utilize this function call to update the last location
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses: List<Address>
        val location = lastLocation
        if (location != null) {
            try {
                addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (addresses.isNotEmpty()) {
                    val country = addresses[0].countryName
                    val dateRangePair = getDateRangePair()
                    val fromTime = dateRangePair.first
                    val toTime = dateRangePair.second
                    coronaViewModel.getCoronaConfirmed(country, fromTime, toTime)
                    coronaViewModel.getCoronaRecovered(country, fromTime, toTime)
                    coronaViewModel.getCoronaDeaths(country, fromTime, toTime)
                    countryResultTextView.text = country
                }
            } catch (e: IOException) {
            }
        }
    }

    private fun setNearByButtonListener() = nearByCoronaButton.setOnClickListener {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        // start to scan bluetooth, the results will be get in onReceive() of bluetoothReceiver
        bluetoothAdapter?.startDiscovery()
    }

    private fun findLastLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        } else {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                lastLocation = location
            }
        }
    }

    private fun observeCoronaResults() {
        coronaViewModel.confirmedLiveData.observe(this) {
            confirmedTextView.text = getString(R.string.confirmed, it)
        }
        coronaViewModel.recoveredLiveData.observe(this) {
            recoveredTextView.text = getString(R.string.recovered, it)
        }
        coronaViewModel.deathsLiveData.observe(this) {
            deathsTextView.text = getString(R.string.deaths, it)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setDateRangeTextView() {
        fromTextView.text = "$fromDay/${fromMonth+1}/$fromYear"
        toTextView.text = "$toDay/${toMonth+1}/$toYear"
    }

    // the pair first is from date, and the second pair is to date (need to check that the "to" date is after the "from" date, doesn't done here)
    private fun getDateRangePair(): Pair<String, String> {
        // ger from date
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        calendar.set(Calendar.YEAR, fromYear)
        calendar.set(Calendar.MONTH, fromMonth)
        calendar.set(Calendar.DAY_OF_MONTH, fromDay)
        var fromTime: String = format.format(calendar.time)
        fromTime += "T00:00:00Z"

        // get to date
        calendar.set(Calendar.YEAR, toYear)
        calendar.set(Calendar.MONTH, toMonth)
        calendar.set(Calendar.DAY_OF_MONTH, toDay)
        var toTime: String = format.format(calendar.time)
        toTime += "T00:00:00Z"

        return Pair(fromTime, toTime)
    }

    class DatePickerFragment(private val selected: (year: Int, month: Int, day: Int) -> Unit) : DialogFragment(), DatePickerDialog.OnDateSetListener {
        constructor() : this({_, _, _ -> }) // default constructor, because the system sometimes instantiate the fragment and need a default constructor

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            // Use the current date as the default date in the picker
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            // Create a new instance of DatePickerDialog and return it
            return DatePickerDialog(requireContext(), this, year, month, day)
        }

        override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
            if (validDate(year, month, day)) {
                selected(year, month, day)
            }
        }

        // check that not a future date has been selected
        private fun validDate(year: Int, month: Int, day: Int): Boolean {
            val selectedCalender = Calendar.getInstance()
            selectedCalender.set(Calendar.YEAR, year)
            selectedCalender.set(Calendar.MONTH, month)
            selectedCalender.set(Calendar.DAY_OF_MONTH, day)
            val nowCalendar = Calendar.getInstance()
            return nowCalendar.timeInMillis > selectedCalender.timeInMillis
        }
    }

    class CoronaNearbyAlertDialogFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder =
                AlertDialog.Builder(activity).setMessage(getString(R.string.coronaPersonNearby)).setPositiveButton(R.string.continu) { _, _ -> dismiss() }
            return builder.create()
        }
    }
}

