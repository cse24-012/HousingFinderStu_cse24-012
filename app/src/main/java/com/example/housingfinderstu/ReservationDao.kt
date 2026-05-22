package com.example.housingfinderstu

import androidx.room.*

@Dao
interface ReservationDao {
    @Insert
    suspend fun insertReservation(reservation: ReservationEntity)

    @Query("SELECT * FROM reservations WHERE house_id = :houseId")
    suspend fun getReservationByHouseId(houseId: Long): ReservationEntity?
}