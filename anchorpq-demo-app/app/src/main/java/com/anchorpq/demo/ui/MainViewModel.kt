package com.anchorpq.demo.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anchorpq.demo.model.VerificationResponse
import com.anchorpq.demo.model.VerificationStatus
import com.anchorpq.demo.network.IntegrityRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for the main integrity verification screen.
 */
class MainViewModel : ViewModel() {

    // UI State
    private val _uiState = MutableLiveData<UiState>(UiState.Idle)
    val uiState: LiveData<UiState> = _uiState

    // Verification progress
    private val _progressMessage = MutableLiveData<String>()
    val progressMessage: LiveData<String> = _progressMessage

    // Server response details
    private val _serverResponse = MutableLiveData<String>()
    val serverResponse: LiveData<String> = _serverResponse

    private var repository: IntegrityRepository? = null

    /**
     * UI State sealed class representing all possible states.
     */
    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val status: VerificationStatus, val message: String) : UiState()
        data class Error(val message: String) : UiState()
    }

    /**
     * Initializes the repository with the server URL.
     */
    fun initialize(serverUrl: String) {
        repository = IntegrityRepository(serverUrl)
    }

    /**
     * Performs integrity verification.
     *
     * @param merkleRoot The application's Merkle root hash
     * @param version Application version
     * @param variant Build variant
     */
    fun verifyIntegrity(merkleRoot: String, version: String, variant: String) {
        val repo = repository ?: run {
            _uiState.value = UiState.Error("Repository not initialized")
            return
        }

        _uiState.value = UiState.Loading
        _serverResponse.value = ""

        viewModelScope.launch {
            val progressCallback = object : IntegrityRepository.ProgressCallback {
                override fun onProgress(step: IntegrityRepository.VerificationStep) {
                    _progressMessage.postValue(getProgressMessage(step))
                }
            }

            when (val result = repo.verifyIntegrity(merkleRoot, version, variant, progressCallback)) {
                is IntegrityRepository.VerificationResult.Success -> {
                    handleSuccess(result.response)
                }
                is IntegrityRepository.VerificationResult.Error -> {
                    handleError(result.message)
                }
            }
        }
    }

    private fun handleSuccess(response: VerificationResponse) {
        _uiState.postValue(UiState.Success(response.status, response.message))
        _serverResponse.postValue(buildResponseDetails(response))
    }

    private fun handleError(message: String) {
        _uiState.postValue(UiState.Error(message))
        _serverResponse.postValue("Error: $message")
    }

    private fun getProgressMessage(step: IntegrityRepository.VerificationStep): String {
        return when (step) {
            IntegrityRepository.VerificationStep.FETCHING_PUBLIC_KEY ->
                "Fetching ML-KEM public key..."
            IntegrityRepository.VerificationStep.ENCRYPTING_PAYLOAD ->
                "Encrypting integrity payload..."
            IntegrityRepository.VerificationStep.SENDING_REQUEST ->
                "Sending verification request..."
            IntegrityRepository.VerificationStep.PROCESSING_RESPONSE ->
                "Processing server response..."
            IntegrityRepository.VerificationStep.COMPLETE ->
                "Verification complete"
        }
    }

    private fun buildResponseDetails(response: VerificationResponse): String {
        return buildString {
            appendLine("Status: ${response.status}")
            appendLine("Message: ${response.message}")
            appendLine("Timestamp: ${response.timestamp}")
            response.errorCode?.let {
                appendLine("Error Code: $it")
            }
        }
    }

    /**
     * Resets the UI state to idle.
     */
    fun reset() {
        _uiState.value = UiState.Idle
        _serverResponse.value = ""
        _progressMessage.value = ""
    }
}

