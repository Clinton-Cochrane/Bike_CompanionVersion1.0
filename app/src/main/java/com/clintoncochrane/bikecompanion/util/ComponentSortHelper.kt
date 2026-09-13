package com.clintoncochrane.bikecompanion.util

import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.component.PriorUsageCertainty
import com.clintoncochrane.bikecompanion.data.component.ServiceIntervalEntity

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
            components.sortedWith(
                compareBy<ComponentEntity, Int?>(nullsLast()) { component ->
                    componentHealthPercent(component)
                },
            )
    }
}

/**
 * Computes health percent from remaining lifespan. 100 = new, 0 = expected interval reached.
 * Returns null when prior usage is unknown because a percentage would imply false precision.
 */
fun componentHealthPercent(component: ComponentEntity): Int? {
    if (component.priorUsageCertainty == PriorUsageCertainty.UNKNOWN) return null
    if (component.lifespanKm <= 0) return 100
    val usedPercent = (component.lifetimeDistanceKm / component.lifespanKm) * 100
    return (100 - usedPercent).toInt().coerceIn(0, 100)
}

/**
 * Returns the lowest component health only when every component has a trustworthy baseline.
 */
fun minimumComponentHealthPercent(components: List<ComponentEntity>): Int? {
    if (components.isEmpty()) return 100
    val healthPercentages = components.map(::componentHealthPercent)
    if (healthPercentages.any { it == null }) return null
    return healthPercentages.filterNotNull().minOrNull()
}
