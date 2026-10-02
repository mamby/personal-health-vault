package net.mamby.health.di

import dagger.Binds
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import net.mamby.androidkit.compose.form.AndroidKitSettingsStore
import net.mamby.androidkit.compose.form.AndroidKitSettingsStorageProtection
import net.mamby.androidkit.compose.form.AndroidKitSettingsStoreMigration
import net.mamby.androidkit.compose.form.AndroidKitSearchHistorySnapshot
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.first
import java.time.Duration
import androidx.core.app.LocaleManagerCompat
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.util.UUID
import javax.inject.Singleton
import net.mamby.health.crypto.AesGcmVaultCipher
import net.mamby.health.crypto.AndroidKeystoreVaultKeyProvider
import net.mamby.health.crypto.VaultCipher
import net.mamby.health.crypto.VaultKeyProvider
import net.mamby.health.data.DefaultVaultRepository
import net.mamby.health.data.DocumentBlobStore
import net.mamby.health.data.EncryptedDocumentBlobStore
import net.mamby.health.data.EncryptedVaultStore
import net.mamby.health.data.UuidGenerator
import net.mamby.health.data.VaultRepository
import net.mamby.health.data.VaultStore

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreDataBindings {
    @Binds
    @Singleton
    abstract fun bindVaultCipher(implementation: AesGcmVaultCipher): VaultCipher

    @Binds
    @Singleton
    abstract fun bindVaultKeyProvider(
        implementation: AndroidKeystoreVaultKeyProvider,
    ): VaultKeyProvider

    @Binds
    @Singleton
    abstract fun bindVaultStore(implementation: EncryptedVaultStore): VaultStore

    @Binds
    @Singleton
    abstract fun bindDocumentBlobStore(
        implementation: EncryptedDocumentBlobStore,
    ): DocumentBlobStore

    @Binds
    @Singleton
    abstract fun bindVaultRepository(implementation: DefaultVaultRepository): VaultRepository
}

@Module
@InstallIn(SingletonComponent::class)
object CoreDataProviders {
    @Provides
    @Singleton
    fun provideKitSettingsStore(@ApplicationContext context: Context): AndroidKitSettingsStore {
        val legacy = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("settings.preferences_pb") },
        )
        val migration = object : AndroidKitSettingsStoreMigration {
            override val id = "host-settings-v1"
            override suspend fun readHistories(): Map<String, AndroidKitSearchHistorySnapshot> = emptyMap()
            override suspend fun cleanUp() = Unit
            override suspend fun readPreferences(): Preferences {
                val preferences = legacy.data.first().toMutablePreferences()
                val oldTimeout = longPreferencesKey("app_lock_timeout_millis")
                preferences[stringPreferencesKey("app_lock_timeout")] = Duration.ofMillis(
                    (preferences[oldTimeout] ?: 0L).coerceAtLeast(0L),
                ).toString()
                preferences.remove(oldTimeout)
                preferences[stringPreferencesKey("selected_language_tag")] =
                    LocaleManagerCompat.getApplicationLocales(context).get(0)?.language?.takeIf { it.isNotBlank() }
                        ?: preferences[stringPreferencesKey("locale_tag")]?.takeIf { it.isNotBlank() }
                        ?: "system"
                return preferences.toPreferences()
            }
        }
        return AndroidKitSettingsStore.open(context, "settings", AndroidKitSettingsStorageProtection.Encrypted, listOf(migration))
    }

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()

    @Provides
    @Singleton
    fun provideUuidGenerator(): UuidGenerator = UuidGenerator(UUID::randomUUID)

}
