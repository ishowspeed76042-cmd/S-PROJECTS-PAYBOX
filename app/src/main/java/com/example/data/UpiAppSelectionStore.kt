package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UpiAppSelectionStore {
    private const val PREFS_NAME = "upi_selection_prefs"
    private const val KEY_ENABLE_ALL = "enable_all_upi_apps"
    private const val KEY_SELECTED_IDS = "selected_upi_app_ids"

    private val _enableAll = MutableStateFlow(true)
    val enableAll: StateFlow<Boolean> = _enableAll.asStateFlow()

    private val _selectedAppIds = MutableStateFlow(UpiAppCatalog.ALL_APP_IDS)
    val selectedAppIds: StateFlow<Set<String>> = _selectedAppIds.asStateFlow()

    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val allEnabled = prefs.getBoolean(KEY_ENABLE_ALL, true)
            val storedIds = prefs.getStringSet(KEY_SELECTED_IDS, null)
            _enableAll.value = allEnabled
            _selectedAppIds.value = if (storedIds == null) {
                UpiAppCatalog.ALL_APP_IDS
            } else {
                storedIds.intersect(UpiAppCatalog.ALL_APP_IDS)
            }
            initialized = true
        }
    }

    fun setEnableAll(context: Context, enabled: Boolean) {
        init(context)
        _enableAll.value = enabled
        val newSet = if (enabled) UpiAppCatalog.ALL_APP_IDS else _selectedAppIds.value
        _selectedAppIds.value = newSet
        persist(context, enabled, newSet)
    }

    fun setAppEnabled(context: Context, appId: String, enabled: Boolean) {
        init(context)
        if (appId !in UpiAppCatalog.ALL_APP_IDS) return

        val updated = _selectedAppIds.value.toMutableSet()
        if (enabled) {
            updated.add(appId)
        } else {
            updated.remove(appId)
        }
        val allSelected = updated.size == UpiAppCatalog.ALL_APP_IDS.size
        _selectedAppIds.value = updated
        _enableAll.value = allSelected
        persist(context, allSelected, updated)
    }

    fun isAppAllowed(context: Context, appId: String): Boolean {
        init(context)
        return _enableAll.value || appId in _selectedAppIds.value
    }

    fun getEffectiveAllowedAppIds(context: Context): Set<String> {
        init(context)
        return if (_enableAll.value) UpiAppCatalog.ALL_APP_IDS else _selectedAppIds.value
    }

    private fun persist(context: Context, enableAll: Boolean, selectedIds: Set<String>) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLE_ALL, enableAll)
            .putStringSet(KEY_SELECTED_IDS, selectedIds.toSet())
            .apply()
    }
}
