package com.raxdenstudios.platform.ui.composable

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.android.compose.model.ImageResource
import org.junit.Test

internal class EmptyStateTest {

    private val tint = Color.Red
    private val vector = ImageResource.Vector(Icons.Default.Close)
    private val drawable = ImageResource.Drawable(1)

    @Test
    fun `Given vector and tintVectors When imageTint Then returns tint`() {
        assertThat(imageTint(vector, tintVectors = true, tint = tint)).isEqualTo(tint)
    }

    @Test
    fun `Given vector and no tintVectors When imageTint Then returns null`() {
        assertThat(imageTint(vector, tintVectors = false, tint = tint)).isNull()
    }

    @Test
    fun `Given drawable and tintVectors When imageTint Then returns null`() {
        assertThat(imageTint(drawable, tintVectors = true, tint = tint)).isNull()
    }

    @Test
    fun `Given drawable and no tintVectors When imageTint Then returns null`() {
        assertThat(imageTint(drawable, tintVectors = false, tint = tint)).isNull()
    }
}
