package com.example.bancoandino

import com.example.bancoandino.model.Formato
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatoTest {

    @Test
    fun leeElQrQueMuestraRutaLog() {
        assertEquals("QR-1A2B3C4D5E", Formato.codigoDeCobro("BANCOANDINO:QR-1A2B3C4D5E"))
        assertEquals("QR-1A2B3C4D5E", Formato.codigoDeCobro("  bancoandino:qr-1a2b3c4d5e "))
        assertEquals("QR-1A2B3C4D5E", Formato.codigoDeCobro("QR-1A2B3C4D5E"))
    }

    @Test
    fun rechazaCodigosQueNoSonDelBanco() {
        assertNull(Formato.codigoDeCobro("RLP-26-100001-1")) // la guía de RutaLog no es un cobro
        assertNull(Formato.codigoDeCobro("BANCOANDINO:QR-123"))
        assertNull(Formato.codigoDeCobro(""))
    }

    @Test
    fun formatoDeSoles() {
        assertEquals("S/ 3,450.00", Formato.soles(3450.0))
        assertEquals("- S/ 85.50", Formato.soles(-85.5, conSigno = true))
        assertEquals("+ S/ 300.00", Formato.soles(300.0, conSigno = true))
    }

    @Test
    fun fechaEnHoraDeLima() {
        assertEquals("20/08/2026 07:00", Formato.fechaHora("2026-08-20T12:00:00Z"))
    }

    @Test
    fun cuentaOculta() {
        assertEquals("***-***4001-0", Formato.cuentaOculta("191-2604001-0"))
    }
}
