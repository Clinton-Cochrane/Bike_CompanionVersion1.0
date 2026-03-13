package com.you.bikecompanion.data.image

import android.net.Uri
import io.mockk.coEvery
import io.mockk.coVerify
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

    @Test
    fun saveBikeImage_returnsPath_whenCopySucceeds() = runTest {
        val path = repository.saveBikeImage(1L, Uri.parse("content://test/1"))

        assertEquals(File(baseDir, "images/bikes/bike_1.jpg").absolutePath, path)
        assert(File(path!!).exists())
    }

    @Test
    fun saveBikeImage_replacesExisting_whenCalledTwice() = runTest {
        repository.saveBikeImage(2L, Uri.parse("content://test/2"))
        val path2 = repository.saveBikeImage(2L, Uri.parse("content://test/2"))

        assertEquals(File(baseDir, "images/bikes/bike_2.jpg").absolutePath, path2)
    }

    @Test
    fun saveComponentImage_returnsPath_whenCopySucceeds() = runTest {
        val path = repository.saveComponentImage(10L, Uri.parse("content://test/10"))

        assertEquals(File(baseDir, "images/components/component_10.jpg").absolutePath, path)
        assert(File(path!!).exists())
    }

    @Test
    fun deleteBikeImage_removesFile_whenExists() = runTest {
        val path = repository.saveBikeImage(3L, Uri.parse("content://test/3"))
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
        val path = repository.saveComponentImage(20L, Uri.parse("content://test/20"))
        assert(File(path!!).exists())

        repository.deleteComponentImage(20L)

        assert(!File(path).exists())
    }

    @Test
    fun deleteImageAtPath_removesFile_whenPathInBikesDir() = runTest {
        val path = repository.saveBikeImage(4L, Uri.parse("content://test/4"))
        assert(File(path!!).exists())

        repository.deleteImageAtPath(path)

        assert(!File(path).exists())
    }

    @Test
    fun deleteImageAtPath_removesFile_whenPathInComponentsDir() = runTest {
        val path = repository.saveComponentImage(30L, Uri.parse("content://test/30"))
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

        val path = failingRepository.saveBikeImage(5L, Uri.parse("content://test/5"))

        assertNull(path)
    }
}
