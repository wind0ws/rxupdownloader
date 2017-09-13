package com.threshold.updownloader.event

import java.util.*

abstract class UpDownEvent(source: Any, val type: Type, val taskId: Int) : EventObject(source) {

    enum class Type(val type: Int) {
        DOWNLOAD(0),
        UPLOAD(1)
    }
}

class UpDownProgressEvent
/**
 * Constructs a prototypical Event.
 *
 * @param source The object on which the Event initially occurred.
 * @param taskId The id of this UpDown task
 * @param percent percentage progress(0~100) of this task. Note: if we don't know the length of content, which will cause percentage below ZERO.
 * Be careful if you use this percentage to update download/upload progress.especially in "octet-stream"
 * @throws IllegalArgumentException if source is null.
 */
(source: Any, type: Type, taskId: Int, val percent: Int) : UpDownEvent(source, type, taskId) {
    override fun toString(): String {
        return "UpDownProgressEvent(taskId=$taskId, percent=$percent)"
    }
}

abstract class UpDownFinishedEvent
/**
 * Constructs a prototypical Event.
 *
 * @param source The object on which the Event initially occurred.
 * @param type Up or Download
 * @param taskId @throws IllegalArgumentException if source is null.
 * @param succeedResult When is isSucceed is true,which means up or down is succeed. this object is result.
 * @param error When isSucceed is false,which means up or down is failed, this throwable is detail.
 */
constructor(source: Any, type: Type,
            taskId: Int, val isSucceed: Boolean,
            val succeedResult: Any? = null,
            val error: Throwable? = null) : UpDownEvent(source, type, taskId)

class UpDownSucceedEvent
constructor(source: Any,
            type: UpDownEvent.Type,
            taskId: Int,
            succeedResult: Any) : UpDownFinishedEvent(source, type, taskId, true, succeedResult) {
    override fun toString(): String {
        return "UpDownSucceedEvent(succeedResult=$succeedResult)"
    }
}

class UpDownFailedEvent
constructor(source: Any, type: UpDownEvent.Type,
            taskId: Int,
            error: Throwable) : UpDownFinishedEvent(source, type, taskId, false, null, error) {
    override fun toString(): String {
        return "UpDownFailedEvent(error=$error)"
    }
}