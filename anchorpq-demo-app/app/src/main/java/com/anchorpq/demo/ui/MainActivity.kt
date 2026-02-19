package com.anchorpq.demo.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.anchorpq.demo.BuildConfig
import com.anchorpq.demo.R
import com.anchorpq.demo.databinding.ActivityMainBinding
import com.anchorpq.demo.model.VerificationStatus
import com.anchorpq.demo.util.IntegrityConfig

/**
 * Main activity displaying the integrity verification UI.
 *
 * This activity:
 * 1. Displays the application's Merkle root (injected by AnchorPQ plugin)
 * 2. Allows users to trigger integrity verification
 * 3. Shows the verification status from the server
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    // Integrity values from BuildConfig (injected by AnchorPQ plugin)
    private val merkleRoot: String by lazy {
        loadMerkleRoot()
    }
    private val version: String = BuildConfig.VERSION_NAME
    private val variant: String = BuildConfig.BUILD_TYPE
    private val serverUrl: String = BuildConfig.SERVER_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupObservers()
        viewModel.initialize(serverUrl)
    }

    private fun setupUI() {
        // Display integrity information
        binding.tvMerkleRoot.text = formatMerkleRoot(merkleRoot)
        binding.tvVersion.text = version
        binding.tvVariant.text = variant
        binding.tvServerUrl.text = serverUrl

        // Verify button click handler
        binding.btnVerify.setOnClickListener {
            viewModel.verifyIntegrity(merkleRoot, version, variant)
        }

        // Copy Merkle root to clipboard
        binding.btnCopyRoot.setOnClickListener {
            copyToClipboard(merkleRoot)
        }
    }

    private fun setupObservers() {
        // Observe UI state changes
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is MainViewModel.UiState.Idle -> showIdleState()
                is MainViewModel.UiState.Loading -> showLoadingState()
                is MainViewModel.UiState.Success -> showSuccessState(state.status, state.message)
                is MainViewModel.UiState.Error -> showErrorState(state.message)
            }
        }

        // Observe progress messages
        viewModel.progressMessage.observe(this) { message ->
            if (message.isNotEmpty()) {
                binding.tvStatus.text = message
            }
        }

        // Observe server response
        viewModel.serverResponse.observe(this) { response ->
            binding.tvServerResponse.text = response.ifEmpty {
                "Server response will appear here..."
            }
        }
    }

    private fun showIdleState() {
        binding.progressBar.visibility = View.GONE
        binding.btnVerify.isEnabled = true
        binding.tvStatus.text = getString(R.string.status_idle)
        binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_idle))
        resetCardStyle()
    }

    private fun showLoadingState() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnVerify.isEnabled = false
        binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.primary))
        resetCardStyle()
    }

    private fun showSuccessState(status: VerificationStatus, message: String) {
        binding.progressBar.visibility = View.GONE
        binding.btnVerify.isEnabled = true

        when (status) {
            VerificationStatus.APPROVED -> {
                binding.tvStatus.text = getString(R.string.status_approved)
                binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_approved))
                binding.cardStatus.strokeColor = ContextCompat.getColor(this, R.color.status_approved)
                binding.cardStatus.strokeWidth = resources.getDimensionPixelSize(R.dimen.card_stroke_width)
            }
            VerificationStatus.RESTRICTED -> {
                binding.tvStatus.text = getString(R.string.status_restricted)
                binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_restricted))
                binding.cardStatus.strokeColor = ContextCompat.getColor(this, R.color.status_restricted)
                binding.cardStatus.strokeWidth = resources.getDimensionPixelSize(R.dimen.card_stroke_width)
            }
            VerificationStatus.REJECTED -> {
                binding.tvStatus.text = getString(R.string.status_rejected)
                binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_rejected))
                binding.cardStatus.strokeColor = ContextCompat.getColor(this, R.color.status_rejected)
                binding.cardStatus.strokeWidth = resources.getDimensionPixelSize(R.dimen.card_stroke_width)
            }
        }
    }

    private fun showErrorState(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.btnVerify.isEnabled = true
        binding.tvStatus.text = getString(R.string.status_error, message)
        binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_rejected))
        binding.cardStatus.strokeColor = ContextCompat.getColor(this, R.color.status_rejected)
        binding.cardStatus.strokeWidth = resources.getDimensionPixelSize(R.dimen.card_stroke_width)
    }

    private fun resetCardStyle() {
        binding.cardStatus.strokeWidth = 0
    }

    /**
     * Loads the Merkle root from BuildConfig or fallback sources.
     */
    private fun loadMerkleRoot(): String {
        // Try to get from BuildConfig (injected by AnchorPQ plugin)
        return try {
            val field = BuildConfig::class.java.getDeclaredField("MERKLE_ROOT")
            field.get(null) as? String ?: loadFallbackMerkleRoot()
        } catch (e: NoSuchFieldException) {
            // Field not found, try fallback
            loadFallbackMerkleRoot()
        }
    }

    /**
     * Fallback method to get Merkle root from assets or IntegrityConfig.
     */
    private fun loadFallbackMerkleRoot(): String {
        return try {
            IntegrityConfig.loadFromAssets(this)?.merkleRoot ?: "NOT_AVAILABLE"
        } catch (e: Exception) {
            "NOT_AVAILABLE"
        }
    }

    /**
     * Formats the Merkle root for display (truncated with ellipsis).
     */
    private fun formatMerkleRoot(root: String): String {
        return if (root.length > 32) {
            "${root.take(16)}...${root.takeLast(16)}"
        } else {
            root
        }
    }

    /**
     * Copies text to the system clipboard.
     */
    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Merkle Root", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Merkle root copied to clipboard", Toast.LENGTH_SHORT).show()
    }
}

