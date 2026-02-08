package com.you.bikecompanion.data.component

/**
 * Domain model for component context (notes, install date, purchase link, etc.).
 * Used for UI and repository payloads; persisted as [ComponentContextEntity].
 */
data class ComponentContext(
    val componentId: Long,
    val notes: String,
    val installDateMs: Long? = null,
    val purchaseLink: String? = null,
    val serialNumber: String? = null,
    val lastServiceNotes: String? = null,
    val purchasePrice: String? = null,
    val purchaseDateMs: Long? = null,
) {
    fun toEntity(): ComponentContextEntity = ComponentContextEntity(
        componentId = componentId,
        notes = notes,
        installDateMs = installDateMs,
        purchaseLink = purchaseLink?.takeIf { it.isNotBlank() },
        serialNumber = serialNumber?.takeIf { it.isNotBlank() },
        lastServiceNotes = lastServiceNotes?.takeIf { it.isNotBlank() },
        purchasePrice = purchasePrice?.takeIf { it.isNotBlank() },
        purchaseDateMs = purchaseDateMs,
    )

    companion object {
        fun fromEntity(entity: ComponentContextEntity): ComponentContext = ComponentContext(
            componentId = entity.componentId,
            notes = entity.notes,
            installDateMs = entity.installDateMs,
            purchaseLink = entity.purchaseLink,
            serialNumber = entity.serialNumber,
            lastServiceNotes = entity.lastServiceNotes,
            purchasePrice = entity.purchasePrice,
            purchaseDateMs = entity.purchaseDateMs,
        )
    }
}
