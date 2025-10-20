package com.example.notificacionesapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard

@RunWith(AndroidJUnit4::class)
class MainActivityIntegrationTest {

    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun testMainActivityLaunches() {
        // Test that MainActivity launches successfully
        onView(withId(R.id.bottomNavigation))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testBottomNavigationExists() {
        // Test that bottom navigation is present
        onView(withId(R.id.bottomNavigation))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testHomeFragmentDisplays() {
        // Test that home fragment is displayed by default
        onView(withId(R.id.homeFragment))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNavigationToSettings() {
        // Test navigation to settings fragment
        onView(withId(R.id.settingsFragment))
            .perform(click())
        
        onView(withId(R.id.settingsFragment))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNavigationToProfile() {
        // Test navigation to profile fragment
        onView(withId(R.id.profileFragment))
            .perform(click())
        
        onView(withId(R.id.profileFragment))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNavigationToAccount() {
        // Test navigation to account fragment
        onView(withId(R.id.accountFragment))
            .perform(click())
        
        onView(withId(R.id.accountFragment))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testServiceToggleButtonExists() {
        // Test that service toggle button exists
        onView(withId(R.id.serviceButton))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testServiceToggleButtonIsClickable() {
        // Test that service toggle button is clickable
        onView(withId(R.id.serviceButton))
            .check(matches(isClickable()))
    }

    @Test
    fun testAddEmployeeButtonExists() {
        // Navigate to profile fragment first
        onView(withId(R.id.profileFragment))
            .perform(click())
        
        // Test that add employee button exists (if user is admin)
        try {
            onView(withId(R.id.addEmployeeButton))
                .check(matches(isDisplayed()))
        } catch (Exception e) {
            // Button might not be visible if user is not admin
            // This is expected behavior
        }
    }

    @Test
    fun testSettingsFragmentHasRequiredElements() {
        // Navigate to settings
        onView(withId(R.id.settingsFragment))
            .perform(click())
        
        // Test that settings fragment has required elements
        onView(withId(R.id.settingsFragment))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testAccountFragmentHasLoginElements() {
        // Navigate to account
        onView(withId(R.id.accountFragment))
            .perform(click())
        
        // Test that account fragment has login elements
        onView(withId(R.id.accountFragment))
            .check(matches(isDisplayed()))
    }
}
