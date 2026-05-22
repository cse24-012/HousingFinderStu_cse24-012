package com.example.housingfinderstu

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reservations")
data class ReservationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val user_id: Long,
    val house_id: Long,
    val reference_number: String,
    val deposit_paid: Int,
    val date: String
)