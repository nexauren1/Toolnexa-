package com.toolnexa.app

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.InputStream
import org.json.JSONArray
import org.json.JSONObject

class BasicPitchWebEngine(
    private val activity: Activity,
    private val audioUri: Uri,
    private val minMidi: Int,
    private val maxMidi: Int,
    private val onsetThreshold: Float,
    private val frameThreshold: Float,
    private val minNoteLengthFrames: Int,
    private val onProgress: (Int, String) -> Unit,
    private val onSuccess: (List<NeuralNote>) -> Unit,
    private val onError: (String) -> Unit
) {

    data class NeuralNote(
        val startSeconds: Double,
        val durationSeconds: Double,
        val pitch: Int,
        val amplitude: Double,
        val pitchBends: List<Int>
    )

    private val mainHandler =
        Handler(Looper.getMainLooper())

    private var webView: WebView? = null
    private var destroyed = false
    private var started = false

    private val audioUrl =
        "https://toolnexa.local/audio/current"

    fun start() {
        activity.runOnUiThread {
            if (destroyed) {
                return@runOnUiThread
            }

            val view =
                WebView(activity)

            webView =
                view

            view.setBackgroundColor(
                android.graphics.Color.TRANSPARENT
            )

            view.settings.javaScriptEnabled = true
            view.settings.domStorageEnabled = true
            view.settings.allowFileAccess = false
            view.settings.allowContentAccess = false
            view.settings.mediaPlaybackRequiresUserGesture = false
            view.settings.javaScriptCanOpenWindowsAutomatically = false

            view.webViewClient =
                object : WebViewClient() {

                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: android.webkit.WebResourceRequest
                    ): WebResourceResponse? {
                        if (
                            request.url.toString() ==
                                audioUrl
                        ) {
                            return openAudioResponse()
                        }

                        return super.shouldInterceptRequest(
                            view,
                            request
                        )
                    }

                    override fun onPageFinished(
                        view: WebView,
                        url: String
                    ) {
                        super.onPageFinished(
                            view,
                            url
                        )

                        if (!started) {
                            started = true
                            startPolling()
                        }
                    }

                    override fun onReceivedError(
                        view: WebView,
                        request: android.webkit.WebResourceRequest,
                        error: android.webkit.WebResourceError
                    ) {
                        if (request.isForMainFrame) {
                            fail(
                                "Não foi possível iniciar o motor neural."
                            )
                        }

                        super.onReceivedError(
                            view,
                            request,
                            error
                        )
                    }
                }

            val params =
                ViewGroup.LayoutParams(
                    2,
                    2
                )

            activity.addContentView(
                view,
                params
            )

            val configuredHtml =
                HTML
                    .replace(
                        "ONSET_THRESHOLD_TOKEN",
                        onsetThreshold.toString()
                    )
                    .replace(
                        "FRAME_THRESHOLD_TOKEN",
                        frameThreshold.toString()
                    )
                    .replace(
                        "MIN_NOTE_LENGTH_TOKEN",
                        minNoteLengthFrames.toString()
                    )
                    .replace(
                        "MAX_MIDI_TOKEN",
                        maxMidi.toString()
                    )
                    .replace(
                        "MIN_MIDI_TOKEN",
                        minMidi.toString()
                    )

            view.loadDataWithBaseURL(
                "https://toolnexa.local/",
                configuredHtml,
                "text/html",
                "UTF-8",
                null
            )
        }
    }

    fun close() {
        destroyed = true
        mainHandler.removeCallbacksAndMessages(null)

        activity.runOnUiThread {
            webView?.stopLoading()
            webView?.loadUrl("about:blank")
            webView?.destroy()
            webView = null
        }
    }

    private fun startPolling() {
        pollState()

        mainHandler.postDelayed(
            object : Runnable {
                override fun run() {
                    if (
                        destroyed ||
                        webView == null
                    ) {
                        return
                    }

                    pollState()
                    mainHandler.postDelayed(
                        this,
                        180L
                    )
                }
            },
            180L
        )
    }

    private fun pollState() {
        val view =
            webView ?: return

        view.evaluateJavascript(
            "window.toolNexaStateJson()",
            null
        )

        view.evaluateJavascript(
            "JSON.stringify(window.toolNexaState || {status:'booting',progress:0,message:'A preparar o motor neural...'})"
        ) { raw ->
            if (
                destroyed ||
                raw == null ||
                raw == "null"
            ) {
                return@evaluateJavascript
            }

            val jsonText =
                decodeJavascriptString(raw)

            try {
                val state =
                    JSONObject(jsonText)

                val progress =
                    state.optInt(
                        "progress",
                        0
                    ).coerceIn(
                        0,
                        100
                    )

                val message =
                    state.optString(
                        "message",
                        "A processar..."
                    )

                activity.runOnUiThread {
                    if (!destroyed) {
                        onProgress(
                            progress,
                            message
                        )
                    }
                }

                when (
                    state.optString(
                        "status"
                    )
                ) {
                    "complete" -> {
                        handleComplete(
                            state
                        )
                    }

                    "error" -> {
                        fail(
                            state.optString(
                                "error",
                                "O motor neural não conseguiu processar o áudio."
                            )
                        )
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun handleComplete(
        state: JSONObject
    ) {
        try {
            val notesJson =
                state.optJSONArray(
                    "notes"
                )
                    ?: throw IllegalStateException(
                        "O motor neural não devolveu notas."
                    )

            val notes =
                mutableListOf<NeuralNote>()

            for (
                index in
                0 until notesJson.length()
            ) {
                val note =
                    notesJson.getJSONObject(
                        index
                    )

                val pitch =
                    note.optInt(
                        "pitchMidi",
                        -1
                    )

                if (
                    pitch < 0 ||
                    pitch > 127
                ) {
                    continue
                }

                val bends =
                    mutableListOf<Int>()

                val bendArray =
                    note.optJSONArray(
                        "pitchBends"
                    )

                if (bendArray != null) {
                    for (
                        bendIndex in
                        0 until bendArray.length()
                    ) {
                        bends.add(
                            bendArray.optInt(
                                bendIndex,
                                0
                            )
                        )
                    }
                }

                notes.add(
                    NeuralNote(
                        startSeconds =
                            note.optDouble(
                                "startTimeSeconds",
                                0.0
                            ),
                        durationSeconds =
                            note.optDouble(
                                "durationSeconds",
                                0.0
                            ),
                        pitch =
                            pitch.coerceIn(
                                minMidi,
                                maxMidi
                            ),
                        amplitude =
                            note.optDouble(
                                "amplitude",
                                0.5
                            ).coerceIn(
                                0.0,
                                1.0
                            ),
                        pitchBends =
                            bends
                    )
                )
            }

            if (notes.isEmpty()) {
                throw IllegalStateException(
                    "A IA terminou, mas não encontrou notas claras."
                )
            }

            destroyed = true

            activity.runOnUiThread {
                webView?.stopLoading()
                webView?.destroy()
                webView = null
                onSuccess(notes)
            }
        } catch (error: Exception) {
            fail(
                error.message
                    ?: "Não foi possível ler o resultado neural."
            )
        }
    }

    private fun fail(
        message: String
    ) {
        if (destroyed) {
            return
        }

        destroyed = true
        mainHandler.removeCallbacksAndMessages(
            null
        )

        activity.runOnUiThread {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
            onError(message)
        }
    }

    private fun openAudioResponse():
        WebResourceResponse? {
        return try {
            val input =
                activity.contentResolver
                    .openInputStream(audioUri)
                    ?: return null

            WebResourceResponse(
                "application/octet-stream",
                "binary",
                input
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeJavascriptString(
        raw: String
    ): String {
        return try {
            JSONObject(
                "{\"value\":" +
                    raw +
                    "}"
            ).getString(
                "value"
            )
        } catch (_: Exception) {
            raw.trim(
                '"'
            )
        }
    }

    private companion object {
        const val HTML = """
<!doctype html>
<html>
<head>
<meta charset="utf-8">
<meta
    name="viewport"
    content="width=device-width,
             initial-scale=1,
             maximum-scale=1,
             user-scalable=no"
>
</head>
<body>
<script type="module">
const MODEL_URL =
  "https://cdn.jsdelivr.net/npm/" +
  "@spotify/basic-pitch@1.0.1/model/model.json";

const state = {
  status: "booting",
  progress: 0,
  message: "A preparar o motor neural...",
  notes: [],
  error: ""
};

window.toolNexaState = state;

window.toolNexaStateJson = () => {
  return JSON.stringify(state);
};

function setState(
  status,
  progress,
  message,
  error = ""
) {
  state.status = status;
  state.progress =
    Math.max(
      0,
      Math.min(
        100,
        Math.round(progress)
      )
    );
  state.message = message;
  state.error = error;
}

function midiToHz(
  midi
) {
  return 440 *
    Math.pow(
      2,
      (midi - 69) / 12
    );
}

function downmixAndResample(
  input
) {
  const sourceRate =
    input.sampleRate;

  const channels =
    input.numberOfChannels;

  const targetRate =
    22050;

  const sourceLength =
    input.length;

  const outputLength =
    Math.max(
      1,
      Math.round(
        sourceLength *
          targetRate /
          sourceRate
      )
    );

  const data =
    new Float32Array(
      outputLength
    );

  const sourceChannels =
    [];

  for (
    let c = 0;
    c < channels;
    c++
  ) {
    sourceChannels.push(
      input.getChannelData(c)
    );
  }

  const ratio =
    sourceRate /
    targetRate;

  for (
    let i = 0;
    i < outputLength;
    i++
  ) {
    const position =
      i * ratio;

    const left =
      Math.floor(
        position
      );

    const right =
      Math.min(
        sourceLength - 1,
        left + 1
      );

    const fraction =
      position -
      left;

    let sum = 0;

    for (
      let c = 0;
      c < channels;
      c++
    ) {
      const source =
        sourceChannels[c];

      const a =
        source[
          Math.min(
            left,
            source.length - 1
          )
        ];

      const b =
        source[right];

      sum +=
        a +
        (b - a) *
          fraction;
    }

    data[i] =
      sum /
      Math.max(
        1,
        channels
      );
  }

  return data;
}

async function readAudio() {
  const response =
    await fetch(
      "https://toolnexa.local/audio/current"
    );

  if (!response.ok) {
    throw new Error(
      "Não foi possível ler o áudio."
    );
  }

  const bytes =
    await response.arrayBuffer();

  const context =
    new AudioContext();

  try {
    const decoded =
      await context.decodeAudioData(
        bytes.slice(0)
      );

    return downmixAndResample(
      decoded
    );
  } finally {
    try {
      await context.close();
    } catch (_) {
    }
  }
}

async function run() {
  try {
    setState(
      "loading",
      2,
      "A carregar o modelo neural..."
    );

    const module =
      await import(
        "https://esm.sh/" +
        "@spotify/basic-pitch@1.0.1" +
        "?deps=@tensorflow/tfjs@3.2.0"
      );

    const {
      BasicPitch,
      outputToNotesPoly,
      addPitchBendsToNoteEvents,
      noteFramesToTime
    } = module;

    setState(
      "loading",
      8,
      "Modelo neural carregado. A preparar o áudio..."
    );

    const audio =
      await readAudio();

    setState(
      "loading",
      14,
      "Áudio preparado a 22,05 kHz..."
    );

    const basicPitch =
      new BasicPitch(
        MODEL_URL
      );

    const frames = [];
    const onsets = [];
    const contours = [];

    await basicPitch.evaluateModel(
      audio,
      (
        frameData,
        onsetData,
        contourData
      ) => {
        frames.push(
          ...frameData
        );

        onsets.push(
          ...onsetData
        );

        contours.push(
          ...contourData
        );
      },
      (percent) => {
        setState(
          "transcribing",
          14 +
            percent * 66,
          "A transcrever notas com IA..."
        );
      }
    );

    setState(
      "decoding",
      84,
      "A reconstruir acordes, notas e ataques..."
    );

    const notes =
      outputToNotesPoly(
        frames,
        onsets,
         ,
         ,
         ,
        true,
        midiToHz( ),
        midiToHz( ),
        true,
        11
      );

    const withBends =
      addPitchBendsToNoteEvents(
        contours,
        notes
      );

    const timed =
      noteFramesToTime(
        withBends
      );

    const prepared =
      timed
        .filter(
          (note) =>
            note.pitchMidi >=
              __MIN_MIDI__ &&
            note.pitchMidi <=
              __MAX_MIDI__ &&
            note.durationSeconds >
              0.03
        )
        .map(
          (note) => ({
            startTimeSeconds:
              note.startTimeSeconds,
            durationSeconds:
              note.durationSeconds,
            pitchMidi:
              note.pitchMidi,
            amplitude:
              note.amplitude,
            pitchBends:
              Array.isArray(
                note.pitchBends
              )
                ? note.pitchBends
                : []
          })
        );

    if (!prepared.length) {
      throw new Error(
        "A IA não encontrou notas musicais claras. Tente outro perfil ou uma gravação mais limpa."
      );
    }

    setState(
      "complete",
      100,
      "Transcrição neural concluída."
    );

    state.notes =
      prepared;
  } catch (error) {
    setState(
      "error",
      state.progress,
      "O motor neural encontrou um problema.",
      error && error.message
        ? error.message
        : "Falha desconhecida no motor neural."
    );
  }
}

run();
</script>
</body>
</html>
"""
    }
}
