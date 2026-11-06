package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = DarkPetrolPrimary,
    onPrimary = OnDarkPetrolPrimary,
    primaryContainer = DarkPetrolPrimaryContainer,
    onPrimaryContainer = OnDarkPetrolPrimaryContainer,
    secondary = DarkEmeraldSecondary,
    onSecondary = OnDarkEmeraldSecondary,
    secondaryContainer = DarkEmeraldSecondaryContainer,
    onSecondaryContainer = OnDarkEmeraldSecondaryContainer,
    tertiary = DarkAmberTertiary,
    onTertiary = OnDarkAmberTertiary,
    tertiaryContainer = DarkAmberTertiaryContainer,
    onTertiaryContainer = OnDarkAmberTertiaryContainer,
    error = CrimsonError,
    onError = OnCrimsonError,
    errorContainer = CrimsonErrorContainer,
    onErrorContainer = OnCrimsonErrorContainer,
    background = DarkBackground,
    onBackground = OnDarkBackground,
    surface = DarkSurface,
    onSurface = OnDarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = PetrolPrimary,
    onPrimary = OnPetrolPrimary,
    primaryContainer = PetrolPrimaryContainer,
    onPrimaryContainer = OnPetrolPrimaryContainer,
    secondary = EmeraldSecondary,
    onSecondary = OnEmeraldSecondary,
    secondaryContainer = EmeraldSecondaryContainer,
    onSecondaryContainer = OnEmeraldSecondaryContainer,
    tertiary = AmberTertiary,
    onTertiary = OnAmberTertiary,
    tertiaryContainer = AmberTertiaryContainer,
    onTertiaryContainer = OnAmberTertiaryContainer,
    error = CrimsonError,
    onError = OnCrimsonError,
    errorContainer = CrimsonErrorContainer,
    onErrorContainer = OnCrimsonErrorContainer,
    background = LightBackground,
    onBackground = OnLightBackground,
    surface = LightSurface,
    onSurface = OnLightSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = OnLightSurfaceVariant
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
