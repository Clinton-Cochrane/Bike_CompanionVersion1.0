package com.you.bikecompanion.ui.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.you.bikecompanion.data.bike.BikeEntity
import com.you.bikecompanion.data.bike.BikeRepository
import com.you.bikecompanion.data.component.ComponentEntity
import com.you.bikecompanion.data.component.ComponentRepository
import com.you.bikecompanion.data.component.DefaultSeedComponent
import com.you.bikecompanion.data.ride.RideEntity
import com.you.bikecompanion.data.ride.RideRepository
import com.you.bikecompanion.data.ride.RideSource
import com.you.bikecompanion.healthconnect.HealthConnectImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Result of Health Connect import for UI to display via string resources. */
sealed class HealthConnectImportResult {
    data class Success(val count: Int) : HealthConnectImportResult()
    data object None : HealthConnectImportResult()
    data object NoBikeSelected : HealthConnectImportResult()
    data object Error : HealthConnectImportResult()
}

/** Info for one missing part slot: expected component and optional garage matches. */
data class MissingPartInfo(
    val expected: DefaultSeedComponent,
    val garageMatches: List<ComponentEntity>,
)

data class TripUiState(
    val bikes: List<BikeEntity> = emptyList(),
    val rides: List<RideEntity> = emptyList(),
    val selectedBike: BikeEntity? = null,
    val lastRiddenBike: BikeEntity? = null,
    /** When non-null, show missing-parts dialog before starting ride. */
    val missingParts: List<MissingPartInfo>? = null,
)

@HiltViewModel
class TripViewModel @Inject constructor(
    private val bikeRepository: BikeRepository,
    private val rideRepository: RideRepository,
    private val componentRepository: ComponentRepository,
    private val healthConnectImporter: HealthConnectImporter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripUiState())
    val uiState: StateFlow<TripUiState> = _uiState.asStateFlow()

    private val _healthConnectImportResult = MutableSharedFlow<HealthConnectImportResult>(replay = 0, extraBufferCapacity = 1)
    val healthConnectImportResult: SharedFlow<HealthConnectImportResult> = _healthConnectImportResult.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                bikeRepository.getAllBikes(),
                rideRepository.getAllRides(),
            ) { bikes, rides ->
                Pair(bikes, rides)
            }.collect { (bikes, rides) ->
                val lastRidden = bikeRepository.getMostRecentlyRiddenBike()
                _uiState.value = TripUiState(
                    bikes = bikes,
                    rides = rides.sortedByDescending { it.endedAt },
                    selectedBike = _uiState.value.selectedBike?.let { selected ->
                        bikes.find { it.id == selected.id }
                    } ?: lastRidden,
                    lastRiddenBike = lastRidden,
                )
            }
        }
    }

    fun selectBike(bike: BikeEntity?) {
        _uiState.value = _uiState.value.copy(selectedBike = bike)
    }

    /**
     * Checks if the selected bike has missing parts. If so, sets missingParts for the dialog.
     * @return true if OK to proceed (no missing parts), false if dialog should be shown.
     */
    suspend fun checkMissingPartsBeforeStart(): Boolean {
        val bike = _uiState.value.selectedBike ?: return true
        val missing = componentRepository.getMissingComponentsForBike(bike)
        if (missing.isEmpty()) return true
        val infos = missing.map { expected ->
            val garageMatches = componentRepository.getComponentsInGarageMatching(expected.type, expected.position)
            MissingPartInfo(expected = expected, garageMatches = garageMatches)
        }
        _uiState.value = _uiState.value.copy(missingParts = infos)
        return false
    }

    fun clearMissingParts() {
        _uiState.value = _uiState.value.copy(missingParts = null)
    }

    /** Adds a placeholder component for the given slot and removes it from missing list. */
    fun addPlaceholderFor(missing: MissingPartInfo) {
        val bike = _uiState.value.selectedBike ?: return
        viewModelScope.launch {
            val entity = com.you.bikecompanion.data.component.ComponentEntity(
                bikeId = bike.id,
                type = missing.expected.type,
                name = "Placeholder ${missing.expected.name}",
                lifespanKm = missing.expected.defaultLifespanKm,
                position = missing.expected.position,
                installedAt = System.currentTimeMillis(),
            )
            componentRepository.insertComponent(entity)
            removeMissingPart(missing.expected.type, missing.expected.position)
        }
    }

    /** Installs a garage component on the bike and removes from missing list. */
    fun installFromGarage(component: ComponentEntity) {
        val bike = _uiState.value.selectedBike ?: return
        viewModelScope.launch {
            componentRepository.installComponent(component, bike.id)
            removeMissingPart(component.type, component.position)
        }
    }

    /** Adds placeholders for all missing parts and clears the dialog. */
    fun addAllPlaceholders() {
        val list = _uiState.value.missingParts ?: return
        list.forEach { addPlaceholderFor(it) }
        _uiState.value = _uiState.value.copy(missingParts = null)
    }

    private fun removeMissingPart(type: String, position: String) {
        val current = _uiState.value.missingParts ?: return
        val updated = current.filter { it.expected.type != type || it.expected.position != position }
        _uiState.value = _uiState.value.copy(missingParts = if (updated.isEmpty()) null else updated)
    }

    fun importFromHealthConnect() {
        val bikeId = _uiState.value.selectedBike?.id ?: -1L
        if (bikeId < 0) {
            viewModelScope.launch { _healthConnectImportResult.emit(HealthConnectImportResult.NoBikeSelected) }
            return
        }
        viewModelScope.launch {
            try {
                val sessions = healthConnectImporter.readCyclingSessions()
                if (sessions.isEmpty()) {
                    _healthConnectImportResult.emit(HealthConnectImportResult.None)
                    return@launch
                }
                var count = 0
                sessions.forEach { session ->
                    val ride = RideEntity(
                        bikeId = bikeId,
                        distanceKm = session.distanceKm,
                        durationMs = session.durationMs,
                        startedAt = session.startTimeMs,
                        endedAt = session.endTimeMs,
                        source = RideSource.HEALTH_CONNECT,
                    )
                    rideRepository.saveRideAndUpdateBikeAndComponents(ride)
                    count++
                }
                _healthConnectImportResult.emit(HealthConnectImportResult.Success(count))
            } catch (_: Exception) {
                _healthConnectImportResult.emit(HealthConnectImportResult.Error)
            }
        }
    }
}
