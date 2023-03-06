package de.hsfl.team46.campusflag.interfaces

import com.lokibt.bluetooth.BluetoothDevice
import com.lokibt.bluetooth.BluetoothSocket

interface BluetoothActionListener {
    fun onDeviceFound(device: BluetoothDevice)
    fun onDeviceDiscoverable()
    fun onBluetoothConnectionFound(socket: BluetoothSocket)
    fun onClientStart(socket: BluetoothSocket)
    fun onDeviceConnectable()
    fun onBluetoothStarted()
}