package com.example.housingfinderstu

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "houses")
data class HouseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val price: Int,
    val location: String,
    val type: String,
    val amenities: String,
    val availability_date: String,
    val deposit: Int,
    val is_reserved: Int = 0,  // 0 = available, 1 = reserved
    val reserved_by: Long = -1,
    val imageName: String = "house1"  //  - default image
)