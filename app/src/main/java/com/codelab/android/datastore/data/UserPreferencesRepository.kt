/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codelab.android.datastore.data

import android.content.Context
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

data class UserPreference(
    val showCompleted: Boolean,
    val sortOrder: SortOrder
)

private const val USER_PREFERENCES_NAME = "user_preferences"
private const val SORT_ORDER_KEY = "sort_order"

object PreferencesKey {
    val SHOW_COMPLETED = booleanPreferencesKey("show_completed")
    val SORT_ORDER = stringPreferencesKey("sort_order")
}

enum class SortOrder {
    NONE,
    BY_DEADLINE,
    BY_PRIORITY,
    BY_DEADLINE_AND_PRIORITY
}

/**
 * Class that handles saving and retrieving user preferences
 */
class UserPreferencesRepository constructor(
    private val dataStore: DataStore<Preferences>,
    context: Context) {

    private val sharedPreferences =
        context.applicationContext.getSharedPreferences(USER_PREFERENCES_NAME, Context.MODE_PRIVATE)

    val userPreferencesFlow: Flow<UserPreference> = dataStore.data
        .catch { exception ->
            if(exception is IOException) {
                emit(emptyPreferences())
            }else {
                throw exception
            }
        }
        .map { preferences ->

            val sortOrder = SortOrder.valueOf(
                preferences[PreferencesKey.SORT_ORDER] ?: SortOrder.NONE.name
            )

            val showCompleted = preferences[PreferencesKey.SHOW_COMPLETED]?: false
            UserPreference(showCompleted, sortOrder)
        }

    suspend fun updateShowCompleted(showCompleted: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKey.SHOW_COMPLETED] = showCompleted
        }
    }


    /**
     * Update the sort order by priority and deadline and notify the observer
     */
    suspend fun enableSortByDeadline(enable: Boolean) {
        //store the current sortOrder value into currentOrder variable
        dataStore.edit { preferences ->
            val currentOrder = SortOrder.valueOf(
                preferences[PreferencesKey.SORT_ORDER] ?: SortOrder.NONE.name
            )
            //store the sortOrder BY_DEADLINE_AND_PRIORITY into currentOrder variable if enabled and the currentOrder is equals to BY_PRIORITY
            //store BY_DEADLINE into currentOrder variable if enabled and the currentOrder is not equals to BY_PRIORITY
            //store BY_PRIORITY into currentOrder variable if not enabled and currentOrder is equals BY_DEADLINE_AND_PRIORITY
            //store NONE into currentOrder variable of if not enabled and the currentOrder is not equals BY_DEAD_LINE_AND_PRIORITY
            val newSortOrder =
                if (enable) {
                    if (currentOrder == SortOrder.BY_PRIORITY) {
                        SortOrder.BY_DEADLINE_AND_PRIORITY
                    } else {
                        SortOrder.BY_DEADLINE
                    }
                } else {
                    if (currentOrder == SortOrder.BY_DEADLINE_AND_PRIORITY) {
                        SortOrder.BY_PRIORITY
                    } else {
                        SortOrder.NONE
                    }
                }

            //update the sort order with the new value
            updateSortOrder(newSortOrder)

            //This will notify the observer
            preferences[PreferencesKey.SORT_ORDER] = newSortOrder.name
        }
    }

    /**
     * Update the sort order by priority
     */
    suspend fun enableSortByPriority(enable: Boolean) {
        dataStore.edit { preferences ->
            val currentOrder = SortOrder.valueOf(
                preferences[PreferencesKey.SORT_ORDER] ?: SortOrder.NONE.name
            )
            val newSortOrder =
                if (enable) {
                    if (currentOrder == SortOrder.BY_DEADLINE) {
                        SortOrder.BY_DEADLINE_AND_PRIORITY
                    } else {
                        SortOrder.BY_PRIORITY
                    }
                } else {
                    if (currentOrder == SortOrder.BY_DEADLINE_AND_PRIORITY) {
                        SortOrder.BY_DEADLINE
                    } else {
                        SortOrder.NONE
                    }
                }
            updateSortOrder(newSortOrder)
            preferences[PreferencesKey.SORT_ORDER] = newSortOrder.name
        }
    }

    private fun updateSortOrder(sortOrder: SortOrder) {
        sharedPreferences.edit {
            putString(SORT_ORDER_KEY, sortOrder.name)
        }
    }
}
