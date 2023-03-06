package de.hsfl.team46.campusflag

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.lokibt.bluetooth.BluetoothAdapter
import com.lokibt.bluetooth.BluetoothDevice
import com.lokibt.bluetooth.BluetoothSocket
import de.hsfl.team46.campusflag.custom.CustomMapViewGame
import de.hsfl.team46.campusflag.databinding.FragmentGameBinding
import de.hsfl.team46.campusflag.interfaces.BluetoothActionListener
import de.hsfl.team46.campusflag.model.Point
import de.hsfl.team46.campusflag.repository.BluetoothRepository
import de.hsfl.team46.campusflag.viewmodels.ViewModel
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.*
import kotlin.concurrent.timerTask


class GameFragment : Fragment(), BluetoothActionListener {
    private val mainViewModel: ViewModel by activityViewModels()

    private var _binding: FragmentGameBinding? = null
    private val binding get() = _binding!!

    private var customMapViewGame: CustomMapViewGame? = null

    // Bluetooth
    private lateinit var bluetoothRepository: BluetoothRepository

    // GPS Location Permission
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("Permission", "Granted")
            } else {
                Log.d("Permission", "Denied")
            }
        }

    private var winnerTeam: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentGameBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = mainViewModel

        customMapViewGame = binding.mapViewGame
        customMapViewGame!!.viewModel = mainViewModel

        binding.leaveBtnGame.setOnClickListener {
            mainViewModel.removePlayer { isError ->
                if (!isError) {
                    mainViewModel.setCurrentLoggedUser(null)
                    findNavController().navigate(R.id.action_game_to_start)
                }
            }
        }

        Timer().scheduleAtFixedRate(timerTask {
            mainViewModel.fetchPoints {}
        }, 0, 10000)

        mainViewModel.points.observe(binding.lifecycleOwner!!) {
            if (mainViewModel.getGame().value?.points != null) {
                val customMapVGame = CustomMapViewGame(binding.root.context, null)
                customMapVGame.viewModel = mainViewModel

                customMapVGame.layoutParams = binding.mapViewGame.layoutParams

                binding.frameLayout5.removeView(binding.mapViewGame)
                binding.frameLayout5.addView(customMapVGame, binding.mapViewGame.layoutParams)

                checkGameStatus()
            }
        }

        mainViewModel.currentLocation.observe(binding.lifecycleOwner!!) {
            if (mainViewModel.currentLocation.value != null && mainViewModel.points.value != null) {
                conquerFlag(mainViewModel.currentLocation.value!!)
            }
        }

        mainViewModel.gameStatus.observe(binding.lifecycleOwner!!) {
            if (mainViewModel.gameStatus.value == 2) {
                mainViewModel.endGame {
                    if (!it) {
                        // setup the alert builder
                        val builder = AlertDialog.Builder(binding.root.context)
                        builder.setTitle("Game ended")
                        builder.setMessage("Team $winnerTeam won!")

                        // add a button
                        builder.setPositiveButton(
                            "OK"
                        ) { dialog, _ ->
                            mainViewModel.resetSettings()
                            findNavController().navigate(R.id.action_game_to_start)
                            dialog.dismiss()
                        }

                        // create and show the alert dialog
                        val dialog = builder.create()
                        dialog.show()
                    }
                }
            }
        }

        displayCurrentLocation()

        return binding.root
    }

    override fun onStart() {
        super.onStart()

        bluetoothRepository = mainViewModel.bluetoothRepository!!
        bluetoothRepository.listener = this

        setupServer()
    }

    private fun setupServer() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 6000)

        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        intentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
        enableBluetooth(intent, intentFilter)
    }

    private fun enableBluetooth(intent: Intent, intentFilter: IntentFilter) {
        requireActivity().registerReceiver(mainViewModel.broadcastReceiver, intentFilter)
        startActivity(intent)
    }

    override fun onDeviceFound(device: BluetoothDevice) {
        bluetoothRepository.startDiscovery(device, mainViewModel.bluetoothServerSocketUUID)
    }

    override fun onDeviceDiscoverable() {
        if (!bluetoothRepository.isServerStarted) {
            bluetoothRepository.startServer(
                mainViewModel.bluetoothServerSocketName,
                mainViewModel.bluetoothServerSocketUUID
            )
        }
    }

    override fun onBluetoothConnectionFound(socket: BluetoothSocket) {
        if (!bluetoothRepository.isServerStarted) {
            bluetoothRepository.startServer(
                mainViewModel.bluetoothServerSocketName,
                mainViewModel.bluetoothServerSocketUUID
            )
        }
    }

    override fun onClientStart(socket: BluetoothSocket) {
        Log.d("BluetoothClient", "Start BluetoothClient")

        val reader = BufferedReader(InputStreamReader(socket.inputStream))
        val writer = BufferedWriter(OutputStreamWriter(socket.outputStream))

        val data = "${Gson().toJson(mainViewModel.getCurrentLoggedUser().value)}\n"

        writer.write(data)
        writer.flush()
        reader.readLine()

        bluetoothRepository.adapter.disable()
    }

    override fun onDeviceConnectable() {
        if (bluetoothRepository.isServerStarted) {
            bluetoothRepository.stopServer()
        }
    }

    override fun onBluetoothStarted() {
        bluetoothRepository.adapter.startDiscovery()
    }

    private fun displayCurrentLocation() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                binding.root.context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                mainViewModel.getLocationRepository().requestCurrentLocation(binding.root.context)
            }
            else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }
    }

    private fun conquerFlag(currentLocation: Location) {
        var flagPosition: Point? = null

        mainViewModel.points.value?.forEach { pt ->
            val loc = Location(LocationManager.GPS_PROVIDER)
            loc.latitude = pt.lat
            loc.longitude = pt.long

            if (currentLocation.distanceTo(loc) <= 5 && pt.team != mainViewModel.getCurrentLoggedUser().value?.team!!) {
                flagPosition = pt
            }
        }

        flagPosition?.let {
            mainViewModel.conquerPoint(it, -1) {}

            Handler(Looper.getMainLooper()).postDelayed(
                {
                    mainViewModel.conquerPoint(
                        it,
                        mainViewModel.getCurrentLoggedUser().value?.team!!
                    ) {}
                },
                10000 // value in milliseconds
            )
        }
    }

    private fun checkGameStatus() {
        var teamOne = listOf<Point>()
        var teamTwo = listOf<Point>()

        mainViewModel.points.value?.forEach { pt ->
            if (pt.team == 1) {
                teamOne = teamOne + listOf(pt)
            }

            if (pt.team == 2) {
                teamTwo = teamTwo + listOf(pt)
            }
        }

        if (teamOne.size == mainViewModel.points.value?.size) {
            mainViewModel.gameStatus.value = 2
            winnerTeam = 1
        }
        if (teamTwo.size == mainViewModel.points.value?.size) {
            mainViewModel.gameStatus.value = 2
            winnerTeam = 2
        }
    }
}