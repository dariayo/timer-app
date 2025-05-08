package com.example.myapplication
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class AppBlockerService : AccessibilityService() {
    private var blockedPackage: String? = null

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.DEFAULT
        }
        this.serviceInfo = info
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.getStringExtra("command")) {
                "block" -> {
                    blockedPackage = it.getStringExtra("package_name")
                    Toast.makeText(this, "Блокировка активирована для $blockedPackage", Toast.LENGTH_SHORT).show()
                }
                "unblock" -> {
                    blockedPackage = null
                }
            }
        }
        return START_STICKY
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val currentPackage = event.packageName?.toString()
            if (currentPackage == blockedPackage) {
                performGlobalAction(GLOBAL_ACTION_HOME)
                Toast.makeText(this, "Приложение $currentPackage закрыто по таймеру", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onInterrupt() {
        Toast.makeText(this, "Сервис прерван", Toast.LENGTH_SHORT).show()
    }
}