package com.lifeos.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.data.LifeOSState
import com.lifeos.data.LifeOSStorage
import com.lifeos.data.addingActivity
import com.lifeos.data.addingDeadline
import com.lifeos.data.addingGoal
import com.lifeos.data.addingNote
import com.lifeos.data.removingActivity
import com.lifeos.data.removingDeadline
import com.lifeos.data.removingGoal
import com.lifeos.data.removingNote
import com.lifeos.data.settingGoalProgress
import com.lifeos.data.togglingActivity
import com.lifeos.data.togglingDeadline
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LifeOSViewModel(context: Context) : ViewModel() {

    private val appContext = context.applicationContext
    private val _state = MutableStateFlow(LifeOSState())
    val state: StateFlow<LifeOSState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            LifeOSStorage.flow(appContext).collect { _state.value = it }
        }
    }

    fun update(mutate: (LifeOSState) -> LifeOSState) {
        _state.value = mutate(_state.value)
        viewModelScope.launch { LifeOSStorage.update(appContext, mutate) }
    }

    fun onActivityToggle(id: String, done: Boolean) = update { it.togglingActivity(id, done) }
    fun onActivityRemove(id: String) = update { it.removingActivity(id) }
    fun onDeadlineToggle(id: String, done: Boolean) = update { it.togglingDeadline(id, done) }
    fun onDeadlineRemove(id: String) = update { it.removingDeadline(id) }
    fun onGoalProgress(id: String, progressPercent: Int) =
        update { it.settingGoalProgress(id, progressPercent) }
    fun onGoalRemove(id: String) = update { it.removingGoal(id) }
    fun onNoteRemove(id: String) = update { it.removingNote(id) }

    fun addActivity(title: String) = update { it.addingActivity(title) }
    fun addNote(title: String, body: String) = update { it.addingNote(title, body) }
    fun addDeadline(title: String) = update { it.addingDeadline(title) }
    fun addGoal(title: String) = update { it.addingGoal(title) }
}
