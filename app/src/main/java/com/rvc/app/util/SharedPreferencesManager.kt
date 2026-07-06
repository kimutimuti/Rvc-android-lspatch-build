package com.rvc.app.util

import android.content.Context
import com.rvc.app.data.ModelInfo

object SharedPreferencesManager {
    fun getPitch(context: Context): Int = 0
    fun getNaturality(context: Context): Int = 50
    fun getCurrentModelInfo(context: Context): ModelInfo = ModelInfo("Default", "path", "ONNX")
    
    fun savePitch(context: Context, pitch: Int) {}
    fun saveNaturality(context: Context, naturality: Int) {}
}
