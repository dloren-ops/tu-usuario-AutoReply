package com.autoreply.bot.update

import android.content.Context
import android.util.Log
import com.autoreply.bot.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Consulta GitHub Releases para detectar y descargar actualizaciones.
 * No usa librerias externas: HttpURLConnection + org.json (incluidos en Android).
 */
class UpdateRepository(private val context: Context) {

    private val latestReleaseUrl =
        "https://api.github.com/repos/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases/latest"

    /**
     * Obtiene la ultima release publicada. Devuelve null si no hay releases
     * o si ocurre un error de red.
     */
    suspend fun fetchLatest(): UpdateInfo? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(latestReleaseUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "AutoReply-App")
                connectTimeout = 15000
                readTimeout = 15000
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "GitHub respondio ${connection.responseCode}")
                return@withContext null
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parseRelease(body)
        } catch (e: Exception) {
            Log.w(TAG, "Error consultando actualizaciones", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseRelease(json: String): UpdateInfo? {
        val obj = JSONObject(json)
        if (obj.optBoolean("draft", false) || obj.optBoolean("prerelease", false)) return null

        val tag = obj.optString("tag_name").ifBlank { return null }
        val notes = obj.optString("body")
        val htmlUrl = obj.optString("html_url")

        // Buscar el primer asset que termine en .apk
        val assets = obj.optJSONArray("assets") ?: return null
        var apkUrl: String? = null
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val name = asset.optString("name")
            if (name.endsWith(".apk", ignoreCase = true)) {
                apkUrl = asset.optString("browser_download_url")
                break
            }
        }
        val url = apkUrl ?: return null

        return UpdateInfo(
            versionTag = tag,
            versionName = VersionUtils.normalize(tag),
            apkUrl = url,
            releaseNotes = notes,
            htmlUrl = htmlUrl
        )
    }

    /**
     * Descarga el APK al cache interno. [onProgress] recibe 0..100 (o -1 si
     * el tamano es desconocido). Devuelve el archivo descargado o null si falla.
     */
    suspend fun downloadApk(
        update: UpdateInfo,
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            // Limpiar APKs viejos.
            dir.listFiles()?.forEach { it.delete() }
            val outFile = File(dir, "AutoReply-${update.versionName}.apk")

            connection = (URL(update.apkUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "AutoReply-App")
                instanceFollowRedirects = true
                connectTimeout = 20000
                readTimeout = 60000
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "Descarga fallida: ${connection.responseCode}")
                return@withContext null
            }

            val total = connection.contentLength
            connection.inputStream.use { input ->
                outFile.outputStream().use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            onProgress(((downloaded * 100) / total).toInt())
                        } else {
                            onProgress(-1)
                        }
                    }
                }
            }
            onProgress(100)
            outFile
        } catch (e: Exception) {
            Log.w(TAG, "Error descargando APK", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        private const val TAG = "UpdateRepository"
    }
}
