package com.raxdenstudios.platform.core.ui.mapper

import com.raxdenstudios.commons.core.util.DataMapper
import java.util.Locale

interface LanguageModelMapper {
    fun transform(source: Locale): String
}

class LanguageModelMapperImpl : DataMapper<Locale, String>(), LanguageModelMapper {

    override fun transform(source: Locale): String {
        return source.getDisplayLanguage(Locale.getDefault())
    }
}
