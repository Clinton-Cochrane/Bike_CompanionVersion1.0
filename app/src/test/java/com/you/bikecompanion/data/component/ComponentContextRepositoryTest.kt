package com.you.bikecompanion.data.component

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ComponentContextRepository] save/load and validation.
 */
class ComponentContextRepositoryTest {

    private lateinit var dao: ComponentContextDao
    private lateinit var repository: ComponentContextRepository

    @Before
    fun setUp() {
        dao = mockk()
        repository = ComponentContextRepository(dao)
    }

    @Test
    fun getComponentContext_returnsNull_whenNoEntity() = runBlocking {
        coEvery { dao.getByComponentId(1L) } returns null

        val result = repository.getComponentContext(1L)

        assertNull(result)
        coVerify(exactly = 1) { dao.getByComponentId(1L) }
    }

    @Test
    fun getComponentContext_returnsContext_whenEntityExists() = runBlocking {
        val entity = ComponentContextEntity(
            componentId = 1L,
            notes = "Test notes",
            installDateMs = 1000L,
            purchaseLink = "https://example.com",
            serialNumber = "SN123",
            lastServiceNotes = "Last service",
        )
        coEvery { dao.getByComponentId(1L) } returns entity

        val result = repository.getComponentContext(1L)

        assertEquals(1L, result?.componentId)
        assertEquals("Test notes", result?.notes)
        assertEquals(1000L, result?.installDateMs)
        assertEquals("https://example.com", result?.purchaseLink)
        assertEquals("SN123", result?.serialNumber)
        assertEquals("Last service", result?.lastServiceNotes)
    }

    @Test
    fun upsertComponentContext_returnsError_whenNotesEmpty() = runBlocking {
        val payload = ComponentContext(
            componentId = 1L,
            notes = "   ",
            installDateMs = null,
            purchaseLink = null,
            serialNumber = null,
            lastServiceNotes = null,
        )

        val result = repository.upsertComponentContext(1L, payload)

        assertTrue(result is ComponentContextValidation.Error)
        assertEquals("Notes are required", (result as ComponentContextValidation.Error).message)
        coVerify(exactly = 0) { dao.upsert(any()) }
    }

    @Test
    fun upsertComponentContext_returnsError_whenPurchaseLinkInvalid() = runBlocking {
        val payload = ComponentContext(
            componentId = 1L,
            notes = "Some notes",
            purchaseLink = "not-a-valid-url",
        )

        val result = repository.upsertComponentContext(1L, payload)

        assertTrue(result is ComponentContextValidation.Error)
        assertEquals("Purchase link must be a valid URL", (result as ComponentContextValidation.Error).message)
        coVerify(exactly = 0) { dao.upsert(any()) }
    }

    @Test
    fun upsertComponentContext_succeeds_whenNotesNonEmptyAndValidUrl() = runBlocking {
        coEvery { dao.upsert(any()) } coAnswers { }
        val payload = ComponentContext(
            componentId = 1L,
            notes = "Valid notes",
            purchaseLink = "https://example.com/product",
        )

        val result = repository.upsertComponentContext(1L, payload)

        assertTrue(result is ComponentContextValidation.Ok)
        coVerify(exactly = 1) {
            dao.upsert(match { entity ->
                entity.componentId == 1L &&
                    entity.notes == "Valid notes" &&
                    entity.purchaseLink == "https://example.com/product"
            })
        }
    }

    @Test
    fun upsertComponentContext_succeeds_whenPurchaseLinkNull() = runBlocking {
        coEvery { dao.upsert(any()) } coAnswers { }
        val payload = ComponentContext(
            componentId = 2L,
            notes = "Notes only",
            purchaseLink = null,
        )

        val result = repository.upsertComponentContext(2L, payload)

        assertTrue(result is ComponentContextValidation.Ok)
        coVerify(exactly = 1) {
            dao.upsert(match { entity ->
                entity.componentId == 2L && entity.notes == "Notes only" && entity.purchaseLink == null
            })
        }
    }

    @Test
    fun getComponentContexts_returnsEmptyMap_whenNoIds() = runBlocking {
        val result = repository.getComponentContexts(emptyList())

        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { dao.getByComponentId(any()) }
    }

    @Test
    fun getComponentContexts_returnsMapWithNullsAndContexts() = runBlocking {
        coEvery { dao.getByComponentId(1L) } returns null
        coEvery { dao.getByComponentId(2L) } returns ComponentContextEntity(2L, "Notes", null, null, null, null)

        val result = repository.getComponentContexts(listOf(1L, 2L))

        assertEquals(2, result.size)
        assertNull(result[1L])
        assertEquals("Notes", result[2L]?.notes)
    }
}
