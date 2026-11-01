package com.raxdenstudios.platform.ui.mapper

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale

internal class LanguageModelMapperImplTest {

    private val mapper = LanguageModelMapperImpl()
    private lateinit var defaultLocale: Locale

    @Before
    fun setUp() {
        defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `transform returns display language in default locale`() {
        assertThat(mapper.transform(Locale.GERMAN)).isEqualTo("German")
        assertThat(mapper.transform(Locale.forLanguageTag("es"))).isEqualTo("Spanish")
    }
}
