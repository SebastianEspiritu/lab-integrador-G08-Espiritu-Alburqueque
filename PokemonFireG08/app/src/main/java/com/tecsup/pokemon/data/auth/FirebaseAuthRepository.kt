package com.tecsup.pokemon.data.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.tecsup.pokemon.domain.model.Rol
import com.tecsup.pokemon.domain.model.Sesion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Ingreso con Firebase Authentication. El rol NO se decide en la app:
 * se lee del documento usuarios/{uid} en Firestore (campo "rol": "admin" o "usuario").
 */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthRepository {

    override suspend fun iniciarSesion(correo: String, clave: String): Result<Sesion> = try {
        val usuario = auth.signInWithEmailAndPassword(correo, clave).await().user
            ?: error("Firebase no devolvió el usuario.")
        Result.success(Sesion(usuario.uid, usuario.email ?: correo, leerRol(usuario.uid)))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Si falló la lectura del rol no se deja una sesión a medias.
        auth.signOut()
        Result.failure(IllegalStateException(mensajeDe(e), e))
    }

    override suspend fun sesionActual(): Sesion? {
        // Firebase recuerda al usuario entre aperturas de la app.
        val usuario = auth.currentUser ?: return null
        val rol = try {
            leerRol(usuario.uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Sin red y sin caché de Firestore se entra con el rol de menor permiso.
            Rol.USUARIO
        }
        return Sesion(usuario.uid, usuario.email ?: "", rol)
    }

    override fun cerrarSesion() = auth.signOut()

    private suspend fun leerRol(uid: String): Rol {
        val documento = db.collection("usuarios").document(uid).get().await()
        val rol = documento.getString("rol")?.trim()?.lowercase()
        return if (rol == "admin") Rol.ADMIN else Rol.USUARIO
    }

    private fun mensajeDe(e: Exception): String = when (e) {
        is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos."
        is FirebaseAuthInvalidUserException -> "No existe una cuenta activa con ese correo."
        is FirebaseNetworkException -> "No hay conexión a internet. Inténtalo de nuevo."
        is FirebaseTooManyRequestsException -> "Demasiados intentos. Espera un momento e inténtalo de nuevo."
        is FirebaseFirestoreException -> "Ingresaste, pero no se pudo leer tu rol en Firestore."
        else -> "No se pudo iniciar sesión."
    }
}
