package com.raxdenstudios.platform.ui.mapper

import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.android.provider.StringProvider
import com.raxdenstudios.platform.ui.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.threeten.bp.Duration

internal class DurationModelMapperImplTest {

    private val stringProvider: StringProvider = mockk()
    private val mapper = DurationModelMapperImpl(stringProvider)

    @Test
    fun `transform returns hours and minutes when more than one hour with minutes`() {
        every { stringProvider.getString(R.string.hours_minutes, 2L, 30) } returns "2h 30m"

        val result = mapper.transform(Duration.ofHours(2).plusMinutes(30))

        assertThat(result).isEqualTo("2h 30m")
    }

    @Test
    fun `transform returns hours when more than one hour without minutes`() {
        every { stringProvider.getString(R.string.hours, 3L) } returns "3h"

        val result = mapper.transform(Duration.ofHours(3))

        assertThat(result).isEqualTo("3h")
    }

    @Test
    fun `transform returns hour and minutes when exactly one hour with minutes`() {
        every { stringProvider.getString(R.string.hour_minutes, 1L, 15) } returns "1h 15m"

        val result = mapper.transform(Duration.ofHours(1).plusMinutes(15))

        assertThat(result).isEqualTo("1h 15m")
    }

    @Test
    fun `transform returns hour when exactly one hour`() {
        every { stringProvider.getString(R.string.hour, 1L) } returns "1h"

        val result = mapper.transform(Duration.ofHours(1))

        assertThat(result).isEqualTo("1h")
    }

    @Test
    fun `transform returns minutes when more than one minute`() {
        every { stringProvider.getString(R.string.minutes, 5) } returns "5m"

        val result = mapper.transform(Duration.ofMinutes(5))

        assertThat(result).isEqualTo("5m")
    }

    @Test
    fun `transform returns empty for one minute or less`() {
        assertThat(mapper.transform(Duration.ofMinutes(1))).isEmpty()
        assertThat(mapper.transform(Duration.ofSeconds(30))).isEmpty()
    }
}
