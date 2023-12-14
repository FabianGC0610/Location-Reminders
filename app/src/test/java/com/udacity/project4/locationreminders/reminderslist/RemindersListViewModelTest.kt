package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeRemindersLocalRepository
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var remindersRepository: FakeRemindersLocalRepository

    private lateinit var remindersListViewModel: RemindersListViewModel

    private lateinit var reminderSaved1: ReminderDTO
    private lateinit var reminderSaved2: ReminderDTO
    private lateinit var reminderExpected1: ReminderDataItem
    private lateinit var reminderExpected2: ReminderDataItem

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setupViewModel() {
        reminderSaved1 = ReminderDTO(
            "TITLE1",
            "DESCRIPTION1",
            "LOCATION1",
            104.0000,
            -35.024,
        )
        reminderSaved2 = ReminderDTO(
            "TITLE12",
            "DESCRIPTION2",
            "LOCATION2",
            96.0000,
            -62.024,
        )
        reminderExpected1 = ReminderDataItem(
            "TITLE1",
            "DESCRIPTION1",
            "LOCATION1",
            104.0000,
            -35.024,
            reminderSaved1.id,
        )
        reminderExpected2 = ReminderDataItem(
            "TITLE12",
            "DESCRIPTION2",
            "LOCATION2",
            96.0000,
            -62.024,
            reminderSaved2.id,
        )

        remindersRepository = FakeRemindersLocalRepository()

        remindersListViewModel = RemindersListViewModel(remindersRepository)
    }

    @Test
    fun loadReminders_loading() {
        // Pause dispatcher
        mainCoroutineRule.pauseDispatcher()

        // WHEN the system begins loading reminders
        remindersListViewModel.loadReminders()

        // THEN while it is loading, the loading element view will be displayed
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(true))

        // Resume dispatcher
        mainCoroutineRule.resumeDispatcher()

        // THEN if the system finishes of load reminders the loading element view disappear
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun loadRemindersWhenRemindersAreUnavailable_callEmptyListToDisplay() {
        // WHEN the system loads the reminders, but the list of reminder is empty
        remindersListViewModel.loadReminders()

        // THEN the showNoData view is displayed and the reminderList is empty
        assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is`(true))
        assertThat(remindersListViewModel.remindersList.getOrAwaitValue(), `is`(emptyList()))
    }

    @Test
    fun loadRemindersWhenRemindersHaveAnError_callErrorToDisplay() {
        // GIVEN a system error or external error
        remindersRepository.setReturnError(true)

        // WHEN the system is loading the reminders
        remindersListViewModel.loadReminders()

        // THEN the snackBar to show us the error message is displayed and also de showNoData element
        assertThat(remindersListViewModel.showSnackBar.getOrAwaitValue(), `is`("Test exception"))
        assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is`(true))
    }

    @Test
    fun loadRemindersWhenRemindersHaveASuccess_callSuccessToDisplayList() = runBlocking {
        // GIVEN a list of reminders retrieved
        remindersRepository.saveReminder(reminderSaved1)
        remindersRepository.saveReminder(reminderSaved2)
        remindersListViewModel = RemindersListViewModel(remindersRepository)

        // WHEN the system load the reminders
        remindersListViewModel.loadReminders()

        // THEN the system displays the reminderList with the elements retrieved from the DB
        assertThat(
            remindersListViewModel.remindersList.getOrAwaitValue(),
            `is`(listOf(reminderExpected1, reminderExpected2)),
        )
    }

    @Test
    fun setAuthenticationState_setAsAuthenticated() {
        // WHEN the user is authenticated
        remindersListViewModel.setAuthenticationState(
            RemindersListViewModel.AuthenticationState.AUTHENTICATED,
        )

        // THEN the authenticationState is set as AUTHENTICATED to proceed with the flow
        assertThat(
            remindersListViewModel.authenticationState.getOrAwaitValue(),
            `is`(RemindersListViewModel.AuthenticationState.AUTHENTICATED),
        )
    }

    @Test
    fun setAuthenticationState_setAsUnauthenticated() {
        // WHEN the user is unauthenticated
        remindersListViewModel.setAuthenticationState(
            RemindersListViewModel.AuthenticationState.UNAUTHENTICATED,
        )

        // THEN the authenticationState is set as UNAUTHENTICATED to not proceed with the flow
        assertThat(
            remindersListViewModel.authenticationState.getOrAwaitValue(),
            `is`(RemindersListViewModel.AuthenticationState.UNAUTHENTICATED),
        )
    }
}
