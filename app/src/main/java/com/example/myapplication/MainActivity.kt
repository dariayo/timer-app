package com.example.myapplication

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.EditText
import android.widget.TextView
import android.app.Activity
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Button
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var textView: TextView
    private lateinit var buttonStart: Button
    private lateinit var inputHours: EditText
    private lateinit var inputMinutes: EditText
    private lateinit var inputSeconds: EditText

    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация UI элементов
        textView = findViewById(R.id.textView)
        buttonStart = findViewById(R.id.buttonStart)
        inputHours = findViewById(R.id.inputHours)
        inputMinutes = findViewById(R.id.inputMinutes)
        inputSeconds = findViewById(R.id.inputSeconds)

        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, MyDeviceAdminReceiver::class.java)

        // Проверка админских прав
        if (!dpm.isAdminActive(adminComponent)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Разрешите управление устройством для блокировки экрана.")
            }
            startActivity(intent)
        }

        buttonStart.setOnClickListener {
            startTimer()
        }
    }

    private fun startTimer() {
        // Получаем данные из EditText
        val hours = inputHours.text.toString().toIntOrNull() ?: 0
        val minutes = inputMinutes.text.toString().toIntOrNull() ?: 0
        val seconds = inputSeconds.text.toString().toIntOrNull() ?: 0

        // Переводим в миллисекунды
        val totalMillis = (hours * 3600 + minutes * 60 + seconds) * 1000L

        if (totalMillis <= 0) {
            Toast.makeText(this, "Введите корректное время!", Toast.LENGTH_SHORT).show()
            return
        }

        // Останавливаем предыдущий таймер, если он идет
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(totalMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val h = millisUntilFinished / 3600000
                val m = (millisUntilFinished % 3600000) / 60000
                val s = (millisUntilFinished % 60000) / 1000
                textView.text = String.format("%02d:%02d:%02d", h, m, s)
            }

            override fun onFinish() {
                textView.text = "00:00:00"
                showAlarmScreen()
            }
        }.start()
    }

    private fun showAlarmScreen() {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Время вышло!")
            .setMessage("Ваше время использования устройства закончилось.")
            .setPositiveButton("OK") { _, _ ->
                lockScreen()
            }
            .setCancelable(false)
            .create()

        alertDialog.show()
    }

    private fun lockScreen() {
        if (dpm.isAdminActive(adminComponent)) {
            dpm.lockNow()
        }
    }
}
