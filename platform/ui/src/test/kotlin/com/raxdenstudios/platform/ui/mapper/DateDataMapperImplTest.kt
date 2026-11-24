package com.raxdenstudios.platform.ui.mapper

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
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
    fun `toHourMinutes formats time as HH mm`() {
        val result = mapper.toHourMinutes(LocalDateTime.of(2024, 1, 15, 9, 5))

        assertThat(result).isEqualTo("09:05")
    }
}
