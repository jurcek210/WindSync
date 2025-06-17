package database

import okhttp3.*
import com.google.gson.Gson
import models.*
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import okhttp3.Callback

val client = OkHttpClient()
val gson = Gson()

fun addUserToApi(username: String, email: String, password: String, onResult: (Boolean, String) -> Unit) {
    val userJson = gson.toJson(mapOf("username" to username, "email" to email, "password" to password))

    val request = Request.Builder()
        .url("http://localhost:3001/api/users/register")
        .post(RequestBody.create("application/json".toMediaType(), userJson))
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onResult(false, "Napaka: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                onResult(true, "Uporabnik uspešno dodan")
            } else {
                onResult(false, "Napaka: ${response.message}")
            }
        }
    })
}
fun fetchUsersFromApi(onResult: (List<UserSimple>) -> Unit) {
    val request = Request.Builder()
        .url("http://localhost:3001/api/users")
        .get()
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            println("Napaka: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            response.body?.string()?.let { json ->
                val users = gson.fromJson(json, Array<UserSimple>::class.java).toList()
                onResult(users)
            }
        }
    })
}

