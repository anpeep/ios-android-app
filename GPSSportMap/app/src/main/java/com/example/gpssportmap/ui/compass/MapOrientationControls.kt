package com.example.gpssportmap.ui.compass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
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
    initialMode: MapOrientationMode,
    onModeChange: (MapOrientationMode) -> Unit
) {
    var currentMode by remember { mutableStateOf(MapOrientationMode.COMPASS) }
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

        // The container for the orientation controls, which animates in and out
        AnimatedVisibility(
            visible = !isHidden,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Compass Mode Button
                OrientationIconButton(
                    iconRes = R.drawable.ic_compass,
                    contentDescription = "Compass Mode",
                    isSelected = initialMode == MapOrientationMode.COMPASS,
                    onClick = { onModeChange(MapOrientationMode.COMPASS) }
                )
                Spacer(Modifier.height(12.dp))
                // North-Up Mode Button
                OrientationIconButton(
                    iconRes = R.drawable.ic_north,
                    contentDescription = "North-Up Mode",
                    isSelected = initialMode == MapOrientationMode.NORTH,
                    onClick = { onModeChange(MapOrientationMode.NORTH) }
                )
                Spacer(Modifier.height(12.dp))
                // Center Lock Mode Button
                OrientationIconButton(
                    iconRes = R.drawable.ic_center,
                    contentDescription = "Center Lock Mode",
                    isSelected = initialMode == MapOrientationMode.CENTER,
                    onClick = { onModeChange(MapOrientationMode.CENTER) }
                )
                Spacer(Modifier.height(12.dp))
                // User Choose/Pan Mode Button
                OrientationIconButton(
                    iconRes = R.drawable.ic_user_choose,
                    contentDescription = "Manual Pan Mode",
                    isSelected = initialMode == MapOrientationMode.USER_CHOOSE,
                    onClick = { onModeChange(MapOrientationMode.USER_CHOOSE) }
                )
            }
        }
    }
}

/**
 * A reusable, styled icon button for the orientation controls.
 * It shows a special "glowing" state when selected.
 */
@Composable
private fun OrientationIconButton(
    iconRes: Int,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        // Distinctive color for the selected item's background
        MaterialTheme.colorScheme.primary
    } else {
        // Default background
        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
    }

    val iconColor = if (isSelected) {
        // High-contrast icon color when selected
        MaterialTheme.colorScheme.onPrimary
    } else {
        // Default icon color
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
// A preview to see the component in isolation in Android Studio
@Preview(showBackground = true, name = "Interactive Preview")
@Composable
fun PreviewMapOrientationControls() {
    // 1. Create a state variable *inside* the preview
    var selectedMode by remember { mutableStateOf(MapOrientationMode.COMPASS) }

    MapOrientationControls(
        modifier = Modifier.padding(16.dp),
        // 2. Pass the state variable to the component
        initialMode = selectedMode,
        // 3. Update the state variable when the callback is invoked
        onModeChange = { newMode ->
            selectedMode = newMode
            println("Selected mode: $newMode")
        }
    )
}

enum class MapOrientationMode {
    COMPASS, // Rotates with the phone's physical orientation
    NORTH,   // Always points North up
    CENTER,  // Keeps the user's location centered
    USER_CHOOSE // User is manually panning/zooming
}
