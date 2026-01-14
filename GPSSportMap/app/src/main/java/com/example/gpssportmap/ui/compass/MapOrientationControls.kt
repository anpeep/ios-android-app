package com.example.gpssportmap.ui.compass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gpssportmap.R

/**
 * A cluster of controls for managing map orientation and visibility.
 *
 * @param onModeChange A callback that is invoked with the new MapOrientationMode when a user selects a mode.
 */
@Composable
fun MapOrientationControls(
    modifier: Modifier = Modifier,
    azimuth: Float,
    initialMode: MapOrientationMode,
    onModeChange: (MapOrientationMode) -> Unit
) {
    var isHidden by remember { mutableStateOf(false) }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = if (isHidden) painterResource(id = R.drawable.ic_eye)
            else painterResource(id = R.drawable.ic_hide),
            contentDescription = if (isHidden) "Show Controls" else "Hide Controls",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { isHidden = !isHidden }
                .padding(8.dp)
        )

        Spacer(Modifier.height(8.dp))
        AnimatedVisibility(
            visible = !isHidden,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OrientationIconButton(
                    iconRes = R.drawable.ic_compass,
                    contentDescription = "Compass Mode",
                    isSelected = initialMode == MapOrientationMode.COMPASS,
                    onClick = { onModeChange(MapOrientationMode.COMPASS) }
                )
                Spacer(Modifier.height(12.dp))
                OrientationIconButton(
                    iconRes = R.drawable.ic_north,
                    contentDescription = "North-Up Mode",
                    isSelected = initialMode == MapOrientationMode.NORTH,
                    onClick = { onModeChange(MapOrientationMode.NORTH) }
                )
                Spacer(Modifier.height(12.dp))
                OrientationIconButton(
                    iconRes = R.drawable.ic_center,
                    contentDescription = "Center Lock Mode",
                    isSelected = initialMode == MapOrientationMode.CENTER,
                    onClick = { onModeChange(MapOrientationMode.CENTER) }
                )
                Spacer(Modifier.height(12.dp))
                OrientationIconButton(
                    iconRes = R.drawable.ic_user_choose,
                    contentDescription = "Manual Pan Mode",
                    isSelected = initialMode == MapOrientationMode.USER_CHOOSE,
                    onClick = { onModeChange(MapOrientationMode.USER_CHOOSE) }
                )
                Spacer(Modifier.height(8.dp))

                CompassView(
                    azimuth = azimuth,
                    size = 48.dp
                )
            }

        }
    }
}

@Composable
private fun OrientationIconButton(
    iconRes: Int,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
    }

    val iconColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Icon(
        painter = painterResource(id = iconRes),
        contentDescription = contentDescription,
        tint = iconColor,
        modifier = Modifier
            .size(48.dp)
            .shadow(4.dp, CircleShape) // Add a shadow for depth
            .background(backgroundColor, CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(12.dp)
    )
}

@Composable
fun CompassView(
    azimuth: Float,
    modifier: Modifier = Modifier, // <-- ADD THIS PARAMETER
    size: Dp = 38.dp
) {

    val smoothAzimuth by animateFloatAsState(
        targetValue = azimuth,
        label = "compassAnim"
    )

    Box(
        modifier = modifier
            .size(size)
            .background(Color(0xAA000000), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_compass),
            contentDescription = "Compass",
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .rotate(-smoothAzimuth)
        )
    }
}

enum class MapOrientationMode {
    COMPASS, // Rotates with the phone's physical orientation
    NORTH,   // Always points North up
    CENTER,  // Keeps the user's location centered
    USER_CHOOSE // User is manually panning/zooming
}
