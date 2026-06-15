package com.utopia.finance.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.utopia.finance.domain.model.CurrencyCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

interface SettingsRepository {
    val defaultCurrency: Flow<CurrencyCode>
    val exportTreeUri: Flow<String?>
    val biometricEnabled: Flow<Boolean>
    suspend fun setDefaultCurrency(currency: CurrencyCode)
    suspend fun setExportTreeUri(uri: String?)
    suspend fun setBiometricEnabled(enabled: Boolean)
}

class DataStoreSettingsRepository(
    context: Context,
) : SettingsRepository {
    private val dataStore = context.settingsDataStore

    override val defaultCurrency: Flow<CurrencyCode> =
        dataStore.data.map { preferences ->
            preferences[DEFAULT_CURRENCY]?.let(CurrencyCode::valueOf) ?: CurrencyCode.CNY
        }

    override val exportTreeUri: Flow<String?> =
        dataStore.data.map { preferences ->
            preferences[EXPORT_TREE_URI]
        }

    override val biometricEnabled: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[BIOMETRIC_ENABLED] ?: true
        }

    override suspend fun setDefaultCurrency(currency: CurrencyCode) {
        dataStore.edit { preferences ->
            preferences[DEFAULT_CURRENCY] = currency.name
        }
    }

    override suspend fun setExportTreeUri(uri: String?) {
        dataStore.edit { preferences ->
            if (uri == null) {
                preferences.remove(EXPORT_TREE_URI)
            } else {
                preferences[EXPORT_TREE_URI] = uri
            }
        }
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[BIOMETRIC_ENABLED] = enabled
        }
    }

    companion object {
        private val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        private val EXPORT_TREE_URI = stringPreferencesKey("export_tree_uri")
        private val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    }
}
