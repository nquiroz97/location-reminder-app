package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.EspressoIdlingResource.wrapEspressoIdlingResource
import com.udacity.project4.utils.GpsUtils
import com.udacity.project4.utils.GpsUtils.onGpsListener
import com.udacity.project4.utils.hasAllLocationPermissions
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.showPermissionSnackBar
import org.koin.android.ext.android.inject
import java.util.*


class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {


    private var isGPS = false
    private var wayLatitude = 0.0
    private var wayLongitude = 0.0
    val GPS_REQUEST = 1001

    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val TAG = SelectLocationFragment::class.java.simpleName

    private lateinit var selectedMarker: Marker
    private lateinit var selectedPointOfInterest: PointOfInterest

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_select_location,
                container,
                false
            )

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        //Done: add the map setup implementation
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        GpsUtils(requireActivity()).turnGPSOn(object : onGpsListener {
            override fun gpsStatus(isGPSEnable: Boolean) {
                // turn on GPS
                isGPS = isGPSEnable
            }
        })

        /**Tip: LocationRequest avoids returning a null value and causing the fragment activity to crash
         * when trying to display the map
         **Credit to:
         * https://droidbyme.medium.com/get-current-location-using-fusedlocationproviderclient-in-android-cb7ebf5ab88e
         **/
        locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = (20 * 1000).toLong()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult == null) {
                    return
                }
                for (location in locationResult.locations) {
                    if (location != null) {
                        wayLatitude = location.latitude
                        wayLongitude = location.longitude
                    }
                    if (fusedLocationClient != null) {
                        fusedLocationClient.removeLocationUpdates(locationCallback);
                    }
                }
            }
        }

//        Done: zoom to the user location after taking his permission
//        Done: add style to the map
//        Done: put a marker to location that the user selected
//        Done: call this function after the user confirms on the selected location

        binding.saveLocation.setOnClickListener {
            onLocationSelected()
        }

        return binding.root
    }


    private fun onLocationSelected() {
        //        Done: When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence
        if (this::selectedPointOfInterest.isInitialized) {
            _viewModel.selectedPOI.value = selectedPointOfInterest
            _viewModel.reminderSelectedLocationStr.value = selectedPointOfInterest.name
            _viewModel.latitude.value = selectedPointOfInterest.latLng.latitude
            _viewModel.longitude.value = selectedPointOfInterest.latLng.longitude
        }
        findNavController().popBackStack()
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // Done: Change the map type based on the user's selection.
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setMapStyle(map)
        setPoiClick(map)
        setMapLongClick(map)
        setMyLocation()
    }


    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            if (this::selectedMarker.isInitialized) {
                selectedMarker.remove()
            }

            val snippet = String.format(
                Locale.getDefault(),
                getString(R.string.lat_long_snippet),
                latLng.latitude,
                latLng.longitude
            )

            selectedPointOfInterest = PointOfInterest(latLng, snippet, snippet)

            selectedMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.reminder_location))
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )
            selectedMarker.showInfoWindow()
            wrapEspressoIdlingResource {
                _viewModel.locationSelected.postValue(true)
            }
        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            if (this::selectedMarker.isInitialized) {
                selectedMarker.remove()
            }

            selectedMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )

            selectedPointOfInterest = poi

            selectedMarker.showInfoWindow()
            wrapEspressoIdlingResource {
                _viewModel.locationSelected.postValue(true)
            }
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style)
            )
            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }

        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GPS_REQUEST) {
                isGPS = true // flag maintain before get location
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun setMyLocation() {
        if (requireActivity().hasAllLocationPermissions()) {
            map.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener {
                val snippet = String.format(
                    Locale.getDefault(),
                    getString(R.string.lat_long_snippet),
                    wayLatitude,
                    wayLongitude
                )
                val myLatLng = LatLng(wayLatitude, wayLongitude)

                selectedPointOfInterest = PointOfInterest(myLatLng, snippet, "My Current Location")

                selectedMarker = map.addMarker(
                    MarkerOptions()
                        .position(myLatLng)
                        .title(getString(R.string.reminder_location))
                        .snippet(snippet)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                )

                val zoomLevel = 8f

                map.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, zoomLevel))

                selectedMarker.showInfoWindow()
            }
        } else {
            requireActivity().showPermissionSnackBar(binding.root)
            findNavController().popBackStack()
        }
    }

}
