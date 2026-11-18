package com.example.bancoandino.model

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Reglas y formatos de la billetera, sin Android: se prueban con JUnit. */
object Formato {

    private val soles = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    private val fecha = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Lima"))
    private val formatoQr = Regex("^(BANCOANDINO:)?(QR-[0-9A-F]{10})$")

    /** 1234.5 → "S/ 1,234.50"; con signo para los movimientos: "+ S/ 300.00" / "- S/ 85.50". */
    fun soles(monto: Double, conSigno: Boolean = false): String {
        val texto = "S/ ${soles.format(kotlin.math.abs(monto))}"
        return when {
            !conSigno -> if (monto < 0) "- $texto" else texto
            monto < 0 -> "- $texto"
            else -> "+ $texto"
        }
    }

    /** "2026-08-20T12:00:00.000Z" → "20/08/2026 07:00" (hora de Lima). */
    fun fechaHora(iso: String?): String = runCatching { fecha.format(Instant.parse(iso)) }.getOrDefault(iso.orEmpty())

    /**
     * Texto leído del QR (o escrito a mano) → código del cobro "QR-XXXXXXXXXX", o null si no es de Banco Andino.
     * Acepta el texto completo "BANCOANDINO:QR-..." que muestran las apps de RutaLog, con espacios o en minúsculas.
     */
    fun codigoDeCobro(texto: String): String? = formatoQr.find(texto.trim().uppercase())?.groupValues?.get(2)

    /** 191-2604001-0 → ***-***4001-0 */
    fun cuentaOculta(numero: String): String = if (numero.length > 6) "***-***${numero.takeLast(6)}" else numero
}
