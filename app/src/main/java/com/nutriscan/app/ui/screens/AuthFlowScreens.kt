package com.nutriscan.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.PersonAddAlt1
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nutriscan.app.R
import com.nutriscan.app.ui.components.InlineErrorCard
import com.nutriscan.app.ui.theme.UiTokens
import com.nutriscan.app.ui.theme.card_stroke
import com.nutriscan.app.ui.theme.muted_text
import com.nutriscan.app.viewmodel.AuthViewModel

@Composable
fun AuthChoiceScreen(
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onLater: () -> Unit
) {
    AuthShell(
        title = stringResource(R.string.auth_choice_title),
        subtitle = stringResource(R.string.auth_choice_subtitle)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)
            ) {
                Button(
                    onClick = onLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(UiTokens.ButtonHeight),
                    shape = RoundedCornerShape(UiTokens.Radius)
                ) {
                    Icon(Icons.Outlined.Login, contentDescription = null)
                    Spacer(modifier = Modifier.size(UiTokens.SpaceXS))
                    Text(stringResource(R.string.auth_choice_login))
                }

                Button(
                    onClick = onRegister,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(UiTokens.ButtonHeight),
                    shape = RoundedCornerShape(UiTokens.Radius)
                ) {
                    Icon(Icons.Outlined.PersonAddAlt1, contentDescription = null)
                    Spacer(modifier = Modifier.size(UiTokens.SpaceXS))
                    Text(stringResource(R.string.auth_choice_register))
                }

                TextButton(
                    onClick = onLater,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(stringResource(R.string.auth_choice_later))
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    AuthFormScreen(
        title = stringResource(R.string.login_title),
        subtitle = stringResource(R.string.login_subtitle),
        actionLabel = stringResource(R.string.login_action),
        loadingLabel = stringResource(R.string.login_loading),
        onNavigateBack = onNavigateBack,
        onSubmit = viewModel::signIn,
        onSuccess = onSuccess,
        viewModel = viewModel
    )
}

@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    AuthFormScreen(
        title = stringResource(R.string.register_title),
        subtitle = stringResource(R.string.register_subtitle),
        actionLabel = stringResource(R.string.register_action),
        loadingLabel = stringResource(R.string.register_loading),
        onNavigateBack = onNavigateBack,
        onSubmit = viewModel::register,
        onSuccess = onSuccess,
        viewModel = viewModel
    )
}

@Composable
private fun AuthFormScreen(
    title: String,
    subtitle: String,
    actionLabel: String,
    loadingLabel: String,
    onNavigateBack: () -> Unit,
    onSubmit: (String, String) -> Unit,
    onSuccess: () -> Unit,
    viewModel: AuthViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onSuccess()
            viewModel.resetAuthState()
        }
    }

    AuthShell(
        title = title,
        subtitle = subtitle,
        topAction = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)
        ) {
            if (uiState.errorMessage != null) {
                InlineErrorCard(
                    title = title,
                    message = uiState.errorMessage ?: stringResource(R.string.error_generic),
                    modifier = Modifier.padding(bottom = UiTokens.SpaceXS),
                    actionLabel = stringResource(R.string.retry),
                    onAction = viewModel::clearError
                )
            }

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    viewModel.clearError()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.email)) },
                singleLine = true,
                shape = RoundedCornerShape(UiTokens.Radius),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                leadingIcon = {
                    Icon(Icons.Outlined.MailOutline, contentDescription = null)
                }
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    viewModel.clearError()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.password)) },
                singleLine = true,
                shape = RoundedCornerShape(UiTokens.Radius),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                )
            )

            Button(
                onClick = { onSubmit(email, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(UiTokens.ButtonHeight),
                enabled = !uiState.isLoading && email.isNotBlank() && password.length >= 6,
                shape = RoundedCornerShape(UiTokens.Radius)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.size(UiTokens.SpaceXS))
                    Text(loadingLabel)
                } else {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun AuthShell(
    title: String,
    subtitle: String,
    topAction: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        topAction?.let {
            Box(modifier = Modifier.align(Alignment.TopStart)) {
                it()
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, card_stroke),
            elevation = CardDefaults.cardElevation(defaultElevation = UiTokens.CardElevationMedium)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceM)
            ) {
                Surface(
                    modifier = Modifier.size(60.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.app_name).take(1),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = muted_text,
                        textAlign = TextAlign.Start
                    )
                }

                content()
            }
        }
    }
}
