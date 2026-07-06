package com.raxdenstudios.platform.ui.composable

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController

object Input {

    @Composable
    fun OutlinedTextField(
        value: String,
        onValueChange: (String) -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        placeholder: @Composable (() -> Unit)? = null,
        supportingText: @Composable (() -> Unit)? = null,
        isError: Boolean = false,
        keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
        keyboardActions: KeyboardActions = KeyboardActions.Default,
        keyboardController: SoftwareKeyboardController? = LocalSoftwareKeyboardController.current,
        singleLine: Boolean = false,
    ) {
        androidx.compose.material3.OutlinedTextField(
            modifier = modifier
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) keyboardController?.hide()
                },
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            placeholder = placeholder,
            singleLine = singleLine,
            isError = isError,
            supportingText = supportingText,
            keyboardOptions = keyboardOptions,
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardActions.onDone?.invoke(this)
                    keyboardController?.hide()
                },
                onGo = keyboardActions.onGo?.let { action -> { action(this); keyboardController?.hide() } },
                onNext = keyboardActions.onNext?.let { action -> { action(this) } },
                onPrevious = keyboardActions.onPrevious?.let { action -> { action(this) } },
                onSearch = keyboardActions.onSearch?.let { action -> { action(this); keyboardController?.hide() } },
                onSend = keyboardActions.onSend?.let { action -> { action(this); keyboardController?.hide() } },
            ),
        )
    }
}