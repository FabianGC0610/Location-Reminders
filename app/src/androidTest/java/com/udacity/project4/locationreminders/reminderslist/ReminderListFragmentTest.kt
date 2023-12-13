package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.ServiceLocator
import com.udacity.project4.locationreminders.data.FakeAndroidTestRepository
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@MediumTest
class ReminderListFragmentTest {

//    TODO: test the navigation of the fragments.
//    TODO: test the displayed data on the UI.
//    TODO: add testing for the error messages.

    private lateinit var repository: ReminderDataSource

    private val reminderToSave = ReminderDTO(
        "TITLE3",
        "DESCRIPTION3",
        "LOCATION3",
        35.0000,
        -78.024,
    )

    private val anotherReminder = ReminderDTO(
        "TITLE4",
        "DESCRIPTION4",
        "LOCATION4",
        120.0000,
        -18.024,
    )

    @Before
    fun initRepository() {
        repository = FakeAndroidTestRepository()
        ServiceLocator.tasksRepository = repository
    }

    @After
    fun cleanup() {
        ServiceLocator.resetRepository()
    }

    @Test
    fun clickReminder_navigateToSaveReminderFragment() = runBlockingTest {
        // GIVEN - On the reminders list screen with two reminders
        repository.saveReminder(reminderToSave)
        repository.saveReminder(anotherReminder)

        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        val navController = Mockito.mock(NavController::class.java)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
        // WHEN - Click on the first list item
        onView(withId(R.id.reminderssRecyclerView))
            .perform(
                RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText("TITLE3")),
                    ViewActions.click(),
                ),
            )

        // THEN - Verify that we navigate to the save reminder fragment
        verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder(),
        )
    }

    @Test
    fun clickAddReminder_navigateToSaveReminderFragment() = runBlockingTest {
        // GIVEN - On the reminders list screen with two reminders
        repository.saveReminder(reminderToSave)
        repository.saveReminder(anotherReminder)

        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        val navController = Mockito.mock(NavController::class.java)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
        // WHEN - Click on the floating action button
        onView(withId(R.id.addReminderFAB))
            .perform(
                click(),
            )

        // THEN - Verify that we navigate to the save reminder fragment
        verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder(),
        )
    }
}
