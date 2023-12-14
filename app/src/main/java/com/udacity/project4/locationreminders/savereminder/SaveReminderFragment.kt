package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.GeofencingConstants
import com.udacity.project4.utils.LandmarkDataObject
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.showToast
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {

    // Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    private lateinit var geofencingClient: GeofencingClient

    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
        android.os.Build.VERSION_CODES.Q

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                requireContext(),
                0,
                intent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        } else {
            PendingIntent.getBroadcast(
                requireContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val layoutId = R.layout.fragment_save_reminder
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)

        setDisplayHomeAsUpEnabled(true)
        binding.viewModel = _viewModel
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
        requestNotificationPermission()
        binding.selectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            val directions = SaveReminderFragmentDirections
                .actionSaveReminderFragmentToSelectLocationFragment()
            _viewModel.navigationCommand.value = NavigationCommand.To(directions)
        }

        binding.saveReminder.setOnClickListener {
            _viewModel.onSaveReminder()
        }

        _viewModel.saveReminderEvent.observe(viewLifecycleOwner) { saveReminder ->
            if (saveReminder) {
                checkPermissionsAndStartGeofencing()
            }
        }

        _viewModel.canSaveGeofence.observe(viewLifecycleOwner) { canSaveGeofence ->
            if (canSaveGeofence) {
                addGeofenceForReminder(_viewModel.reminder.value!!)

                _viewModel.onGeofenceSaved()
            }
        }

        _viewModel.locationPermissionGranted.observe(viewLifecycleOwner) { permissionGranted ->
            if (permissionGranted) {
                val title = _viewModel.reminderTitle.value
                val description = _viewModel.reminderDescription.value
                val location = _viewModel.reminderSelectedLocationStr.value
                val latitude = _viewModel.latitude.value
                val longitude = _viewModel.longitude.value

                _viewModel.reminder.value = ReminderDataItem(
                    title,
                    description,
                    location,
                    latitude,
                    longitude,
                )

                _viewModel.validateAndSaveReminder(_viewModel.reminder.value!!)

                _viewModel.onSaveReminderCompleted()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionResult")
        if (requestCode == REQUEST_CODE_PERMISSION_NOTIFICATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast(getString(R.string.notification_permission_granted), requireContext())
            } else {
                showToast(getString(R.string.notification_permission_not_granted), requireContext())
            }
        } else {
            if (
                grantResults.isEmpty() ||
                grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
                (
                    requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                        grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                        PackageManager.PERMISSION_DENIED
                    )
            ) {
                Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    R.string.permission_denied_explanation,
                    Snackbar.LENGTH_INDEFINITE,
                )
                    .setAction(R.string.settings) {
                        startActivity(
                            Intent().apply {
                                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            },
                        )
                    }.show()
            } else {
                checkDeviceLocationSettingsAndStartGeofence()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    private fun checkPermissionsAndStartGeofencing() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartGeofence()
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
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
                    exception.startResolutionForResult(
                        requireActivity(),
                        REQUEST_TURN_DEVICE_LOCATION_ON,
                    )
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
                _viewModel.setLocationPermissionGranted()
            }
        }
    }

    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
            PackageManager.PERMISSION_GRANTED ==
                ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION,
                )
            )
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                    ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    @TargetApi(29)
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            return
        }
        val permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        /** By my side I could not be able to show the native pop up to request the background location
         * (I tried with my physical device and other emulators and that specific option does not appear :( )
         * , instead of that as a solution I show a snackBar to request the user turn on the background location manually */
        if (runningQOrLater) {
            Snackbar.make(
                requireActivity().findViewById(android.R.id.content),
                getString(R.string.general_location_required),
                Snackbar.LENGTH_SHORT,
            )
                .setAction(R.string.settings) {
                    startActivity(
                        Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        },
                    )
                }.show()
        }
        ActivityCompat.requestPermissions(
            requireActivity(),
            permissionsArray,
            resultCode,
        )
    }

    @SuppressLint("MissingPermission")
    private fun addGeofenceForReminder(reminder: ReminderDataItem, geofenceRadius: Float? = null) {
        val geofenceObject = LandmarkDataObject(
            id = reminder.id,
            title = reminder.title ?: "Default title",
            latLong = LatLng(reminder.latitude ?: 0.0, reminder.longitude ?: 0.0),
        )

        val geofence = Geofence.Builder()
            .setRequestId(geofenceObject.id)
            .setCircularRegion(
                geofenceObject.latLong.latitude,
                geofenceObject.latLong.longitude,
                geofenceRadius ?: GeofencingConstants.GEOFENCE_RADIUS_IN_METERS,
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
            addOnSuccessListener {
                Log.e("Add Geofence", geofence.requestId)
            }
            addOnFailureListener {
                Toast.makeText(
                    requireActivity(),
                    R.string.geofences_not_added,
                    Toast.LENGTH_SHORT,
                ).show()
                if ((it.message != null)) {
                    Log.w(TAG, it.message!!)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermission() {
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(permission), REQUEST_CODE_PERMISSION_NOTIFICATION)
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSION_NOTIFICATION = 123
        internal const val ACTION_GEOFENCE_EVENT =
            "ReminderListFragment.reminders.action.ACTION_GEOFENCE_EVENT"
    }
}

private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val TAG = "HuntMainActivity"
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
