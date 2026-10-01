package net.mamby.health.security

import android.content.Context
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.rules.ExternalResource
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import net.mamby.health.data.VaultRepository
import net.mamby.health.MainActivity
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ScreenshotProtectionInstrumentedTest {
    @Inject
    lateinit var vaultRepository: VaultRepository

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val workManagerRule = object : ExternalResource() {
        override fun before() {
            hiltRule.inject()
            WorkManagerTestInitHelper.initializeTestWorkManager(
                ApplicationProvider.getApplicationContext<Context>(),
                Configuration.Builder().build(),
            )
            runBlocking { vaultRepository.initialize() }
        }
    }

    @get:Rule(order = 2)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun screenshotPreferenceUpdatesWindowAndCannotOverrideAppLock() {
        val activity = composeRule.activity
        val repository = activity.settingsRepository
        val original = runBlocking { repository.settings.first() }
        fun awaitProtection(secure: Boolean) {
            composeRule.waitUntil(timeoutMillis = 10_000) {
                (activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0) == secure
            }
        }
        try {
            runBlocking {
                repository.setAppLockEnabled(false)
                repository.setAllowScreenshots(false)
            }
            awaitProtection(true)
            runBlocking { repository.setAllowScreenshots(true) }
            awaitProtection(false)
            composeRule.activityRule.scenario.recreate()
            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE == 0
            }
            val recreated = composeRule.activity
            runBlocking { recreated.settingsRepository.setAppLockEnabled(true) }
            composeRule.waitUntil(timeoutMillis = 10_000) {
                recreated.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0
            }
            runBlocking {
                recreated.settingsRepository.setAppLockEnabled(false)
                recreated.settingsRepository.setAllowScreenshots(false)
            }
            composeRule.waitUntil(timeoutMillis = 10_000) {
                recreated.appLockManager.state.value is AppLockState.Disabled
            }
            assertFalse(runBlocking { recreated.settingsRepository.settings.first() }.allowScreenshots)
        } finally {
            runBlocking {
                composeRule.activity.settingsRepository.setAllowScreenshots(original.allowScreenshots)
                composeRule.activity.settingsRepository.setAppLockEnabled(original.appLockEnabled)
            }
        }
    }
}
