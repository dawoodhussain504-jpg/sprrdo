package com.speedo.rider.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class VoiceBookingResult(
    val rawTranscript: String,
    val cleanDestination: String,
    val vehicleType: String? = null
)

object VoiceIntentParser {
    private const val TAG = "VoiceIntentParser"

    // Vehicle keywords
    private val AUTO_KEYWORDS = listOf(
        "auto", "rickshaw", "autorickshaw", "auto rickshaw", "tuk tuk", "tuktuk", "e-rickshaw", "erickshaw",
        "ऑटो", "आटो", "रिक्शा", "रिक्षा", "ई रिक्शा"
    )

    private val BIKE_KEYWORDS = listOf(
        "bike", "motorcycle", "moto", "scooter", "scooty", "two wheeler", "twowheeler",
        "बाइक", "बाईक", "मोटरसाइकिल", "स्कूटी", "स्कूटर", "टू व्हीलर"
    )

    private val CAR_KEYWORDS = listOf(
        "car", "cab", "taxi", "uber", "four wheeler",
        "कार", "कैब", "गाड़ी", "गाड़ी", "टैक्सी"
    )

    // Conversational leading patterns (Hindi, Hinglish, English)
    private val PREFIX_PATTERNS = listOf(
        // English
        Regex("^(?:please\\s+)?(?:can\\s+you\\s+)?(?:take|drop|drive|bring)\\s+me\\s+to\\s+", RegexOption.IGNORE_CASE),
        Regex("^(?:i\\s+want\\s+to\\s+go\\s+to|i\\s+need\\s+to\\s+go\\s+to)\\s+", RegexOption.IGNORE_CASE),
        Regex("^(?:book\\s+a?\\s*(?:ride|cab|auto|bike|taxi)?\\s*(?:to|for)?)\\s+", RegexOption.IGNORE_CASE),
        Regex("^(?:let's\\s+go\\s+to|lets\\s+go\\s+to|heading\\s+to|going\\s+to|go\\s+to)\\s+", RegexOption.IGNORE_CASE),

        // Hindi / Hinglish salutations & pronouns
        Regex("^(?:bhaiya|bhai|bhaiji|boss|dost|driver\\s+bhaiya|suno|arre|arey|kripya|please)\\s*,?\\s*", RegexOption.IGNORE_CASE),
        Regex("^(?:mujhe|hume|humko|mereko|hamko|apko)\\s+", RegexOption.IGNORE_CASE),
        Regex("^(?:zara|jaldi|ek)\\s+", RegexOption.IGNORE_CASE)
    )

    // Conversational trailing patterns
    private val SUFFIX_PATTERNS = listOf(
        // Hindi actions
        Regex("\\s+(?:jana\\s+hai|jaana\\s+hai|chalna\\s+hai|le\\s+chalo|chalo|drop\\s+karo|pahuncha\\s+do)[.,!?:;\\-\\s]*$", RegexOption.IGNORE_CASE),
        Regex("\\s+(?:tak|pe|par|me|mein|pass|ke\\s+paas)[.,!?:;\\-\\s]*$", RegexOption.IGNORE_CASE),
        Regex("\\s+(?:se|mein)[.,!?:;\\-\\s]*$", RegexOption.IGNORE_CASE),

        // English actions
        Regex("\\s+(?:please|ride|trip|drop|pickup)[.,!?:;\\-\\s]*$", RegexOption.IGNORE_CASE)
    )

