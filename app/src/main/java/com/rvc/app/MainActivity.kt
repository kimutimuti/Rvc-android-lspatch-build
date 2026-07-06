package com.rvc.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.rvc.app.data.ModelInfo
import com.rvc.app.service.RVCProcessingService
import com.rvc.app.service.RVCProcessingService.RVCBinder
import com.rvc.app.util.SharedPreferencesManager
import com.rvc.module.R

/**
 * Interface utilisateur principale pour le moteur RVC.
 * Elle est l'interface "Game Turbo" permettant la configuration et le monitoring.
 */
class MainActivity : AppCompatActivity() {

    private val TAG = "RVC_MainActivity"

    // UI Components
    private lateinit var switchPower: Switch
    private lateinit var sliderPitch: SeekBar
    private lateinit var sliderNaturality: SeekBar
    private lateinit var tvPitchValue: TextView
    private lateinit var tvNaturalityValue: TextView
    private lateinit var tvCurrentModel: TextView
    private lateinit var tvLatency: TextView // HUD de Performance
    private lateinit var tvDSPLoad: TextView // HUD de Performance
    private lateinit var btnSelectModel: Button

    // Service RVC
    private var rvcService: RVCProcessingService? = null
    private var isBound = false

    // Handler pour les mises à jour UI en temps réel (HUD)
    private val uiHandler = Handler(Looper.getMainLooper())
    private val updateInterval: Long = 500 // Mise à jour toutes les 500 ms

    /**
     * Connexion au service RVCProcessingService
     */
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as RVCBinder
            rvcService = binder.getService()
            isBound = true
            Log.i(TAG, "RVCProcessingService lié avec succès.")
            
            // Initialiser l'état de l'UI à partir du service
            initializeUIState()
            
            // Démarrer la mise à jour périodique du HUD
            uiHandler.post(updateHudRunnable)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            rvcService = null
            Log.w(TAG, "RVCProcessingService délié.")
            uiHandler.removeCallbacks(updateHudRunnable)
        }
    }

    /**
     * Runnable pour mettre à jour les statistiques de performance (HUD).
     */
    private val updateHudRunnable = object : Runnable {
        override fun run() {
            rvcService?.let { service ->
                val stats = service.getEngineStats()
                
                // Mettre à jour le HUD V12.0
                tvLatency.text = "Latence Max: ${stats.latencyMaxMs}ms"
                tvDSPLoad.text = "Charge DSP: ${stats.dspLoadPercent}% / ${if (stats.isDegraded) "🚨FP16" else "🟢FP32"}"
                
                // Mettre à jour l'interrupteur d'alimentation en fonction de l'état du service
                switchPower.isChecked = stats.isEngineRunning

            } ?: run {
                // Si non lié, affiche l'état par défaut
                tvLatency.text = "Latence Max: --"
                tvDSPLoad.text = "Charge DSP: --"
            }
            uiHandler.postDelayed(this, updateInterval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Note: Le fichier layout/activity_main.xml doit être créé
        setContentView(R.layout.activity_main) 
        
        initializeUIComponents()
        setupListeners()
        
        // Démarrer et lier le service de traitement (qui lance le moteur NDK)
        startAndBindService()
    }

    /**
     * Initialise tous les composants UI
     */
    private fun initializeUIComponents() {
        switchPower = findViewById(R.id.switch_power)
        sliderPitch = findViewById(R.id.slider_pitch)
        sliderNaturality = findViewById(R.id.slider_naturality)
        tvPitchValue = findViewById(R.id.tv_pitch_value)
        tvNaturalityValue = findViewById(R.id.tv_naturality_value)
        tvCurrentModel = findViewById(R.id.tv_current_model)
        tvLatency = findViewById(R.id.tv_latency_hud)
        tvDSPLoad = findViewById(R.id.tv_dsp_load_hud)
        btnSelectModel = findViewById(R.id.btn_select_model)
        
        // Configuration initiale des sliders
        sliderPitch.min = -12 // Octave inférieure
        sliderPitch.max = 12  // Octave supérieure
        sliderNaturality.min = 0
        sliderNaturality.max = 100
    }
    
    /**
     * Récupère l'état actuel (Pitch, Modèle) à partir du service ou des préférences.
     */
    private fun initializeUIState() {
        // Récupérer les valeurs de préférence (ou l'état du service)
        val currentPitch = SharedPreferencesManager.getPitch(this)
        val currentNaturality = SharedPreferencesManager.getNaturality(this)
        val currentModel = SharedPreferencesManager.getCurrentModelInfo(this)

        sliderPitch.progress = currentPitch
        tvPitchValue.text = currentPitch.toString()
        
        sliderNaturality.progress = currentNaturality
        tvNaturalityValue.text = "$currentNaturality%"
        
        tvCurrentModel.text = "Modèle Actif: ${currentModel.name}"
    }

    /**
     * Configure tous les écouteurs d'événements.
     */
    private fun setupListeners() {
        
        // 1. Bouton d'alimentation (SwitchPower)
        switchPower.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Tente de démarrer le moteur RVC
                rvcService?.startEngine()
                Log.i(TAG, "Moteur RVC Démarré.")
            } else {
                // Arrête le moteur RVC (passe en mode Low Power Pass-Through)
                rvcService?.stopEngine()
                Log.i(TAG, "Moteur RVC Arrêté.")
            }
        }
        
        // 2. Slider du Pitch (Tonalité)
        sliderPitch.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvPitchValue.text = progress.toString()
                if (fromUser) {
                    // Mettre à jour le moteur C++ en temps réel
                    rvcService?.setPitchValue(progress) 
                    SharedPreferencesManager.savePitch(this@MainActivity, progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 3. Slider de Naturalité (Timbre)
        sliderNaturality.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvNaturalityValue.text = "$progress%"
                if (fromUser) {
                    rvcService?.setNaturalityValue(progress) 
                    SharedPreferencesManager.saveNaturality(this@MainActivity, progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 4. Sélection de Modèle
        btnSelectModel.setOnClickListener {
            // Logique de lancement de l'activité ModelSelection
            startActivity(Intent(this, ModelSelectionActivity::class.java))
        }
    }

    /**
     * Démarre le RVCProcessingService et le lie à cette activité.
     */
    private fun startAndBindService() {
        val intent = Intent(this, RVCProcessingService::class.java)
        
        // Démarrer le service en premier (il devient un service de premier plan)
        startForegroundService(intent) 
        
        // Lier l'activité au service pour l'échange de données
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Arrêter la mise à jour du HUD
        uiHandler.removeCallbacks(updateHudRunnable) 
        
        // Si lié, délier le service
        if (isBound) {
            unbindService(connection)
        }
    }
}
