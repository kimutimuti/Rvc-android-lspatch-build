package com.rvc.app.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class RVCProcessingService : Service() {
    inner class RVCBinder : Binder() {
        fun getService(): RVCProcessingService = this@RVCProcessingService
    }

    override fun onBind(intent: Intent?): IBinder = RVCBinder()
    
    fun startEngine() {}
    fun stopEngine() {}
    fun setPitchValue(value: Int) {}
    fun setNaturalityValue(value: Int) {}
    fun getEngineStats(): EngineStats = EngineStats()
}

data class EngineStats(
    val latencyMaxMs: Int = 0,
    val dspLoadPercent: Int = 0,
    val isDegraded: Boolean = false,
    val isEngineRunning: Boolean = false
)
