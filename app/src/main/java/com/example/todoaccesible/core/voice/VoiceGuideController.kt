package com.example.todoaccesible.core.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * Guía de voz accesible, usando el `TextToSpeech` del propio SDK de Android
 * (no requiere ninguna dependencia nueva). Equivalente a
 * `VoiceGuideContext.jsx` en la web (que usa `SpeechSynthesisUtterance` del
 * navegador): [text] es el texto vigente para la pantalla actual (fijado
 * por ruta desde `TodoAccesibleNavGraph`, o sobreescrito por una pantalla
 * con pasos internos vía [setInstructions]), y [toggle] alterna entre leer
 * y detener. [supported] refleja si el motor TTS pudo inicializarse con
 * español (el botón flotante se oculta si no).
 */
class VoiceGuideController(context: Context) {

    companion object {
        private const val UTTERANCE_ID = "todo_accesible_voice_guide"
    }

    private var tts: TextToSpeech? = null

    private val _supported = MutableStateFlow(false)
    val supported: StateFlow<Boolean> = _supported

    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val languageResult = tts?.setLanguage(Locale("es", "MX"))
                _supported.value = languageResult != TextToSpeech.LANG_MISSING_DATA &&
                    languageResult != TextToSpeech.LANG_NOT_SUPPORTED
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { _speaking.value = true }
                    override fun onDone(utteranceId: String?) { _speaking.value = false }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { _speaking.value = false }
                })
            } else {
                _supported.value = false
            }
        }
    }

    /** Fija el texto que se leerá al presionar el botón (no lo lee todavía). */
    fun setInstructions(texto: String) {
        _text.value = texto
    }

    fun speak() {
        val engine = tts ?: return
        if (!_supported.value || _text.value.isBlank()) return
        engine.stop()
        _speaking.value = true
        engine.speak(_text.value, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    fun stop() {
        tts?.stop()
        _speaking.value = false
    }

    fun toggle() {
        if (_speaking.value) stop() else speak()
    }
}
