package com.example.housingfinderstu

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var prefs: SharedPreferences

    // Notification Channel ID for Smart Alerts
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "housing_alerts_channel"
        private const val NOTIFICATION_CHANNEL_NAME = "Housing Finder Alerts"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Room database
        db = AppDatabase.getInstance(this)
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

        // Create notification
        createNotificationChannel()

        // Check if already logged in
        val currentUserId = prefs.getLong("current_user_id", -1)
        if (currentUserId != -1L) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Login using Room - run in coroutine
            lifecycleScope.launch {
                val user = db.userDao().login(email, password)

                runOnUiThread {
                    if (user != null) {
                        // Save user session
                        prefs.edit().putLong("current_user_id", user.id).apply()
                        Toast.makeText(this@LoginActivity, "Welcome ${user.fullName}!", Toast.LENGTH_SHORT).show()

                        // Check for matching houses and send smart alert
                        checkForMatchingHouses(user.id)

                        // Navigate to MainActivity
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            "Invalid credentials.\nTry: student1@test.com / pass123",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    /**
     * SMART ALERT FUNCTION - Checks for houses matching user's saved preferences
     * When user logs in, this checks if any available houses match their saved preferences
     * If matches found, sends a local notification
     */
    private fun checkForMatchingHouses(userId: Long) {
        lifecycleScope.launch {
            // Get user's saved preferences
            val user = db.userDao().getUserById(userId)

            if (user != null && user.prefLocation != null && user.prefLocation!!.isNotEmpty()) {
                // Filter houses based on user preferences
                val matchingHouses = db.houseDao()
                    .filterHouses(
                        minPrice = 0,
                        maxPrice = user.prefMaxPrice ?: 10000,
                        location = user.prefLocation ?: "",
                        availabilityDate = "2026-12-31"
                    )
                    .first()  // Get the list from Flow

                if (matchingHouses.isNotEmpty()) {
                    // Send notification
                    sendSmartAlert(
                        title = "🏠 New Housing Matches!",
                        message = "You have ${matchingHouses.size} house(s) matching your preferences.\nTap to view."
                    )

                    // Show Toast for immediate feedback
                    Toast.makeText(
                        this@LoginActivity,
                        "🎯 Smart Alert: ${matchingHouses.size} houses match your preferences!",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "No houses currently match your preferences. Update them in Profile.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                // User hasn't set preferences yet
                Toast.makeText(
                    this@LoginActivity,
                    "Set your preferences in Profile to get smart alerts!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Send local notification to user
     */
    private fun sendSmartAlert(title: String, message: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    /**
     * Create notification channel
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when houses match your saved preferences"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}