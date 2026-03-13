package com.you.bikecompanion.data.image

import android.net.Uri
import java.io.File
import java.io.InputStream
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles storage and deletion of bike and component thumbnail images.
 * Images are stored in app-private storage under images/bikes/ and images/components/.
 *
 * @param baseDir Root directory for images (e.g. context.filesDir/images).
 * @param openUriStream Function to open an InputStream from a content URI (e.g. from image picker).
 */
@Singleton
class ImageRepository(
    private val baseDir: File,
    private val openUriStream: (Uri) -> InputStream?,
) {
    private val bikesDir: File by lazy {
        File(baseDir, BIKE_IMAGES_SUBDIR).also { it.mkdirs() }
    }

    private val componentsDir: File by lazy {
        File(baseDir, COMPONENT_IMAGES_SUBDIR).also { it.mkdirs() }
    }

    /**
     * Copies the image from the source URI into app storage for the given bike.
     * Replaces any existing image for this bike.
     *
     * @return Absolute file path for [BikeEntity.thumbnailUri], or null if copy failed.
     */
    suspend fun saveBikeImage(bikeId: Long, sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        val destFile = File(bikesDir, fileNameForBike(bikeId))
        copyUriToFile(sourceUri, destFile)
    }

    /**
     * Copies the image from the source URI into app storage for the given component.
     * Replaces any existing image for this component.
     *
     * @return Absolute file path for [ComponentEntity.thumbnailUri], or null if copy failed.
     */
    suspend fun saveComponentImage(componentId: Long, sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        val destFile = File(componentsDir, fileNameForComponent(componentId))
        copyUriToFile(sourceUri, destFile)
    }

    /**
     * Deletes the bike's thumbnail image file if it exists.
     * Safe to call when no image exists.
     */
    suspend fun deleteBikeImage(bikeId: Long) = withContext(Dispatchers.IO) {
        File(bikesDir, fileNameForBike(bikeId)).takeIf { it.exists() }?.delete()
    }

    /**
     * Deletes the component's thumbnail image file if it exists.
     * Safe to call when no image exists.
     */
    suspend fun deleteComponentImage(componentId: Long) = withContext(Dispatchers.IO) {
        File(componentsDir, fileNameForComponent(componentId)).takeIf { it.exists() }?.delete()
    }

    /**
     * Deletes the image at the given path if it belongs to our storage.
     * Used when user removes a photo (path comes from entity.thumbnailUri).
     */
    suspend fun deleteImageAtPath(path: String?) = withContext(Dispatchers.IO) {
        if (path.isNullOrBlank()) return@withContext
        val file = File(path)
        if (file.exists() && (file.parentFile == bikesDir || file.parentFile == componentsDir)) {
            file.delete()
        }
    }

    private fun copyUriToFile(sourceUri: Uri, destFile: File): String? {
        val input = openUriStream(sourceUri) ?: return null
        return try {
            input.use { inp ->
                destFile.outputStream().use { out ->
                    inp.copyTo(out)
                }
            }
            destFile.absolutePath
        } catch (_: Exception) {
            destFile.takeIf { it.exists() }?.delete()
            null
        }
    }

    private fun fileNameForBike(bikeId: Long) = "bike_$bikeId.jpg"
    private fun fileNameForComponent(componentId: Long) = "component_$componentId.jpg"

    companion object {
        private const val BIKE_IMAGES_SUBDIR = "images/bikes"
        private const val COMPONENT_IMAGES_SUBDIR = "images/components"
    }
}
