package ge.cleaninghouse.quest.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Finds new builds on the GitHub Releases page, downloads them and hands them to the system installer. */
object Updater {
    private const val RELEASES = "https://api.github.com/repos/megrelisk/CleaningHouse/releases?per_page=15"

    data class Release(val build: Int, val apkUrl: String, val size: Long)

    fun installedBuild(context: Context): Int {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return PackageInfoCompat.getLongVersionCode(info).toInt()
    }

    /** The newest release whose build number is higher than the installed one, or null. */
    suspend fun findUpdate(context: Context): Release? = withContext(Dispatchers.IO) {
        val conn = URL(RELEASES).openConnection() as HttpURLConnection
        val text = try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            if (conn.responseCode !in 200..299) throw IOException("GitHub HTTP ${conn.responseCode}")
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
        val newest = Json.parseToJsonElement(text).jsonArray.mapNotNull { el ->
            val release = el.jsonObject
            val build = release["tag_name"]?.jsonPrimitive?.contentOrNull
                ?.removePrefix("build-")?.toIntOrNull() ?: return@mapNotNull null
            val apk = release["assets"]?.jsonArray?.map { it.jsonObject }?.firstOrNull {
                it["name"]?.jsonPrimitive?.contentOrNull?.endsWith(".apk") == true
            } ?: return@mapNotNull null
            Release(
                build = build,
                apkUrl = apk["browser_download_url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null,
                size = apk["size"]?.jsonPrimitive?.longOrNull ?: 0L,
            )
        }.maxByOrNull { it.build }
        newest?.takeIf { it.build > installedBuild(context) }
    }

    suspend fun download(context: Context, release: Release, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val file = File(dir, "CleanQuest-${release.build}.apk")
            val conn = openFollowingRedirects(URL(release.apkUrl))
            try {
                val total = conn.contentLengthLong.takeIf { it > 0 } ?: release.size
                conn.inputStream.use { input ->
                    file.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        var done = 0L
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            if (total > 0) onProgress(done.toFloat() / total)
                        }
                    }
                }
            } finally {
                conn.disconnect()
            }
            file
        }

    // GitHub redirects asset downloads to another host; follow them explicitly.
    private fun openFollowingRedirects(start: URL): HttpURLConnection {
        var url = start
        repeat(6) {
            val conn = url.openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 20_000
            conn.readTimeout = 60_000
            val code = conn.responseCode
            if (code in 300..399) {
                url = URL(url, conn.getHeaderField("Location"))
                conn.disconnect()
            } else if (code in 200..299) {
                return conn
            } else {
                conn.disconnect()
                throw IOException("Download HTTP $code")
            }
        }
        throw IOException("Too many redirects")
    }

    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun openInstallPermission(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
