package de.hsfl.team46.campusflag.model

import org.json.JSONArray
import org.json.JSONObject


data class Host(
    var game: Int?, var name: String?, var token: String?
)

data class Game(
    var game: Int?, var state: Int?, var players: JSONArray?, var points: List<Point>?
)

data class Player(
    var game: Int?, var name: String?, var team: Int?, var token: String?, var addr: String?
)

data class Position(
    val lat: Double = 0.0,
    val long: Double = 0.0
)

data class Point(
    val id: Int?, val team: Int?, val lat: Double = 0.0, val long: Double = 0.0
)

data class Response(
    val status: Int, val result: JSONObject?, val error: String?
)

data class CustomError(
    var error: String?
)

