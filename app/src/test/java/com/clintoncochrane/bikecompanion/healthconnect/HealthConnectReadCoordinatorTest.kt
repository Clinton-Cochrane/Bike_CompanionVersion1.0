package com.clintoncochrane.bikecompanion.healthconnect

import androidx.health.connect.client.HealthConnectClient
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HealthConnectReadCoordinatorTest {

    private val requiredPermissions = setOf("read exercise")

    @Test
    fun readHealthConnectSessions_sdkUnavailable_returnsUnavailable() = runTest {
        val result = readHealthConnectSessions(
            sdkStatus = { HealthConnectClient.SDK_UNAVAILABLE },
            requiredPermissions = requiredPermissions,
            grantedPermissions = { error("Permissions should not be checked") },
            readSessions = { error("Sessions should not be queried") },
            logFailure = {},
        )

        assertEquals(HealthConnectReadResult.Unavailable, result)
    }

    @Test
    fun readHealthConnectSessions_providerUpdateRequired_returnsProviderUpdateRequired() = runTest {
        val result = readHealthConnectSessions(
            sdkStatus = { HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED },
            requiredPermissions = requiredPermissions,
            grantedPermissions = { error("Permissions should not be checked") },
            readSessions = { error("Sessions should not be queried") },
            logFailure = {},
        )

        assertEquals(HealthConnectReadResult.ProviderUpdateRequired, result)
    }

    @Test
    fun readHealthConnectSessions_permissionMissing_returnsPermissionRequired() = runTest {
        val result = readHealthConnectSessions(
            sdkStatus = { HealthConnectClient.SDK_AVAILABLE },
            requiredPermissions = requiredPermissions,
            grantedPermissions = { emptySet() },
            readSessions = { error("Sessions should not be queried") },
            logFailure = {},
        )

        assertEquals(HealthConnectReadResult.PermissionRequired, result)
    }

    @Test
    fun readHealthConnectSessions_successfulEmptyQuery_returnsSuccessWithNoSessions() = runTest {
        val result = readHealthConnectSessions(
            sdkStatus = { HealthConnectClient.SDK_AVAILABLE },
            requiredPermissions = requiredPermissions,
            grantedPermissions = { requiredPermissions },
            readSessions = { emptyList() },
            logFailure = {},
        )

        assertEquals(HealthConnectReadResult.Success(emptyList()), result)
    }

    @Test
    fun readHealthConnectSessions_successfulQuery_returnsSessions() = runTest {
        val session = HealthConnectSession(1L, 2L, 1L, 0.0)
        val result = readHealthConnectSessions(
            sdkStatus = { HealthConnectClient.SDK_AVAILABLE },
            requiredPermissions = requiredPermissions,
            grantedPermissions = { requiredPermissions },
            readSessions = { listOf(session) },
            logFailure = {},
        )

        assertEquals(HealthConnectReadResult.Success(listOf(session)), result)
    }

    @Test
    fun readHealthConnectSessions_unexpectedException_returnsFailureAndLogsOnlySafeDetails() = runTest {
        val diagnostics = mutableListOf<String>()
        val sensitiveMessage = "ride at 123 Main Street"

        val result = readHealthConnectSessions(
            sdkStatus = { HealthConnectClient.SDK_AVAILABLE },
            requiredPermissions = requiredPermissions,
            grantedPermissions = { requiredPermissions },
            readSessions = { throw IllegalStateException(sensitiveMessage) },
            logFailure = diagnostics::add,
        )

        assertEquals(HealthConnectReadResult.Failure, result)
        assertEquals(listOf("Health Connect query failed (IllegalStateException)"), diagnostics)
        assertFalse(diagnostics.single().contains(sensitiveMessage))
    }
}
