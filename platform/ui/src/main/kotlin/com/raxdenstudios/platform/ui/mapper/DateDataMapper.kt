package com.raxdenstudios.platform.ui.mapper

import com.raxdenstudios.commons.time.ext.toHourMinutes
import com.raxdenstudios.commons.time.ext.toLocalizedString
import java.time.LocalDate
import java.time.LocalDateTime

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
    ): String = source.toLocalizedString()

    override fun toHourMinutes(
        source: LocalDateTime,
    ): String = source.toHourMinutes()
}
