package com.udacity.project4

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.Room
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import com.udacity.project4.locationreminders.data.local.RemindersLocalDataSource
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

object ServiceLocator {
    private val lock = Any()

    private var database: RemindersDatabase? = null

    @Volatile
    var tasksRepository: ReminderDataSource? = null
        @VisibleForTesting set

    fun provideTasksRepository(context: Context): ReminderDataSource {
        synchronized(this) {
            return tasksRepository ?: createTasksRepository(context)
        }
    }

    private fun createTasksRepository(context: Context): ReminderDataSource {
        val newRepo = RemindersLocalRepository(createTaskLocalDataSource(context))
        tasksRepository = newRepo
        return newRepo
    }

    private fun createTaskLocalDataSource(context: Context): ReminderDataSource {
        val database = database ?: createDataBase(context)
        return RemindersLocalDataSource(database.reminderDao())
    }

    private fun createDataBase(context: Context): RemindersDatabase {
        val result = Room.databaseBuilder(
            context.applicationContext,
            RemindersDatabase::class.java,
            "Reminders.db",
        ).build()
        database = result
        return result
    }

    @VisibleForTesting
    fun resetRepository() {
        // Clear all data to avoid test pollution.
        database?.apply {
            clearAllTables()
            close()
        }
        database = null
        tasksRepository = null
    }
}
