package com.example.housingfinderstu

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        db = AppDatabase.getInstance(this)

        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etStudentId = findViewById<EditText>(R.id.etStudentId)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnRegister.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val studentId = etStudentId.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirm = etConfirmPassword.text.toString()

            // Input validation
            when {
                fullName.isEmpty() -> Toast.makeText(this, "Enter full name", Toast.LENGTH_SHORT).show()
                studentId.isEmpty() -> Toast.makeText(this, "Enter student ID", Toast.LENGTH_SHORT).show()
                email.isEmpty() -> Toast.makeText(this, "Enter email", Toast.LENGTH_SHORT).show()
                password.isEmpty() -> Toast.makeText(this, "Enter password", Toast.LENGTH_SHORT).show()
                password != confirm -> Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show()
                else -> {
                    // Register user using Room
                    registerUser(fullName, studentId, email, password)
                }
            }
        }
    }

    /**
     * Register a new user in Room database
     * Checks if email already exists before inserting
     */
    private fun registerUser(fullName: String, studentId: String, email: String, password: String) {
        lifecycleScope.launch {
            // Check if email already exists
            val existingUser = db.userDao().getUserByEmail(email)

            if (existingUser != null) {
                runOnUiThread {
                    Toast.makeText(this@RegisterActivity, "Email already exists", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Create new user
                val newUser = User(
                    fullName = fullName,
                    studentId = studentId,
                    email = email,
                    password = password,
                    prefLocation = "",      // Default: no preference
                    prefMaxPrice = 5000     // Default max price
                )

                // Insert into database
                val userId = db.userDao().insertUser(newUser)

                runOnUiThread {
                    if (userId != -1L) {
                        Toast.makeText(this@RegisterActivity, "Registration successful! Please login.", Toast.LENGTH_LONG).show()
                        finish()  // Go back to login screen
                    } else {
                        Toast.makeText(this@RegisterActivity, "Registration failed. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}