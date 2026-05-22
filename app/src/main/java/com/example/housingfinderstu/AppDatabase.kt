package com.example.housingfinderstu

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        HouseEntity::class,
        ReservationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun houseDao(): HouseDao
    abstract fun reservationDao(): ReservationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "housing_database"
                ).addCallback(DatabaseCallback(context)).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * Callback to pre-populate the database with 50 users and 50 houses
 */
class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        CoroutineScope(Dispatchers.IO).launch {
            prePopulateData()
        }
    }

    private suspend fun prePopulateData() {
        val db = AppDatabase.getInstance(context)

        // ========== SEED 50 USERS ==========
        val existingUsers = db.userDao().getAllUsers().first()
        if (existingUsers.isEmpty()) {
            for (i in 1..50) {
                val user = User(
                    fullName = "Student ${String.format("%03d", i)}",
                    studentId = "2024${String.format("%03d", i)}",
                    email = "student$i@test.com",
                    password = "pass123",
                    prefLocation = "",
                    prefMaxPrice = 5000
                )
                db.userDao().insertUser(user)
            }
        }

        // ========== SEED 50 HOUSES  ==========
        val existingHouses = db.houseDao().getAllHouses().first()
        if (existingHouses.isEmpty()) {
            val locations = listOf("Gaborone West", "Gaborone North", "Broadhurst", "Village", "Phase 4", "CBD", "Block 3", "Tlokweng")
            val types = listOf("Single Room", "Apartment", "Studio", "Shared Room", "House", "Backroom")
            val amenities = listOf("WiFi,Parking,Water", "WiFi,Electricity,Security", "Parking,Water,Furnished", "All utilities included")
            val imageNames = listOf("house1", "house2", "house3", "house4", "house5", "house6", "house7", "house8")

            for (i in 1..50) {
                val house = HouseEntity(
                    title = "Modern ${types[i % types.size]} - Unit $i",
                    price = 1200 + (i * 80),
                    location = locations[i % locations.size],
                    type = types[i % types.size],
                    amenities = amenities[i % amenities.size],
                    availability_date = "2026-${(i % 12) + 1}-${(i % 28) + 1}",
                    deposit = 800 + (i % 500),
                    is_reserved = 0,
                    reserved_by = -1,
                    imageName = imageNames[i % imageNames.size]
                )
                db.houseDao().insertHouse(house)
            }
        }
    }
}