package com.clintoncochrane.bikecompanion.ui.garage

import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.component.DefaultComponentTypes
import com.clintoncochrane.bikecompanion.data.component.PriorUsageCertainty

data class AddComponentFormState(
    val typeKey: String = DefaultComponentTypes.SUGGESTED.first().type,
    val displayName: String = "",
    val make: String = "",
    val model: String = "",
    val priorDistanceText: String = "",
    val approximate: Boolean = false,
)

data class AddComponentRequest(
    val type: String,
    val displayName: String,
    val make: String,
    val model: String,
    val lifespanKm: Double,
    val baselineKm: Double,
    val priorUsageCertainty: PriorUsageCertainty,
) {
    fun toEntity(bikeId: Long?, installedAt: Long): ComponentEntity = ComponentEntity(
        bikeId = bikeId,
        type = type,
        name = displayName,
        make = make,
        model = model,
        lifespanKm = lifespanKm,
        baselineKm = baselineKm,
        priorUsageCertainty = priorUsageCertainty,
        installedAt = installedAt,
    )
}

sealed interface AddComponentFormSubmission {
    data class Valid(val request: AddComponentRequest) : AddComponentFormSubmission
    data object InvalidType : AddComponentFormSubmission
    data object InvalidPriorDistance : AddComponentFormSubmission
}

fun AddComponentFormState.submit(): AddComponentFormSubmission {
    val componentType = DefaultComponentTypes.SUGGESTED.firstOrNull { it.type == typeKey }
        ?: return AddComponentFormSubmission.InvalidType
    val priorDistance = priorDistanceText.trim()
    val baselineKm = if (priorDistance.isEmpty()) {
        0.0
    } else {
        priorDistance.toDoubleOrNull()
            ?.takeIf { it.isFinite() && it >= 0.0 }
            ?: return AddComponentFormSubmission.InvalidPriorDistance
    }
    val certainty = when {
        priorDistance.isEmpty() -> PriorUsageCertainty.UNKNOWN
        approximate -> PriorUsageCertainty.APPROXIMATE
        else -> PriorUsageCertainty.KNOWN
    }

    return AddComponentFormSubmission.Valid(
        AddComponentRequest(
            type = componentType.type,
            displayName = displayName.trim(),
            make = make.trim(),
            model = model.trim(),
            lifespanKm = componentType.defaultLifespanKm,
            baselineKm = baselineKm,
            priorUsageCertainty = certainty,
        ),
    )
}
