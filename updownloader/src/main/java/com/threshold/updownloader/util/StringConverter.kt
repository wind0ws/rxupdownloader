package com.threshold.updownloader.util

import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

object StringConverter : Converter<ResponseBody, String> {
    override fun convert(value: ResponseBody?): String {
        return if (value != null) value.string() else throw RuntimeException("unable convert $value to string")
    }
}

object StringConverterFactory : Converter.Factory() {
    override fun responseBodyConverter(type: Type?, annotations: Array<out Annotation>?, retrofit: Retrofit?): Converter<ResponseBody, *>? {
        if (type == String::class.java) {
            return StringConverter
        }
        return super.responseBodyConverter(type, annotations, retrofit)
    }
}