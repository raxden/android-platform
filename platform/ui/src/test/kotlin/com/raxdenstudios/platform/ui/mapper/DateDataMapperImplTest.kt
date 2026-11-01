package com.raxdenstudios.platform.ui.mapper

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import java.util.Locale

internal class DateDataMapperImplTest {

    private val mapper = DateDataMapperImpl()
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
    fun `toString formats date with long style`() {
        val result = mapper.toString(LocalDate.of(2024, 1, 15))

        assertThat(result).isEqualTo("January 15, 2024")
    }

    @Test
    fun `toHourMinutes formats time as HH mm`() {
        val result = mapper.toHourMinutes(LocalDateTime.of(2024, 1, 15, 9, 5))

        assertThat(result).isEqualTo("09:05")
    }
}
