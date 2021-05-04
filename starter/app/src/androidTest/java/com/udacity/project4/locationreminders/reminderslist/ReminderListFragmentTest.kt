package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest {

    private lateinit var repository: ReminderDataSource
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
        //From https://stackoverflow.com/a/64801291
        repository = GlobalContext.get().koin.get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    //Test the navigation
    @Test
    fun clickAddReminderFAB_navigateToSaveReminderFragment() {
        //GIVEN on the reminders list screen
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController((it.view!!), navController)
        }

        //WHEN - click on the fab button
        onView(withId(R.id.addReminderFAB))
                .perform(click())

        //THEN - verify that we navigate to the save reminder fragment
        verify(navController).navigate(
                ReminderListFragmentDirections.toSaveReminder()
        )
    }


    //test the displayed data on the UI.
    @Test
    fun savedReminder_DisplayedInUI(): Unit = runBlocking {
        //GIVEN - add reminder to the database
        val reminder = ReminderDTO("Titel1", "Description1", "Location1", 50.0, 100.0)
        repository.saveReminder(reminder)

        //WHEN - list fragment launched to display the reminders
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        //THEN - the reminder information is displayed on the screen
        onView(withId(R.id.title)).check(matches(isDisplayed()))
        onView(withId(R.id.title)).check(matches(withText("Titel1")))
        onView(withId(R.id.description)).check(matches(isDisplayed()))
        onView(withId(R.id.description)).check(matches(withText("Description1")))
        onView(withId(R.id.location)).check(matches(isDisplayed()))
        onView(withId(R.id.location)).check(matches(withText("Location1")))
    }

    //add testing for the error messages.
    @Test
    fun noReminders_DisplayPlaceHolder() {
        //GIVEN - empty repository

        //WHEN - list fragment launched to display the reminders
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        //THEN - the placeholder is displayed
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }
}