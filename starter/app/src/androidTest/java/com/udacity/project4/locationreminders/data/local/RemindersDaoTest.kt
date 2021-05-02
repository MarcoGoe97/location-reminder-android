package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    //Subject under test
    private lateinit var database: RemindersDatabase

    @Before
    fun initDb() {
        // Using an in-memory database so that the information stored here disappears when the
        // process is killed.
        database = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun insertReminderAndGetByID() = runBlockingTest {
        //GIVEN - Insert a reminder
        val reminder = ReminderDTO("Titel1", "Description1", "Location1", 50.0, 100.0)
        database.reminderDao().saveReminder(reminder)

        //WHEN - Get the reminder by id from the database
        val loaded = database.reminderDao().getReminderById(reminder.id)

        //THEN - The loaded data contains the expected values
        assertThat(loaded as ReminderDTO, notNullValue())
        assertThat(loaded.id, `is`(reminder.id))
        assertThat(loaded.title, `is`(reminder.title))
        assertThat(loaded.description, `is`(reminder.description))
        assertThat(loaded.location, `is`(reminder.location))
        assertThat(loaded.longitude, `is`(reminder.longitude))
        assertThat(loaded.latitude, `is`(reminder.latitude))
    }

    @Test
    fun getAllRemindersAndDelete() = runBlockingTest {
        //GIVEN - Insert two  reminder
        val reminder = ReminderDTO("Titel1", "Description1", "Location1", 50.0, 100.0)
        val reminder2 = ReminderDTO("Titel12", "Description2", "Location2", 150.0, 200.0)
        database.reminderDao().saveReminder(reminder)
        database.reminderDao().saveReminder(reminder2)

        //WHEN - Get all reminders from database
        val loadedList = database.reminderDao().getReminders()

        //THEN - the size should be two
        assertThat(loadedList, notNullValue())
        assertThat(loadedList.size, `is`(2))

        //WHEN - All reminders deleted
        database.reminderDao().deleteAllReminders()

        //THEN - get all reminders is empty
        val loadedEmptyList = database.reminderDao().getReminders()
        assertThat(loadedEmptyList, `is`(emptyList()))
    }
}