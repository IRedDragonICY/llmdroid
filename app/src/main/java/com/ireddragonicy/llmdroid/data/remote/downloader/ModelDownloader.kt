package com.ireddragonicy.llmdroid.data.remote.downloader

import android.content.Context
import com.ireddragonicy.llmdroid.data.local.SecureStorage
import com.ireddragonicy.llmdroid.data.model.DownloadProgress
import com.ireddragonicy.llmdroid.di.IoDispatcher
import com.ireddragonicy.llmdroid.domain.model.LlmModelConfig
import com.ireddragonicy.llmdroid.domain.model.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

class ModelDownloader @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val secureStorage: SecureStorage,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    private var downloadJob: Job? = null
    private val downloadScope = CoroutineScope(ioDispatcher + Job())

    fun downloadModel(model: LlmModelConfig, destinationPath: String): Flow<Result<DownloadProgress>> = callbackFlow {
        if (model.url.isEmpty()) {
            trySend(Result.Error(IOException("Model URL is empty.")))
            close()
            return@callbackFlow
        }

        downloadJob?.cancel() // Cancel previous download if any
        val job = downloadScope.launch {
            try {
                val requestBuilder = Request.Builder().url(model.url)

                if (model.needsAuth) {
                    val accessToken = secureStorage.getToken()
                    if (accessToken.isNullOrEmpty()) {
                        trySend(Result.Error(AuthenticationException("Access token required but not found.")))
                        close()
                        return@launch
                    }
                    requestBuilder.addHeader("Authorization", "Bearer $accessToken")
                }

                val outputFile = File(destinationPath)
                outputFile.parentFile?.mkdirs() // Ensure directory exists

                val request = requestBuilder.build()
                val response: Response = okHttpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    if (response.code == 401 && model.needsAuth) {
                         secureStorage.removeToken() // Token might be invalid
                         trySend(Result.Error(AuthenticationException("Unauthorized (401). Token might be invalid or expired.")))
                    } else {
                        trySend(Result.Error(IOException("Download failed with code: ${response.code}")))
                    }
                    close()
                    return@launch
                }

                response.body?.use { body ->
                    val contentLength = body.contentLength()
                    body.byteStream().use { inputStream ->
                        FileOutputStream(outputFile).use { outputStream ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var bytesRead: Int
                            var totalBytesRead = 0L
                            trySend(Result.Success(DownloadProgress(0))) // Initial progress

                            while (inputStream.read(buffer).also { bytesRead = it } != -1 && isActive) {
                                outputStream.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead
                                val progress = if (contentLength > 0) {
                                    (totalBytesRead * 100 / contentLength).toInt()
                                } else {
                                     // Indicate indeterminate progress if size unknown
                                    -1 // Or handle differently
                                }
                                trySend(Result.Success(DownloadProgress(progress.coerceIn(0, 100))))
                            }
                            outputStream.flush()
                            if (isActive) {
                                trySend(Result.Success(DownloadProgress(100, true))) // Final progress
                            } else {
                                // Cancellation detected
                                outputFile.delete() // Clean up partial file
                                trySend(Result.Error(IOException("Download cancelled")))
                            }
                        }
                    }
                } ?: run {
                    trySend(Result.Error(IOException("Response body is null.")))
                }

            } catch (e: Exception) {
                 // Attempt to clean up partial file on any error
                File(destinationPath).delete()
                if (e !is kotlinx.coroutines.CancellationException) {
                    trySend(Result.Error(e))
                } else {
                     trySend(Result.Error(IOException("Download cancelled")))
                }
            } finally {
                close() // Close the flow
            }
        }
        downloadJob = job

        awaitClose {
            job.cancel()
             // Clean up on cancellation if not already done
            if (!job.isCompleted) {
                 downloadScope.launch { File(destinationPath).delete() }
            }
            println("Download Flow Closed")
        }
    }.flowOn(ioDispatcher).catch { emit(Result.Error(Exception(it))) } // Catch unexpected upstream errors


     fun cancelDownload() {
        downloadJob?.cancel()
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 4096
    }
}

class AuthenticationException(message: String) : Exception(message)