    /**
     * Parses spoken text to extract the vehicle type and destination.
     * Example: "Bhaiya, mujhe Ranchi Railway station jana hai auto se."
     * Output: cleanDestination = "Ranchi Railway station", vehicleType = "auto"
     */
    fun parse(rawText: String): VoiceBookingResult {
        if (rawText.isBlank()) {
            return VoiceBookingResult("", "")
        }

        var text = rawText.trim()
        var detectedVehicle: String? = null

        // 1. Detect and strip vehicle references
        val lower = text.lowercase(Locale.ROOT)

        when {
            AUTO_KEYWORDS.any { lower.contains(it) } -> detectedVehicle = "auto"
            BIKE_KEYWORDS.any { lower.contains(it) } -> detectedVehicle = "bike"
            CAR_KEYWORDS.any { lower.contains(it) } -> detectedVehicle = "car"
        }

        // Strip vehicle mentions like "by auto", "auto se", "bike se", "cab se", "in auto", "bike ride"
        val vehicleRegexes = listOf(
            Regex("(?:by|in|with)\\s+(?:bike|motorcycle|scooter|scooty|auto|rickshaw|cab|car|taxi)\\b", RegexOption.IGNORE_CASE),
            Regex("(?:bike|motorcycle|scooter|scooty|auto|rickshaw|cab|car|taxi|बाइक|ऑटो|कार|गाड़ी|कैब)\\s+(?:ride|trip|se|mein|me|se\\s+jana\\s+hai|se\\s+jaana\\s+hai)\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(?:auto|bike|cab|car|taxi|बाइक|ऑटो|कार|कैब)\\b", RegexOption.IGNORE_CASE)
        )

        for (vRegex in vehicleRegexes) {
            text = text.replace(vRegex, " ")
        }

        // Strip boundary punctuation after vehicle removal
        text = text.replace(Regex("^[.,!?:;\\-\\s]+"), "").replace(Regex("[.,!?:;\\-\\s]+$"), "").trim()

        // 2. Strip prefix phrases iteratively
        var changed = true
        var loopCount = 0
        while (changed && loopCount < 5) {
            val before = text
            for (pattern in PREFIX_PATTERNS) {
                text = text.replace(pattern, "")
            }
            text = text.replace(Regex("^[.,!?:;\\-\\s]+"), "").replace(Regex("[.,!?:;\\-\\s]+$"), "").trim()
            changed = (before != text)
            loopCount++
        }

        // 3. Strip suffix phrases iteratively
        changed = true
        loopCount = 0
        while (changed && loopCount < 5) {
            val before = text
            for (pattern in SUFFIX_PATTERNS) {
                text = text.replace(pattern, "")
            }
            text = text.replace(Regex("^[.,!?:;\\-\\s]+"), "").replace(Regex("[.,!?:;\\-\\s]+$"), "").trim()
            changed = (before != text)
            loopCount++
        }

        // 4. Clean up any leftover punctuation or extra spaces
        val cleanDest = text
            .replace(Regex("^[.,!?:;\\-\\s]+"), "")
            .replace(Regex("[.,!?:;\\-\\s]+$"), "")
            .replace(Regex("\\s{2,}"), " ")
            .trim()

        return VoiceBookingResult(
            rawTranscript = rawText,
            cleanDestination = cleanDest,
            vehicleType = detectedVehicle
        )
    }
}

class VoiceBookingHelper(private val context: Context) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _parsedResult = MutableStateFlow<VoiceBookingResult?>(null)
    val parsedResult: StateFlow<VoiceBookingResult?> = _parsedResult.asStateFlow()

    private val _audioVolume = MutableStateFlow(0f)
    val audioVolume: StateFlow<Float> = _audioVolume.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(language: String = "hi-IN") {
        mainHandler.post {
            try {
                stopListeningInternal()
                _errorMessage.value = null
                _liveTranscript.value = ""
                _parsedResult.value = null
                _audioVolume.value = 0f

                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    _errorMessage.value = "Voice recognition is not available on this device."
                    return@post
                }

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _isListening.value = true
                        }

                        override fun onBeginningOfSpeech() {
                            _isListening.value = true
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                            _audioVolume.value = normalized
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _isListening.value = false
                            _audioVolume.value = 0f
                        }

                        override fun onError(error: Int) {
                            _isListening.value = false
                            _audioVolume.value = 0f
                            val message = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "Couldn't understand. Please speak again clearly."
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap mic to try again."
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Check microphone."
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network issue. Please check your connection."
                                else -> "Tap mic to speak your destination."
                            }
                            Log.w("VoiceBookingHelper", "Speech error: $error -> $message")
                            _errorMessage.value = message
                        }

                        override fun onResults(results: Bundle?) {
                            _isListening.value = false
                            _audioVolume.value = 0f
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                val topMatch = matches[0]
                                _liveTranscript.value = topMatch
                                val parsed = VoiceIntentParser.parse(topMatch)
                                _parsedResult.value = parsed
                            } else {
                                _errorMessage.value = "Couldn't catch your words. Tap to retry."
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!partial.isNullOrEmpty()) {
                                val text = partial[0]
                                _liveTranscript.value = text
                                _parsedResult.value = VoiceIntentParser.parse(text)
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-IN", "en-US"))
                }

                speechRecognizer?.startListening(intent)
                _isListening.value = true
            } catch (e: Exception) {
                Log.e("VoiceBookingHelper", "Failed to start listening", e)
                _isListening.value = false
                _errorMessage.value = e.localizedMessage ?: "Failed to start microphone."
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            stopListeningInternal()
        }
    }

    private fun stopListeningInternal() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w("VoiceBookingHelper", "Error stopping recognizer", e)
        } finally {
            speechRecognizer = null
            _isListening.value = false
            _audioVolume.value = 0f
        }
    }

    fun destroy() {
        stopListening()
    }
}
