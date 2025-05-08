package com.example.myapplication

import android.app.Activity
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var buttonSelectApp: Button
    private lateinit var inputMinutes: EditText
    private lateinit var selectedAppTextView: TextView

    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    private var selectedApp: String? = null
    private lateinit var inputSeconds: EditText
    private lateinit var buttonStart: Button
    private lateinit var textView: TextView
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)
        buttonStart = findViewById(R.id.buttonStart)
        buttonSelectApp = findViewById(R.id.buttonSelectApp)
        inputMinutes = findViewById(R.id.inputMinutes)
        selectedAppTextView = findViewById(R.id.selectedAppTextView)

        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, MyDeviceAdminReceiver::class.java)

        requestUsageAccessPermission()

        buttonSelectApp.setOnClickListener {
            showAppSelectionDialog()
        }

        buttonStart.setOnClickListener {
            startTimer()
        }
    }

    private fun hasUsageAccessPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageAccessPermission() {
        if (!hasUsageAccessPermission()) {
            Toast.makeText(this, "Необходимо разрешение на доступ к статистике", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    private fun getInstalledApps(): List<String> {
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return apps.filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .map { it.packageName }
    }

    private fun showAppSelectionDialog() {
        val packageManager = packageManager
        val installedApps = packageManager.getInstalledApplications(0)

        val appNames = installedApps.map { packageManager.getApplicationLabel(it).toString() }
        val appPackages = installedApps.map { it.packageName }

        AlertDialog.Builder(this)
            .setTitle("Выберите приложение")
            .setItems(appNames.toTypedArray()) { _, which ->
                selectedApp = appPackages[which]
                selectedAppTextView.text = "Выбрано: ${appNames[which]}"
                Toast.makeText(this, "Выбрано: ${appNames[which]}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun getAppUsageTime(packageName: String): Long {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 * 60 // последние 60 минут

        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        val appStats: UsageStats? = stats.find { it.packageName == packageName }
        return appStats?.totalTimeInForeground ?: 0
    }

    private fun startTimer() {
        val input = inputMinutes.text.toString().toDoubleOrNull() ?: 0.0
        val totalMillis = (input * 60 * 1000).toLong()

        if (selectedApp == null) {
            Toast.makeText(this, "Выберите приложение!", Toast.LENGTH_SHORT).show()
            return
        }

        if (totalMillis <= 0) {
            Toast.makeText(this, "Введите корректное время!", Toast.LENGTH_SHORT).show()
            return
        }

        countDownTimer?.cancel()

        // Сохраняем выбранное приложение в SharedPreferences
        val prefs = getSharedPreferences("app_blocker", MODE_PRIVATE)
        prefs.edit().putString("blocked_app", selectedApp).apply()

        // Запоминаем текущее время использования приложения
        val startUsageTime = getAppUsageTime(selectedApp!!)

        countDownTimer = object : CountDownTimer(totalMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val currentUsage = getAppUsageTime(selectedApp!!)
                val elapsedUsage = currentUsage - startUsageTime
                val remainingTime = totalMillis - elapsedUsage

                val m = remainingTime / 60000
                val s = (remainingTime % 60000) / 1000
                textView.text = String.format("%02d:%02d", m, s)

                if (remainingTime <= 0) {
                    cancel()
//                    showAlarmScreen()
                    closeSelectedApp()
                }
            }

            override fun onFinish() {
                textView.text = "00:00"
//                showAlarmScreen()
                closeSelectedApp()
            }
        }.start()
    }

    private fun showAlarmScreen() {
        AlertDialog.Builder(this)
            .setTitle("Время вышло!")
            .setMessage("Лимит на использование приложения исчерпан.")
            .setPositiveButton("OK") { _, _ ->
                closeSelectedApp() // Изменено с lockScreen() на closeSelectedApp()
            }
            .setCancelable(false)
            .show()
    }

    private fun closeSelectedApp() {
        selectedApp?.let { packageName ->
            if (isAccessibilityServiceEnabled()) {
                val intent = Intent(this, AppBlockerService::class.java).apply {
                    putExtra("command", "block")
                    putExtra("package_name", packageName)
                }
                startService(intent)
                Toast.makeText(this, "Приложение будет закрыто", Toast.LENGTH_SHORT).show()
            } else {
                showAccessibilityServiceDialog()
            }
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = ComponentName(this, AppBlockerService::class.java)
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(service.flattenToString()) ?: false
    }

    private fun showAccessibilityServiceDialog() {
        AlertDialog.Builder(this)
            .setTitle("Требуется включить сервис доступности")
            .setMessage("Для закрытия приложений необходимо включить сервис доступности")
            .setPositiveButton("Настройки") { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
