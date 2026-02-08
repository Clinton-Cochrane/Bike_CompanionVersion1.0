package com.you.bikecompanion.util

import com.you.bikecompanion.data.component.ComponentEntity
import com.you.bikecompanion.data.component.ServiceIntervalEntity

/**
 * Sort orders for component lists. Default is TYPE_AZ.
 */
enum class ComponentSortOrder {
    /** Sort by component type A–Z, then by name. */
    TYPE_AZ,
    /** Sort by next service due (soonest first). Considers both distance and time intervals. */
    NEXT_SERVICE,
    /** Sort by health (lowest first, most urgent). */
    HEALTH,
}

/**
 * Sorts components by the specified order.
 *
 * @param components List of components to sort.
 * @param sortOrder Desired sort order.
 * @param intervalsByComponentId Map of component ID to its service intervals. Required for [ComponentSortOrder.NEXT_SERVICE].
 * @return New sorted list (does not mutate input).
 */
fun sortComponents(
    components: List<ComponentEntity>,
    sortOrder: ComponentSortOrder,
    intervalsByComponentId: Map<Long, List<ServiceIntervalEntity>> = emptyMap(),
): List<ComponentEntity> {
    return when (sortOrder) {
        ComponentSortOrder.TYPE_AZ ->
            components.sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER, ComponentEntity::type)
                    .thenBy(String.CASE_INSENSITIVE_ORDER, ComponentEntity::name),
            )
        ComponentSortOrder.NEXT_SERVICE ->
            components.sortedBy { component ->
                val intervals = intervalsByComponentId[component.id] ?: emptyList()
                ServiceIntervalHelper.minHealthForSort(intervals)
            }
        ComponentSortOrder.HEALTH ->
            components.sortedBy { component ->
                componentHealthPercent(component)
            }
    }
}

/**
 * Computes health percent from remaining lifespan. 100 = new, 0 = end of life.
 */
fun componentHealthPercent(component: ComponentEntity): Int {
    if (component.lifespanKm <= 0) return 100
    val usedPercent = (component.distanceUsedKm / component.lifespanKm) * 100
    return (100 - usedPercent).toInt().coerceIn(0, 100)
}
