package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat

class NoKeyboardService : InputMethodService() {

    private lateinit var preferences: KeyboardPreferences
    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onCreate() {
        super.onCreate()
        preferences = KeyboardPreferences(this)
        createNotificationChannel()
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        // Never go into full-screen extract mode
        return false
    }

    override fun onEvaluateInputViewShown(): Boolean {
        super.onEvaluateInputViewShown()
        // If mode is COMPLETELY_HIDDEN, return false so Android doesn't show any window
        return preferences.mode != KeyboardMode.COMPLETELY_HIDDEN
    }

    override fun onCreateInputView(): View {
        return when (preferences.mode) {
            KeyboardMode.COMPLETELY_HIDDEN -> createInvisibleView()
            KeyboardMode.TRANSPARENT_BAR -> createTransparentBarView()
            KeyboardMode.FLOATING_BUTTON -> createFloatingButtonView()
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // Ensure extract and candidates are disabled so no extra UI appears
        setExtractViewShown(false)
        setCandidatesViewShown(false)

        if (preferences.showNotification) {
            showActiveNotification()
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        hideActiveNotification()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideActiveNotification()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (preferences.hardwarePassthrough && event != null) {
            val ic = currentInputConnection
            if (ic != null && keyCode != KeyEvent.KEYCODE_BACK) {
                ic.sendKeyEvent(event)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (preferences.hardwarePassthrough && event != null) {
            val ic = currentInputConnection
            if (ic != null && keyCode != KeyEvent.KEYCODE_BACK) {
                ic.sendKeyEvent(event)
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun createInvisibleView(): View {
        return View(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0
            )
            setBackgroundColor(Color.TRANSPARENT)
            isClickable = false
            isFocusable = false
        }
    }

    private fun createTransparentBarView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(52)
            )
            val bg = GradientDrawable().apply {
                setColor(Color.argb(200, 24, 28, 36)) // 78% opacity dark translucent
                cornerRadius = 0f
            }
            background = bg
            setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
        }

        // Label
        val label = TextView(this).apply {
            text = "⌨️ Hideable"
            setTextColor(Color.argb(220, 255, 255, 255))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        root.addView(label)

        // Switch button
        val switchBtn = createStyledButton("Сменить", Color.argb(180, 59, 130, 246)) {
            triggerSwitchKeyboard()
        }
        root.addView(switchBtn)

        val space0 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(6), ViewGroup.LayoutParams.MATCH_PARENT)
        }
        root.addView(space0)

        // Space key button
        val spaceBtn = createStyledButton("␣", Color.argb(140, 75, 85, 99)) {
            val ic = currentInputConnection
            ic?.commitText(" ", 1)
        }
        root.addView(spaceBtn)

        val space1 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(6), ViewGroup.LayoutParams.MATCH_PARENT)
        }
        root.addView(space1)

        // Backspace key button
        val backspaceBtn = createStyledButton("⌫", Color.argb(140, 75, 85, 99)) {
            val ic = currentInputConnection
            if (ic?.deleteSurroundingText(1, 0) == false) {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
            }
        }
        root.addView(backspaceBtn)

        val space2 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(6), ViewGroup.LayoutParams.MATCH_PARENT)
        }
        root.addView(space2)

        // Enter action button
        val enterBtn = createStyledButton("↵", Color.argb(140, 75, 85, 99)) {
            val ic = currentInputConnection
            val handled = ic?.performEditorAction(EditorInfo.IME_ACTION_DONE) ?: false
            if (!handled) {
                ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
        }
        root.addView(enterBtn)

        val space3 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(6), ViewGroup.LayoutParams.MATCH_PARENT)
        }
        root.addView(space3)

        // Hide button
        val hideBtn = createStyledButton("⌄", Color.argb(120, 55, 65, 81)) {
            requestHideSelf(0)
        }
        root.addView(hideBtn)

        return root
    }

    private fun createFloatingButtonView(): View {
        val root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(60)
            )
            setBackgroundColor(Color.TRANSPARENT)
        }

        val pill = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val lp = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dpToPx(44)
            ).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.END
                marginEnd = dpToPx(16)
            }
            layoutParams = lp
            val bg = GradientDrawable().apply {
                setColor(Color.argb(210, 30, 41, 59))
                cornerRadius = dpToPx(22).toFloat()
                setStroke(dpToPx(1), Color.argb(100, 255, 255, 255))
            }
            background = bg
            setPadding(dpToPx(14), 0, dpToPx(14), 0)
        }

        val switchBtn = TextView(this).apply {
            text = "⌨️ Сменить клавиатуру"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setOnClickListener { triggerSwitchKeyboard() }
        }
        pill.addView(switchBtn)

        val closeBtn = TextView(this).apply {
            text = "  ✕"
            setTextColor(Color.argb(180, 255, 255, 255))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setOnClickListener { requestHideSelf(0) }
        }
        pill.addView(closeBtn)

        root.addView(pill)
        return root
    }

    private fun createStyledButton(text: String, bgColor: Int, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            val drawable = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = dpToPx(8).toFloat()
            }
            background = drawable
            minHeight = 0
            minimumHeight = 0
            minWidth = 0
            minimumWidth = 0
            setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
            setOnClickListener { onClick() }
        }
    }

    private fun triggerSwitchKeyboard() {
        val switched = try {
            switchToNextInputMethod(false)
        } catch (_: Exception) {
            false
        }

        if (!switched) {
            try {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showInputMethodPicker()
            } catch (_: Exception) {
                val intent = Intent(this, SwitchKeyboardActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Hideable Keyboard Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Показывает статус скрытой клавиатуры и кнопку быстрой смены"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showActiveNotification() {
        try {
            val switchIntent = Intent(this, SwitchKeyboardActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                switchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_input_get)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_content))
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setAutoCancel(false)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) {
            // Gracefully ignore if permissions not granted
        }
    }

    private fun hideActiveNotification() {
        try {
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (_: Exception) {
            // Ignore
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val CHANNEL_ID = "hideable_keyboard_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
