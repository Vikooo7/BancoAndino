package com.example.bancoandino

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bancoandino.data.BancoApi
import com.example.bancoandino.data.BancoRepository
import com.example.bancoandino.data.Sesion
import com.example.bancoandino.ui.LoginScreen
import com.example.bancoandino.ui.PagoScreen
import com.example.bancoandino.ui.PanelScreen
import com.example.bancoandino.ui.TemaBanco
import com.example.bancoandino.viewmodel.BancoViewModel

/**
 * Banco Andino · billetera del cliente. Antes (reto de la Semana 2) el login y los datos eran fijos;
 * ahora todo viene del núcleo de Banco Andino (/banco/api) y se puede pagar con QR lo que cobra RutaLog.
 */
class MainActivity : ComponentActivity() {

    /** Código que llega por el enlace bancoandino://pagar?codigo=QR-... (botón "Pagar con Banco Andino" de RutaLog). */
    private val codigoEntrante = mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        leerEnlace(intent)
    }

    private fun leerEnlace(intent: Intent?) {
        intent?.data?.takeIf { it.scheme == "bancoandino" }?.getQueryParameter("codigo")?.let { codigoEntrante.value = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) leerEnlace(intent)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))

        val sesion = Sesion(applicationContext)
        val repositorio = BancoRepository(BancoApi.crear { sesion.token }, sesion)

        setContent {
            TemaBanco {
                val vm: BancoViewModel = viewModel(factory = BancoViewModel.factory(repositorio))
                val sesionActiva by vm.sesionActiva.collectAsState()
                val login by vm.login.collectAsState()
                val panel by vm.panel.collectAsState()
                val pago by vm.pago.collectAsState()
                var pagando by rememberSaveable { mutableStateOf(false) }
                val codigo by codigoEntrante

                // Si se abrió desde RutaLog, va directo a confirmar el pago (después de ingresar, si hace falta).
                LaunchedEffect(sesionActiva, codigo) {
                    val pendiente = codigo
                    if (sesionActiva && pendiente != null) {
                        vm.leerCodigo(pendiente)
                        pagando = true
                        codigoEntrante.value = null
                    }
                }

                if (!sesionActiva) {
                    pagando = false
                    LoginScreen(estado = login, onIngresar = vm::ingresar)
                } else if (pagando) {
                    BackHandler { pagando = false }
                    PagoScreen(
                        estado = pago,
                        onLeer = vm::leerCodigo,
                        onConfirmar = vm::confirmarPago,
                        onOtroCodigo = vm::empezarPago,
                        onTerminar = { pagando = false }
                    )
                } else {
                    LaunchedEffect(Unit) { vm.cargarPanel() }
                    PanelScreen(
                        nombre = vm.nombre,
                        estado = panel,
                        onActualizar = vm::cargarPanel,
                        onPagar = {
                            vm.empezarPago()
                            pagando = true
                        },
                        onCerrarSesion = vm::cerrarSesion
                    )
                }
            }
        }
    }
}
