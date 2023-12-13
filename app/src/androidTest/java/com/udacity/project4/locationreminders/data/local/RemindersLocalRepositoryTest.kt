package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@MediumTest
class RemindersLocalRepositoryTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var remindersLocalDataSource: RemindersLocalDataSource
    private lateinit var database: RemindersDatabase

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
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java,
        ).allowMainThreadQueries().build()

        remindersLocalDataSource =
            RemindersLocalDataSource(
                database.reminderDao(),
                Dispatchers.Main,
            )
    }

    @After
    fun cleanUp() {
        database.close()
    }

    @Test
    fun saveReminder_retrievesReminder() = runBlocking {
        // GIVEN - A new reminder saved in the database.
        remindersLocalDataSource.saveReminder(reminderToSave)

        // WHEN - Reminder retrieved by ID.
        val result = remindersLocalDataSource.getReminder(reminderToSave.id) as Result.Success

        // THEN - Same reminder is returned
        assertThat(result.data, `is`(reminderToSave))
        assertThat(result.data.title, `is`("TITLE3"))
        assertThat(result.data.description, `is`("DESCRIPTION3"))
        assertThat(result.data.location, `is`("LOCATION3"))
        assertThat(result.data.longitude, `is`(-78.024))
        assertThat(result.data.latitude, `is`(35.0000))
    }

    @Test
    fun saveReminder_notRetrievesReminder() = runBlocking {
        // GIVEN - A new reminder saved in the database.
        remindersLocalDataSource.saveReminder(reminderToSave)

        // WHEN - Reminder retrieved by ID is wrong.
        val result = remindersLocalDataSource.getReminder(anotherReminder.id) as Result.Error

        // THEN - A message is retrieved to indicate that Reminder not found!
        assertThat(result.message, `is`("Reminder not found!"))
    }

    @Test
    fun getReminders_retrievesAllReminders() = runBlocking {
        // GIVEN - Reminders are saved in the database.
        remindersLocalDataSource.saveReminder(reminderToSave)
        remindersLocalDataSource.saveReminder(anotherReminder)

        // WHEN - All reminders retrieved.
        val result = remindersLocalDataSource.getReminders() as Result.Success

        // THEN - Same reminders list is returned
        assertThat(result.data, `is`(listOf(reminderToSave, anotherReminder)))
    }

    @Test
    fun deleteReminders_deletesAllReminders() = runBlocking {
        // GIVEN - Reminders are saved in the database.
        remindersLocalDataSource.saveReminder(reminderToSave)
        remindersLocalDataSource.saveReminder(anotherReminder)

        val result = remindersLocalDataSource.getReminders() as Result.Success

        assertThat(result.data, `is`(listOf(reminderToSave, anotherReminder)))

        // WHEN - All reminders are deleted.
        remindersLocalDataSource.deleteAllReminders()

        val secondResult = remindersLocalDataSource.getReminders() as Result.Success

        // THEN - A message is retrieved to indicate that Reminder not found!
        assertThat(secondResult.data, `is`(emptyList()))
    }
}
