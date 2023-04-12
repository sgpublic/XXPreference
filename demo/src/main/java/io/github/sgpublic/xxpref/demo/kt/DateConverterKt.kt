package io.github.sgpublic.xxpref.demo.kt

import io.github.sgpublic.xxpref.annotations.PrefConverter
import io.github.sgpublic.xxpref.interfaces.Converter
import java.util.*

@PrefConverter
class DateConverterKt: Converter<Date, Long> {
    override fun toPreference(origin: Date): Long {
        return origin.time
    }

    override fun fromPreference(target: Long): Date {
        return Date(target)
    }
}