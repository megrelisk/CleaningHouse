package ge.cleaninghouse.quest.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Base64
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

/** Turns captured media into Gemini request parts, keeping the request small enough to send inline. */
object MediaPrep {
    private const val INLINE_VIDEO_LIMIT = 9L * 1024 * 1024
    private const val MAX_SIDE = 1024
    private const val FRAME_COUNT = 14

    fun buildParts(video: File?, photos: List<File>): List<JsonObject> {
        val parts = mutableListOf<JsonObject>()
        if (video != null && video.exists() && video.length() > 0) {
            if (video.length() <= INLINE_VIDEO_LIMIT) {
                parts += inline("video/mp4", video.readBytes())
            } else {
                extractFrames(video).forEach { parts += inline("image/jpeg", it) }
            }
        }
        photos.filter { it.exists() }.forEach { file ->
            decodeScaled(file)?.let { parts += inline("image/jpeg", toJpeg(it)) }
        }
        return parts
    }

    private fun inline(mime: String, bytes: ByteArray): JsonObject = buildJsonObject {
        putJsonObject("inline_data") {
            put("mime_type", mime)
            put("data", Base64.encodeToString(bytes, Base64.NO_WRAP))
        }
    }

    private fun extractFrames(video: File): List<ByteArray> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(video.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            (0 until FRAME_COUNT).mapNotNull { i ->
                val timeUs = durationMs * 1000L * (2 * i + 1) / (2 * FRAME_COUNT)
                retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?.let { toJpeg(scale(it)) }
            }
        } catch (e: Exception) {
            emptyList()
        } finally {
            retriever.release()
        }
    }

    private fun decodeScaled(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val bitmap = BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample },
        ) ?: return null
        return scale(bitmap)
    }

    private fun scale(bitmap: Bitmap): Bitmap {
        val side = max(bitmap.width, bitmap.height)
        if (side <= MAX_SIDE) return bitmap
        val f = MAX_SIDE.toFloat() / side
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * f).toInt(), (bitmap.height * f).toInt(), true)
    }

    private fun toJpeg(bitmap: Bitmap): ByteArray =
        ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 78, out)
            out.toByteArray()
        }
}
