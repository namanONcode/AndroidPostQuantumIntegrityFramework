package com.anchorpq.demo

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anchorpq.demo.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for MainActivity.
 * Tests UI interactions and display.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityInstrumentedTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testMerkleRootIsDisplayed() {
        // Verify Merkle root TextView is displayed
        onView(withId(R.id.tvMerkleRoot))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testVersionIsDisplayed() {
        // Verify version TextView is displayed
        onView(withId(R.id.tvVersion))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testVariantIsDisplayed() {
        // Verify variant TextView is displayed
        onView(withId(R.id.tvVariant))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testVerifyButtonIsDisplayed() {
        // Verify the verify button is displayed and enabled
        onView(withId(R.id.btnVerify))
            .check(matches(isDisplayed()))
            .check(matches(isEnabled()))
    }

    @Test
    fun testServerUrlIsDisplayed() {
        // Verify server URL is displayed
        onView(withId(R.id.tvServerUrl))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testStatusCardIsDisplayed() {
        // Verify status card is displayed
        onView(withId(R.id.cardStatus))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testInitialStatusIsIdle() {
        // Verify initial status shows ready state
        onView(withId(R.id.tvStatus))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.status_idle)))
    }

    @Test
    fun testCopyButtonIsClickable() {
        // Verify copy button is clickable
        onView(withId(R.id.btnCopyRoot))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
    }
}

