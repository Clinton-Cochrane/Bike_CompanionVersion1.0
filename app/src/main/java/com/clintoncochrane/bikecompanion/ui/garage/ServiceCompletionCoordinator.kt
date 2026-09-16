package com.clintoncochrane.bikecompanion.ui.garage

import com.clintoncochrane.bikecompanion.data.component.ComponentRepository
import com.clintoncochrane.bikecompanion.data.component.ServiceIntervalRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

data class ServiceCompletionResult(
    val successfulIntervalIds: Set<Long>,
    val failedIntervalIds: Set<Long>,
)

/** Applies a batch as independent atomic writes while preserving per-component ordering. */
@Singleton
class ServiceCompletionCoordinator @Inject constructor(
    private val serviceIntervalRepository: ServiceIntervalRepository,
    private val componentRepository: ComponentRepository,
) {
    suspend fun complete(
        requirements: List<DueServiceRequirement>,
        sessionId: String,
        completedAt: Long = System.currentTimeMillis(),
    ): ServiceCompletionResult {
        val successful = linkedSetOf<Long>()
        val failed = linkedSetOf<Long>()

        requirements.groupBy { it.componentId }.values.forEach { componentRequirements ->
            val normalRequirements = componentRequirements.filterNot { it.isReplacement }
            val replacementRequirements = componentRequirements.filter { it.isReplacement }
            var prerequisiteFailed = false

            normalRequirements.forEach { requirement ->
                val completed = try {
                    serviceIntervalRepository.completeServiceRequirement(
                        requirement.intervalId,
                        sessionId,
                        completedAt,
                    )
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    false
                }
                if (completed) successful += requirement.intervalId
                else {
                    failed += requirement.intervalId
                    prerequisiteFailed = true
                }
            }

            if (prerequisiteFailed) {
                failed += replacementRequirements.map { it.intervalId }
            } else {
                replacementRequirements.forEach { requirement ->
                    val replacementId = try {
                        componentRepository.replaceComponentForService(
                            requirement.intervalId,
                            sessionId,
                            completedAt,
                        )
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        null
                    }
                    if (replacementId != null) successful += requirement.intervalId
                    else failed += requirement.intervalId
                }
            }
        }
        return ServiceCompletionResult(successful, failed)
    }
}
