package com.toolnexa.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class AudioMidiPianoRollView(
    context: Context,
    private var durationSeconds: Double,
    notes: List<Note>,
    private val onChanged: (List<Note>) -> Unit
) : View(context) {

    data class Note(
        var startSeconds: Double,
        var durationSeconds: Double,
        var pitch: Int,
        var velocity: Int,
        var pitchBends: List<Int> = emptyList(),
        var channel: Int = 0
    )

    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG)

    private val gridPaint =
        Paint(Paint.ANTI_ALIAS_FLAG)

    private val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG)

    private val noteRect =
        RectF()

    private val mutableNotes =
        notes.map { it.copy() }.toMutableList()

    private var selectedIndex = -1
    private var dragMode = DragMode.NONE
    private var downX = 0f
    private var downY = 0f
    private var initialStart = 0.0
    private var initialDuration = 0.1
    private var initialPitch = 60

    private val minPitch = 24
    private val maxPitch = 108
    private val pixelsPerSecond = 72f

    private enum class DragMode {
        NONE,
        MOVE,
        RESIZE
    }

    init {
        isFocusable = true
        setBackgroundColor(
            0xFFF7F9FD.toInt()
        )
    }

    fun notes(): List<Note> {
        return mutableNotes.map {
            it.copy()
        }
    }

    fun selectedNote(): Note? {
        return mutableNotes.getOrNull(
            selectedIndex
        )?.copy()
    }

    fun selectIndex(index: Int) {
        selectedIndex =
            index.coerceIn(
                -1,
                mutableNotes.lastIndex
            )
        invalidate()
    }

    fun addNote(
        startSeconds: Double,
        pitch: Int,
        durationSeconds: Double,
        velocity: Int = 96
    ) {
        mutableNotes.add(
            Note(
                startSeconds =
                    startSeconds.coerceIn(
                        0.0,
                        max(
                            0.0,
                            durationSecondsValue() -
                                0.04
                        )
                    ),
                durationSeconds =
                    durationSeconds.coerceIn(
                        0.04,
                        max(
                            0.04,
                            durationSecondsValue()
                        )
                    ),
                pitch =
                    pitch.coerceIn(
                        minPitch,
                        maxPitch
                    ),
                velocity =
                    velocity.coerceIn(
                        1,
                        127
                    )
            )
        )

        selectedIndex =
            mutableNotes.lastIndex

        normalize()
        changed()
    }

    fun deleteSelected() {
        if (
            selectedIndex !in
                mutableNotes.indices
        ) {
            return
        }

        mutableNotes.removeAt(
            selectedIndex
        )

        selectedIndex =
            min(
                selectedIndex,
                mutableNotes.lastIndex
            )

        changed()
    }

    fun duplicateSelected() {
        val selected =
            mutableNotes.getOrNull(
                selectedIndex
            ) ?: return

        val copy =
            selected.copy(
                startSeconds =
                    (
                        selected.startSeconds +
                            selected.durationSeconds
                        ).coerceAtMost(
                            max(
                                0.0,
                                durationSecondsValue() -
                                    selected.durationSeconds
                            )
                        )
            )

        mutableNotes.add(copy)
        selectedIndex =
            mutableNotes.lastIndex
        normalize()
        changed()
    }

    fun setDuration(
        duration: Double
    ) {
        durationSeconds =
            duration.coerceAtLeast(
                0.1
            )

        normalize()
        invalidate()
    }

    override fun onDraw(
        canvas: Canvas
    ) {
        super.onDraw(canvas)

        val width =
            width.toFloat()

        val height =
            height.toFloat()

        drawGrid(
            canvas,
            width,
            height
        )

        drawKeyboard(
            canvas,
            height
        )

        val contentLeft =
            keyboardWidth()

        mutableNotes.forEachIndexed { index, note ->
            val rect =
                noteRectFor(
                    note,
                    height
                )

            paint.color =
                if (
                    index == selectedIndex
                ) {
                    0xFF6A5AE0.toInt()
                } else {
                    if (
                        note.pitch % 12 in
                            listOf(
                                1,
                                3,
                                6,
                                8,
                                10
                            )
                    ) {
                        0xFF58C7D8.toInt()
                    } else {
                        0xFF7D6BEF.toInt()
                    }
                }

            canvas.drawRoundRect(
                rect,
                dp(5).toFloat(),
                dp(5).toFloat(),
                paint
            )

            if (
                rect.width() >
                    dp(28)
            ) {
                textPaint.color =
                    0xFFFFFFFF.toInt()
                textPaint.textSize =
                    dp(10).toFloat()
                canvas.drawText(
                    noteName(note.pitch),
                    rect.left + dp(5),
                    rect.centerY() +
                        dp(4),
                    textPaint
                )
            }

            if (
                index == selectedIndex
            ) {
                paint.color =
                    0xFFFFFFFF.toInt()
                canvas.drawCircle(
                    rect.right -
                        dp(5),
                    rect.centerY(),
                    dp(3).toFloat(),
                    paint
                )
            }
        }

        val selected =
            selectedNote()

        if (
            selected != null
        ) {
            paint.color =
                0xFF1C173B.toInt()
            paint.strokeWidth =
                dp(1).toFloat()

            canvas.drawLine(
                contentLeft.toFloat(),
                0f,
                contentLeft.toFloat(),
                height,
                paint
            )
        }
    }

    override fun onTouchEvent(
        event: MotionEvent
    ): Boolean {
        val contentX =
            event.x -
                keyboardWidth()

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX =
                    contentX
                downY =
                    event.y

                val hit =
                    findNote(
                        contentX,
                        event.y
                    )

                selectedIndex =
                    hit

                if (
                    hit >= 0
                ) {
                    val note =
                        mutableNotes[hit]

                    initialStart =
                        note.startSeconds

                    initialDuration =
                        note.durationSeconds

                    initialPitch =
                        note.pitch

                    val edge =
                        (
                            note.startSeconds +
                                note.durationSeconds
                            ) *
                            pixelsPerSecond

                    dragMode =
                        if (
                            contentX >
                                edge -
                                    dp(12)
                        ) {
                            DragMode.RESIZE
                        } else {
                            DragMode.MOVE
                        }
                } else {
                    dragMode =
                        DragMode.NONE
                }

                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (
                    selectedIndex !in
                        mutableNotes.indices
                ) {
                    return true
                }

                val note =
                    mutableNotes[
                        selectedIndex
                    ]

                val deltaTime =
                    (
                        contentX -
                            downX
                        ) /
                        pixelsPerSecond

                val rowHeight =
                    rowHeight()

                val deltaPitch =
                    -(
                        (
                            event.y -
                                downY
                            ) /
                            rowHeight
                        ).roundToInt()

                when (
                    dragMode
                ) {
                    DragMode.MOVE -> {
                        note.startSeconds =
                            (
                                initialStart +
                                    deltaTime
                                ).coerceIn(
                                    0.0,
                                    max(
                                        0.0,
                                        durationSecondsValue() -
                                            note.durationSeconds
                                    )
                                )

                        note.pitch =
                            (
                                initialPitch +
                                    deltaPitch
                                ).coerceIn(
                                    minPitch,
                                    maxPitch
                                )
                    }

                    DragMode.RESIZE -> {
                        note.durationSeconds =
                            (
                                initialDuration +
                                    deltaTime
                                ).coerceIn(
                                    0.04,
                                    max(
                                        0.04,
                                        durationSecondsValue() -
                                            note.startSeconds
                                    )
                            )
                    }

                    DragMode.NONE -> Unit
                }

                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (
                    dragMode !=
                        DragMode.NONE
                ) {
                    normalize()
                    changed()
                }

                dragMode =
                    DragMode.NONE

                performClick()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                dragMode =
                    DragMode.NONE
                return true
            }
        }

        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun drawGrid(
        canvas: Canvas,
        width: Float,
        height: Float
    ) {
        canvas.drawColor(
            0xFFF7F9FD.toInt()
        )

        val contentLeft =
            keyboardWidth()

        gridPaint.color =
            0xFFDCE4F1.toInt()
        gridPaint.strokeWidth =
            dp(1).toFloat()

        var second = 0
        while (
            second.toDouble() <=
                durationSecondsValue() + 0.1
        ) {
            val x =
                contentLeft +
                    second *
                    pixelsPerSecond

            canvas.drawLine(
                x,
                0f,
                x,
                height,
                gridPaint
            )

            second +=
                if (second % 4 == 0) {
                    4
                } else {
                    1
                }
        }

        for (
            pitch
            in minPitch..maxPitch
        ) {
            val y =
                pitchToY(
                    pitch,
                    height
                )

            val black =
                pitch % 12 in
                    listOf(
                        1,
                        3,
                        6,
                        8,
                        10
                    )

            gridPaint.color =
                if (black) {
                    0xFFD6DEEB.toInt()
                } else {
                    0xFFE8EDF5.toInt()
                }

            canvas.drawLine(
                contentLeft,
                y,
                width,
                y,
                gridPaint
            )
        }

        textPaint.color =
            0xFF607089.toInt()
        textPaint.textSize =
            dp(10).toFloat()

        var labelSecond = 0
        while (
            labelSecond.toDouble() <=
                durationSecondsValue()
        ) {
            val x =
                contentLeft +
                    labelSecond *
                    pixelsPerSecond +
                    dp(4)

            canvas.drawText(
                formatTime(
                    labelSecond
                ),
                x,
                dp(13).toFloat(),
                textPaint
            )

            labelSecond += 4
        }
    }

    private fun drawKeyboard(
        canvas: Canvas,
        height: Float
    ) {
        val keyWidth =
            keyboardWidth()

        for (
            pitch
            in minPitch..maxPitch
        ) {
            val top =
                pitchToY(
                    pitch,
                    height
                )

            val bottom =
                top +
                    rowHeight()

            val black =
                pitch % 12 in
                    listOf(
                        1,
                        3,
                        6,
                        8,
                        10
                    )

            paint.color =
                if (black) {
                    0xFFCBD5E3.toInt()
                } else {
                    0xFFFFFFFF.toInt()
                }

            canvas.drawRect(
                0f,
                top,
                keyWidth,
                bottom,
                paint
            )

            if (
                pitch % 12 == 0
            ) {
                textPaint.color =
                    0xFF46566D.toInt()
                textPaint.textSize =
                    dp(9).toFloat()

                canvas.drawText(
                    noteName(pitch),
                    dp(4).toFloat(),
                    top +
                        rowHeight() -
                        dp(3),
                    textPaint
                )
            }
        }

        gridPaint.color =
            0xFFBAC6D7.toInt()

        canvas.drawLine(
            keyWidth,
            0f,
            keyWidth,
            height,
            gridPaint
        )
    }

    private fun noteRectFor(
        note: Note,
        height: Float
    ): RectF {
        val x =
            keyboardWidth() +
                (
                    note.startSeconds.coerceAtLeast(
                        0.0
                    ).toFloat() *
                        pixelsPerSecond
                    )

        val width =
            max(
                dp(9).toFloat(),
                note.durationSeconds.toFloat() *
                    pixelsPerSecond
            )

        val y =
            pitchToY(
                note.pitch,
                height
            ) +
                dp(1)

        return RectF(
            x,
            y,
            x + width,
            y +
                max(
                    dp(10).toFloat(),
                    rowHeight() -
                        dp(2)
                )
        )
    }

    private fun findNote(
        x: Float,
        y: Float
    ): Int {
        for (
            index
            in mutableNotes.indices.reversed()
        ) {
            if (
                noteRectFor(
                    mutableNotes[index],
                    height.toFloat()
                ).contains(
                    x +
                        keyboardWidth(),
                    y
                )
            ) {
                return index
            }
        }

        return -1
    }

    private fun pitchToY(
        pitch: Int,
        height: Float
    ): Float {
        return (
            maxPitch -
                pitch
            ) *
            rowHeight()
    }

    private fun rowHeight(): Float {
        return height.toFloat() /
            (
                maxPitch -
                    minPitch +
                    1
                ).toFloat()
    }

    private fun keyboardWidth(): Float {
        return dp(46).toFloat()
    }

    private fun durationSecondsValue():
        Double {
        return durationSeconds.coerceAtLeast(
            1.0
        )
    }

    private fun normalize() {
        mutableNotes.forEach { note ->
            note.startSeconds =
                note.startSeconds.coerceIn(
                    0.0,
                    durationSecondsValue()
                )

            note.durationSeconds =
                note.durationSeconds.coerceIn(
                    0.04,
                    max(
                        0.04,
                        durationSecondsValue() -
                            note.startSeconds
                    )
                )

            note.pitch =
                note.pitch.coerceIn(
                    minPitch,
                    maxPitch
                )

            note.velocity =
                note.velocity.coerceIn(
                    1,
                    127
                )
        }

        mutableNotes.sortBy {
            it.startSeconds
        }
    }

    private fun changed() {
        onChanged(
            notes()
        )
        invalidate()
    }

    private fun noteName(
        pitch: Int
    ): String {
        val names =
            arrayOf(
                "C",
                "C#",
                "D",
                "D#",
                "E",
                "F",
                "F#",
                "G",
                "G#",
                "A",
                "A#",
                "B"
            )

        return names[
            pitch.coerceIn(
                0,
                127
            ) % 12
        ] +
            (
                pitch / 12 -
                    1
                )
    }

    private fun formatTime(
        seconds: Int
    ): String {
        val minutes =
            seconds / 60

        val remainder =
            seconds % 60

        return String.format(
            "%d:%02d",
            minutes,
            remainder
        )
    }

    private fun dp(
        value: Int
    ): Int {
        return (
            value *
                resources.displayMetrics.density
            ).roundToInt()
    }
}
