package com.mato.syai.voiceAssistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VoiceAssistantViewModel : ViewModel() {

    private val _state = MutableStateFlow(VoiceAssistantState())
    val state: StateFlow<VoiceAssistantState> = _state.asStateFlow()

    private val _effect = Channel<VoiceAssistantEffect>()
    val effect = _effect.receiveAsFlow()

    fun onEvent(event: VoiceAssistantEvent) {
        when (event) {

            is VoiceAssistantEvent.StartService -> {
                updateState { copy(isServiceRunning = true) }
                sendEffect(VoiceAssistantEffect.StartService)
            }

            is VoiceAssistantEvent.StopService -> {
                updateState { copy(isServiceRunning = false) }
                sendEffect(VoiceAssistantEffect.StopService)
            }

            is VoiceAssistantEvent.CheckPermissions -> {
                sendEffect(VoiceAssistantEffect.RequestPermissions)
            }

            is VoiceAssistantEvent.UpdateStatus -> {
                updateState { copy(statusText = event.text) }
            }

            is VoiceAssistantEvent.OnWakeWordDetected -> {
                updateState { copy(isWakeWordDetected = event.detected) }
            }

            is VoiceAssistantEvent.OnLastCommand -> {
                updateState { copy(lastCommand = event.command) }
            }
        }
    }

    private fun updateState(reducer: VoiceAssistantState.() -> VoiceAssistantState) {
        _state.value = _state.value.reducer()
    }

    private fun sendEffect(effect: VoiceAssistantEffect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }
}


data class VoiceAssistantState(
    val statusText: String = "Starting voice assistant...",
    val lastCommand: String = "",
    val isServiceRunning: Boolean = false,
    val isWakeWordDetected: Boolean = false
)

sealed interface VoiceAssistantEvent {
    object StartService : VoiceAssistantEvent
    object StopService : VoiceAssistantEvent
    object CheckPermissions : VoiceAssistantEvent

    data class UpdateStatus(val text: String) : VoiceAssistantEvent
    data class OnWakeWordDetected(val detected: Boolean) : VoiceAssistantEvent
    data class OnLastCommand(val command: String) : VoiceAssistantEvent
}

sealed interface VoiceAssistantEffect {
    data object RequestPermissions : VoiceAssistantEffect
    data object StartService : VoiceAssistantEffect
    data object StopService : VoiceAssistantEffect
}


