package com.udacity.project4

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
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
import org.hamcrest.Matchers.not
import org.hamcrest.core.Is.`is`
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var saveReminderViewModel: SaveReminderViewModel
    private lateinit var appContext: Application

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()
        saveReminderViewModel = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    /**
     * Idling resources tell Espresso that the app is idle or busy. This is needed when operations
     * are not scheduled in the main Looper (for example when executed on a different thread).
     */
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    /**
     * Unregister your Idling Resource so it can be garbage collected and does not leak any memory.
     */
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Test
    fun saveReminder(): Unit = runBlocking {

        //Set up activity and with it the reminder list screen
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        //Click the add button
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).check(matches(withText("")))
        onView(withId(R.id.reminderDescription)).check(matches(withText("")))
        onView(withId(R.id.selectedLocation)).check(matches(withText("")))

        //Click the reminder location button
        onView(withId(R.id.selectLocation)).perform(click())
        onView(withId(R.id.btnSave)).check(matches(withText("Please select a location")))

        //Fake the selection
        //Better approach but not working: https://stackoverflow.com/questions/29924564/using-espresso-to-unit-test-google-maps/30519001#30519001
        saveReminderViewModel.selectedPOI.postValue(PointOfInterest(LatLng(0.0, 0.0), "TestLocation", "ID"))
        saveReminderViewModel.reminderSelectedLocationStr.postValue("TestLocation")
        saveReminderViewModel.latitude.postValue(0.0)
        saveReminderViewModel.longitude.postValue(0.0)

        //Check if button is enabled now
        onView(withId(R.id.btnSave)).check(matches(withText("Save")))

        //Navigate back
        onView(withId(R.id.btnSave)).perform(click())

        //Check if location is set correct
        onView(withId(R.id.selectedLocation)).check(matches(withText("TestLocation")))

        //Set the input fields
        onView(withId(R.id.reminderTitle)).perform(replaceText("TestTitle"))
        onView(withId(R.id.reminderDescription)).perform(replaceText("TestDescription"))

        //Save
        onView(withId(R.id.saveReminder)).perform(click())

        //Check for toast From: https://knowledge.udacity.com/questions/412114
        var remindersActivity: RemindersActivity? = null
        activityScenario.onActivity { activity ->
            remindersActivity = activity
        }
        onView(withText(R.string.reminder_saved)).inRoot(withDecorView(not(`is`(remindersActivity?.window?.decorView)))).check(matches(isDisplayed()))

        //Check if in the list everything is displayed correct
        onView(withId(R.id.title)).check(matches(withText("TestTitle")))
        onView(withId(R.id.description)).check(matches(withText("TestDescription")))
        onView(withId(R.id.location)).check(matches(withText("TestLocation")))

        //Make sure the activity is closed before resetting the db
        activityScenario.close()
    }

    @Test
    fun saveReminder_withoutTitle() {

        //Set up activity and with it the reminder list screen
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        //Click add button
        onView(withId(R.id.addReminderFAB)).perform(click())

        //Click save
        onView(withId(R.id.saveReminder)).perform(click())

        //Check for snackbar
        onView(withText(R.string.err_enter_title)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }
}
