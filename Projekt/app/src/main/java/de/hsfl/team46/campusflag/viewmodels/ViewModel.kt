package de.hsfl.team46.campusflag.viewmodels

import android.app.Application
import android.content.BroadcastReceiver
import android.location.Location
import android.text.Editable
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.lokibt.bluetooth.BluetoothDevice
import de.hsfl.team46.campusflag.enums.Colors
import de.hsfl.team46.campusflag.model.*
import de.hsfl.team46.campusflag.repository.ApiRepository
import de.hsfl.team46.campusflag.repository.BluetoothRepository
import de.hsfl.team46.campusflag.repository.LocationRepository
import java.util.*

class ViewModel(application: Application) : AndroidViewModel(application) {
    private val topLeftCoordinates = Position(54.778514, 9.442749)
    private val bottomRightCoordinates = Position(54.769009, 9.464722)

    // Repositories
    private val apiRepository = ApiRepository.getInstance(getApplication())
    private val locationRepository = LocationRepository()
    fun getLocationRepository(): LocationRepository = locationRepository

    // Bluetooth
    val bluetoothServerSocketName = "de.hsfl.team49.captureflag"
    val bluetoothServerSocketUUID: UUID = UUID.fromString("38d98b19-b4aa-4561-9882-058db64dd7b7")
    var bluetoothRepository: BluetoothRepository? = null
    var broadcastReceiver: BroadcastReceiver? = null
    var bluetoothDevice: BluetoothDevice? = null

    // Game Inputs
    private var hostName: MutableLiveData<String> = MutableLiveData("")
    fun getHostName(): MutableLiveData<String> = hostName
    fun setHostName(s: Editable) {
        hostName.value = s.toString()

    }

    private val gameId: MutableLiveData<String> = MutableLiveData("")
    fun getGameId(): MutableLiveData<String> = gameId
    fun setGameId(s: Editable) {
        if (s.toString().isNotEmpty()) {
            gameId.value = s.toString()
        }
    }

    fun setGameId(s: Int) {
        gameId.value = s.toString()
    }


    // Game Data Objects
    private var host: MutableLiveData<Host> = MutableLiveData()
    fun getHost(): MutableLiveData<Host> = host

    private val game: MutableLiveData<Game> = MutableLiveData()
    fun getGame(): MutableLiveData<Game> = game

    private var player: MutableLiveData<Player> = MutableLiveData()
    fun getPlayer(): MutableLiveData<Player> = player
    fun setPlayer(s: Editable) {
        player.value = Player(
            null,
            s.toString(),
            null,
            null,
            null
        )
    }

    fun setPlayer(s: Player) {
        player.value = s
    }

    var currentLocation: LiveData<Location> = locationRepository.getCurrentLocation()

    var currentHostFlagPositions: MutableList<Position> = mutableListOf()
    fun setCurrentHostFlagPositions(location: Position) {
        currentHostFlagPositions.add(location)
    }

    private val currentLoggedUser: MutableLiveData<Player?> = MutableLiveData()
    fun getCurrentLoggedUser(): MutableLiveData<Player?> = currentLoggedUser
    fun setCurrentLoggedUser(p: Player?) {
        currentLoggedUser.value = p

    }

    private val currentLoggedUserFlag: MutableLiveData<String?> = MutableLiveData()
    fun getCurrentLoggedUserFlag(): MutableLiveData<String?> = currentLoggedUserFlag
    fun setCurrentLoggedUserFlag(s: String?) {
        currentLoggedUserFlag.value = s

    }

    private val players: MutableLiveData<List<Player>> = MutableLiveData(mutableListOf())
    fun getPlayers(): MutableLiveData<List<Player>> = players

    private val connectedPlayers: MutableLiveData<List<Player>> = MutableLiveData(mutableListOf())
    fun getConnectedPlayers(): LiveData<List<Player>> = connectedPlayers
    fun setConnectedPlayers(s: List<Player>) {
        connectedPlayers.postValue(s)
    }

    private val positions: MutableLiveData<List<Position>> = MutableLiveData(mutableListOf())
    fun getPositions(): LiveData<List<Position>> = positions
    fun setPositions(s: List<Position>) {
        positions.postValue(s)
    }

    var points: MutableLiveData<List<Point>> = MutableLiveData()

    var gameStatus: MutableLiveData<Int> = MutableLiveData(1)


    // Game Methods
    fun createGame(callback: (Boolean) -> (Unit)) {
        apiRepository.postGame(hostName.value!!, currentHostFlagPositions) {
            if (it.status == -1) {
                Toast.makeText(getApplication(), it.error, Toast.LENGTH_LONG).show()
                callback(true)
            } else {
                host.value = Host(
                    it.result?.getInt("game"),
                    it.result?.getString("name"),
                    it.result?.getString("token")
                )

                game.value = Game(it.result?.getInt("game"), null, null, null)

                currentLoggedUser.value = Player(
                    it.result?.getInt("game"),
                    it.result?.getString("name"),
                    1,
                    it.result?.getString("token"),
                    null,
                )
                currentLoggedUserFlag.value = "host"

                callback(false)
            }
        }
    }

