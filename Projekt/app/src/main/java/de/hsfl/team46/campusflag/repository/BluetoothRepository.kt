package de.hsfl.team46.campusflag.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.lokibt.bluetooth.BluetoothAdapter
import com.lokibt.bluetooth.BluetoothDevice
import com.lokibt.bluetooth.BluetoothServerSocket
import com.lokibt.bluetooth.BluetoothSocket
import de.hsfl.team46.campusflag.interfaces.BluetoothActionListener
import java.util.*

class BluetoothRepository(private val bluetoothActionListener: BluetoothActionListener?) {
    companion object {
        const val TAG = "BluetoothRepository"
    }

    var adapter = BluetoothAdapter.getDefaultAdapter()
    var listener = bluetoothActionListener

    var serverSocket: BluetoothServerSocket? = null
    var dataSocket: BluetoothSocket? = null

    var isBluetoothEnabled = false
    var isServerStarted = false


    fun createBroadcastReceiver(): BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent!!.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    bluetoothStateListener(
                        context!!,
                        intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                    )
                }

                BluetoothAdapter.ACTION_SCAN_MODE_CHANGED -> {
                    bluetoothModeListener(
                        context!!,
                        intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1)
                    )
                }

                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Toast.makeText(
                        context,
                        "Discovering devices...",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d(TAG, "Discovering devices...")
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d(TAG, "Discovery finished")
                }

                BluetoothDevice.ACTION_FOUND -> {
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                    Toast.makeText(
                        context,
                        "Bluetooth device found with address: ${device!!.address}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.d(TAG, "Bluetooth device found with address: $device")

                    listener!!.onDeviceFound(device)
                }
            }
        }

    }

    private fun bluetoothModeListener(context: Context, mode: Int) {
        when (mode) {
            BluetoothAdapter.SCAN_MODE_NONE -> {
                Toast.makeText(
                    context,
                    "Device not visible for other devices!",
                    Toast.LENGTH_SHORT
                ).show()
                Log.d(TAG, "Device not visible for other devices!")
            }

            BluetoothAdapter.SCAN_MODE_CONNECTABLE -> {
                listener!!.onDeviceConnectable()
                Toast.makeText(
                    context,
                    "Device is connectable for other devices!",
                    Toast.LENGTH_SHORT
                ).show()
                Log.d(TAG, "Device is connectable for other devices!")
            }

            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE -> {
                listener!!.onDeviceDiscoverable()
                Log.d(TAG, "Device is visible for other devices!")
            }

            else -> {
                throw Exception("BluetoothScanModeException: Bluetooth Scan Mode not found!")
            }
        }
    }

    private fun bluetoothStateListener(context: Context, state: Int) {
        when (state) {

            BluetoothAdapter.STATE_ON -> {
                isBluetoothEnabled = true
                Toast.makeText(
                    context,
                    "Bluetooth is enabled!",
                    Toast.LENGTH_SHORT
                ).show()
                Log.d(TAG, "Bluetooth is enabled!")

                listener!!.onBluetoothStarted()
            }

            BluetoothAdapter.STATE_OFF -> {
                isBluetoothEnabled = false
                Toast.makeText(
                    context,
                    "Bluetooth is disabled!",
                    Toast.LENGTH_SHORT
                ).show()

                Log.d(TAG, "Bluetooth is disabled!")
            }

            BluetoothAdapter.STATE_TURNING_ON -> {
                Log.d(TAG, "Enabling Bluetooth...")
            }

            BluetoothAdapter.STATE_TURNING_OFF -> {
                Log.d(TAG, "Disabling Bluetooth...")
            }

            else -> {
                throw Exception("BluetoothStateException: BluetoothState not found!")
            }
        }
    }


    fun startServer(name: String, uuid: UUID) {
        Thread {
            try {
                isServerStarted = true

                Log.d(TAG, "Starting Server")

                serverSocket = adapter.listenUsingRfcommWithServiceRecord(name, uuid)

                Log.d(TAG, "Server Started")
                Log.d(TAG, "Waiting for connections...")

                while (true) {
                    dataSocket = serverSocket!!.accept()
                    listener!!.onBluetoothConnectionFound(dataSocket!!)
                    dataSocket!!.close()
                }
            } catch (e: Exception) {
                Log.d(TAG, "BluetoothServerSocketError: $e")
            }

            try {
                if (serverSocket != null) {
                    serverSocket!!.close()
                    Log.d(TAG, "serverSocket closed")
                }

                if (dataSocket != null) {
                    dataSocket!!.close()
                    Log.d(TAG, "DataSocket closed")
                }
            } catch (e: Exception) {
                Log.d(TAG, "Error while starting server: $e")
            }

            isServerStarted = false
        }.start()
    }

    fun stopServer() {
        try {
            Log.d(TAG, "Stopping Server")

            if (serverSocket != null) {
                serverSocket!!.close()
            }

            if (dataSocket != null) {
                dataSocket!!.close()
            }

            Log.d(TAG, "Server stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error while stopping server: $e")
        }

        isServerStarted = false
    }

    fun startDiscovery(device: BluetoothDevice, uuid: UUID) {
        Thread {
            try {
                Log.d(TAG, "Starting Discovery...")

                dataSocket = device.createRfcommSocketToServiceRecord(uuid)

                dataSocket!!.connect()
                Log.d(TAG, "Connected with ${device.address}")

                Log.d(TAG, "Sending data...")
                listener!!.onClientStart(dataSocket!!)

                dataSocket!!.close()
                Log.d(TAG, "Data has been sent")

            } catch (e: Exception) {
                Log.d(TAG, "StartDiscovery Error: $e")
            }

            try {
                if (dataSocket != null) {
                    Log.d(TAG, "Stopping Discovery...")

                    dataSocket!!.close()
                    Log.d(TAG, "Discovery stopped")
                }

            } catch (e: Exception) {
                Log.d(TAG, "StopDiscovery Error: $e")
            }

        }.start()
    }
}