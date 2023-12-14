package com.udacity.project4.locationreminders.savereminder

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.launch

class SaveReminderViewModel(private val remindersRepository: ReminderDataSource) :
    BaseViewModel() {
    val reminderTitle = MutableLiveData<String?>()
    val reminderDescription = MutableLiveData<String?>()
    val reminderSelectedLocationStr = MutableLiveData<String?>()
    val selectedPOI = MutableLiveData<PointOfInterest?>()
    val latitude = MutableLiveData<Double?>()
    val longitude = MutableLiveData<Double?>()

    val reminder = MutableLiveData<ReminderDataItem>()

    private val _confirmLocationEvent = MutableLiveData<Boolean>()
    val confirmLocationEvent: LiveData<Boolean> get() = _confirmLocationEvent

    private val _locationPermissionGranted = MutableLiveData<Boolean>()
    val locationPermissionGranted: LiveData<Boolean> get() = _locationPermissionGranted

    private val _locationPermissionActivated= MutableLiveData<Boolean>()
    val locationPermissionActivated: LiveData<Boolean> get() = _locationPermissionActivated

    private val _saveReminderEvent = MutableLiveData<Boolean>()
    val saveReminderEvent: LiveData<Boolean> get() = _saveReminderEvent

    private val _isAMarketSelected = MutableLiveData<Boolean>()
    val isAMarketSelected: LiveData<Boolean> get() = _isAMarketSelected

    private val _canSaveGeofence = MutableLiveData<Boolean>()
    val canSaveGeofence: LiveData<Boolean> get() = _canSaveGeofence

    /**
     * Clear the live data objects to start fresh next time the view model gets called
     */
    fun onClear() {
        reminderTitle.value = null
        reminderDescription.value = null
        reminderSelectedLocationStr.value = null
        selectedPOI.value = null
        latitude.value = null
        longitude.value = null
        _locationPermissionGranted.value = false
    }

    fun onClearLocationFragment() {
        _confirmLocationEvent.value = false
        _isAMarketSelected.value = false
    }

    /**
     * Validate the entered data then saves the reminder data to the DataSource
     */
    fun validateAndSaveReminder(reminderData: ReminderDataItem) {
        if (validateEnteredData(reminderData)) {
            _canSaveGeofence.value = true
            saveReminder(reminderData)
        }
    }

    /**
     * Save the reminder to the data source
     */
    private fun saveReminder(reminderData: ReminderDataItem) {
        showLoading.value = true
        viewModelScope.launch {
            remindersRepository.saveReminder(
                ReminderDTO(
                    reminderData.title,
                    reminderData.description,
                    reminderData.location,
                    reminderData.latitude,
                    reminderData.longitude,
                    reminderData.id,
                ),
            )
            showLoading.value = false
            showToast.value = "Reminder Saved !"
            navigationCommand.value = NavigationCommand.Back
        }
    }

    /**
     * Validate the entered data and show error to the user if there's any invalid data
     */
    private fun validateEnteredData(reminderData: ReminderDataItem): Boolean {
        if (reminderData.title.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_enter_title
            return false
        }

        if (reminderData.location.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_select_location
            return false
        }
        return true
    }

    fun setLocationPermissionGranted() {
        _locationPermissionGranted.value = true
    }

    fun setLocationPermissionIsNotGranted() {
        _locationPermissionGranted.value = false
    }

    fun setLocationPermissionActivated() {
        _locationPermissionActivated.value = true
    }

    fun setMarketSelected() {
        _isAMarketSelected.value = true
    }

    fun onGeofenceSaved() {
        _canSaveGeofence.value = false
    }

    fun onConfirmLocation() {
        _confirmLocationEvent.value = true
    }

    fun onConfirmLocationComplete() {
        _confirmLocationEvent.value = false
    }

    fun onSaveReminder() {
        _saveReminderEvent.value = true
    }

    fun onSaveReminderCompleted() {
        _saveReminderEvent.value = false
    }
}
