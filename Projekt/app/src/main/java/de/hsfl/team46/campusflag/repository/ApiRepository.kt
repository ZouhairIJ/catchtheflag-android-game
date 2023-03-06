package de.hsfl.team46.campusflag.repository

import android.app.Application
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import de.hsfl.team46.campusflag.model.*
import org.json.JSONArray
import org.json.JSONObject

class ApiRepository(application: Application) {
    companion object {
        private var instance: ApiRepository? = null
        fun getInstance(application: Application) = instance ?: ApiRepository(application)
    }

    private val requestQueue = Volley.newRequestQueue(application)

    fun postGame(hostName: String, points: MutableList<Position>, callback: (Response) -> (Unit)) {
        val url = "https://ctf.letorbi.de/game/register"
        val requestObject = JSONObject(
            mapOf(
                "name" to hostName,
                "points" to JSONArray(Gson().toJson(points))
            )
        )

        val req = JsonObjectRequest(
            Request.Method.POST, url, (requestObject),
            {
                callback(
                    Response(
                        1,
                        it,
                        null
                    )
                )
            },
            {
                Response(
                    -1,
                    null,
                    it.networkResponse.data.decodeToString()
                )
            }
        )

        requestQueue.add(req)
    }

    fun joinGame(player: Player, callback: (Response) -> (Unit)) {

        val url = "https://ctf.letorbi.de/game/join"
        val requestObject = JSONObject(
            mapOf(
                "game" to player.game,
                "name" to player.name,
                "team" to 0
            )
        )

        val req = JsonObjectRequest(
            Request.Method.POST, url, (requestObject),
            {
                callback(
                    Response(
                        1,
                        it,
                        null
                    )
                )
            },
            {
                Response(
                    -1,
                    null,
                    it.networkResponse.data.decodeToString()
                )
            }
        )

        requestQueue.add(req)
    }

    fun getGamePlayers(gameId: Int, name: String, token: String, callback: (Response) -> (Unit)) {

        val url = "https://ctf.letorbi.de/players"
        val requestObject = JSONObject(
            mapOf(
                "game" to gameId,
                "auth" to mapOf("name" to name, "token" to token)
            )
        )

        val req = JsonObjectRequest(
            Request.Method.POST, url, (requestObject),
            {
                callback(
                    Response(
                        1,
                        it,
                        null
                    )
                )
            },
            {
                Response(
                    -1,
                    null,
                    it.networkResponse.data.decodeToString()
                )
            }
        )

        requestQueue.add(req)
    }

    fun getGamePoints(gameId: Int, name: String, token: String, callback: (Response) -> (Unit)) {

        val url = "https://ctf.letorbi.de/points"

        val requestObject = JSONObject(
            mapOf(
                "game" to gameId,
                "auth" to mapOf("name" to name, "token" to token)
            )
        )

        val req = JsonObjectRequest(
            Request.Method.POST, url, (requestObject),
            {
                callback(
                    Response(
                        1,
                        it,
                        null
                    )

                )
            },
            {
                Response(
                    -1,
                    null,
                    it.networkResponse.data.decodeToString()
                )
            }
        )

        requestQueue.add(req)
    }

    fun removePlayer(player: Player, callback: (Response) -> (Unit)) {

        val url = "https://ctf.letorbi.de/player/remove"

        val requestObject = JSONObject(
            mapOf(
                "game" to player.game,
                "name" to player.name,
                "auth" to mapOf("name" to player.name, "token" to player.token)
            )
        )

        val req = JsonObjectRequest(
            Request.Method.POST, url, (requestObject),
            {
                callback(
                    Response(
                        1,
                        it,
                        null
                    )
                )
            },
            {
                callback(
                    Response(
                        -1,
                        null,
                        it.networkResponse.data.decodeToString()
                    )
                )
            }
        )

        requestQueue.add(req)
    }

    fun changePlayer(player: Player, callback: (Response) -> (Unit)) {

        val url = "https://ctf.letorbi.de/player/change"

        val requestObject = JSONObject(
            mapOf(
                "game" to player.game,
                "name" to player.name,
                "addr" to player.addr,
                "auth" to mapOf("name" to player.name, "token" to player.token)
            )
        )

        val req = JsonObjectRequest(
            Request.Method.POST, url, (requestObject),
            {
                callback(
                    Response(
                        1,
                        it,
                        null
                    )
                )
            },
            {
                callback(
                    Response(
                        -1,
                        null,
                        it.networkResponse.data.decodeToString()
                    )
                )
            }
        )

        requestQueue.add(req)
    }

    fun conquerPoint(
        point: Point,
        game: Game,
        player: Player,
        state: Int,
        callback: (Response) -> (Unit)
    ) {

        val url = "https://ctf.letorbi.de/point/conquer"

        val requestObject = JSONObject(
            mapOf(
                "game" to game.game,
                "point" to point.id,
                "team" to state,
                "auth" to mapOf("name" to player.name, "token" to player.token)
            )
        )

        val req = JsonObjectRequest(
            Request.Method.POST, url, (requestObject),
            {
                callback(
                    Response(
                        1,
                        it,
                        null
                    )
                )
            },
            {
                callback(
                    Response(
                        -1,
                        null,
                        it.networkResponse.data.decodeToString()
                    )
                )
            }
        )

        requestQueue.add(req)
    }

    fun startGame(host: Host, callback: (Response) -> (Unit)) {

        val url = "https://ctf.letorbi.de/game/start"

        val requestObject = JSONObject(
            mapOf(
                "game" to host.game,
                "auth" to mapOf("name" to host.name, "token" to host.token)
            )
        )

        val req = JsonObjectRequest(
            Request.Method.POST, url, (requestObject),
            {
                callback(
                    Response(
                        1,
                        it,
                        null
                    )
                )
            },
            {
                callback(
                    Response(
                        -1,
                        null,
                        it.networkResponse.data.decodeToString()
                    )
                )
            }
        )

        requestQueue.add(req)
    }

    fun endGame(player: Player, callback: (Response) -> (Unit)) {

        val url = "https://ctf.letorbi.de/game/end"

        val requestObject = JSONObject(
            mapOf(
                "game" to player.game,
                "auth" to mapOf("name" to player.name, "token" to player.token)
            )
        )

        val req = JsonObjectRequest(
            Request.Method.POST, url, (requestObject),
            {
                callback(
                    Response(
                        1,
                        it,
                        null
                    )
                )
            },
            {
                callback(
                    Response(
                        -1,
                        null,
                        it.networkResponse.data.decodeToString()
                    )
                )
            }
        )

        requestQueue.add(req)
    }
}