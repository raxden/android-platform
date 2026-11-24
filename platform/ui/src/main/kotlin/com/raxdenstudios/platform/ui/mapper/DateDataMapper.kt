package com.raxdenstudios.platform.ui.mapper

import com.raxdenstudios.commons.time.ext.toHourMinutes
import java.time.LocalDateTime

interface DateDataMapper {
    fun toHourMinutes(
        source: LocalDateTime,
    ): String
}

class DateDataMapperImpl : DateDataMapper {

    override fun toHourMinutes(
        source: LocalDateTime,
    ): String = source.toHourMinutes()
}
