package com.example.bancoandino.data

/** Acceso al núcleo de Banco Andino: inicio de sesión, saldo y movimientos, y pago de cobros QR. */
class BancoRepository(private val api: BancoApi, private val sesion: Sesion) {

    val haySesion: Boolean get() = sesion.token != null
    val nombre: String get() = sesion.nombre.orEmpty()
    val ultimoUsuario: String get() = sesion.ultimoUsuario

    suspend fun login(usuario: String, clave: String): Resultado<ClienteDto> =
        when (val r = llamar { api.login(LoginRequest(usuario, clave)) }) {
            is Resultado.Exito -> {
                sesion.token = r.valor.token
                sesion.nombre = r.valor.cliente.nombre
                sesion.ultimoUsuario = usuario // reto extra de la Semana 2: recordar el último usuario
                Resultado.Exito(r.valor.cliente)
            }
            is Resultado.Error -> r
        }

    suspend fun resumen(): Resultado<ResumenDto> = llamar { api.resumen() }

    suspend fun verQr(codigo: String): Resultado<CobroQrDto> = llamar { api.verQr(codigo) }

    /** [uuid] se genera una vez por confirmación: si el pago se reintenta, el banco no cobra dos veces. */
    suspend fun pagar(codigo: String, uuid: String): Resultado<PagoResponse> = llamar { api.pagar(codigo, PagoRequest(uuid)) }

    fun cerrarSesion() = sesion.cerrar()
}
