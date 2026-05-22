package com.example.housingfinderstu

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * ProfileActivity - User profile and preferences screen
 *
 * Features:
 * - Save user preferences for smart alerts (location and max price)
 * - When preferences match filtered houses, user gets notification
 */
class ProfileActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var prefs: SharedPreferences
    private var currentUserId = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize Room database
        db = AppDatabase.getInstance(this)
        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        currentUserId = prefs.getLong("current_user_id", -1)

        // Check if user is logged in
        if (currentUserId == -1L) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Load and display user data using Room
        loadUserData()

        // Save preferences button - saves user's alert preferences
        findViewById<Button>(R.id.btnSavePreferences).setOnClickListener {
            saveUserPreferences()
        }

        // Back to main screen button
        findViewById<Button>(R.id.btnBackToMain).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    /**
     * Load user data from Room database and display in EditText fields
     */
    private fun loadUserData() {
        lifecycleScope.launch {
            // Get user from Room database
            val user = db.userDao().getUserById(currentUserId)

            runOnUiThread {
                if (user != null) {
                    // Display current preferences
                    findViewById<EditText>(R.id.etPrefLocation).setText(user.prefLocation ?: "")
                    findViewById<EditText>(R.id.etPrefMaxPrice).setText(user.prefMaxPrice?.toString() ?: "5000")

                    // Also display user info (optional)
                    findViewById<TextView>(R.id.tvUserName).text = user.fullName
                    findViewById<TextView>(R.id.tvUserEmail).text = user.email
                }
            }
        }
    }

    /**
     * Save user preferences to Room database
     * These preferences are used for Smart Alerts when filtering houses
     */
    private fun saveUserPreferences() {
        val location = findViewById<EditText>(R.id.etPrefLocation).text.toString()
        val maxPriceText = findViewById<EditText>(R.id.etPrefMaxPrice).text.toString()
        val maxPrice = maxPriceText.toIntOrNull() ?: 5000

        // Validate input
        if (location.isEmpty()) {
            Toast.makeText(this, "Please enter a preferred location", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            // Get current user
            val user = db.userDao().getUserById(currentUserId)

            if (user != null) {
                // Update user with new preferences
                val updatedUser = user.copy(
                    prefLocation = location,
                    prefMaxPrice = maxPrice
                )
                db.userDao().updateUser(updatedUser)

                runOnUiThread {
                    Toast.makeText(
                        this@ProfileActivity,
                        "✅ Preferences saved! You'll get alerts for houses in $location under BWP $maxPrice",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}