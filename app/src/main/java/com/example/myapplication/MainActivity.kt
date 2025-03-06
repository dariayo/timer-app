package com.example.myapplication


import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.TextView
import android.app.Activity
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Button

class MainActivity : Activity() {
    private lateinit var textView: TextView
    private lateinit var buttonStart: Button
    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)
        buttonStart = findViewById(R.id.buttonStart)

        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, MyDeviceAdminReceiver::class.java)

        // Проверка админа
        if (!dpm.isAdminActive(adminComponent)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Разрешите управление устройством для блокировки экрана.")
            }
            startActivity(intent)
        }

        buttonStart.setOnClickListener {
            startTimer(10 * 1000) // 10 секунд для теста
        }
    }

    private fun startTimer(duration: Long) {
        object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                textView.text = "Осталось: ${millisUntilFinished / 1000} сек"
            }

            override fun onFinish() {
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