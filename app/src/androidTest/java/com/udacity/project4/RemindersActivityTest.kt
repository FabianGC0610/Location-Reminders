package com.udacity.project4

import android.app.Application
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.android.material.internal.ContextUtils.getActivity
import com.udacity.project4.locationreminders.CheckRecyclerViewUtils
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get

// END TO END test to black box test the app
@RunWith(AndroidJUnit4::class)
@LargeTest
class RemindersActivityTest :
    KoinTest { // Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin() // stop the original app koin
        appContext = getApplicationContext()
        repository = ServiceLocator.provideTasksRepository(getApplicationContext())
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    get(),
                )
            }
            single {
                SaveReminderViewModel(
                    get(),
                )
            }
            single { repository }
            single { RemindersLocalRepository(repository) }
            single { LocalDB.createRemindersDao(appContext) }
            single { appContext }
        }
        // declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        // Get our real repository
        repository = get()

        // clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @After
    fun reset() {
        ServiceLocator.resetRepository()
    }

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource) }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    /** Remember to be authenticated before running the test */
    @Test
    fun addAReminder() = runBlocking {
        // Start up Reminders screen.
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Click on the add reminder button
        Thread.sleep(500)
        onView(withId(R.id.addReminderFAB)).perform(click())

        // Click on the reminder location text to open the map
        Thread.sleep(500)
        onView(withId(R.id.selectLocation)).perform(click())

        // Click on the map to set the location
        Thread.sleep(500)
        onView(withId(R.id.map)).perform(click())

        // Click on the confirm button
        Thread.sleep(500)
        onView(withId(R.id.confirm_location_button))
            .perform(click())

        // Confirm that we return to the save reminder screen
        onView(withId(R.id.selectLocation))
            .check(matches(isDisplayed()))
        onView(withId(R.id.saveReminder))
            .check(matches(isDisplayed()))

        // Add a title for the reminder
        onView(withId(R.id.reminderTitle))
            .perform(ViewActions.typeText("Test reminder title"), ViewActions.closeSoftKeyboard())
        onView(withId(R.id.reminderDescription))
            .perform(ViewActions.typeText("Test reminder description"), ViewActions.closeSoftKeyboard())

        // Click on save reminder button
        Thread.sleep(500)
        onView(withId(R.id.saveReminder))
            .perform(click())

        Thread.sleep(500)
        // Confirm that the save reminder successfully toast alert is shown
        onView(withText(R.string.reminder_saved)).inRoot(
            withDecorView(
                not(
                    `is`(
                        getActivity(appContext)?.window?.decorView,
                    ),
                ),
            ),
        ).check(
            matches(
                isDisplayed(),
            ),
        )

        // Confirm that we are in the reminders list screen
        Thread.sleep(500)
        onView(withId(R.id.reminderssRecyclerView))
            .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(0))
            .check(
                matches(
                    CheckRecyclerViewUtils.atPosition(
                        0,
                        ViewMatchers.hasDescendant(ViewMatchers.withText("Test reminder title")),
                    ),
                ),
            )
            .check(matches(CheckRecyclerViewUtils.atPosition(0, isDisplayed())))
        Thread.sleep(500)

        activityScenario.close()
    }
}
