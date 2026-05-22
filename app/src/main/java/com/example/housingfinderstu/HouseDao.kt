package com.example.housingfinderstu

import androidx.room.*
import kotlinx.coroutines.flow.Flow

//database to which house info is stored

@Dao
interface HouseDao {
    @Insert
    suspend fun insertHouse(house: HouseEntity)

    @Query("SELECT * FROM houses")
    fun getAllHouses(): Flow<List<HouseEntity>>

    @Query("SELECT * FROM houses WHERE is_reserved = 0 ORDER BY price ASC")
    fun getAvailableHouses(): Flow<List<HouseEntity>>

    @Query("SELECT * FROM houses WHERE is_reserved = 0 AND price BETWEEN :minPrice AND :maxPrice AND (:location = '' OR location = :location) AND (:availabilityDate = '' OR availability_date <= :availabilityDate)")
    fun filterHouses(
        minPrice: Int,
        maxPrice: Int,
        location: String,
        availabilityDate: String
    ): Flow<List<HouseEntity>>

    @Query("SELECT * FROM houses WHERE id = :houseId")
    suspend fun getHouseById(houseId: Long): HouseEntity?

    @Update
    suspend fun updateHouse(house: HouseEntity)

    @Query("SELECT * FROM houses WHERE is_reserved = 0 AND location = :location AND price <= :maxPrice")
    suspend fun getMatchingHouses(location: String, maxPrice: Int): List<HouseEntity>
}