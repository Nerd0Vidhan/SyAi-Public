package com.mato.syai.step_tracker

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import android.content.SharedPreferences

class StepCounterViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences = application.getSharedPreferences("step_prefs", Context.MODE_PRIVATE)

    private val _stepCount = MutableStateFlow(0)
    val stepCount = _stepCount.asStateFlow()

    private val sensorManager: SensorManager = application.getSystemService(SensorManager::class.java)
    private val stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private var baseStepCount = prefs.getFloat("baseStepCount", -1f)

    private val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null || event.values.isEmpty()) return
            val totalSteps = event.values[0]

            if (baseStepCount < 0) {
                baseStepCount = totalSteps
                prefs.edit().putFloat("baseStepCount", baseStepCount).apply()
            }

            val currentSteps = (totalSteps - baseStepCount).toInt()
            viewModelScope.launch {
                _stepCount.emit(currentSteps)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    init {
        stepCounterSensor?.let {
            sensorManager.registerListener(stepListener, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Save baseStepCount on ViewModel clear to persist
        prefs.edit().putFloat("baseStepCount", baseStepCount).apply()
        sensorManager.unregisterListener(stepListener)
    }
}



class StepCounterViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StepCounterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StepCounterViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
