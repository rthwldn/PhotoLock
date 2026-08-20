package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainVaultScreen
import com.example.ui.VaultViewModel
import com.example.ui.lock.LockScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VaultBackground

class MainActivity : FragmentActivity() {

    private val viewModel: VaultViewModel by viewModels()
    private var backgroundTimestamp: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VaultBackground
                ) {
                    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()

                    Crossfade(
                        targetState = isUnlocked,
                        label = "vault_lock_fade"
                    ) { unlocked ->
                        if (unlocked) {
                            MainVaultScreen(
                                viewModel = viewModel,
                                onLockVault = { viewModel.lockVault() }
                            )
                        } else {
                            LockScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        backgroundTimestamp = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        if (backgroundTimestamp > 0) {
            val elapsedSec = (System.currentTimeMillis() - backgroundTimestamp) / 1000
            val threshold = viewModel.securityPrefs.autoLockSeconds
            if (threshold >= 0 && elapsedSec >= threshold) {
                viewModel.lockVault()
            }
            backgroundTimestamp = 0
        }
    }
}

