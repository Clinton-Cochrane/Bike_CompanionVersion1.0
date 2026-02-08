package com.you.bikecompanion.data.bike

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BikeRepository @Inject constructor(
    private val bikeDao: BikeDao,
) {
    fun getAllBikes(): Flow<List<BikeEntity>> = bikeDao.getAllBikes()

    /** True when at least one bike exists; used to choose start destination (Garage vs Trip). */
    fun hasAnyBike(): Flow<Boolean> = bikeDao.getBikeCount().map { it > 0 }

    suspend fun getBikeById(id: Long): BikeEntity? = bikeDao.getBikeById(id)

    suspend fun getMostRecentlyRiddenBike(): BikeEntity? = bikeDao.getMostRecentlyRiddenBike()

    suspend fun insertBike(bike: BikeEntity): Long = bikeDao.insert(bike)

    suspend fun updateBike(bike: BikeEntity) = bikeDao.update(bike)

    suspend fun deleteBike(bike: BikeEntity) = bikeDao.deleteById(bike.id)

    /** Resets chain replacement count after cassette/freewheel/chainrings inspection or replacement. */
    suspend fun resetChainReplacementCount(bikeId: Long) {
        val bike = bikeDao.getBikeById(bikeId) ?: return
        bikeDao.update(bike.copy(chainReplacementCount = 0))
    }
}
