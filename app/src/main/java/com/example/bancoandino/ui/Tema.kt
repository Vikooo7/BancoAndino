package com.example.bancoandino.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Los mismos colores del reto de la Semana 2.
val AzulAndino = Color(0xFF0D47A1)
val AzulOscuro = Color(0xFF08306B)
val Positivo = Color(0xFF2E7D32)
val Negativo = Color(0xFFC62828)

private val Claro = lightColorScheme(
    primary = AzulAndino,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = AzulOscuro,
    background = Color(0xFFF2F5FA),
    surface = Color.White,
    onSurfaceVariant = Color(0xFF5C6B7A),
    outline = Color(0xFFC7D1DE),
    error = Negativo
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFF9EC2FF),
    onPrimary = Color(0xFF002F67),
    primaryContainer = Color(0xFF0D3C7A),
    onPrimaryContainer = Color(0xFFD6E3FF),
    background = Color(0xFF0F141C),
    surface = Color(0xFF171D27),
    error = Color(0xFFFFB4AB)
)

@Composable
fun TemaBanco(contenido: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Oscuro else Claro, content = contenido)
}
