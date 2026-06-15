package com.raxdenstudios.platform.ui.mapper

import com.raxdenstudios.commons.android.provider.StringProvider
import com.raxdenstudios.commons.core.util.DataMapper
import com.raxdenstudios.platform.ui.R
import org.threeten.bp.Duration

interface DurationModelMapper {
    fun transform(source: Duration): String
}

class DurationModelMapperImpl(
    private val stringProvider: StringProvider,
) : DataMapper<Duration, String>(), DurationModelMapper {

    override fun transform(source: Duration): String = when {
        source.toHours() > 1 && source.toMinutesPart() > 0 ->
            stringProvider.getString(
                R.string.hours_minutes,
                source.toHours(),
                source.toMinutesPart()
            )

        source.toHours() > 1 ->
            stringProvider.getString(R.string.hours, source.toHours())

        source.toHours() >= 1 && source.toMinutesPart() > 0 ->
            stringProvider.getString(
                R.string.hour_minutes,
                source.toHours(),
                source.toMinutesPart()
            )

        source.toHours() >= 1 ->
            stringProvider.getString(R.string.hour, source.toHours())

        source.toMinutes() > 1 ->
            stringProvider.getString(R.string.minutes, source.toMinutesPart())

        else -> ""
    }
}
