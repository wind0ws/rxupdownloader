package com.threshold.rxupdownloader

import android.app.Application
import com.threshold.logger.*
import com.threshold.logger.adapter.AndroidLogAdapter
import com.threshold.logger.printer.LoggerPrinter
import com.threshold.logger.strategy.PrettyFormatStrategy
import com.threshold.rxbus2.RxBus
import com.threshold.updownloader.UpDownLogger
import com.threshold.updownloader.UpDownService
import io.reactivex.android.schedulers.AndroidSchedulers

class App : Application(),PrettyLogger {
    override fun onCreate() {
        super.onCreate()
        initPrettyLogger()
        RxBus.setMainScheduler(AndroidSchedulers.mainThread())
//        initUpDownLogger()
    }

    private fun initUpDownLogger() {
        UpDownService.logger = object: UpDownLogger  {
            override fun debug(throwable: Throwable?, message: () -> Any?) {
                this@App.debug(throwable, message)
            }

            override fun error(throwable: Throwable?, message: () -> Any?) {
                this@App.error(throwable,message)
            }

            override fun warn(throwable: Throwable?, message: () -> Any?) {
                this@App.warn(throwable, message)
            }

            override fun info(message: () -> Any?) {
                this@App.info(message)
            }

            override fun verbose(message: () -> Any?) {
                this@App.verbose(message)
            }

            override fun wtf(throwable: Throwable?, message: () -> Any?) {
                this@App.wtf(throwable, message)
            }
        }
    }

    private fun initPrettyLogger() {
        val prettyFormatStrategy = PrettyFormatStrategy.build {
            tag = "RxUpDown"
            showThreadInfo = true
        }
        addAdapter(object: AndroidLogAdapter(prettyFormatStrategy){
            override fun isLoggable(priority: Int, tag: String?): Boolean {
                return BuildConfig.DEBUG && super.isLoggable(priority, tag)
            }
        })
    }
}