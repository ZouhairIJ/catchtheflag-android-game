package de.hsfl.team46.campusflag

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.lokibt.bluetooth.BluetoothAdapter
import com.lokibt.bluetooth.BluetoothDevice
import com.lokibt.bluetooth.BluetoothSocket
import de.hsfl.team46.campusflag.databinding.FragmentLobbyyBinding
import de.hsfl.team46.campusflag.interfaces.BluetoothActionListener
import de.hsfl.team46.campusflag.model.CustomError
import de.hsfl.team46.campusflag.model.Player
import de.hsfl.team46.campusflag.repository.BluetoothRepository
import de.hsfl.team46.campusflag.viewmodels.ViewModel
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.*
import kotlin.concurrent.timerTask


class LobbyyFragment : Fragment(), BluetoothActionListener {
    companion object {
        const val TAG = "LobbyyFragment"
    }

    // ViewModel
    private val mainViewModel: ViewModel by activityViewModels()

    // Bluetooth
    private lateinit var bluetoothRepository: BluetoothRepository

    private var _binding: FragmentLobbyyBinding? = null
    private val binding get() = _binding!!

    private val players = mutableListOf<Player>()
    private val playersAdapter = PlayersAdapter(players) {
        Toast.makeText(context, "Hello ${it.name} :)", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inflate the layout for this fragment
        _binding = FragmentLobbyyBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = mainViewModel

        // Start Game button
        binding.startGameBtnLobby.setOnClickListener {
            mainViewModel.startGame { isError ->
                if (!isError) {
                    findNavController().navigate(R.id.action_lobbyy_to_game)
                }
            }
        }

        // Leave button
        binding.leaveBtnLobby.setOnClickListener {
            mainViewModel.removePlayer { isError ->
                if (!isError) {
                    mainViewModel.setCurrentLoggedUser(null)
                    findNavController().navigate(R.id.action_lobbyyFragment_to_startFragment2)
                }
            }
        }

        // Players List
        binding.recyclerViewPlayers.adapter = playersAdapter
        mainViewModel.getPlayers().observe(viewLifecycleOwner) {
            players.clear()
            players.addAll(it)
            playersAdapter.notifyDataSetChanged()
        }

        // Update bluetooth address for each player
        if (mainViewModel.getCurrentLoggedUserFlag().value == "host") {
            mainViewModel.getConnectedPlayers().observe(viewLifecycleOwner) {
                mainViewModel.getConnectedPlayers().value?.forEach { pl ->
                    mainViewModel.changePlayer(pl) {}
                }
            }
        }

        // Display Start Game button only for host
        if (mainViewModel.getCurrentLoggedUserFlag().value == "host") {
            binding.startGameBtnLobby.visibility = View.VISIBLE
        } else {
            binding.startGameBtnLobby.visibility = View.GONE
            mainViewModel.getGame().observe(viewLifecycleOwner) {
                if (mainViewModel.getGame().value?.state == 1) {
                    findNavController().navigate(R.id.action_lobbyy_to_game)
                }
            }
        }

        Timer().scheduleAtFixedRate(timerTask {
            mainViewModel.fetchPlayers()
        }, 0, 10000)


        return binding.root
    }

    override fun onStart() {
        super.onStart()

        // Bluetooth
        if (mainViewModel.bluetoothRepository == null) {
            mainViewModel.bluetoothRepository = BluetoothRepository(this)
            bluetoothRepository = mainViewModel.bluetoothRepository!!
        }

        // Broadcast Receiver
        if (mainViewModel.broadcastReceiver == null) {
            mainViewModel.broadcastReceiver =
                mainViewModel.bluetoothRepository!!.createBroadcastReceiver()
        }

        if (mainViewModel.getCurrentLoggedUserFlag().value == "host") {
            setupServer()
        } else {
            setupDiscovery()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothRepository.stopServer()
    }

    private fun setupServer() {
        Log.d(TAG, "Setup Server")
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)

        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        intentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
        enableBluetooth(intent, intentFilter)
    }

    private fun setupDiscovery() {
        Log.d(TAG, "Setup Discovery")
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        intentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND)
        enableBluetooth(intent, intentFilter)
    }

    private fun enableBluetooth(intent: Intent, intentFilter: IntentFilter) {
        requireActivity().registerReceiver(mainViewModel.broadcastReceiver, intentFilter)
        startActivity(intent)
    }

    private fun startServer() {
        bluetoothRepository.startServer(
            mainViewModel.bluetoothServerSocketName,
            mainViewModel.bluetoothServerSocketUUID
        )
    }

    override fun onDeviceFound(device: BluetoothDevice) {
        mainViewModel.bluetoothDevice = device
        bluetoothRepository.startDiscovery(
            device,
            mainViewModel.bluetoothServerSocketUUID
        )
    }

    override fun onDeviceDiscoverable() {
        startServer()
    }

    override fun onBluetoothConnectionFound(socket: BluetoothSocket) {
        Log.d("BluetoothServer", "Bluetooth Connection Found")

        val reader = BufferedReader(InputStreamReader(socket.inputStream))
        val writer = BufferedWriter(OutputStreamWriter(socket.outputStream))

        val dataInput = Gson().fromJson(reader.readLine(), Player::class.java)
        Log.d("BluetoothServer", "Found Player: $dataInput")

        val connectedPlayer = Player(
            dataInput.game,
            dataInput.name,
            dataInput.team,
            dataInput.token,
            socket.remoteDevice.address
        )

        val hostPlayer = Player(
            dataInput.game,
            mainViewModel.getHost().value?.name,
            null,
            mainViewModel.getHost().value?.token,
            dataInput.addr,
        )

        mainViewModel.setConnectedPlayers(listOf(connectedPlayer, hostPlayer))

        val data = "${Gson().toJson(CustomError(null))}\n"
        writer.write(data)
        writer.flush()
    }


    override fun onClientStart(socket: BluetoothSocket) {
        Log.d("BluetoothClient", "Start BluetoothClient")

        val reader = BufferedReader(InputStreamReader(socket.inputStream))
        val writer = BufferedWriter(OutputStreamWriter(socket.outputStream))

        val data = Player(
            mainViewModel.getPlayer().value?.game,
            mainViewModel.getPlayer().value?.name,
            mainViewModel.getPlayer().value?.team,
            mainViewModel.getPlayer().value?.token,
            socket.remoteDevice.address,
        )

        val rawData = "${Gson().toJson(data)}\n"
        Log.d("BluetoothClient", "Send Data $rawData to Host (${socket.remoteDevice.address}?)")

        writer.write(rawData)
        writer.flush()

        val response = Gson().fromJson(reader.readLine(), CustomError::class.java)
        Log.d("BluetoothClient", "Received Host Response: $response")
    }

    override fun onDeviceConnectable() {
        if (bluetoothRepository.isServerStarted) {
            bluetoothRepository.stopServer()
        }
    }

    override fun onBluetoothStarted() {
        if (mainViewModel.getCurrentLoggedUserFlag().value == "player") {
            bluetoothRepository.adapter.startDiscovery()
        }
    }
}