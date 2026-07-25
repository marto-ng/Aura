package com.aura.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite_locations ORDER BY id DESC")
    fun getAllFavorites(): Flow<List<FavoriteLocation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteLocation): Long

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteLocation)

    @Query("DELETE FROM favorite_locations WHERE latitude BETWEEN (:lat - 0.01) AND (:lat + 0.01) AND longitude BETWEEN (:lon - 0.01) AND (:lon + 0.01)")
    suspend fun deleteByCoordinates(lat: Double, lon: Double)

    @Query("SELECT COUNT(*) FROM favorite_locations WHERE latitude BETWEEN (:lat - 0.01) AND (:lat + 0.01) AND longitude BETWEEN (:lon - 0.01) AND (:lon + 0.01)")
    fun isFavoriteExists(lat: Double, lon: Double): Flow<Int>
}
