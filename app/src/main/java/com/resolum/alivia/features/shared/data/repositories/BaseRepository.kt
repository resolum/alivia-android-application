package com.resolum.alivia.features.shared.data.repositories

import com.google.gson.JsonParser
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

/** Runs backend calls off the main thread and turns failures into messages in Spanish. */
abstract class BaseRepository {

    protected suspend fun <T> safeCall(call: suspend () -> T): NetworkResult<T> = withContext(Dispatchers.IO) {
        try {
            NetworkResult.Success(call())
        } catch (exception: HttpException) {
            NetworkResult.Error(messageOf(exception), exception.code())
        } catch (exception: IOException) {
            NetworkResult.Error("No pudimos conectar con Alivia. Revisa tu conexión e inténtalo otra vez.")
        }
    }

    private fun messageOf(exception: HttpException): String {
        val detail = runCatching {
            val body = exception.response()?.errorBody()?.string().orEmpty()
            JsonParser.parseString(body).asJsonObject.get("detail")?.asString
        }.getOrNull()
        return when (exception.code()) {
            401 -> "Tu sesión terminó. Vuelve a ingresar."
            403 -> detail ?: "No tienes permiso para esta acción."
            404 -> "No encontramos la orden."
            in 500..599 -> "El servicio no está disponible. Inténtalo en unos minutos."
            else -> detail ?: "No se pudo completar la acción."
        }
    }
}
