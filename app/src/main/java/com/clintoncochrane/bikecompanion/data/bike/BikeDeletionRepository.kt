package com.clintoncochrane.bikecompanion.data.bike

import androidx.room.withTransaction
import com.clintoncochrane.bikecompanion.data.BikeCompanionDatabase
import com.clintoncochrane.bikecompanion.data.component.ComponentRepository
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
    }
}
