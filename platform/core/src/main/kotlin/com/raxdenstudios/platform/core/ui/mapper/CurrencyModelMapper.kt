package com.raxdenstudios.platform.core.ui.mapper

import com.raxdenstudios.commons.core.util.DataMapper
import java.text.NumberFormat
import java.util.Currency

interface CurrencyModelMapper {
    fun transform(source: Double): String
}

class CurrencyModelMapperImpl : DataMapper<Double, String>(), CurrencyModelMapper {

    override fun transform(source: Double): String {
        if (source == 0.0) return ""
        val currencyFormat = NumberFormat.getCurrencyInstance()
        currencyFormat.currency = Currency.getInstance("USD")
        return currencyFormat.format(source)
    }
}
