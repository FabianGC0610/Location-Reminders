package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.ServiceLocator
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.FakeAndroidTestRepository
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
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

    private lateinit var repository: FakeAndroidTestRepository

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

    @Test
    fun retrieveAEmptyList_stayInTheRemindersListFragment() = runBlockingTest {
        // GIVEN - An empty list of reminder in the Reminders List Fragment
        repository.deleteAllReminders()

        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        val navController = Mockito.mock(NavController::class.java)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
        // WHEN - We wait to show a expected list of reminders, but it is empty

        // THEN - Verify that we navigate to the save reminder fragment
        onView(withId(R.id.noDataTextView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun retrieveAListOfReminders_stayInTheRemindersListFragment() = runBlockingTest {
        // GIVEN - // GIVEN - On the reminders list screen with two reminders
        repository.saveReminder(reminderToSave)
        repository.saveReminder(anotherReminder)

        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        val navController = Mockito.mock(NavController::class.java)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
        // WHEN - We wait to show a expected list of reminders with data

        // THEN - Verify that the elements are displayed
        onView(withId(R.id.reminderssRecyclerView))
            .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(0))
            .check(matches(atPosition(0, hasDescendant(withText("TITLE3")))))
            .check(matches(atPosition(0, isDisplayed())))
            .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(1))
            .check(matches(atPosition(1, hasDescendant(withText("TITLE4")))))
            .check(matches(atPosition(1, isDisplayed())))
    }

    @Test
    fun retrieveAReminderList_callAnErrorInTheRemindersListFragment() = runBlockingTest {
        // GIVEN - An error retrieving the reminders list
        repository.setReturnError(true)
        repository.getReminders()

        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        val navController = Mockito.mock(NavController::class.java)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
        // WHEN - We wait to show a expected list of reminders, but it is an error

        // THEN - Verify that the snackBar is shown properly with the no data view
        onView(withId(R.id.noDataTextView))
            .check(matches(isDisplayed()))
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText("Test exception")))
    }

    @Test
    fun clickLogout_navigateToTheAuthenticationActivity() {
        // GIVEN - start up Reminders List screen (is the fist screen contained inside of RemindersActivity)
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)

        // WHEN - user clicks in the logout action bar menu option
        onView(withId(R.id.logout)).perform(click())

        Thread.sleep(1000)
        // THEN - the user navigates to the authentication screen
        onView(withId(R.id.login_text)).check(matches(isDisplayed()))
        onView(withId(R.id.login_button)).check(matches(isDisplayed()))

        activityScenario.close()
    }
}

// Check at a specific position of the RecyclerView
fun atPosition(position: Int, itemMatcher: Matcher<View>): Matcher<View> {
    return object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("at position $position: ")
            itemMatcher.describeTo(description)
        }

        override fun matchesSafely(view: View): Boolean {
            if (view !is RecyclerView) {
                return false
            }

            val viewHolder = view.findViewHolderForAdapterPosition(position)
            return viewHolder != null && itemMatcher.matches(viewHolder.itemView)
        }
    }
}
