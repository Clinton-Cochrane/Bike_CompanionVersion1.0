package com.clintoncochrane.bikecompanion.data.bike

import androidx.room.withTransaction
import com.clintoncochrane.bikecompanion.data.BikeCompanionDatabase
import com.clintoncochrane.bikecompanion.data.component.ComponentRepository
import com.clintoncochrane.bikecompanion.data.image.ImageRepository
import javax.inject.Inject
import javax.inject.Singleton

enum class BikeDeletionComponentDisposition {
    MOVE_TO_GARAGE,
    RETIRE,
}

/** Coordinates the component lifecycle operation and bike deletion in one database transaction. */
@Singleton
class BikeDeletionRepository @Inject constructor(
    private val database: BikeCompanionDatabase,
    private val bikeDao: BikeDao,
    private val componentRepository: ComponentRepository,
    private val imageRepository: ImageRepository,
) {
    suspend fun deleteBike(
        bike: BikeEntity,
        componentDisposition: BikeDeletionComponentDisposition,
    ) {
        database.withTransaction {
            componentRepository.getComponentsByBikeIdOnce(bike.id).forEach { component ->
                when (componentDisposition) {
                    BikeDeletionComponentDisposition.MOVE_TO_GARAGE -> {
                        componentRepository.removeToGarage(component)
                    }
                    BikeDeletionComponentDisposition.RETIRE -> {
                        componentRepository.retireComponent(component)
                    }
                }
            }
            bikeDao.deleteById(bike.id)
        }
        imageRepository.deleteBikeImage(bike.id)
    }
}
