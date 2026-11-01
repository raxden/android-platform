package com.raxdenstudios.platform.ui.mapper

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale

internal class CurrencyModelMapperImplTest {

    private val mapper = CurrencyModelMapperImpl()
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
    fun `transform returns empty string for zero`() {
        assertThat(mapper.transform(0.0)).isEmpty()
    }

    @Test
    fun `transform formats amount as USD`() {
        assertThat(mapper.transform(1234.5)).isEqualTo("$1,234.50")
    }
}
