# RxUpDownloader
[![](https://jitpack.io/v/wind0ws/rxupdownloader.svg)](https://jitpack.io/#wind0ws/rxupdownloader)

>中文说明，请点[这里](http://www.jianshu.com/p/430e82dc0cb7)查看.

>RxUpDownloader use RxJava2+Retrofit2+OkHttp3 to download/upload files through IntentService. It also support progress callback, task status and cancel task manually. It is very easy to use.

## [Getting started](https://jitpack.io/#wind0ws/rxupdownloader)
The first step is to include RxUpDownloader into your project, for example, as a Gradle compile dependency:
* Because of using [jitpack.io](https://jitpack.io/), so we need add the jitpack.io repository in your root project gradle:
```groovy
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
* and then add rxupdownloader dependency into your module gradle:
```groovy
dependencies {
			...
	        implemention 'com.github.wind0ws:rxupdownloader:x.y.z'
	}
```
> If your gradle version below 3.0, just replace `implemention` keyword to `compile`.
> Note: you need replace `x.y.z` to the correct version, you can find it on [release page](https://github.com/wind0ws/rxupdownloader/releases)

All right, we are done for integration.

## Hello,World.
RxUpDownloader need add [UpDownService](https://github.com/wind0ws/rxupdownloader/blob/master/updownloader/src/main/java/com/threshold/updownloader/UpDownService.kt) to your manifest.xml, because we use this service for download/upload files.
Here is a example:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.threshold.rxupdownloader">
    <!-- we need permission to read/write external storage and internet access -->
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>

    <application
        android:name=".App"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/AppTheme">

        <!-- Here is our service for download/upload files -->
        <service android:name="com.threshold.updownloader.UpDownService"/>

        <activity
            android:name=".MainActivity"
            android:label="@string/app_name"
            android:theme="@style/AppTheme.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>

</manifest>
```
>Note: Start from android 6.0, android add runtime permissions, this library need write external storage, so you should request and get `WRITE_EXTERNAL_STORAGE` permission before upload/download files.

Now we have ability to upload/download files:
* Download files:
```Kotlin
 UpDownService.startServiceForDownload(context,1002,true,
                    "http://issuecdn.baidupcs.com/issue/netdisk/yunguanjia/BaiduNetdisk_5.6.3.exe")
```
* Upload files:
```Kotlin
UpDownService.startServiceForUpload(context,1001,
                                  "http://your.restful.website/files","/sdcard/1.txt")
```
>Parameter value "1001" and "1002" is taskId(you can use whatever integer as you want), which you can get task disposable by `UpDownService.getTaskDisposable(taskId)`, and you can use this disposable to cancel task.
>All download/upload task will execute at background thread.

### Listen upload/download status：
>We use [RxBus2](https://github.com/wind0ws/rxbus2) for emit/listen progress/succeed/failed event.

Here is a example:
```Kotlin
          RxBus.getDefault()
                .ofType(UpDownEvent::class.java)
                .filter {
                    //   it.taskId == 1002
                    it.type == UpDownEvent.Type.DOWNLOAD
                }
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    when (it) {
                        is UpDownProgressEvent -> {
                            info { "Current progress: ${it.percent}" }
                        }
                        is UpDownSucceedEvent -> debug { "Download succeed: $it" }
                        is UpDownFailedEvent -> debug { "Download failed: $it" }
                    }
                }
```
>Note: progress is not always usable:  It depend on your server.  If your server return `content-type` is `application/octet-stream`, we can't get length of content, so we can't calculate percentage of current progress.
The default content length is -1, so if you find percentage below **ZERO**, that means this percentage of progress is not reliable, you shouldn't use it.
Usually, percentage of progress range is 0~100.
`UpDownSucceedEvent`  `UpDownFailedEvent ` event is always reliable.
>>* `UpDownSucceedEvent` means upload/download task **succeed**.
>>* `UpDownFailedEvent ` means upload/download task **failed**.

### Want demo?
See app module in this repo.
>If you have any question, run app module first before ask, that demo might help you. Any useful pull-request are welcome!