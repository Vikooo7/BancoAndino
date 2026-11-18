package com.example.bancoandino.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Sesión del cliente del banco: el token va cifrado. El último usuario se recuerda aparte (sin cifrar,
 * no es secreto) para prellenar el login, como en el reto de la Semana 2.
 */
class Sesion(context: Context) {

    private val cifradas: SharedPreferences = EncryptedSharedPreferences.create(
        "banco_sesion",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC), // clave AES-256 guardada en el Android Keystore
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    private val preferencias = context.getSharedPreferences("BancoAndinoPrefs", Context.MODE_PRIVATE)

    var token: String?
        get() = cifradas.getString("token", null)
        set(valor) = cifradas.edit().putString("token", valor).apply()

    var nombre: String?
        get() = cifradas.getString("nombre", null)
        set(valor) = cifradas.edit().putString("nombre", valor).apply()

    var ultimoUsuario: String
        get() = preferencias.getString("ultimoUsuario", "").orEmpty()
        set(valor) = preferencias.edit().putString("ultimoUsuario", valor).apply()

    fun cerrar() {
        cifradas.edit().clear().apply()
    }
}
