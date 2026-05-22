package com.example.housingfinderstu

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val studentId: String,
    val email: String,
    val password: String,
    val prefLocation: String? = "",      // ← For smart alerts
    val prefMaxPrice: Int? = 5000        // ← For smart alerts
)