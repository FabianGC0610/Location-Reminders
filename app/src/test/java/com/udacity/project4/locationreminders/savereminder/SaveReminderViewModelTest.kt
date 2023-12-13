package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeRemindersLocalRepository
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SaveReminderViewModelTest {

    private lateinit var remindersRepository: FakeRemindersLocalRepository

    private lateinit var saveReminderViewModel: SaveReminderViewModel

    private lateinit var reminderToSave: ReminderDataItem

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setupViewModel() {
        reminderToSave = ReminderDataItem(
            "TITLE1",
            "DESCRIPTION1",
            "LOCATION1",
            104.0000,
            -35.024,
        )

        remindersRepository = FakeRemindersLocalRepository()

        saveReminderViewModel = SaveReminderViewModel(remindersRepository)
    }

    @Test
    fun saveReminder_loading() {
        // Pause dispatcher
        mainCoroutineRule.pauseDispatcher()

        // WHEN the system checks if the reminder can be saved and proceed with the saving
        saveReminderViewModel.validateAndSaveReminder(reminderToSave)

        // THEN while it is loading, the loading element view will be displayed
        assertThat(
            saveReminderViewModel.showLoading.getOrAwaitValue(),
            `is`(true),
        )

        // Resume dispatcher
        mainCoroutineRule.resumeDispatcher()

        // THEN if the system finishes of save the reminder the loading element view disappear
        assertThat(
            saveReminderViewModel.showLoading.getOrAwaitValue(),
            `is`(false),
        )
    }

    @Test
    fun saveReminder_completed() {
        // WHEN the system saves a reminder properly
        saveReminderViewModel.validateAndSaveReminder(reminderToSave)

        // THEN the system save the reminder as a Geofence
        assertThat(saveReminderViewModel.canSaveGeofence.getOrAwaitValue(), `is`(true))
        // THEN the system shows us a toast with the text "Reminder Saved !"
        assertThat(saveReminderViewModel.showToast.getOrAwaitValue(), `is`("Reminder Saved !"))
        // THEN the system navigates back to the Reminders List Screen
        assertThat(
            saveReminderViewModel.navigationCommand.getOrAwaitValue(),
            `is`(NavigationCommand.Back),
        )
    }

    @Test
    fun onClearData_whenTheSaveReminderFragmentIsDestroyed() {
        // Pause dispatcher
        mainCoroutineRule.pauseDispatcher()

        // WHEN there are data in the viewModel for the SaveReminderFragment
        saveReminderViewModel.reminderTitle.value = "TITLE"
        saveReminderViewModel.reminderDescription.value = "DESCRIPTION"

        // THEN the fields populated persist in the viewModel
        assertThat(saveReminderViewModel.reminderTitle.getOrAwaitValue(), `is`("TITLE"))
        assertThat(saveReminderViewModel.reminderDescription.getOrAwaitValue(), `is`("DESCRIPTION"))

        // Resume dispatcher
        mainCoroutineRule.resumeDispatcher()

        // BUT WHEN the fragment is destroyed and the onClear method is called
        saveReminderViewModel.onClear()

        // THEN all the data in the viewModel is reset to null
        assertThat(saveReminderViewModel.reminderTitle.getOrAwaitValue(), nullValue())
        assertThat(saveReminderViewModel.reminderDescription.getOrAwaitValue(), nullValue())
    }

    @Test
    fun onClearData_whenTheSelectLocationFragmentIsDestroyed() {
        // Pause dispatcher
        mainCoroutineRule.pauseDispatcher()

        // WHEN there are data in the viewModel SelectLocationFragment
        saveReminderViewModel.setMarketSelected()

        // THEN the fields populated persist in the viewModel
        assertThat(saveReminderViewModel.isAMarketSelected.getOrAwaitValue(), `is`(true))

        // Resume dispatcher
        mainCoroutineRule.resumeDispatcher()

        // BUT WHEN the fragment is destroyed and the onClearLocationFragment method is called
        saveReminderViewModel.onClearLocationFragment()

        // THEN all the data in the viewModel is reset to false
        assertThat(saveReminderViewModel.isAMarketSelected.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun saveGeofence_whenAllTheFieldsAreCorrect() {
        // WHEN the system now can save the Geofence
        saveReminderViewModel.onGeofenceSaved()

        // THEN the system set canSaveGeofence as true to continue with the saving
        assertThat(saveReminderViewModel.canSaveGeofence.getOrAwaitValue(), `is`(true))
    }

    @Test
    fun selectALocation_confirmThatTheUserSelectSomethingInTheMap() {
        // WHEN the user selects a location in the map and clicks in the confirm button
        saveReminderViewModel.onConfirmLocation()

        // THEN the system set confirmLocationEvent as true to continue with the saving in the SaveReminderFragment
        assertThat(saveReminderViewModel.confirmLocationEvent.getOrAwaitValue(), `is`(true))
    }

    @Test
    fun saveReminderProcess_whenTheDataIsSubmitted() {
        // WHEN the user enter all the data and saves the reminder
        saveReminderViewModel.onSaveReminder()

        // THEN the system set saveReminderEvent as true to complete the process of saving
        assertThat(saveReminderViewModel.saveReminderEvent.getOrAwaitValue(), `is`(true))
    }

    @Test
    fun saveReminderWithoutTitle_callSnackBarToDisplay() {
        // WHEN the user does not add a title for the reminder and try to save it
        saveReminderViewModel.validateAndSaveReminder(
            ReminderDataItem(
                title = null,
                description = "DESCRIPTION",
                location = "LOCATION",
                latitude = 15.000,
                longitude = -25.355,
            ),
        )

        // THEN a snackBar is display with the string resource err_enter_title
        assertThat(
            saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
            `is`(R.string.err_enter_title),
        )
    }

    @Test
    fun saveReminderWithoutLocation_callSnackBarToDisplay() {
        // WHEN the user does not add a location for the reminder and try to save it
        saveReminderViewModel.validateAndSaveReminder(
            ReminderDataItem(
                title = "TITLE",
                description = "DESCRIPTION",
                location = null,
                latitude = 15.000,
                longitude = -25.355,
            ),
        )

        // THEN a snackBar is display with the string resource err_select_location
        assertThat(
            saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
            `is`(R.string.err_select_location),
        )
    }
}