    fun joinGame(callback: (Boolean) -> (Unit)) {
        player.value!!.game = gameId.value?.toInt()

        apiRepository.joinGame(player.value!!) {
            if (it.result?.getString("token") == null) {
                Toast.makeText(
                    getApplication(), it.error,
                    Toast.LENGTH_LONG
                ).show()
                callback(true)
            } else {
                player.value = Player(
                    it.result.getInt("game"),
                    it.result.getString("name"),
                    it.result.getInt("team"),
                    it.result.getString("token"),
                    null
                )
                currentLoggedUser.value = Player(
                    it.result.getInt("game"),
                    it.result.getString("name"),
                    it.result.getInt("team"),
                    it.result.getString("token"),
                    null
                )
                game.value = Game(gameId.value?.toInt(), null, null, null)

                callback(false)
            }
        }
    }

    fun fetchPlayers() {
        if (currentLoggedUser.value != null) {
            apiRepository.getGamePlayers(
                game.value?.game!!,
                currentLoggedUser.value!!.name!!,
                currentLoggedUser.value!!.token!!
            ) {

                if (it.result != null) {
                    game.value?.players = it.result.getJSONArray("players")
                    players.value = Gson().fromJson(
                        it.result.getJSONArray("players").toString(),
                        Array<Player>::class.java
                    ).toList()

                    val newGame: Game = game.value!!
                    newGame.state = it.result.getInt("state")
                    game.postValue(newGame)
                }
            }
        }
    }

    fun fetchPoints(callback: (Game) -> (Unit)) {


        if (currentLoggedUser.value != null) {
            apiRepository.getGamePoints(
                game.value?.game!!,
                currentLoggedUser.value!!.name!!,
                currentLoggedUser.value!!.token!!
            ) {
                game.value!!.points = Gson().fromJson(
                    it.result?.getJSONArray("points").toString(),
                    Array<Point>::class.java
                ).toList()
                points.value = Gson().fromJson(
                    it.result?.getJSONArray("points").toString(),
                    Array<Point>::class.java
                ).toList()
                callback(
                    Game(
                        it.result?.getInt("game"),
                        it.result?.getInt("state"),
                        null,
                        Gson().fromJson(
                            it.result?.getJSONArray("points").toString(),
                            Array<Point>::class.java
                        ).toList()
                    )
                )
            }
        }
    }

    fun removePlayer(callback: (Boolean) -> (Unit)) {
        if (currentLoggedUser.value != null) {
            apiRepository.removePlayer(currentLoggedUser.value!!) {
                if (it.status == -1) {
                    Toast.makeText(
                        getApplication(), it.error,
                        Toast.LENGTH_LONG
                    ).show()

                    if (currentLoggedUserFlag.value == "host") {
                        host.value = null
                    } else {
                        player.value = null
                    }

                    callback(true)
                } else {
                    callback(false)
                }
            }
        } else {
            callback(false)
        }
    }

    fun changePlayer(player: Player, callback: (Boolean) -> (Unit)) {
        apiRepository.changePlayer(player) {
            if (it.status == -1) {
                Toast.makeText(getApplication(), it.error, Toast.LENGTH_LONG).show()
                callback(true)
            } else {
                callback(false)
            }
        }
    }

    // Y
    fun getLatScaling(lat: Double): Double {
        return ((lat - topLeftCoordinates.lat) / (bottomRightCoordinates.lat - topLeftCoordinates.lat) / 2 * 1536 / 2)
    }

    // X
    fun getLongScaling(long: Double): Double {
        return ((long - topLeftCoordinates.long) / (bottomRightCoordinates.long - topLeftCoordinates.long) / 2 * 2048 / 2)
    }

    fun getColorFromApi(num: Int): Int {
        var c = 0

        if (num == -1) c = Colors.YELLOW.rgb
        if (num == 0) c = Colors.GRAY.rgb
        if (num == 1) c = Colors.BLUE.rgb
        if (num == 2) c = Colors.RED.rgb

        return c
    }

    fun conquerPoint(point: Point, state: Int, callback: (Boolean) -> (Unit)) {
        apiRepository.conquerPoint(point, game.value!!, currentLoggedUser.value!!, state) {
            if (it.status == -1) {
                callback(true)
            } else {
                callback(false)
            }
        }
    }

    fun startGame(callback: (Boolean) -> (Unit)) {
        apiRepository.startGame(host.value!!) {
            if (it.status == -1) {
                Toast.makeText(getApplication(), it.error, Toast.LENGTH_LONG).show()
                callback(true)
            } else {
                callback(false)
            }
        }
    }

    fun endGame(callback: (Boolean) -> (Unit)) {
        apiRepository.endGame(currentLoggedUser.value!!) {
            if (it.status == -1) {
                Toast.makeText(getApplication(), it.error, Toast.LENGTH_LONG).show()
                callback(true)
            } else {
                callback(false)
            }
        }
    }

    fun resetSettings() {
        host.value = null
        gameId.value = null
        game.value = null
        player.value = null

        currentHostFlagPositions = mutableListOf()
        currentLoggedUser.value = null
        currentLoggedUserFlag.value = null

        players.value = mutableListOf()
        connectedPlayers.value = mutableListOf()

        positions.value = null
        points.value = null

        hostName.value = ""
    }
}