package com.you.bikecompanion.data.image

import android.net.Uri
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File

/**
 * Unit tests for [ImageRepository].
 * Uses a temp directory and a fake URI stream provider.
 * Mocks [Uri] to avoid Android SDK stubs in JVM unit tests.
 */
class ImageRepositoryTest {

    private lateinit var baseDir: File
    private lateinit var openUriStream: (Uri) -> java.io.InputStream?
    private lateinit var repository: ImageRepository

    @Before
    fun setUp() {
        baseDir = File.createTempFile("image_repo_test", "").apply { delete(); mkdirs() }
        openUriStream = { _ ->
            ByteArrayInputStream(byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte())) // minimal PNG header
        }
        repository = ImageRepository(baseDir, openUriStream)
    }

    private fun mockUri(): Uri = mockk(relaxed = true)

    @Test
    fun saveBikeImage_returnsPath_whenCopySucceeds() = runTest {
        val uri = mockUri()
        val path = repository.saveBikeImage(1L, uri)

        assertEquals(File(baseDir, "bikes/bike_1.jpg").absolutePath, path)
        assert(File(path!!).exists())
    }

    @Test
    fun saveBikeImage_replacesExisting_whenCalledTwice() = runTest {
        val uri = mockUri()
        repository.saveBikeImage(2L, uri)
        val path2 = repository.saveBikeImage(2L, uri)

        assertEquals(File(baseDir, "bikes/bike_2.jpg").absolutePath, path2)
    }

    @Test
    fun saveComponentImage_returnsPath_whenCopySucceeds() = runTest {
        val uri = mockUri()
        val path = repository.saveComponentImage(10L, uri)

        assertEquals(File(baseDir, "components/component_10.jpg").absolutePath, path)
        assert(File(path!!).exists())
    }

    @Test
    fun deleteBikeImage_removesFile_whenExists() = runTest {
        val path = repository.saveBikeImage(3L, mockUri())
        assert(File(path!!).exists())

        repository.deleteBikeImage(3L)

        assert(!File(path).exists())
    }

    @Test
    fun deleteBikeImage_succeeds_whenFileDoesNotExist() = runTest {
        repository.deleteBikeImage(999L)
        // No exception
    }

    @Test
    fun deleteComponentImage_removesFile_whenExists() = runTest {
        val path = repository.saveComponentImage(20L, mockUri())
        assert(File(path!!).exists())

        repository.deleteComponentImage(20L)

        assert(!File(path).exists())
    }

    @Test
    fun deleteImageAtPath_removesFile_whenPathInBikesDir() = runTest {
        val path = repository.saveBikeImage(4L, mockUri())
        assert(File(path!!).exists())

        repository.deleteImageAtPath(path)

        assert(!File(path).exists())
    }

    @Test
    fun deleteImageAtPath_removesFile_whenPathInComponentsDir() = runTest {
        val path = repository.saveComponentImage(30L, mockUri())
        assert(File(path!!).exists())

        repository.deleteImageAtPath(path)

        assert(!File(path).exists())
    }

    @Test
    fun deleteImageAtPath_ignoresNullPath() = runTest {
        repository.deleteImageAtPath(null)
        repository.deleteImageAtPath("")
        // No exception
    }

    @Test
    fun saveBikeImage_returnsNull_whenOpenUriStreamFails() = runTest {
        val failingRepository = ImageRepository(baseDir) { null }

        val path = failingRepository.saveBikeImage(5L, mockUri())

        assertNull(path)
    }
}
