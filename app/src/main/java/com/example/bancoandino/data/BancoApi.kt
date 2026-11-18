package com.example.bancoandino.data

import com.example.bancoandino.BuildConfig

import com.google.gson.Gson
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.io.IOException
import java.util.concurrent.TimeUnit

// ---------- DTO del núcleo de Banco Andino (/banco/api) ----------

data class LoginRequest(val usuario: String, val clave: String)
data class ClienteDto(val usuario: String, val nombre: String)
data class LoginResponse(val token: String, val cliente: ClienteDto)

data class CuentaDto(val numero: String, val tipo: String, val titular: String, val saldo: Double)
data class MovimientoDto(
    val cuenta: String,
    val nroOperacion: String?,
    val descripcion: String,
    val monto: Double,
    val saldo: Double,
    val fecha: String
)
data class ResumenDto(val cliente: ClienteDto, val cuentas: List<CuentaDto>, val movimientos: List<MovimientoDto>)

/** Cobro QR que se va a pagar: lo muestra la app antes de confirmar. */
data class CobroQrDto(
    val id: String,
    val comercio: String?,
    val monto: Double,
    val concepto: String,
    val referencia: String?,
    val estado: String,
    val nroOperacion: String?,
    val venceEn: String?
)
data class PagoRequest(val uuid: String)
data class PagoResponse(val nroOperacion: String, val monto: Double, val saldo: Double?, val concepto: String?, val duplicado: Boolean?)
data class ErrorDto(val codigo: String?, val mensaje: String?)

interface BancoApi {
    @POST("banco/api/clientes/login")
    suspend fun login(@Body cuerpo: LoginRequest): Response<LoginResponse>

    @GET("banco/api/clientes/yo")
    suspend fun resumen(): Response<ResumenDto>

    @GET("banco/api/qr/{codigo}")
    suspend fun verQr(@Path("codigo") codigo: String): Response<CobroQrDto>

    @POST("banco/api/qr/{codigo}/pagar")
    suspend fun pagar(@Path("codigo") codigo: String, @Body cuerpo: PagoRequest): Response<PagoResponse>

    companion object {
        /** El núcleo simulado de Banco Andino corre en el mismo servidor que la API de RutaLog (en /banco/api). */
        /** Se define al compilar (app/build.gradle.kts, propiedad urlApi); por defecto, la API en Vercel. */
        val URL_BASE: String = BuildConfig.URL_API

        fun crear(token: () -> String?): BancoApi {
            val http = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor { cadena ->
                    val peticion = cadena.request().newBuilder().apply {
                        token()?.let { header("Authorization", "Bearer $it") }
                    }.build()
                    cadena.proceed(peticion)
                }
                .build()
            return Retrofit.Builder()
                .baseUrl(URL_BASE)
                .client(http)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(BancoApi::class.java)
        }
    }
}

/** Resultado de una llamada: el valor, o un mensaje listo para mostrar. */
sealed interface Resultado<out T> {
    data class Exito<T>(val valor: T) : Resultado<T>
    data class Error(val mensaje: String, val sesionVencida: Boolean = false, val codigo: String? = null) : Resultado<Nothing>
}

private val gson = Gson()

suspend fun <T> llamar(bloque: suspend () -> Response<T>): Resultado<T> = try {
    val respuesta = bloque()
    val cuerpo = respuesta.body()
    if (respuesta.isSuccessful && cuerpo != null) {
        Resultado.Exito(cuerpo)
    } else {
        val error = runCatching { gson.fromJson(respuesta.errorBody()?.string(), ErrorDto::class.java) }.getOrNull()
        Resultado.Error(
            mensaje = error?.mensaje ?: "El banco respondió con error ${respuesta.code()}.",
            sesionVencida = respuesta.code() == 401 && error?.codigo != "CREDENCIALES_INVALIDAS",
            codigo = error?.codigo
        )
    }
} catch (e: IOException) {
    Resultado.Error("Sin conexión con Banco Andino. Revisa tu internet e intenta de nuevo.")
}
