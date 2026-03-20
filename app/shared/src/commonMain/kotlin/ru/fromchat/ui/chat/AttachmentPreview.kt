package ru.fromchat.ui.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.withSaveLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import com.pr0gramm3r101.utils.conditional
import com.pr0gramm3r101.utils.crypto.Base64
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import ru.fromchat.api.DmEnvelope
import ru.fromchat.api.DmFile

private val IMAGE_SIZE = 160.dp
private val IMAGE_RADIUS = 12.dp

internal fun isImageFilename(name: String): Boolean =
    name.endsWith(".png", true) || name.endsWith(".jpg", true) ||
        name.endsWith(".jpeg", true) || name.endsWith(".gif", true) || name.endsWith(".webp", true)

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun AttachmentPreview(
    file: DmFile?,
    dmEnvelope: DmEnvelope?,
    currentUserId: Int?,
    pendingFileUri: String?,
    /** Filename for pending (non-image) files; used when pendingFileUri is set. */
    pendingFilename: String? = null,
    isUploading: Boolean,
    /** 0–100 upload progress when isUploading; null = indefinite */
    uploadProgress: Int? = null,
    fileThumbnail: String? = null,
    fileAspectRatio: Float? = null,
    fileSizeBytes: Long? = null,
    messageId: Int? = null,
    fileIndex: Int? = null,
    onFileClick: (() -> Unit)? = null,
    onImageClick: (() -> Unit)? = null,
    onImageBounds: ((Rect) -> Unit)? = null,
    isExpanded: Boolean = false,
    isAuthor: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isImage = when {
        file != null -> isImageFilename(file.name)
        pendingFileUri != null -> {
            val nameToCheck = pendingFilename?.takeIf { it.isNotBlank() }
                ?: pendingFileUri.substringAfterLast('/').substringBefore('?')
            isImageFilename(nameToCheck)
        }
        else -> false
    }

    val isFile = file != null && !isImage
    val isImageWithThumb = file != null && isImage && dmEnvelope != null && !fileThumbnail.isNullOrBlank()
    val isPendingImage = pendingFileUri != null && isImage
    val isPendingFile = pendingFileUri != null && !isImage

    when {
        isFile -> {
            FileIconContent(
                filename = file.name,
                sizeBytes = fileSizeBytes,
                onClick = onFileClick,
                isAuthor = isAuthor,
                isUploading = false,
                uploadProgress = null,
                modifier = modifier
            )
        }
        isPendingFile -> {
            FileIconContent(
                filename = pendingFilename ?: "File",
                sizeBytes = null,
                onClick = null,
                isAuthor = isAuthor,
                isUploading = isUploading,
                uploadProgress = uploadProgress,
                modifier = modifier
            )
        }
        isImageWithThumb || isPendingImage -> {
            var isFullyLoaded by remember { mutableStateOf(false) }
            Box(
                modifier = modifier
                    .then(
                        if (onImageClick != null && isFullyLoaded && !isExpanded && (isImageWithThumb || !isPendingImage)) Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onImageClick)
                        else Modifier
                    )
                    .conditional(
                        fileAspectRatio != null && fileAspectRatio > 0f,
                        `if` = {
                            Modifier
                                .aspectRatio(fileAspectRatio!!)
                                .sizeIn(maxWidth = IMAGE_SIZE, maxHeight = IMAGE_SIZE)
                        },
                        `else` = {
                            Modifier.size(IMAGE_SIZE)
                        }
                    )
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clip(RoundedCornerShape(IMAGE_RADIUS))
                    .then(
                        if (onImageBounds != null && (isImageWithThumb || !isPendingImage)) {
                            Modifier.onGloballyPositioned { coords ->
                                val pos = coords.positionInRoot()
                                val size = coords.size
                                onImageBounds(
                                    Rect(
                                        pos.x,
                                        pos.y,
                                        pos.x + size.width.toFloat(),
                                        pos.y + size.height.toFloat()
                                    )
                                )
                            }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!isExpanded) {
                    when {
                        isPendingImage -> UnifiedImageContent(
                            localUri = pendingFileUri,
                            messageId = messageId,
                            fileIndex = fileIndex,
                            file = file,
                            envelope = dmEnvelope,
                            currentUserId = currentUserId,
                            isUploading = isUploading,
                            uploadProgress = uploadProgress,
                            onFullyLoaded = { isFullyLoaded = it }
                        )
                        else -> DecryptedImageContent(
                            messageId = messageId ?: -1,
                            fileIndex = fileIndex ?: 0,
                            file = file!!,
                            envelope = dmEnvelope!!,
                            currentUserId = currentUserId,
                            thumbnailBase64 = fileThumbnail!!,
                            aspectRatio = fileAspectRatio,
                            onFullyLoaded = { isFullyLoaded = it }
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@OptIn(ExperimentalHazeMaterialsApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UnifiedImageContent(
    localUri: String,
    messageId: Int?,
    fileIndex: Int?,
    file: DmFile?,
    envelope: DmEnvelope?,
    currentUserId: Int?,
    isUploading: Boolean,
    uploadProgress: Int?,
    onFullyLoaded: (Boolean) -> Unit = {}
) {
    var cachedPath by remember(messageId, fileIndex, file?.path) {
        mutableStateOf(
            if (messageId != null && fileIndex != null && file != null) {
                DecryptedImageCache.getCached(messageId, fileIndex, file.path)
            } else {
                null
            }
        )
    }

    LaunchedEffect(messageId, fileIndex, file?.path, envelope) {
        cachedPath = if (messageId != null && fileIndex != null && file != null && envelope != null) {
            DecryptedImageCache.getOrDecrypt(messageId, fileIndex, file, envelope, currentUserId)
        } else {
            null
        }
    }
    LaunchedEffect(Unit) { onFullyLoaded(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(IMAGE_RADIUS))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        AsyncImage(
            model = localUri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        AnimatedContent(
            targetState = isUploading,
            modifier = Modifier.matchParentSize(),
            transitionSpec = {
                (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.98f, animationSpec = tween(220)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(450, easing = FastOutSlowInEasing)) +
                            scaleOut(targetScale = 1.02f, animationSpec = tween(450, easing = FastOutSlowInEasing))
                    )
            },
            label = "upload_overlay"
        ) { uploading ->
            if (uploading) {
                UploadingImageOverlay(
                    model = localUri,
                    uploadProgress = uploadProgress,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                Box(modifier = Modifier.matchParentSize())
            }
        }
        if (cachedPath != null && file != null) {
            val fullPainter = rememberAsyncImagePainter(
                model = cachedPath!!,
                contentScale = ContentScale.FillWidth
            )
            val fullState by fullPainter.state.collectAsState()
            when (fullState) {
                is coil3.compose.AsyncImagePainter.State.Success -> {
                    LaunchedEffect(Unit) { onFullyLoaded(true) }
                    val alpha = remember { Animatable(0f) }
                    LaunchedEffect(Unit) {
                        alpha.animateTo(1f, animationSpec = tween(300))
                    }
                    Image(
                        painter = fullPainter,
                        contentDescription = file.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(alpha.value),
                        contentScale = ContentScale.FillWidth
                    )
                }
                else -> {
                    LaunchedEffect(Unit) { onFullyLoaded(false) }
                }
            }
        }
    }
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
private fun UploadingImageOverlay(
    model: String,
    uploadProgress: Int?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(IMAGE_RADIUS))
                .hazeEffect(style = HazeMaterials.thin())
        ) {
            AsyncImage(
                model = model,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            ExpressiveUploadIndicator(
                uploadProgress = uploadProgress,
                modifier = Modifier.size(56.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressiveUploadIndicator(
    uploadProgress: Int?,
    modifier: Modifier = Modifier
) {
    val clampedProgress = uploadProgress?.coerceIn(0, 100)
    val indeterminate = clampedProgress == null || clampedProgress == 0
    val waveActive = clampedProgress != null && clampedProgress in 1..99

    val waveAnimSpec = tween<Float>(durationMillis = 320, easing = FastOutSlowInEasing)

    val targetWaveStrength = if (waveActive) 1f else 0f
    val animatedWaveStrength by animateFloatAsState(
        targetValue = targetWaveStrength,
        animationSpec = waveAnimSpec,
        label = "waveStrength"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = (clampedProgress ?: 0) / 100f,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "uploadProgress"
    )
    val primary = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
    val defaultIndicatorAmplitude = WavyProgressIndicatorDefaults.indicatorAmplitude
    val ringModifier = Modifier.size(56.dp)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (indeterminate) {
            CircularWavyProgressIndicator(
                modifier = ringModifier,
                color = primary,
                trackColor = trackColor,
                amplitude = animatedWaveStrength
            )
        } else {
            CircularWavyProgressIndicator(
                progress = { animatedProgress },
                modifier = ringModifier,
                color = primary,
                trackColor = trackColor,
                amplitude = { p -> animatedWaveStrength * defaultIndicatorAmplitude(p) }
            )
        }
    }
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
private fun PendingImageContent(
    uri: String,
    isUploading: Boolean,
    uploadProgress: Int?,
    isImage: Boolean
) {
    if (isImage) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(IMAGE_RADIUS))
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            )
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            AnimatedContent(
                targetState = isUploading,
                modifier = Modifier.matchParentSize(),
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.98f, animationSpec = tween(220)))
                        .togetherWith(
                            fadeOut(animationSpec = tween(450, easing = FastOutSlowInEasing)) +
                                scaleOut(targetScale = 1.02f, animationSpec = tween(450, easing = FastOutSlowInEasing))
                        )
                },
                label = "upload_state"
            ) { uploading ->
                if (uploading) {
                    UploadingImageOverlay(
                        model = uri,
                        uploadProgress = uploadProgress,
                        modifier = Modifier.matchParentSize()
                    )
                } else {
                    Box(modifier = Modifier.matchParentSize())
                }
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isUploading) {
                if (uploadProgress != null) {
                    DeterminateCircularProgress(
                        progress = uploadProgress,
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    IndefiniteCircularProgress(modifier = Modifier.size(40.dp))
                }
            } else {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun IndefiniteCircularProgress(modifier: Modifier = Modifier) {
    CircularProgressIndicator(
        modifier = modifier,
        strokeWidth = 3.dp
    )
}

@Composable
private fun DeterminateCircularProgress(
    progress: Int,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (progress.coerceIn(0, 100) / 100f),
        animationSpec = tween(
            durationMillis = 250,
            easing = FastOutSlowInEasing
        ),
        label = "uploadProgress"
    )
    CircularProgressIndicator(
        progress = { animatedProgress },
        modifier = modifier,
        strokeWidth = 3.dp
    )
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
private fun DecryptedImageContent(
    messageId: Int,
    fileIndex: Int,
    file: DmFile,
    envelope: DmEnvelope,
    currentUserId: Int?,
    thumbnailBase64: String,
    aspectRatio: Float?,
    onFullyLoaded: (Boolean) -> Unit = {}
) {
    var cachedPath by remember(messageId, fileIndex, file.path) {
        mutableStateOf(DecryptedImageCache.getCached(messageId, fileIndex, file.path))
    }
    val thumbnailBytes = remember(thumbnailBase64) {
        runCatching { Base64.decode(thumbnailBase64) }.getOrNull()
    }

    LaunchedEffect(messageId, fileIndex, file.path, envelope) {
        cachedPath = DecryptedImageCache.getOrDecrypt(messageId, fileIndex, file, envelope, currentUserId)
    }
    LaunchedEffect(cachedPath) {
        onFullyLoaded(cachedPath != null)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(IMAGE_RADIUS))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
        when {
            cachedPath != null -> {
                val fullPainter = rememberAsyncImagePainter(
                    model = cachedPath!!,
                    contentScale = ContentScale.FillWidth
                )
                val fullState by fullPainter.state.collectAsState()
                when (fullState) {
                    is coil3.compose.AsyncImagePainter.State.Success -> {
                        Image(
                            painter = fullPainter,
                            contentDescription = file.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                    is coil3.compose.AsyncImagePainter.State.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            IndefiniteCircularProgress(modifier = Modifier.size(32.dp))
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            IndefiniteCircularProgress(modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
            thumbnailBytes == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    IndefiniteCircularProgress(modifier = Modifier.size(32.dp))
                }
            }
            else -> {
                val thumbPainter = rememberAsyncImagePainter(
                    model = thumbnailBytes,
                    contentScale = ContentScale.Crop
                )
                val thumbState by thumbPainter.state.collectAsState()
                when (thumbState) {
                    is coil3.compose.AsyncImagePainter.State.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            IndefiniteCircularProgress(modifier = Modifier.size(32.dp))
                        }
                    }
                    is coil3.compose.AsyncImagePainter.State.Success -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(IMAGE_RADIUS))
                                .hazeEffect(style = HazeMaterials.thin())
                        ) {
                            Image(
                                painter = thumbPainter,
                                contentDescription = file.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        if (cachedPath != null) {
                            val fullPainter = rememberAsyncImagePainter(
                                model = cachedPath!!,
                                contentScale = ContentScale.FillWidth
                            )
                            val fullState by fullPainter.state.collectAsState()
                            when (fullState) {
                                is coil3.compose.AsyncImagePainter.State.Success -> {
                                    val alpha = remember { Animatable(0f) }
                                    LaunchedEffect(Unit) {
                                        alpha.animateTo(1f, animationSpec = tween(300))
                                    }
                                    Image(
                                        painter = fullPainter,
                                        contentDescription = file.name,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .alpha(alpha.value),
                                        contentScale = ContentScale.FillWidth
                                    )
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        IndefiniteCircularProgress(modifier = Modifier.size(32.dp))
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            IndefiniteCircularProgress(modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

@Composable
private fun FileIconContent(
    filename: String,
    sizeBytes: Long?,
    onClick: (() -> Unit)?,
    isAuthor: Boolean,
    isUploading: Boolean = false,
    uploadProgress: Int? = null,
    modifier: Modifier = Modifier
) {
    val contentColor = if (isAuthor) Color.White else MaterialTheme.colorScheme.onSurface
    val circleBackground = if (isAuthor) Color.White else MaterialTheme.colorScheme.primary
    val iconTint = if (isAuthor) MaterialTheme.colorScheme.primary else Color.White
    Row(
        modifier = modifier
            .widthIn(max = 240.dp)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(40.dp).then(
                if (isAuthor) {
                    Modifier
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithContent {
                            drawCircle(
                                color = circleBackground,
                                radius = size.minDimension / 2f,
                                center = center
                            )
                            drawContext.canvas.withSaveLayer(
                                bounds = Rect(0f, 0f, size.width, size.height),
                                paint = Paint().apply { blendMode = BlendMode.DstOut }
                            ) {
                                drawContent()
                            }
                        }
                } else {
                    Modifier
                        .background(circleBackground, RoundedCornerShape(20.dp))
                }
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Download,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = if (isAuthor) Color.White else iconTint
            )
        }
            if (isUploading) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.Center)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (uploadProgress != null) {
                        DeterminateCircularProgress(
                            progress = uploadProgress,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        IndefiniteCircularProgress(modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
        Column(
            modifier = Modifier.padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = filename.take(70) + if (filename.length > 70) "…" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                maxLines = 2
            )
            if (sizeBytes != null) {
                Text(
                    text = formatFileSize(sizeBytes),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 12.sp,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}
