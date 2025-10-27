package com.example.aqua_check

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
private const val TAG = "BluetoothSPPService"
private const val MAC_HC05 = "00:23:10:00:D6:CB"

class BluetoothSPPService(private val context: Context) {

    // SOLUCIÓN: LA ANOTACIÓN FUE ELIMINADA DE AQUÍ (LÍNEA 27)
    @Suppress("DEPRECATION")
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        // Usamos la API moderna: BluetoothManager (API 23+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            manager.adapter
        } else {
            BluetoothAdapter.getDefaultAdapter()
        }
    }

    private var bluetoothSocket: BluetoothSocket? = null
    private var device: BluetoothDevice? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private val _dataFlow = MutableSharedFlow<String>()
    val dataFlow: SharedFlow<String> = _dataFlow

    private val _stateFlow = MutableSharedFlow<String>()
    val stateFlow: SharedFlow<String> = _stateFlow

    val isConnected: Boolean
        get() = bluetoothSocket?.isConnected == true

    // @SuppressLint("MissingPermission") es correcto aquí, ya que cubre las llamadas nativas internas.
    @SuppressLint("MissingPermission")
    suspend fun connect(): Boolean {
        // Asignamos a una variable local no nullable para evitar el error de 'Smart Cast'
        val adapter = bluetoothAdapter

        // 2. SOLUCIÓN AL ERROR DE SMART CAST
        if (adapter == null || !adapter.isEnabled) {
            _stateFlow.emit("Error: Bluetooth apagado o no disponible.")
            return false
        }

        if (isConnected) return true

        try {
            // 1. Obtener el dispositivo por su MAC
            device = adapter.getRemoteDevice(MAC_HC05)
            if (device == null) {
                _stateFlow.emit("Error: Dispositivo no encontrado por MAC.")
                return false
            }

            bluetoothSocket = device!!.createRfcommSocketToServiceRecord(SPP_UUID)
            adapter.cancelDiscovery() // Cubierto por la anotación de la función

            withContext(Dispatchers.IO) {
                bluetoothSocket!!.connect()
            }

            inputStream = bluetoothSocket!!.inputStream
            outputStream = bluetoothSocket!!.outputStream

            _stateFlow.emit("Conectado con éxito a ${device!!.name ?: "HC-05"}")

            withContext(Dispatchers.IO) {
                readDataLoop()
            }
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Fallo la conexión SPP. Error: ${e.message}")
            _stateFlow.emit("Fallo la conexión. Verifique PIN (1234) y emparejamiento.")
            closeConnection()
            return false
        }
    }

    // ... (El resto de funciones se mantienen igual)
    private suspend fun readDataLoop() {
        val buffer = ByteArray(1024)
        val dataBuffer = StringBuilder()

        try {
            while (isConnected) {
                val bytes = inputStream!!.read(buffer)

                if (bytes > 0) {
                    val chunk = String(buffer, 0, bytes)
                    dataBuffer.append(chunk)

                    while (dataBuffer.contains('\n')) {
                        val endOfLine = dataBuffer.indexOf('\n')
                        val fullPacket = dataBuffer.substring(0, endOfLine).trim()

                        _dataFlow.emit(fullPacket)

                        dataBuffer.delete(0, endOfLine + 1)
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Conexión perdida: ${e.message}")
            closeConnection()
            _stateFlow.emit("Conexión perdida.")
        }
    }

    private fun closeConnection() {
        try { bluetoothSocket?.close() } catch (e: IOException) { Log.e(TAG, "Error al cerrar el socket: ${e.message}") }
        bluetoothSocket = null
        inputStream = null
        outputStream = null
    }
}