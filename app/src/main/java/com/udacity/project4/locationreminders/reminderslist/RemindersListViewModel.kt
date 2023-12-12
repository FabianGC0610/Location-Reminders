package com.udacity.project4.locationreminders.reminderslist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import kotlinx.coroutines.launch

class RemindersListViewModel(
    private val remindersRepository: RemindersLocalRepository,
) : BaseViewModel() {
    // list that holds the reminder data to be displayed on the UI
    val remindersList = MutableLiveData<List<ReminderDataItem>>()

    private val _currentLocationPermission = MutableLiveData<CurrentLocationPermission>()
    val currentLocationPermission: LiveData<CurrentLocationPermission> get() = _currentLocationPermission

    private val _isAvailableToSaveAReminder = MutableLiveData<Boolean>()
    val isAvailableToSaveAReminder: LiveData<Boolean> get() = _isAvailableToSaveAReminder

    fun setCurrentLocationPermission(permission: CurrentLocationPermission) {
        _currentLocationPermission.value = permission
    }

    fun setUserAvailableToSaveReminders() {
        _isAvailableToSaveAReminder.value = true
    }

    /**
     * Get all the reminders from the DataSource and add them to the remindersList to be shown on the UI,
     * or show error if any
     */
    fun loadReminders() {
        showLoading.value = true
        viewModelScope.launch {
            // interacting with the dataSource has to be through a coroutine
            val result = remindersRepository.getReminders()
            showLoading.postValue(false)
            when (result) {
                is Result.Success<*> -> {
                    val dataList = ArrayList<ReminderDataItem>()
                    dataList.addAll(
                        (result.data as List<ReminderDTO>).map { reminder ->
                            // map the reminder data from the DB to the be ready to be displayed on the UI
                            ReminderDataItem(
                                reminder.title,
                                reminder.description,
                                reminder.location,
                                reminder.latitude,
                                reminder.longitude,
                                reminder.id,
                            )
                        },
                    )
                    remindersList.value = dataList
                }
                is Result.Error ->
                    showSnackBar.value = result.message
            }

            // check if no data has to be shown
            invalidateShowNoData()
        }
    }

    /**
     * Inform the user that there's not any data if the remindersList is empty
     */
    private fun invalidateShowNoData() {
        showNoData.value = remindersList.value == null || remindersList.value!!.isEmpty()
    }
}

enum class CurrentLocationPermission {
    PRECISE, COARSE, NOT_GRANTED
}
