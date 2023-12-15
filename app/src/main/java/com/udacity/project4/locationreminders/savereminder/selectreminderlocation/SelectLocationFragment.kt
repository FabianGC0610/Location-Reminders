package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.showToast
import org.koin.android.ext.android.inject
import java.util.Locale

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    // Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private val TAG = "SelectLocationFragment"

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var poiMarker: Marker? = null

    private val mapZoom = 18F

    private val requestLocationPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                checkDeviceLocationSettingsAndStartGeofence()
            } else {
                _viewModel.setLocationPermissionIsNotGranted()
            }
        }

    private val turnOnLocationActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                setupPermissionActivatedObserver()
                _viewModel.setLocationPermissionActivated()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val layoutId = R.layout.fragment_select_location
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPermissionGrantedObserver()

        _viewModel.confirmLocationEvent.observe(viewLifecycleOwner) { confirmLocation ->
            if (confirmLocation) {
                findNavController().popBackStack()
            }
        }

        _viewModel.isAMarketSelected.observe(viewLifecycleOwner) { isAMarketSelected ->
            if (isAMarketSelected) {
                binding.confirmLocationButton.visibility = View.VISIBLE
                binding.locationTip.visibility = View.GONE
            } else {
                binding.confirmLocationButton.visibility = View.GONE
                binding.locationTip.visibility = View.VISIBLE
            }
        }
    }

    override fun onStart() {
        super.onStart()
        enableMyLocation()
    }

    @SuppressLint("MissingPermission")
    private fun setupPermissionGrantedObserver() {
        _viewModel.locationPermissionGranted.observe(viewLifecycleOwner) { permissionGranted ->
            if (permissionGranted) {
                checkDeviceLocationSettingsAndStartGeofence()
            } else {
                showPermissionDeniedMessage(getString(R.string.precise_permission_denied_explanation))
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupPermissionActivatedObserver() {
        _viewModel.locationPermissionActivated.observe(viewLifecycleOwner) { permissionActivated ->
            if (permissionActivated) {
                Thread.sleep(500)
                showCurrentLocation(map)
            }
        }
    }

    private fun showPermissionDeniedMessage(message: String) {
        val snackBar = Snackbar.make(
            requireActivity().findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_INDEFINITE,
        )
        snackBar.setAction(getString(R.string.enable_location_button_text)) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
            intent.data = uri
            startActivity(intent)
        }
        snackBar.show()
    }

    private fun isPermissionGranted(): Boolean {
        val permissionCheck = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        return permissionCheck == PackageManager.PERMISSION_GRANTED
    }

    private fun enableMyLocation() {
        if (isPermissionGranted()) {
            _viewModel.setLocationPermissionGranted()
        } else {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.isMyLocationEnabled = true
        map.setOnMyLocationButtonClickListener {
            if (_viewModel.locationPermissionActivated.value == true) {
                showCurrentLocation(map)
            } else {
                checkDeviceLocationSettingsAndStartGeofence()
            }
            true
        }
        setMapStyle(map)
        setMapClick(map)
        setPoiClick(map)
    }

    private fun showCurrentLocation(googleMap: GoogleMap) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        googleMap.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                currentLatLng,
                                mapZoom,
                            ),
                        )
                    } else {
                        showToast(
                            getString(R.string.no_location_obtained),
                            requireContext(),
                        )
                    }
                }
        } catch (e: SecurityException) {
            showToast(getString(R.string.no_permissions_granted), requireContext())
        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            poiMarker?.remove()
            poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name),
            )
            saveMarket(poi)
            _viewModel.setMarketSelected()
            poiMarker?.showInfoWindow()
        }
    }

    private fun setMapClick(map: GoogleMap) {
        map.setOnMapClickListener { latLng ->
            val snippet = String.format(
                Locale.getDefault(),
                getString(R.string.lat_long_snippet),
                latLng.latitude,
                latLng.longitude,
            )

            poiMarker?.remove()
            poiMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .snippet(snippet)
                    .title(getString(R.string.dropped_pin)),
            )

            saveMarket()
            _viewModel.setMarketSelected()
            poiMarker?.showInfoWindow()
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style,
                ),
            )
            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    private fun saveMarket(market: PointOfInterest? = null) {
        _viewModel.selectedPOI.value = market
        _viewModel.reminderSelectedLocationStr.value = poiMarker?.title
        _viewModel.latitude.value = poiMarker?.position?.latitude
        _viewModel.longitude.value = poiMarker?.position?.longitude
    }

    private fun deleteMarket() {
        _viewModel.selectedPOI.value = null
        _viewModel.reminderSelectedLocationStr.value = null
        _viewModel.latitude.value = null
        _viewModel.longitude.value = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
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

    private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    val intentSenderRequest =
                        IntentSenderRequest.Builder(exception.resolution).build()
                    turnOnLocationActivityResultLauncher.launch(intentSenderRequest)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    R.string.location_required_error,
                    Snackbar.LENGTH_INDEFINITE,
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                showCurrentLocation(map)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (_viewModel.confirmLocationEvent.value != true) {
            deleteMarket()
        }
        _viewModel.onConfirmLocationComplete()
        _viewModel.onClearLocationFragment()
    }
}

private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
