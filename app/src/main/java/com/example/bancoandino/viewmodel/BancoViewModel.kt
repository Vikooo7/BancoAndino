package com.example.bancoandino.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.bancoandino.data.BancoRepository
import com.example.bancoandino.data.CobroQrDto
import com.example.bancoandino.data.PagoResponse
import com.example.bancoandino.data.ResumenDto
import com.example.bancoandino.data.Resultado
import com.example.bancoandino.model.Formato
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUi(
    val usuarioInicial: String = "",
    val cargando: Boolean = false,
    val error: String? = null
)

data class PanelUi(
    val cargando: Boolean = false,
    val resumen: ResumenDto? = null,
    val error: String? = null
)

/** Pasos de "Pagar con QR": leer el código → confirmar el monto → comprobante con el n.º de operación. */
sealed interface PagoUi {
    data class Leyendo(val error: String? = null, val buscando: Boolean = false) : PagoUi
    /** [uuid] se crea al mostrar la confirmación: si el usuario reintenta, el banco no cobra dos veces. */
    data class Confirmando(
        val codigo: String,
        val cobro: CobroQrDto,
        val uuid: String,
        val pagando: Boolean = false,
        val error: String? = null
    ) : PagoUi
    data class Pagado(val pago: PagoResponse, val cobro: CobroQrDto) : PagoUi
}

class BancoViewModel(private val repositorio: BancoRepository) : ViewModel() {

    private val _sesionActiva = MutableStateFlow(repositorio.haySesion)
    val sesionActiva: StateFlow<Boolean> = _sesionActiva.asStateFlow()
    val nombre: String get() = repositorio.nombre

    private val _login = MutableStateFlow(LoginUi(usuarioInicial = repositorio.ultimoUsuario))
    val login: StateFlow<LoginUi> = _login.asStateFlow()

    private val _panel = MutableStateFlow(PanelUi())
    val panel: StateFlow<PanelUi> = _panel.asStateFlow()

    private val _pago = MutableStateFlow<PagoUi>(PagoUi.Leyendo())
    val pago: StateFlow<PagoUi> = _pago.asStateFlow()

    fun ingresar(usuario: String, clave: String) {
        val limpio = usuario.trim().lowercase()
        if (limpio.isEmpty() || clave.isEmpty()) {
            _login.update { it.copy(error = "Completa usuario y clave") }
            return
        }
        viewModelScope.launch {
            _login.update { it.copy(cargando = true, error = null) }
            when (val r = repositorio.login(limpio, clave)) {
                is Resultado.Exito -> {
                    _login.update { LoginUi(usuarioInicial = limpio) }
                    _sesionActiva.value = true
                    cargarPanel()
                }
                is Resultado.Error -> _login.update { it.copy(cargando = false, error = r.mensaje) }
            }
        }
    }

    fun cargarPanel() {
        viewModelScope.launch {
            _panel.update { it.copy(cargando = true, error = null) }
            when (val r = repositorio.resumen()) {
                is Resultado.Exito -> _panel.value = PanelUi(resumen = r.valor)
                is Resultado.Error -> if (r.sesionVencida) salir(r.mensaje) else _panel.update { it.copy(cargando = false, error = r.mensaje) }
            }
        }
    }

    fun empezarPago() {
        _pago.value = PagoUi.Leyendo()
    }

    /** Texto del QR (escaneado o escrito): se valida el formato y se consulta el cobro antes de pagar. */
    fun leerCodigo(texto: String) {
        val codigo = Formato.codigoDeCobro(texto)
        if (codigo == null) {
            _pago.value = PagoUi.Leyendo(error = "Ese código no es un cobro de Banco Andino.")
            return
        }
        viewModelScope.launch {
            _pago.value = PagoUi.Leyendo(buscando = true)
            when (val r = repositorio.verQr(codigo)) {
                is Resultado.Exito -> _pago.value = when (r.valor.estado) {
                    "PENDIENTE" -> PagoUi.Confirmando(codigo, r.valor, UUID.randomUUID().toString())
                    "PAGADO" -> PagoUi.Leyendo(error = "Este cobro ya fue pagado (operación ${r.valor.nroOperacion}).")
                    "VENCIDO" -> PagoUi.Leyendo(error = "Este QR venció. Pide uno nuevo.")
                    else -> PagoUi.Leyendo(error = "Este cobro fue anulado.")
                }
                is Resultado.Error -> if (r.sesionVencida) salir(r.mensaje) else _pago.value = PagoUi.Leyendo(error = r.mensaje)
            }
        }
    }

    fun confirmarPago() {
        val actual = _pago.value as? PagoUi.Confirmando ?: return
        if (actual.pagando) return
        viewModelScope.launch {
            _pago.value = actual.copy(pagando = true, error = null)
            when (val r = repositorio.pagar(actual.codigo, actual.uuid)) {
                is Resultado.Exito -> {
                    _pago.value = PagoUi.Pagado(r.valor, actual.cobro)
                    cargarPanel()
                }
                // Mismo uuid en el reintento: si el primer intento sí llegó al banco, no se cobra dos veces.
                is Resultado.Error -> if (r.sesionVencida) salir(r.mensaje) else _pago.value = actual.copy(pagando = false, error = r.mensaje)
            }
        }
    }

    fun cerrarSesion() = salir(null)

    private fun salir(motivo: String?) {
        repositorio.cerrarSesion()
        _sesionActiva.value = false
        _panel.value = PanelUi()
        _pago.value = PagoUi.Leyendo()
        _login.value = LoginUi(usuarioInicial = repositorio.ultimoUsuario, error = motivo)
    }

    companion object {
        fun factory(repositorio: BancoRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { BancoViewModel(repositorio) }
        }
    }
}
