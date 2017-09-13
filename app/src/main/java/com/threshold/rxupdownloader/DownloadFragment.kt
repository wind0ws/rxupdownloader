package com.threshold.rxupdownloader

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.threshold.logger.PrettyLogger
import com.threshold.logger.debug
import com.threshold.logger.info
import com.threshold.rxbus2.RxBus
import com.threshold.updownloader.UpDownService
import com.threshold.updownloader.event.*
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_download.*

class DownloadFragment : Fragment(), PrettyLogger, View.OnClickListener {

    private var rxUpDownSubscriber: Disposable? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater!!.inflate(R.layout.fragment_download, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListenRxUpDownEvent()
        btnDownload.setOnClickListener(this)
        btnCancelDownload.setOnClickListener(this)
    }

    private fun initListenRxUpDownEvent() {
        rxUpDownSubscriber = RxBus.getDefault()
                .ofType(UpDownEvent::class.java)
                .filter {
                    //                    it.taskId == 1002
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
    }

    override fun onClick(view: View?) {
        if (view == btnDownload) {
            UpDownService.startServiceForDownload(context, 1002, true,
                    "http://issuecdn.baidupcs.com/issue/netdisk/yunguanjia/BaiduNetdisk_5.6.3.exe")
            //files/private/alipay.jpg
        } else if (view == btnCancelDownload) {
            UpDownService.getTaskDisposable(1002)?.apply {
                if (!isDisposed) {
                    dispose()
                    debug { "Cancel download task complete" }
                } else {
                    debug { "Download task already complete, can't cancel" }
                }
            }
        }
    }

    companion object {

        fun newInstance(param1: String, param2: String): DownloadFragment {
            val fragment = DownloadFragment()
            return fragment
        }
    }
}
