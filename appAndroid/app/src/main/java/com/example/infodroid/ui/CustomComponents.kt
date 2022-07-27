package com.example.infodroid.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit = {},
    isError: Boolean = false,
    error: String = "",
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    maxLines: Int = Int.MAX_VALUE,
    modifier: Modifier = Modifier
) {
    val background = animateColorAsState(targetValue =
        if (isError)
            MaterialTheme.colorScheme.errorContainer
        else
            MaterialTheme.colorScheme.surfaceVariant
    )

    val onBackground = animateColorAsState(targetValue =
    if (isError)
        MaterialTheme.colorScheme.onErrorContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant
    )

    TextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        isError = isError,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = background.value,
            textColor = onBackground.value,
            errorLabelColor = MaterialTheme.colorScheme.onErrorContainer,
            focusedLabelColor = MaterialTheme.colorScheme.outline,
            unfocusedLabelColor = onBackground.value,
            unfocusedIndicatorColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            cursorColor = onBackground.value
        ),
        maxLines = maxLines
    )
    AnimatedVisibility (isError) {
        Column(Modifier.fillMaxWidth()) {
            Text (error?:"", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 16.dp))
        }
    }
}

@Composable
fun CustomOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit = {},
    isError: Boolean = false,
    error: String = "",
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    maxLines: Int = Int.MAX_VALUE,
    modifier: Modifier = Modifier
) {
    Column (modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            isError = isError,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                errorLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                errorBorderColor = MaterialTheme.colorScheme.error,
                cursorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                textColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            maxLines = maxLines
        )
        AnimatedVisibility(isError) {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

@Composable
fun TonalChipButton (
    onClick: () -> Unit,
    text: String,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    color: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    tonalElevation: Dp = 1.dp
) {
    Surface (
        color = color,
        contentColor = contentColor,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = tonalElevation,
        modifier =
        Modifier
            .height(32.dp)
            .clickable { onClick() },
    ) {
        Row (
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier =
            Modifier
                .padding(
                    if (leadingIcon != null) 8.dp else 16.dp,
                    0.dp,
                    if (trailingIcon != null) 8.dp else 16.dp,
                    0.dp
                )
                .fillMaxHeight()
        ){
            leadingIcon?.let {
                Icon(
                    it,
                    null,
                    modifier =
                        Modifier
                            .size(18.dp)
                )
            }
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            trailingIcon?.let {
                Icon(
                    it,
                    null,
                    modifier =
                        Modifier
                            .size(18.dp)
                )
            }
        }
    }
}