package com.example.bancoandino.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bancoandino.model.Formato
import com.example.bancoandino.viewmodel.PagoUi
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlin.coroutines.resume
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Pagar con QR: se lee el QR que muestra RutaLog (pago del envío o contraentrega en la puerta),
 * se confirma el monto y el banco devuelve el número de operación.
 */
@Composable
fun PagoScreen(
    estado: PagoUi,
    onLeer: (String) -> Unit,
    onConfirmar: () -> Unit,
    onOtroCodigo: () -> Unit,
    onTerminar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AzulAndino)
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onTerminar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White) }
            Text(
                text = if (estado is PagoUi.Pagado) "Comprobante" else "Pagar con QR",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (estado) {
                is PagoUi.Leyendo -> PasoLeer(estado, onLeer)
                is PagoUi.Confirmando -> PasoConfirmar(estado, onConfirmar, onOtroCodigo)
                is PagoUi.Pagado -> PasoComprobante(estado, onTerminar)
            }
        }
    }
}

@Composable
private fun PasoLeer(estado: PagoUi.Leyendo, onLeer: (String) -> Unit) {
    val context = LocalContext.current
    val alcance = rememberCoroutineScope()
    var codigo by rememberSaveable { mutableStateOf("") }

    Text(
        text = "Escanea el QR que te muestra la app de RutaLog: el pago de tu envío o el cobro contraentrega en tu puerta.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Button(
        onClick = { alcance.launch { escanearQr(context)?.let(onLeer) } },
        enabled = !estado.buscando,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Text("Escanear QR", style = MaterialTheme.typography.titleMedium)
    }
    Text("¿No puedes escanear? Escribe el código que aparece debajo del QR:", style = MaterialTheme.typography.bodySmall)
    OutlinedTextField(
        value = codigo,
        onValueChange = { codigo = it.uppercase() },
        label = { Text("Código (QR-…)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onLeer(codigo) }),
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedButton(onClick = { onLeer(codigo) }, enabled = !estado.buscando && codigo.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
        Text("Continuar")
    }
    if (estado.buscando) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }
    estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
}

@Composable
private fun PasoConfirmar(estado: PagoUi.Confirmando, onConfirmar: () -> Unit, onOtroCodigo: () -> Unit) {
    val cobro = estado.cobro
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Vas a pagar a", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(cobro.comercio ?: "Comercio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(cobro.concepto, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text("Monto", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(Formato.soles(cobro.monto), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            cobro.venceEn?.let {
                Text("Vence: ${Formato.fechaHora(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(cobro.id, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
    Button(
        onClick = onConfirmar,
        enabled = !estado.pagando,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        if (estado.pagando) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text("Pagar ${Formato.soles(cobro.monto)}", style = MaterialTheme.typography.titleMedium)
        }
    }
    OutlinedButton(onClick = onOtroCodigo, enabled = !estado.pagando, modifier = Modifier.fillMaxWidth()) { Text("Leer otro código") }
}

@Composable
private fun PasoComprobante(estado: PagoUi.Pagado, onTerminar: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Positivo, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
        }
        Text("Pago realizado", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(Formato.soles(estado.pago.monto), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(estado.cobro.concepto, style = MaterialTheme.typography.bodyMedium)
    }
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("N.º de operación", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                estado.pago.nroOperacion,
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))
            // RF-C06: si el courier no tiene señal, anota este número y RutaLog lo verifica con el banco después.
            Text(
                "Si el courier no tiene señal, muéstrale este número para que lo registre.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            estado.pago.saldo?.let {
                Spacer(Modifier.height(10.dp))
                Text("Saldo disponible: ${Formato.soles(it)}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    Button(onClick = onTerminar, shape = RoundedCornerShape(16.dp), modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)) { Text("Listo") }
}

private val opcionesQr = GmsBarcodeScannerOptions.Builder()
    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
    .enableAutoZoom()
    .build()

/** Lector de Google Play Services: no pide permiso de cámara. Devuelve null si el usuario lo cierra. */
private suspend fun escanearQr(context: Context): String? =
    GmsBarcodeScanning.getClient(context, opcionesQr).startScan().esperar()?.rawValue

private suspend fun <T> Task<T>.esperar(): T? = suspendCancellableCoroutine { continuacion ->
    addOnSuccessListener { continuacion.resume(it) }
    addOnFailureListener { continuacion.resume(null) }
    addOnCanceledListener { continuacion.resume(null) }
}
