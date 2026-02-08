package com.you.bikecompanion.data.component

import com.you.bikecompanion.util.UrlValidator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of validating component context before save.
 */
sealed class ComponentContextValidation {
    data object Ok : ComponentContextValidation()
    data class Error(val message: String) : ComponentContextValidation()
}

@Singleton
class ComponentContextRepository @Inject constructor(
    private val componentContextDao: ComponentContextDao,
) {

    suspend fun getComponentContext(componentId: Long): ComponentContext? {
        val entity = componentContextDao.getByComponentId(componentId) ?: return null
        return ComponentContext.fromEntity(entity)
    }

    /** Loads context for multiple components. Returns map from componentId to context (null if none). */
    suspend fun getComponentContexts(componentIds: List<Long>): Map<Long, ComponentContext?> {
        if (componentIds.isEmpty()) return emptyMap()
        return componentIds.associateWith { getComponentContext(it) }
    }

    /**
     * Validates and upserts context for the component.
     * Notes must have at least one character (trimmed). Purchase link must be a valid URL if present.
     */
    suspend fun upsertComponentContext(componentId: Long, payload: ComponentContext): ComponentContextValidation {
        val notesTrimmed = payload.notes.trim()
        if (notesTrimmed.isEmpty()) {
            return ComponentContextValidation.Error("Notes are required")
        }
        val link = payload.purchaseLink?.trim()
        if (!link.isNullOrEmpty() && !UrlValidator.isValidHttpUrl(link)) {
            return ComponentContextValidation.Error("Purchase link must be a valid URL")
        }
        val toSave = payload.copy(
            componentId = componentId,
            notes = notesTrimmed,
            purchaseLink = link?.takeIf { it.isNotEmpty() },
            serialNumber = payload.serialNumber?.trim()?.takeIf { it.isNotEmpty() },
            lastServiceNotes = payload.lastServiceNotes?.trim()?.takeIf { it.isNotEmpty() },
            purchasePrice = payload.purchasePrice?.trim()?.takeIf { it.isNotEmpty() },
            purchaseDateMs = payload.purchaseDateMs,
        )
        componentContextDao.upsert(toSave.toEntity())
        return ComponentContextValidation.Ok
    }
}
