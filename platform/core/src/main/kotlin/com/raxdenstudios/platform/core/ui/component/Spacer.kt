package com.raxdenstudios.platform.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.raxdenstudios.platform.core.ui.theme.Spacing

object Spacer {

    private val small get() = Spacing.small
    private val medium get() = Spacing.medium
    private val large get() = Spacing.large

    interface Spacings {
        @Composable
        fun Small()

        @Composable
        fun Medium()

        @Composable
        fun Large()

        @Composable
        fun Custom(space: Dp)
    }

    object Vertical : Spacings {

        @Composable
        override fun Small() {
            Custom(small)
        }

        @Composable
        override fun Medium() {
            Custom(medium)
        }

        @Composable
        override fun Large() {
            Custom(large)
        }

        @Composable
        override fun Custom(space: Dp) {
            Spacer(modifier = Modifier.height(space))
        }
    }

    object Horizontal : Spacings {

        @Composable
        override fun Small() {
            Custom(small)
        }

        @Composable
        override fun Medium() {
            Custom(medium)
        }

        @Composable
        override fun Large() {
            Custom(large)
        }

        @Composable
        override fun Custom(space: Dp) {
            Spacer(modifier = Modifier.width(space))
        }
    }

    object System {

        @Composable
        fun NavigationBars(
            background: Color = MaterialTheme.colorScheme.background,
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
                    .background(color = background)
            )
        }

        @Composable
        fun StatusBar(
            background: Color = MaterialTheme.colorScheme.background,
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(color = background)
            )
        }
    }
}
