package com.threshold.updownloader

interface UpDownLogger {

    fun debug(throwable: Throwable? = null, message: () -> Any?)

    fun error(throwable: Throwable? = null, message: () -> Any?)

    fun warn(throwable: Throwable? = null, message: () -> Any?)

    fun info(message: () -> Any?)

    fun verbose(message: () -> Any?)

    fun wtf(throwable: Throwable? = null, message: () -> Any?)
}