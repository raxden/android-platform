package com.raxdenstudios.platform.ui.mapper

import com.raxdenstudios.commons.threetenabp.ext.toFormat
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle

interface DateDataMapper {
    fun toString(source: LocalDate): String
    fun toHourMinutes(source: LocalDateTime): String
}

class DateDataMapperImpl : DateDataMapper {

    override fun toString(source: LocalDate): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(source)

    override fun toHourMinutes(source: LocalDateTime): String =
        source.toFormat("HH:mm")
}
