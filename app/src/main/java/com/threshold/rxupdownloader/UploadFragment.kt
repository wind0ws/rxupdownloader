package com.threshold.rxupdownloader

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.threshold.logger.PrettyLogger
import com.threshold.logger.debug
import com.threshold.logger.error
import com.threshold.logger.info
import com.threshold.rxbus2.RxBus
import com.threshold.updownloader.UpDownService
import com.threshold.updownloader.event.*
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_upload.*
import java.io.File

class UploadFragment : Fragment(), View.OnClickListener, PrettyLogger {

    private var rxUpDownSubscriber: Disposable? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater!!.inflate(R.layout.fragment_upload, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnUpload.setOnClickListener(this)
        btnCancelUpload.setOnClickListener(this)
        initListenRxUpDownEvent()
    }

    private fun initListenRxUpDownEvent() {
        rxUpDownSubscriber = RxBus.getDefault()
                .ofType(UpDownEvent::class.java)
                .filter{
                    it.type == UpDownEvent.Type.UPLOAD
                }
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    when (it) {
                        is UpDownProgressEvent -> {
                            info { "Current progress: ${it.percent}" }
                        }
                        is UpDownFinishedEvent -> {
                            when(it){
                                is UpDownSucceedEvent -> debug { "Upload succeed: $it" }
                                is UpDownFailedEvent -> debug { "Upload failed: $it" }
                            }
                        }
                    }
                }
    }

    override fun onClick(view: View?) {
        view?.let {
            when (it) {
                btnUpload -> uploadFile()
                btnCancelUpload -> cancelUpload()
                else -> debug { "UnHandled view:[$it] clicked event" }
            }
        }
    }

    private fun cancelUpload() {
        UpDownService.getTaskDisposable(1001)?.apply {
            if (!isDisposed) {
                dispose()
                debug { "cancel upload succeed" }
            } else {
                debug { "upload task already complete, can't cancel" }
            }
        }

    }

    private fun uploadFile() {
        debug { "Prepare upload file" }
        val filePath = Environment.getExternalStorageDirectory().absolutePath + File.separator + Environment.DIRECTORY_DOWNLOADS + File.separator + "BaiduNetdisk_5.6.3.exe"
        val uploadFile = File(filePath)
        if (!uploadFile.exists()) {
            error { "$uploadFile is not exists" }
            return
        }
        UpDownService.BASE_URL = "http://office.china-xueche.com:8383/express/"
        UpDownService.startServiceForUpload(context,1001,"files",filePath)
    }

    override fun onDestroyView() {
        rxUpDownSubscriber?.dispose()
        super.onDestroyView()
    }


    companion object {

        fun newInstance(param1: String, param2: String): UploadFragment {
            val fragment = UploadFragment()
            return fragment
        }
    }
}
