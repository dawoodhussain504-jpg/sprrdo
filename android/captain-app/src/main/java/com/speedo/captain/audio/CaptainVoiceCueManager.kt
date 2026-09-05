package com.speedo.captain.audio

import android.content.Context
import android.content.SharedPreferences
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class CaptainVoiceCueManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isMuted = MutableStateFlow(prefs.getBoolean(KEY_MUTED, false))
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _language = MutableStateFlow(prefs.getString(KEY_LANG, "hi") ?: "hi") // "hi" (Hindi) or "en" (English)
    val language: StateFlow<String> = _language.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var pendingSpeech: String? = null

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                applyLocale(_language.value)
                tts?.setSpeechRate(1.05f) // Slightly faster for clear roadside cues
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })

                pendingSpeech?.let { text ->
                    speakRaw(text)
                    pendingSpeech = null
                }
                Log.d(TAG, "TTS Engine Initialized successfully.")
            } else {
                Log.w(TAG, "TTS Engine initialization failed with status: $status")
            }
        }
    }

    private fun applyLocale(lang: String) {
        val targetLocale = if (lang == "hi") {
            Locale("hi", "IN")
        } else {
            Locale("en", "IN")
        }

        val res = tts?.setLanguage(targetLocale)
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Locale $targetLocale not fully supported, falling back to English")
            tts?.language = Locale.ENGLISH
        }
    }

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        prefs.edit().putBoolean(KEY_MUTED, muted).apply()
        if (muted) {
            stop()
        }
    }

    fun toggleMute(): Boolean {
        val newState = !_isMuted.value
        setMuted(newState)
        return newState
    }

    fun setLanguage(lang: String) {
        _language.value = lang
        prefs.edit().putString(KEY_LANG, lang).apply()
        if (isInitialized) {
            applyLocale(lang)
        }
    }

    fun toggleLanguage(): String {
        val newLang = if (_language.value == "hi") "en" else "hi"
        setLanguage(newLang)
        return newLang
    }

    private fun speakRaw(text: String) {
        if (_isMuted.value) {
            Log.d(TAG, "Speech muted. Skipping: $text")
            return
        }

        if (!isInitialized || tts == null) {
            pendingSpeech = text
            return
        }

        try {
            applyLocale(_language.value)
            val utteranceId = "cue_${System.currentTimeMillis()}"
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            Log.d(TAG, "Speaking: $text (lang=${_language.value})")
        } catch (e: Exception) {
            Log.e(TAG, "Error while speaking", e)
        }
    }

    /**
     * Announces incoming ride request details
     */
    fun speakIncomingRide(
        fare: Int,
        distanceKm: Double = 0.0,
        pickup: String = "",
        vehicleType: String = "bike"
    ) {
        val isHindi = _language.value == "hi"
        val distText = if (distanceKm > 0.0) {
            if (isHindi) "${String.format(Locale.ROOT, "%.1f", distanceKm)} किलोमीटर"
            else "${String.format(Locale.ROOT, "%.1f", distanceKm)} kilometers"
        } else ""

        val vehicleName = when (vehicleType.lowercase(Locale.ROOT)) {
            "auto" -> if (isHindi) "ऑटो" else "Auto"
            "car" -> if (isHindi) "कैब" else "Cab"
            else -> if (isHindi) "बाइक" else "Bike"
        }

        val speech = if (isHindi) {
            if (distText.isNotEmpty()) {
                "नया राइड! दूरी $distText. किराया $fare रुपये. $vehicleName."
            } else {
                "नया राइड! किराया $fare रुपये. $vehicleName."
            }
        } else {
            if (distText.isNotEmpty()) {
                "New ride request! Distance $distText. Fare $fare rupees. $vehicleName."
            } else {
                "New ride request! Fare $fare rupees. $vehicleName."
            }
        }

        speakRaw(speech)
    }

    /**
     * Prompt when Captain arrives at rider pickup
     */
    fun speakArrivedAtPickup() {
        val speech = if (_language.value == "hi") {
            "यात्री के स्थान पर पहुँच गए हैं. कृपया यात्री से ओटीपी पूछें."
        } else {
            "Arrived at pickup location. Please ask passenger for OTP."
        }
        speakRaw(speech)
    }

    /**
     * Prompt when OTP is verified and trip starts
     */
    fun speakRideStarted(destination: String = "") {
        val speech = if (_language.value == "hi") {
            if (destination.isNotBlank()) "राइड शुरू हो गई है. गंतव्य $destination की ओर चलें."
            else "राइड शुरू हो गई है. गंतव्य स्थान की ओर चलें."
        } else {
            if (destination.isNotBlank()) "Ride started. Navigating to $destination."
            else "Ride started. Navigating to destination."
        }
        speakRaw(speech)
    }

    /**
     * Prompt when Captain completes the ride
     */
    fun speakRideCompleted(fare: Int, isCash: Boolean = true) {
        val speech = if (_language.value == "hi") {
            if (isCash) {
                "राइड पूरी हो गई है. कृपया यात्री से $fare रुपये नकद लें."
            } else {
                "राइड पूरी हो गई है. किराया $fare रुपये ऑनलाइन प्राप्त हो गया है."
            }
        } else {
            if (isCash) {
                "Ride completed. Please collect $fare rupees cash from the passenger."
            } else {
                "Ride completed. Fare $fare rupees received online."
            }
        }
        speakRaw(speech)
    }

    fun stop() {
        try {
            tts?.stop()
            _isSpeaking.value = false
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping TTS", e)
        }
    }

    companion object {
        private const val TAG = "CaptainVoiceCueManager"
        private const val PREFS_NAME = "speedo_captain_voice_cues"
        private const val KEY_MUTED = "cues_muted"
        private const val KEY_LANG = "cues_lang"

        @Volatile
        private var instance: CaptainVoiceCueManager? = null

        fun getInstance(context: Context): CaptainVoiceCueManager {
            return instance ?: synchronized(this) {
                instance ?: CaptainVoiceCueManager(context).also { instance = it }
            }
        }
    }
}
