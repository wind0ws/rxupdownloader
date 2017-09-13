package com.threshold.updownloader.progress

import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.Response
import okio.*
import java.io.IOException

class UploadProgressInterceptor(private val progressListener: ProgressListener) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        if (originalRequest.body() == null) {
            return chain.proceed(originalRequest)
        }

        val progressRequest = originalRequest.newBuilder()
                .method(originalRequest.method(),
                        CountingRequestBody(originalRequest.body()!!, progressListener))
                .build()

        return chain.proceed(progressRequest)
    }

    /**
     * Decorates an OkHttp request body to count the number of bytes written when writing it. Can
     * decorate any request body, but is most useful for tracking the upload progress of large
     * multipart requests.
     *
     * @author Leo Nikkilä
     */
    private inner class CountingRequestBody internal constructor(internal var delegate: RequestBody, internal var listener: ProgressListener) : RequestBody() {

        override fun contentType(): MediaType? {
            return delegate.contentType()
        }

        override fun contentLength(): Long {
            try {
                return delegate.contentLength()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return -1
        }

        @Throws(IOException::class)
        override fun writeTo(sink: BufferedSink) {
            val bufferedSink = Okio.buffer(CountingSink(sink))

            delegate.writeTo(bufferedSink)

            bufferedSink.flush()
        }

        internal inner class CountingSink(delegate: Sink) : ForwardingSink(delegate) {

            private var bytesWritten: Long = 0L

            @Throws(IOException::class)
            override fun write(source: Buffer, byteCount: Long) {
                super.write(source, byteCount)

                bytesWritten += byteCount
                listener.update(bytesWritten, contentLength())
            }
        }

    }

    interface ProgressListener {
        // for calculate progress,do this:  (100 * bytesWritten) / totalContentLength
        fun update(bytesWritten: Long, totalContentLength: Long)
    }
}