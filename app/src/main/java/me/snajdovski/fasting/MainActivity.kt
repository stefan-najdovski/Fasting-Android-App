package me.snajdovski.fasting

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity


/**
 * Issues:
 * The notification violates the battery optimization rules of Google Play Store
 *
 */

class MainActivity : AppCompatActivity() {
    private lateinit var fastingHoursEditText: EditText
    private lateinit var fastingMinutesEditText: EditText
    private lateinit var fastingSecondsEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fastingHoursEditText = findViewById(R.id.fasting_hours_edit_text)
        fastingMinutesEditText = findViewById(R.id.fasting_minutes_edit_text)
        fastingSecondsEditText = findViewById(R.id.fasting_seconds_edit_text)

        val scheduleNotificationButton: Button = findViewById(R.id.schedule_notification_button)
        scheduleNotificationButton.setOnClickListener { scheduleNotification() }

        // Check if this is the first launch of the app
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val isFirstLaunch = sharedPreferences.getBoolean("isFirstLaunch", true)

        if (isFirstLaunch) {
            // Show the battery optimization dialog
            showBatteryOptimizationDialog()

            val editor = sharedPreferences.edit()
            editor.putBoolean("isFirstLaunch", false)
            editor.apply()
        }
    }

    private fun scheduleNotification() {
        val fastingHours: Int = fastingHoursEditText.text.toString().toIntOrNull() ?: 0
        val fastingMinutes: Int = fastingMinutesEditText.text.toString().toIntOrNull() ?: 0
        val fastingSeconds: Int = fastingSecondsEditText.text.toString().toIntOrNull() ?: 0

        val totalDurationMillis =
            (fastingHours * 3600 + fastingMinutes * 60 + fastingSeconds) * 1000

        val notificationIntent = Intent(this, NotificationReceiver::class.java).apply {
            action = "me.snajdovski.fasting.NOTIFICATION"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = System.currentTimeMillis() + totalDurationMillis

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )

        Toast.makeText(
            this,
            "Notification scheduled for $fastingHours hours, $fastingMinutes minutes, and $fastingSeconds seconds of fasting",
            Toast.LENGTH_SHORT
        ).show()
    }

    @SuppressLint("BatteryLife")
    // This needs to change
    private fun showBatteryOptimizationDialog() {
        // Check if this is the first launch
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val isFirstLaunch = sharedPreferences.getBoolean("isFirstLaunch", true)

        if (isFirstLaunch) {
            AlertDialog.Builder(this)
                .setTitle("Battery Optimization")
                .setMessage("This app requires battery optimization to be disabled in order to work properly. Please disable battery optimization for this app.")
                .setPositiveButton("OK") { _, _ ->
                    //play store issue
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = Uri.parse("package:$packageName")
                    try {
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(
                            this,
                            "battery_optimization_error",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                .show()
            val editor = sharedPreferences.edit()
            editor.putBoolean("isFirstLaunch", false)
            editor.apply()
        }
    }
}
