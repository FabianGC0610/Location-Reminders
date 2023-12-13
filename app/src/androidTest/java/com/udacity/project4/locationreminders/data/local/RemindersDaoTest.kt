package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.nullValue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class RemindersDaoTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

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
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java,
        ).build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun saveReminderAndGetById() = runBlockingTest {
        // GIVEN - Save a reminder
        database.reminderDao().saveReminder(reminderToSave)

        // WHEN - Get the reminder by id from the database
        val loaded = database.reminderDao().getReminderById(reminderToSave.id)

        // THEN - The loaded data contains the expected values
        assertThat(loaded as ReminderDTO, notNullValue())
        assertThat(loaded.id, `is`(reminderToSave.id))
        assertThat(loaded.title, `is`(reminderToSave.title))
        assertThat(loaded.description, `is`(reminderToSave.description))
        assertThat(loaded.location, `is`(reminderToSave.location))
        assertThat(loaded.longitude, `is`(reminderToSave.longitude))
        assertThat(loaded.latitude, `is`(reminderToSave.latitude))
    }

    @Test
    fun saveReminderAndTryToFindAnotherOneById() = runBlockingTest {
        // GIVEN - Save a reminder
        database.reminderDao().saveReminder(reminderToSave)

        // WHEN - Try to get a wrong reminder by id from the database
        val loaded = database.reminderDao().getReminderById(anotherReminder.id)

        // THEN - The loaded data contains the expected values
        assertThat(loaded, nullValue())
    }
}
