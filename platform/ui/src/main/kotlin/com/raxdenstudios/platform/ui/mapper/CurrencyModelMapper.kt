package com.raxdenstudios.platform.ui.mapper

import com.raxdenstudios.commons.core.ext.toCurrencyString
import com.raxdenstudios.commons.core.util.DataMapper
import java.util.Currency

interface CurrencyModelMapper {
    fun transform(
        source: Double,
    ): String
}

class CurrencyModelMapperImpl :
    DataMapper<Double, String>(),
    CurrencyModelMapper {

    override fun transform(
        source: Double,
    ): String {
        if (source == 0.0) return ""
        return source.toCurrencyString(Currency.getInstance("USD"))
    }
}
