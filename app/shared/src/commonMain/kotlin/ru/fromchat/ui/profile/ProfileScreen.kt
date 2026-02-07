package ru.fromchat.ui.profile

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.fromchat.api.ApiClient
import ru.fromchat.api.UserProfile
import ru.fromchat.ui.chat.Avatar

private data class ProfileUiState(
    val profile: UserProfile? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val linkStatus: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: Int?,
    onBack: () -> Unit,
    onChat: (Int) -> Unit,
    hideAvatar: Boolean = false,
    onAvatarSlotBounds: ((Rect) -> Unit)? = null,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedAvatarKey: Any? = null
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    var state by remember { mutableStateOf(ProfileUiState()) }

    val targetUserId = userId.takeIf { it != null && it > 0 }
    val fetchKey = targetUserId ?: 0

    LaunchedEffect(fetchKey) {
        state = state.copy(isLoading = true, error = null)
        runCatching {
            if (targetUserId == null) {
                ApiClient.getOwnProfile()
            } else {
                ApiClient.getProfileById(targetUserId)
            }
        }.onSuccess { profile ->
            state = state.copy(profile = profile, isLoading = false)
        }.onFailure {
            state = state.copy(error = it.message ?: "Unable to load profile", isLoading = false)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            val errorMessage = state.error
            val profile = state.profile
            val displayName = profile?.displayName?.takeIf { it.isNotBlank() }
                ?: profile?.username?.takeIf { it.isNotBlank() }
                ?: "?"

            val useSharedAvatar = sharedTransitionScope != null &&
                animatedVisibilityScope != null &&
                sharedAvatarKey != null

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                when {
                    useSharedAvatar -> {
                        with(sharedTransitionScope!!) {
                            Avatar(
                                profilePictureUrl = profile?.profilePicture,
                                displayName = displayName,
                                size = 128.dp,
                                modifier = Modifier
                                    .padding(top = 16.dp)
                                    .sharedElement(
                                        rememberSharedContentState(key = sharedAvatarKey!!),
                                        animatedVisibilityScope = animatedVisibilityScope!!
                                    )
                                    .size(128.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    !hideAvatar -> {
                        Avatar(
                            profilePictureUrl = profile?.profilePicture,
                            displayName = displayName,
                            size = 128.dp,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    onAvatarSlotBounds != null -> {
                        Box(
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .size(128.dp)
                                .onGloballyPositioned { coords ->
                                    val pos = coords.positionInRoot()
                                    val sz = coords.size
                                    onAvatarSlotBounds(
                                        Rect(
                                            pos.x,
                                            pos.y,
                                            pos.x + sz.width.toFloat(),
                                            pos.y + sz.height.toFloat()
                                        )
                                    )
                                }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    else -> {
                        Spacer(modifier = Modifier.height(16.dp + 128.dp + 12.dp))
                    }
                }

                when {
                    state.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.padding(top = 24.dp))
                    }
                    errorMessage != null -> {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 24.dp)
                        )
                    }
                    profile != null -> {
                        val profileLink = profile.username.takeIf { it.isNotBlank() }
                            ?.let { "https://fromchat.ru/@$it" }
                            ?: "https://fromchat.ru/?u=${profile.id}"

                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "@${profile.username}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { onChat(profile.id) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = "Chat")
                            }
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(profileLink))
                                    state = state.copy(linkStatus = "Link copied!")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = "Link")
                            }
                        }

                        state.linkStatus?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = it, style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                ProfileField(label = "Username", value = profile.username)
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                ProfileField(label = "Bio", value = profile.bio ?: "No bio")
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                ProfileField(label = "Member since", value = profile.createdAt ?: "Unknown")
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                ProfileField(
                                    label = "Status",
                                    value = if ((profile.deleted == true) || (profile.suspended == true)) "Suspended/Deleted" else "Active"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
