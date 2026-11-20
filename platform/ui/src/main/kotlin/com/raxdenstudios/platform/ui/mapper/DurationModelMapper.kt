package com.raxdenstudios.platform.ui.mapper

import com.raxdenstudios.commons.android.provider.StringProvider
import com.raxdenstudios.commons.core.util.DataMapper
import com.raxdenstudios.platform.ui.R
import java.time.Duration

interface DurationModelMapper {
    fun transform(
        source: Duration,
    ): String
}

class DurationModelMapperImpl(
    private val stringProvider: StringProvider,
) : DataMapper<Duration, String>(),
    DurationModelMapper {

    override fun transform(
        source: Duration,
    ): String = when {
        source.toHours() > 1 && source.minutesPart() > 0 ->
            stringProvider.getString(
                R.string.hours_minutes,
                source.toHours(),
                source.minutesPart(),
            )

        source.toHours() > 1 ->
            stringProvider.getString(R.string.hours, source.toHours())

        source.toHours() >= 1 && source.minutesPart() > 0 ->
            stringProvider.getString(
                R.string.hour_minutes,
                source.toHours(),
                source.minutesPart(),
            )

        source.toHours() >= 1 ->
            stringProvider.getString(R.string.hour, source.toHours())

        source.toMinutes() > 1 ->
            stringProvider.getString(R.string.minutes, source.minutesPart())

        else -> ""
    }
}

private const val MINUTES_PER_HOUR = 60

private fun Duration.minutesPart(): Int = (toMinutes() % MINUTES_PER_HOUR).toInt()
