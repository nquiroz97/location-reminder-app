package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.android.material.internal.ContextUtils
import com.udacity.project4.FakeDataSource
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
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
//    DONE: test the navigation of the fragments.
//    DONE: test the displayed data on the UI.
//    DONE: add testing for the error messages.

    private lateinit var fakeDataSource: FakeDataSource
    private lateinit var reminderListViewModel: RemindersListViewModel
    private lateinit var appContext: Application

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @Before
    fun setup() {
        fakeDataSource = FakeDataSource()
        reminderListViewModel =
                RemindersListViewModel(getApplicationContext(), fakeDataSource)
        appContext = getApplicationContext()
        stopKoin()

        val myModule = module {
            single {
                reminderListViewModel
            }
        }
        // new koin module
        startKoin {
            modules(listOf(myModule))
        }

        //clear the data to start fresh
        runBlocking {
            fakeDataSource.deleteAllReminders()
        }
    }


    @Test
    fun displayRemindersList() = runBlockingTest {

        val reminderDTO = ReminderDTO(
                "title",
                "description",
                "location",
                (-360..360).random().toDouble(), (-360..360).random().toDouble())


        fakeDataSource.saveReminder(reminderDTO)

        // GIVEN

        val fragmentScenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(fragmentScenario)

        onView(withText(reminderDTO.title)).check(matches(isDisplayed()))
        onView(withText(reminderDTO.description)).check(matches(isDisplayed()))
        onView(withText(reminderDTO.location)).check(matches(isDisplayed()))

    }

    @Test
    fun navigateToAddReminder() = runBlockingTest {
        // WHEN - Details fragment launched to display task
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        onView(withId(R.id.addReminderFAB)).perform(click())

        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }

    @Test
    fun shouldShowError_onRefreshLayoutSwipeNoReminders() = runBlocking {
        val fragmentScenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(fragmentScenario)

        onView(withId(R.id.refreshLayout)).perform(ViewActions.swipeDown())
        onView(withText("No reminders found"))
                .inRoot(RootMatchers.withDecorView(Matchers.not(ContextUtils.getActivity(appContext)?.window?.decorView)))
                .check(matches(isDisplayed()))

        fakeDataSource.deleteAllReminders()
    }


}
