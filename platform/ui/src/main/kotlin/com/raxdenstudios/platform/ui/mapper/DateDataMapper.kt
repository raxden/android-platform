package com.raxdenstudios.platform.ui.mapper

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

interface DateDataMapper {
    fun toString(
        source: LocalDate,
    ): String
    fun toHourMinutes(
        source: LocalDateTime,
    ): String
}

class DateDataMapperImpl : DateDataMapper {

    override fun toString(
        source: LocalDate,
    ): String = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(source)

    override fun toHourMinutes(
        source: LocalDateTime,
    ): String = source.format(DateTimeFormatter.ofPattern("HH:mm"))
}
