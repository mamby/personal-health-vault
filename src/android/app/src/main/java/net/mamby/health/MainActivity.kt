package net.mamby.health

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.mamby.health.settings.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import net.mamby.health.data.VaultRepository
import net.mamby.health.data.VaultState
import net.mamby.health.navigation.DeepLinkCoordinator
import net.mamby.health.security.AppLockManager
import net.mamby.health.security.AppLockState
import net.mamby.health.security.AppLockWindowProtector
import net.mamby.health.ui.HealthVaultApp

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var deepLinkCoordinator: DeepLinkCoordinator

    @Inject
    lateinit var vaultRepository: VaultRepository

    @Inject
    lateinit var appLockManager: AppLockManager

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(appLockManager)
        splashScreen.setKeepOnScreenCondition {
            vaultRepository.state.value is VaultState.Loading ||
                appLockManager.state.value is AppLockState.Initializing
        }
        enableEdgeToEdge()
        AppLockWindowProtector.protect(window)
        lifecycleScope.launch {
            combine(settingsRepository.settings, appLockManager.state) { settings, lockState ->
                settings.allowScreenshots &&
                    (lockState is AppLockState.Disabled || lockState is AppLockState.Unlocked)
            }.collect { allowed ->
                AppLockWindowProtector.setScreenshotsAllowed(window, allowed)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        deepLinkCoordinator.accept(intent)

        setContent {
            HealthVaultApp(onStatusBarThemeChanged = ::applyStatusBarAppearance)
        }
    }

    private fun applyStatusBarAppearance(isDarkTheme: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = !isDarkTheme
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkCoordinator.accept(intent)
    }
}
