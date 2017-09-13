package com.threshold.updownloader

import android.content.Intent
import com.threshold.updownloader.util.FileUtil
import okio.Okio
import android.text.TextUtils
import io.reactivex.ObservableSource
import com.threshold.rxbus2.RxBus
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import com.threshold.updownloader.progress.UploadProgressInterceptor
import com.threshold.updownloader.progress.DownloadProgressInterceptor
import android.app.IntentService
import android.content.Context
import android.os.Environment
import com.threshold.updownloader.event.*
import com.threshold.updownloader.util.StringConverterFactory
import io.reactivex.Observable
import io.reactivex.Observer
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class UpDownService : IntentService(UPDOWN_EVENT_SOURCE) {

//    private val mNotificationManager: NotificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) {
            logger?.error { "intent == null" }
            return
        }
        val taskID = intent.getIntExtra(EXTRA_TASK_ID, -1)
        if (taskID < 0) {
            throw IllegalStateException("TaskID is invalid! Should bigger than -1, cancel this task")
        }

        val serviceType = intent.getIntExtra(EXTRA_SERVICE_TYPE, SERVICE_TYPE_DOWNLOAD)
//        val isShowProgressNotification = intent.getBooleanExtra(EXTRA_IS_SHOW_PROGRESS_NOTIFICATION, false)
        val okHttpClientBuilder = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
        when (serviceType) {
            SERVICE_TYPE_DOWNLOAD -> okHttpClientBuilder.addNetworkInterceptor(
                    DownloadProgressInterceptor(object : DownloadProgressInterceptor.ProgressListener {
                        var lastProgress = 0
                        override fun update(bytesRead: Long, totalContentLength: Long, isDone: Boolean) {
                            val percent = (100 * bytesRead / totalContentLength).toInt()
//                            if (isShowProgressNotification) {
//                            showDownloadProgressNotification(taskID, percent)
//                            }
                            if (Math.abs(percent - lastProgress) >= 1) {
                                lastProgress = percent
                                RxBus.getDefault().post(
                                        UpDownProgressEvent(UPDOWN_EVENT_SOURCE,
                                                UpDownEvent.Type.DOWNLOAD, taskID, percent))
                            }
                        }
                    })
            )
            SERVICE_TYPE_UPLOAD -> okHttpClientBuilder.addNetworkInterceptor(
                    UploadProgressInterceptor(object : UploadProgressInterceptor.ProgressListener {
                        var lastProgress = 0
                        override fun update(bytesWritten: Long, totalContentLength: Long) {
                            val percent = (100 * bytesWritten / totalContentLength).toInt()
//                            if (isShowProgressNotification) {
//                            showUploadProgressNotification(taskID, percent)
//                            }
                            if (Math.abs(percent - lastProgress) >= 1) {
                                lastProgress = percent
                                RxBus.getDefault().post(
                                        UpDownProgressEvent(UPDOWN_EVENT_SOURCE,
                                                UpDownEvent.Type.UPLOAD, taskID, percent))
                            }
                        }
                    }))
            else -> {
                logger?.wtf { "Unknown service type" }
                return
            }
        }
        val okHttpClient = okHttpClientBuilder.build()
        val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .addConverterFactory(StringConverterFactory)
