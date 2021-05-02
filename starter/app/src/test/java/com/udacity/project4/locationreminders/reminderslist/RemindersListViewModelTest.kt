package com.udacity.project4.locationreminders.reminderslist

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsNot.not
import org.hamcrest.core.IsNull.nullValue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    //Subject under test
    private lateinit var remindersListViewModel: RemindersListViewModel

    private lateinit var fakeDataSource: FakeDataSource

    @Before
    fun setupFireBase() {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext())
    }

    @Before
    fun setupRemindersListViewModel() {
        // Initialise the repository with no tasks
        fakeDataSource = FakeDataSource()

        remindersListViewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
    }

    //From https://stackoverflow.com/questions/57038848/koinappalreadystartedexception-a-koin-application-has-already-been-started
    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun check_loading() {
        // Pause dispatcher so you can verify initial values.
        mainCoroutineRule.pauseDispatcher()

        // WHEN Load the reminders in the view model.
        remindersListViewModel.loadReminders()

        //THEN assert that the loading indicator is shown
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(true))

        // Execute pending coroutines actions.
        mainCoroutineRule.resumeDispatcher()

        //THEN assert that the loading indicator is hidden
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun check_loadReminders() = runBlockingTest {
        //GIVEN a data source with entries
        fakeDataSource.saveReminder(
            ReminderDTO("Title1", "Description1", "Test Location1", 0.0, 0.0))

        //WHEN loading the reminders
        remindersListViewModel.loadReminders()

        //THEN assert that the reminderList is not null
        assertThat(remindersListViewModel.remindersList.getOrAwaitValue(), (not(nullValue())))
    }

    @Test
    fun loadWhenRemindersAreUnavailable_setShowSnackBar() {
        // GIVEN the data source return errors.
        fakeDataSource.setReturnError(true)

        //WHEN loading the reminders
        remindersListViewModel.loadReminders()

        //THEN assert that showSnackBar value is the exception message
        assertThat(
            remindersListViewModel.showSnackBar.getOrAwaitValue(), `is`("Test exception"))
    }

    @Test
    fun loadWhenRemindersAreUnavailable_callInvalidateShowNoData() = runBlockingTest {
        //GIVEN an empty data source

        //WHEN loading the reminders
        remindersListViewModel.loadReminders()

        //THEN assert that showNoData value is true
        assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is`(true))
    }
}