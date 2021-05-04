package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.core.Is
import org.hamcrest.core.IsNot.not
import org.hamcrest.core.IsNull.nullValue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    //Subject under test
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    private lateinit var fakeDataSource: FakeDataSource

    @Before
    fun setupSaveRemindersViewModel() {
        // Initialise the repository with no tasks
        fakeDataSource = FakeDataSource()

        saveReminderViewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
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
        saveReminderViewModel.saveReminder(ReminderDataItem("Titel1", "Description1", "Location1", 0.0, 0.0))

        //THEN assert that the loading indicator is shown
        assertThat(
            saveReminderViewModel.showLoading.getOrAwaitValue(), Is.`is`(true))

        // Execute pending coroutines actions.
        mainCoroutineRule.resumeDispatcher()

        //THEN assert that the loading indicator is hidden
        assertThat(
            saveReminderViewModel.showLoading.getOrAwaitValue(), Is.`is`(false))
    }

    @Test
    fun validateEnteredData_withoutTitle() {
        //GIVEN a ReminderDataItem without title
        val reminder = ReminderDataItem("", "Description1", "Location1", 0.0, 0.0)

        //WHEN validate the data
        val returnValue = saveReminderViewModel.validateEnteredData(reminder)

        //THEN showSnackBarInt is err_enter_title and function returns false
        assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_enter_title))
        assertThat(returnValue, `is`(false))
    }

    @Test
    fun validateEnteredData_withoutLocation() {
        //GIVEN a ReminderDataItem without location
        val reminder = ReminderDataItem("Title", "Description1", "", 0.0, 0.0)

        //WHEN validate the data
        val returnValue = saveReminderViewModel.validateEnteredData(reminder)

        //THEN showSnackBarInt is err_select_location and function returns false
        assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_select_location))
        assertThat(returnValue, `is`(false))
    }

    @Test
    fun validateEnteredData_withValidData() {
        //GIVEN a ReminderDataItem
        val reminder = ReminderDataItem("Title1", "Description1", "Location1", 0.0, 0.0)

        //WHEN validate the data
        val returnValue = saveReminderViewModel.validateEnteredData(reminder)

        //THEN function returns true
        assertThat(returnValue, `is`(true))
    }

    @Test
    fun check_onClear() {
        //GIVEN set variables
        saveReminderViewModel.reminderTitle.value = "Title1"
        saveReminderViewModel.reminderDescription.value = "Description1"
        saveReminderViewModel.reminderSelectedLocationStr.value = "Location1"
        saveReminderViewModel.selectedPOI.value = PointOfInterest(LatLng(0.0, 0.0), "", "")
        saveReminderViewModel.latitude.value = 0.0
        saveReminderViewModel.longitude.value = 0.0

        //WHEN onClear is called
        saveReminderViewModel.onClear()

        //THEN all variables are null
        assertThat(saveReminderViewModel.reminderTitle.getOrAwaitValue(), nullValue())
        assertThat(saveReminderViewModel.reminderSelectedLocationStr.getOrAwaitValue(), nullValue())
        assertThat(saveReminderViewModel.reminderSelectedLocationStr.getOrAwaitValue(), nullValue())
        assertThat(saveReminderViewModel.selectedPOI.getOrAwaitValue(), nullValue())
        assertThat(saveReminderViewModel.latitude.getOrAwaitValue(), nullValue())
        assertThat(saveReminderViewModel.longitude.getOrAwaitValue(), nullValue())
    }

    @Test
    fun check_saveReminder() = runBlockingTest {
        //GIVEN a ReminderDataItem
        val reminder = ReminderDataItem("Title1", "Description1", "Location1", 0.0, 0.0)

        //WHEN saveReminder is called
        saveReminderViewModel.saveReminder(reminder)

        //THEN the reminder is saved in the data source
        val savedReminder = fakeDataSource.getReminder(reminder.id)
        assertThat((savedReminder as? Result.Success<ReminderDTO>)?.data, not(nullValue()))

        //THEN navigationCommand is Back
        assertThat(saveReminderViewModel.navigationCommand.getOrAwaitValue(), `is`(NavigationCommand.Back))

        //THEN showToast is string of reminder_saved
        val app: Application = ApplicationProvider.getApplicationContext()
        assertThat(saveReminderViewModel.showToast.getOrAwaitValue(), `is`(app.getString(R.string.reminder_saved)))
    }
}