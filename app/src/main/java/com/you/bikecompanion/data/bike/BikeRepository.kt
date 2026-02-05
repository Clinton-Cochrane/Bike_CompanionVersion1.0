package com.you.bikecompanion.data.bike

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BikeRepository @Inject constructor(
    private val bikeDao: BikeDao,
) {
    fun getAllBikes(): Flow<List<BikeEntity>> = bikeDao.getAllBikes()

    suspend fun getBikeById(id: Long): BikeEntity? = bikeDao.getBikeById(id)

    suspend fun getMostRecentlyRiddenBike(): BikeEntity? = bikeDao.getMostRecentlyRiddenBike()

    suspend fun insertBike(bike: BikeEntity): Long = bikeDao.insert(bike)

    suspend fun updateBike(bike: BikeEntity) = bikeDao.update(bike)

    suspend fun deleteBike(bike: BikeEntity) = bikeDao.deleteById(bike.id)
}
