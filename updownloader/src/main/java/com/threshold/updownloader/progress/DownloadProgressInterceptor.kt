package com.threshold.updownloader.progress

import java.io.IOException
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.internal.http2.StreamResetException
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Okio
import okio.Source

class DownloadProgressInterceptor(private val progressListener: ProgressListener) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalResponse = chain.proceed(chain.request())
        return originalResponse.newBuilder()
                .body(ProgressResponseBody(originalResponse.body(), progressListener))
                .build()
    }

    private class ProgressResponseBody internal constructor(private val responseBody: ResponseBody?, private val progressListener: ProgressListener) : ResponseBody() {
        private var bufferedSource: BufferedSource? = null

        override fun contentType(): MediaType? {
            return responseBody?.contentType()
        }

        override fun contentLength(): Long {
            return responseBody?.contentLength() ?: -1L
        }

        override fun source(): BufferedSource {
            if (bufferedSource == null && responseBody != null) {
                bufferedSource = Okio.buffer(source(responseBody.source()))
            }
            return bufferedSource!!
        }

        private fun source(source: Source): Source {
            return object : ForwardingSource(source) {
                internal var totalBytesRead = 0L

                @Throws(IOException::class)
                override fun read(sink: Buffer, byteCount: Long): Long {
                    var bytesRead: Long = 0
                    try {
                        bytesRead = super.read(sink, byteCount)
                        // read() returns the number of bytes read, or -1 if this source is exhausted.
                        totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                        progressListener.update(totalBytesRead, contentLength(), bytesRead == -1L)
                    } catch (e: StreamResetException) {
                        e.printStackTrace()
                    }
                    return bytesRead
                }
            }
        }
    }

    interface ProgressListener {
        //for calculate progress,do this:  (100 * bytesRead) / totalContentLength
        fun update(bytesRead: Long, totalContentLength: Long, isDone: Boolean)
    }
}
