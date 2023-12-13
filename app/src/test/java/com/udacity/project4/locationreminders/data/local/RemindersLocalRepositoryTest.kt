package com.udacity.project4.locationreminders.data.local

import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.core.IsEqual
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class RemindersLocalRepositoryTest {

    private val reminder1 = ReminderDTO(
        "TITLE1",
        "DESCRIPTION1",
        "LOCATION1",
        104.0000,
        -35.024,
    )
    private val reminder2 = ReminderDTO(
        "TITLE2",
        "DESCRIPTION2",
        "LOCATION2",
        35.0000,
        -78.024,
    )
    private val reminderToSave = ReminderDTO(
        "TITLE3",
        "DESCRIPTION3",
        "LOCATION3",
        35.0000,
        -78.024,
    )
    private val reminders = listOf(reminder1, reminder2).sortedBy { it.id }

    private lateinit var reminderDataSource: FakeDataSource

    private lateinit var remindersRepository: RemindersLocalRepository

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun createRepository() {
        reminderDataSource = FakeDataSource(reminders.toMutableList())

        remindersRepository = RemindersLocalRepository(reminderDataSource, Dispatchers.Main)
    }

    @Test
    fun getReminders_requestsAllRemindersFromDataSource() = mainCoroutineRule.runBlockingTest {
        // WHEN reminders are requested from the reminders repository
        val remindersResult = remindersRepository.getReminders() as Result.Success

        // THEN reminders are loaded from the data source
        assertThat(remindersResult.data, IsEqual(reminders))
    }

    @Test
    fun saveReminder_savesAReminderFromDataSource() = mainCoroutineRule.runBlockingTest {
        // WHEN a reminder is saved from the reminders repository
        remindersRepository.saveReminder(reminderToSave)

        val remindersResult = remindersRepository.getReminder(reminderToSave.id) as Result.Success

        // THEN a reminder is saved from the data source
        assertThat(remindersResult.data, IsEqual(reminderToSave))
    }

    @Test
    fun getReminder_requestsAReminderFromDataSource() = mainCoroutineRule.runBlockingTest {
        // WHEN a reminder is requested from the reminders repository
        val remindersResult = remindersRepository.getReminder(reminder2.id) as Result.Success

        // THEN a reminder is loaded from the data source
        assertThat(remindersResult.data, IsEqual(reminder2))
    }

    @Test
    fun deleteAllReminders_deletesAllRemindersFromDataSource() = mainCoroutineRule.runBlockingTest {
        // WHEN all reminders are deleted from the reminders repository
        remindersRepository.deleteAllReminders()

        val remindersResult = remindersRepository.getReminders() as Result.Success

        // THEN reminders are deleted from the data source
        assertThat(remindersResult.data, IsEqual(emptyList()))
    }
}