//                .addConverterFactory(
//                        GsonConverterFactory.create(
//                                GsonBuilder()
//                                        .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
//                                        .create()))
                .build()
        when (serviceType) {
            SERVICE_TYPE_DOWNLOAD -> downloadFile(intent, retrofit)
            SERVICE_TYPE_UPLOAD -> uploadFile(intent, retrofit)
            else -> logger?.warn { "unknown service type:$serviceType" }
        }
    }

    private fun uploadFile(intent: Intent, retrofit: Retrofit) {
        val taskID = intent.getIntExtra(EXTRA_TASK_ID, -1)
        val uploadUrl = intent.getStringExtra(EXTRA_UPLOAD_URI)
        val uploadFiles = intent.getStringArrayExtra(EXTRA_UPLOAD_FILES)
//        val isShowProgressNotification = intent.getBooleanExtra(EXTRA_IS_SHOW_PROGRESS_NOTIFICATION, false)
//        val responseType = intent.getSerializableExtra(EXTRA_UPLOAD_RESPONSE_TYPE) as Class<*>
        val fileMap = addUploadFileToMap(*uploadFiles)
        retrofit.create(UpDownApi::class.java)
                .uploadByUrl(uploadUrl, fileMap)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(object : Observer<String> {
                    override fun onSubscribe(disposable: Disposable) {
                        taskDisposables[taskID] = disposable
                    }

                    override fun onNext(response: String) {
                        logger?.debug { "Upload succeed" }
//                        if (isShowProgressNotification) {
//                            showUploadSucceedNotification(taskID)
//                        }
                        RxBus.getDefault().post(UpDownSucceedEvent(
                                UPDOWN_EVENT_SOURCE, UpDownEvent.Type.UPLOAD, taskID, response))
                    }

                    override fun onError(e: Throwable) {
                        logger?.error(e) { "Error on upload" }
//                        if (isShowProgressNotification) {
//                            showUploadFailedNotification(taskID, e)
//                        }
                        RxBus.getDefault().post(UpDownFailedEvent(
                                UPDOWN_EVENT_SOURCE, UpDownEvent.Type.UPLOAD, taskID, e))
                    }

                    override fun onComplete() {
                        taskDisposables.remove(taskID)
                    }
                })
    }

    private fun addUploadFileToMap(vararg uploadFiles: String): Map<String, RequestBody> {
        val fileMap: MutableMap<String, RequestBody> = mutableMapOf()
        for (filePath in uploadFiles) {
            val file = File(filePath)
            if (!file.exists()) {
                logger?.warn { "File not exists：$filePath" }
                continue
            }
            if (file.isDirectory) {
                logger?.warn { "Can't upload folder：$filePath" }
                continue
            }
            val requestBody = RequestBody.create(MultipartBody.FORM, file)
            fileMap.put("file\"; filename=\" ${file.name}", requestBody)
        }
        return fileMap
    }

    private fun downloadFile(intent: Intent, retrofit: Retrofit) {
        val taskID = intent.getIntExtra(EXTRA_TASK_ID, -1)
        val downloadUrl = intent.getStringExtra(EXTRA_DOWNLOAD_URI)
//        val isShowProgressNotification = intent.getBooleanExtra(EXTRA_IS_SHOW_PROGRESS_NOTIFICATION, false)
        retrofit.create(UpDownApi::class.java)
                .downloadByUrl(downloadUrl)
                .flatMap {
                    saveResponseBodyToFile(intent, it)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(object : Observer<File> {
                    override fun onSubscribe(disposable: Disposable) {
                        taskDisposables[taskID] = disposable
                    }

                    override fun onNext(file: File) {
                        logger?.debug { "Download succeed $file" }
//                        if (isShowProgressNotification) {
//                            showDownloadCompleteNotification(taskID, file)
//                        }
                        RxBus.getDefault().post(UpDownSucceedEvent(
                                UPDOWN_EVENT_SOURCE,
                                UpDownEvent.Type.DOWNLOAD, taskID, file))
                    }

                    override fun onError(e: Throwable) {
                        logger?.error(e) { "Download failed" }
//                        if (isShowProgressNotification) {
//                            showDownloadFailedNotification(taskID, e)
//                        }
                        RxBus.getDefault().post(UpDownFailedEvent(
                                UPDOWN_EVENT_SOURCE,
                                UpDownEvent.Type.DOWNLOAD, taskID, e))
                    }

                    override fun onComplete() {
                        taskDisposables.remove(taskID)
                    }
                })
    }


    private fun saveResponseBodyToFile(intent: Intent, response: Response<ResponseBody>): ObservableSource<File> {
        val taskID = intent.getIntExtra(EXTRA_TASK_ID, -1)
        return Observable.create({ emitter ->
            try {
                val contentDispositionHeader = response.headers().get("Content-Disposition")
                val filename = generateDownloadFileNameFromHeader(intent, contentDispositionHeader)
                if (TextUtils.isEmpty(filename)) {
                    throw IllegalStateException("Can't generate filename for this download task")
                }
//                Logger.d("待下载文件的文件名：%s", filename)
                var saveDirectory = intent.getStringExtra(EXTRA_DOWNLOAD_SAVE_PATH)
                if (TextUtils.isEmpty(saveDirectory)) {
                    saveDirectory = Environment.getExternalStorageDirectory().path +
                            File.separator + Environment.DIRECTORY_DOWNLOADS
                } else if (!FileUtil.dirExists(saveDirectory)) {
                    emitter.onError(Throwable("Create download directory failed：" + saveDirectory))
                    return@create
                }
                val destinationFile = File(saveDirectory + File.separator + filename)
                if (destinationFile.exists()) {
                    val isOverrideIfExists = intent.getBooleanExtra(EXTRA_DOWNLOAD_IS_OVERRIDE, true)
                    if (destinationFile.length() != 0L && !isOverrideIfExists) { //file exits and we don't need override it
                        emitter.onNext(destinationFile)
                        emitter.onComplete()
                        return@create
                    }
                    if (!destinationFile.delete()) {
                        emitter.onError(Throwable("The file is already exists, we tried delete it but failed:$destinationFile"))
                        return@create
                    }
                }
                if (!destinationFile.createNewFile()) {
                    emitter.onError(Throwable("Create file failed:$destinationFile"))
                    return@create
                }
                val body = response.body()
                if (body != null) {
                    val bufferedSink = Okio.buffer(Okio.sink(destinationFile))
                    bufferedSink.writeAll(body.source())
                    bufferedSink.close()
                } else {
                    emitter.onError(Throwable("Response body == null, Can't write it to file"))
                    return@create
                }
                emitter.onNext(destinationFile)
                emitter.onComplete()
            } catch (e: IOException) {
                val disposable = taskDisposables[taskID]
                if (disposable != null && disposable.isDisposed) {
                    //eat it
                    e.printStackTrace()
                } else {
                    emitter.onError(e)
                }
            }
        })
    }

    private fun generateDownloadFileNameFromHeader(intent: Intent, contentDispositionHeader: String?): String {
        var filename: String? = null
        if (contentDispositionHeader != null) {
            filename = contentDispositionHeader.replace("attachment; filename = ", "")
                    .replace("attachment; filename=", "").replace("attachment;filename=", "")
                    .replace("\"", "")
        }
        if (filename.isNullOrEmpty()) {
            val downloadUrl = intent.getStringExtra(EXTRA_DOWNLOAD_URI)
            val lastSlashIndex = downloadUrl.lastIndexOf("/")
            filename = if (lastSlashIndex > -1 && lastSlashIndex + 1 < downloadUrl.length) {
                downloadUrl.substring(lastSlashIndex + 1)
            } else {
                downloadUrl
            }
            val lastEqualSymbolIndex = filename!!.lastIndexOf("=")
            if (lastEqualSymbolIndex > -1 && lastEqualSymbolIndex + 1 < filename.length) {
                filename = filename.substring(lastEqualSymbolIndex + 1)
            }
            filename = filename.replace("*", "").replace("?", "").replace("/", "")
        }
        return filename!!
    }

//    private fun showNoPermissionNotification(taskID: Int) {
//        val notification = NotificationCompat.Builder(applicationContext)
//                .setSmallIcon(R.drawable.mipush_small_notification)
//                .setContentTitle(getString(R.string.no_permissions))
//                .setContentText(getString(R.string.no_permissions_content))
//                .setAutoCancel(true)
//                .setColor(resources.getColor(R.color.colorPrimary))
//                .build()
//        mNotificationManager.notify(taskID, notification)
//    }
//
//    private fun showDownloadProgressNotification(taskID: Int, percent: Int) {
//        val notification = NotificationCompat.Builder(applicationContext)
//                .setSmallIcon(R.drawable.mipush_small_notification)
//                .setContentTitle(getString(R.string.downloading))
//                .setProgress(100, percent, false)
//                .setOngoing(true)
//                .setColor(resources.getColor(R.color.colorPrimary))
//                .build()
//        mNotificationManager.notify(taskID, notification)
//    }
//
//    private fun showDownloadCompleteNotification(taskID: Int, downloadedFile: File) {
//        val resultIntent = Intent(Intent.ACTION_VIEW)
//        val fileMimeType = FileUtil.getMimeTypeFromFileUri(downloadedFile.name)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            resultIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
//            val contentUri = FileProvider.getUriForFile(getContext(), Constants.NOUGAT_FILE_PROVIDER, downloadedFile)
//            resultIntent.setDataAndType(contentUri, fileMimeType)
//        } else {
//            resultIntent.setDataAndType(Uri.fromFile(downloadedFile), fileMimeType)
//        }
//        mNotificationManager.cancel(taskID)
//        val pendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)
//        val builder = NotificationCompat.Builder(applicationContext)
//        val notification = builder
//                .setSmallIcon(R.drawable.mipush_small_notification)
//                .setContentTitle(resources.getString(R.string.download_complete))
//                .setContentText(getString(R.string.format_click_to_open, downloadedFile.getName()))
//                .setStyle(NotificationCompat.BigTextStyle(builder).bigText(getString(R.string.format_click_to_open, downloadedFile.getPath())))
//                .setContentIntent(pendingIntent)
//                .setAutoCancel(true)
//                .setColor(resources.getColor(R.color.colorPrimary))
//                .build()
//        mNotificationManager.notify(taskID, notification)
//    }
//
//    private fun showDownloadFailedNotification(taskID: Int, e: Throwable) {
//        mNotificationManager.cancel(taskID)
//        val notification = NotificationCompat.Builder(applicationContext)
//                .setSmallIcon(R.drawable.mipush_small_notification)
//                .setContentTitle(getString(R.string.download_failed))
//                .setContentText(ErrorDeterminer.getError(e))
//                .setAutoCancel(true)
//                .setColor(resources.getColor(R.color.colorPrimary))
//                .build()
//        mNotificationManager.notify(taskID, notification)
//    }
//
//    private fun showUploadProgressNotification(taskID: Int, percent: Int) {
//        val notification = NotificationCompat.Builder(applicationContext)
//                .setSmallIcon(R.drawable.mipush_small_notification)
//                .setContentTitle(getString(R.string.uploading))
//                .setProgress(100, percent, false)
//                .setOngoing(true)
//                .setColor(resources.getColor(R.color.colorPrimary))
//                .build()
//        mNotificationManager.notify(taskID, notification)
//    }
//
//    private fun showUploadSucceedNotification(taskID: Int) {
//        mNotificationManager.cancel(taskID)
//        val notification = NotificationCompat.Builder(applicationContext)
//                .setSmallIcon(R.drawable.mipush_small_notification)
//                .setContentTitle(getString(R.string.upload_complete))
//                .setContentText(getString(R.string.upload_complete_content))
//                .setAutoCancel(true)
//                .setColor(resources.getColor(R.color.colorPrimary))
//                .build()
//        mNotificationManager.notify(taskID, notification)
//    }
//
//    private fun showUploadFailedNotification(taskID: Int, e: Throwable) {
//        mNotificationManager.cancel(taskID)
//        val notification = NotificationCompat.Builder(applicationContext)
//                .setSmallIcon(R.drawable.mipush_small_notification)
//                .setContentTitle(getString(R.string.upload_failed))
//                .setContentText(e.message)
//                .setAutoCancel(true)
//                .setColor(resources.getColor(R.color.colorPrimary))
//                .build()
//        mNotificationManager.notify(taskID, notification)
//    }

    companion object {
        var BASE_URL = "https://www.baidu.com/"
        var logger: UpDownLogger? = null

        private val UPDOWN_EVENT_SOURCE: String = UpDownService::class.java.name

        private val taskDisposables = mutableMapOf<Int, Disposable>()

        fun getTaskDisposable(taskId: Int): Disposable? = taskDisposables[taskId]

        fun removeAllTask() {
            taskDisposables.forEach { (_, disposable) ->
                if (!disposable.isDisposed) {
                    disposable.dispose()
                }
            }
        }

        @JvmOverloads
        fun startServiceForDownload(context: Context,
                                    taskId: Int,
                //                                    isShowProgressbarNotification: Boolean = false,
                                    isOverrideIfExists: Boolean,
                                    downloadUri: String,
                                    savePath: String? = null) {
            val intent = Intent(context, UpDownService::class.java)
            intent.putExtra(EXTRA_SERVICE_TYPE, SERVICE_TYPE_DOWNLOAD)
            intent.putExtra(EXTRA_TASK_ID, taskId)
//            intent.putExtra(EXTRA_IS_SHOW_PROGRESS_NOTIFICATION, isShowProgressbarNotification)
            intent.putExtra(EXTRA_DOWNLOAD_IS_OVERRIDE, isOverrideIfExists)
            intent.putExtra(EXTRA_DOWNLOAD_URI, downloadUri)
            intent.putExtra(EXTRA_DOWNLOAD_SAVE_PATH, savePath)
            context.startService(intent)
        }

        fun startServiceForUpload(context: Context,
                                  taskId: Int,
                //                                  isShowProgressbarNotification: Boolean = false,
//                                  responseType:Class<T>,
                                  uploadUri: String,
                                  vararg uploadFiles: String) {
            val intent = Intent(context, UpDownService::class.java)
            intent.putExtra(EXTRA_SERVICE_TYPE, SERVICE_TYPE_UPLOAD)
            intent.putExtra(EXTRA_TASK_ID, taskId)
//            intent.putExtra(EXTRA_IS_SHOW_PROGRESS_NOTIFICATION, isShowProgressbarNotification)
            intent.putExtra(EXTRA_UPLOAD_URI, uploadUri)
            intent.putExtra(EXTRA_UPLOAD_FILES, uploadFiles)
//            intent.putExtra(EXTRA_UPLOAD_RESPONSE_TYPE,responseType)
            context.startService(intent)
        }

//        private val PERMISSIONS = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        private val EXTRA_SERVICE_TYPE = "EXTRA_SERVICE_TYPE"
        private val SERVICE_TYPE_DOWNLOAD = 0
        private val SERVICE_TYPE_UPLOAD = 1
        private val EXTRA_TASK_ID = "TASK_ID"
        private val EXTRA_IS_SHOW_PROGRESS_NOTIFICATION = "IS_SHOW_PROGRESS_NOTIFICATION"

        private val EXTRA_DOWNLOAD_URI = "DOWNLOAD_URL"
        private val EXTRA_DOWNLOAD_IS_OVERRIDE = "DOWNLOAD_ARG_IS_OVERRIDE"
        private val EXTRA_DOWNLOAD_SAVE_PATH = "DOWNLOAD_SAVE_PATH"

        private val EXTRA_UPLOAD_URI = "UPLOAD_URL"
        private val EXTRA_UPLOAD_FILES = "UPLOAD_FILES"
//        private val EXTRA_UPLOAD_RESPONSE_TYPE = "UPLOAD_RESPONSE_TYPE"
    }
}