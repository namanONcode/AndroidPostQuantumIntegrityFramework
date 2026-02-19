package com.anchorpq.demo

import com.anchorpq.demo.model.VerificationResponse
import com.anchorpq.demo.model.VerificationStatus
import com.anchorpq.demo.ui.MainViewModel
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for MainViewModel.
 */
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Rule

class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        viewModel = MainViewModel()
    }

    @Test
    fun `initial state is Idle`() {
        assertEquals(MainViewModel.UiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `reset returns to Idle state`() {
        viewModel.reset()
        assertEquals(MainViewModel.UiState.Idle, viewModel.uiState.value)
    }
}

