package com.udacity.project4.locationreminders.reminderslist

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.core.IsNot.not
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.P])
class RemindersListViewModelTest {

    //DONE: provide testing to the RemindersListViewModel and its live data objects
    // Subject under test
    private lateinit var reminderListViewModel: RemindersListViewModel

    // Use a fake repository to be injected into the viewmodel
    private lateinit var fakeDataSource: FakeDataSource

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setupViewModel() = mainCoroutineRule.runBlockingTest {

        stopKoin()

        val reminders = mutableListOf<ReminderDTO>(
            ReminderDTO("title", "description", "location", 0.0, 0.0),
            ReminderDTO(
                "title",
                "description",
                "location",
                (-360..360).random().toDouble(),
                (-360..360).random().toDouble()
            ),
            ReminderDTO(
                "title",
                "description",
                "location",
                (-360..360).random().toDouble(),
                (-360..360).random().toDouble()
            ),
            ReminderDTO(
                "title",
                "description",
                "location",
                (-360..360).random().toDouble(),
                (-360..360).random().toDouble()
            )
        )
        val reminder1 = reminders[0]
        val reminder2 = reminders[1]
        val reminder3 = reminders[2]

        val remindersList = mutableListOf(reminder1, reminder2, reminder3)

        fakeDataSource = FakeDataSource(remindersList)

        reminderListViewModel = RemindersListViewModel(
            ApplicationProvider.getApplicationContext(),
            fakeDataSource)
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun getRemindersList() {
        reminderListViewModel.loadReminders()
        assertThat( reminderListViewModel.remindersList.getOrAwaitValue(), (not(emptyList())))
    }

    @Test
    fun shouldReturnError() = mainCoroutineRule.runBlockingTest {
        // Make the fake data source return errors
        fakeDataSource.setReturnError(true)
        reminderListViewModel.loadReminders()

        // Then an error message is shown
        assertThat(reminderListViewModel.showSnackBar.getOrAwaitValue(), `is`("Test exception"))

    }

    @Test
    fun check_loading() = mainCoroutineRule.runBlockingTest {
        // Pause dispatcher so we can verify initial values
        mainCoroutineRule.pauseDispatcher()

        // Load the reminder in the viewmodel
        reminderListViewModel.loadReminders()

        // Then progress indicator is shown
        assertThat(reminderListViewModel.showLoading.getOrAwaitValue(), `is`(true))

        // Execute pending coroutines actions
        mainCoroutineRule.resumeDispatcher()

        // Then progress indicator is hidden
        assertThat(reminderListViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

}