package com.example.aqua_check

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.aqua_check.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val bluetoothService by lazy { BluetoothSPPService(this) }

    // --- 1. DEFINICIÓN DE PERMISOS (CORREGIDA: AÑADE COARSE LOCATION) ---
    private val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            // ALERTA RESUELTA
            binding.estadoConexion.text = getString(R.string.state_permission_ok)
            checkBluetoothAdapterState()
        } else {
            // ALERTA RESUELTA
            Toast.makeText(this, getString(R.string.toast_perm_denied), Toast.LENGTH_LONG).show()
            binding.estadoConexion.text = getString(R.string.state_denied)
        }
    }


    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeBluetoothState()
        observeDataFlow()

        binding.botonConectar.setOnClickListener {
            checkBluetoothAdapterState()
        }
        // ALERTA RESUELTA
        binding.estadoConexion.text = getString(R.string.state_initial)
    }

    // --- LÓGICA DE PERMISOS Y ESTADO DEL ADAPTADOR ---

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun checkBluetoothAdapterState() {
        val adapter = BluetoothAdapter.getDefaultAdapter()

        // Verificación de existencia del adaptador
        if (adapter == null) {
            // ALERTA RESUELTA
            Toast.makeText(this, getString(R.string.toast_no_soporta_bt), Toast.LENGTH_LONG).show()
            binding.estadoConexion.text = getString(R.string.error_no_adapter) // Línea 77
            return
        }

        // Verificación del estado del adaptador
        if (!adapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                checkAndRequestPermissions() // Si falta el permiso, pedimos permisos primero
            } else {
                startActivity(enableBtIntent)
                startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                // ALERTA RESUELTA
                binding.estadoConexion.text = getString(R.string.state_bt_off) // Línea 89
            }
        } else {
            // Adaptador encendido, verificar que los permisos estén dados
            checkAndRequestPermissions()
        }
    }

    private fun checkAndRequestPermissions() {
        val missingPermissions = bluetoothPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (missingPermissions.isEmpty()) {
            initiateConnection()
        } else {
            permissionLauncher.launch(missingPermissions)
        }
    }

    private fun initiateConnection() {
        // ALERTA RESUELTA
        binding.estadoConexion.text = getString(R.string.state_connecting) // Línea 111
        lifecycleScope.launch {
            bluetoothService.connect()
        }
    }

    // --- OBSERVADORES Y PARSING ---

    private fun observeDataFlow() {
        lifecycleScope.launch {
            bluetoothService.dataFlow.collect { packet ->
                parseAndUpdateUI(packet)
            }
        }
    }

    private fun observeBluetoothState() {
        lifecycleScope.launch {
            bluetoothService.stateFlow.collect { state ->
                // NOTA: Los strings internos de 'state' (Conectado, Fallo, etc.) vienen del servicio Bluetooth.
                binding.estadoConexion.text = state
                if (state.contains("Conectado")) {
                    binding.estadoConexion.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_green_dark))
                } else if (state.contains("Fallo") || state.contains("Error") || state.contains("perdida")) {
                    binding.estadoConexion.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_dark))
                } else {
                    binding.estadoConexion.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.black))
                }
            }
        }
    }

    private fun parseAndUpdateUI(packet: String) {
        val parts = packet.split('|')

        for (part in parts) {
            val keyValue = part.split(':')
            if (keyValue.size == 2) {
                val key = keyValue[0].trim()
                val value = keyValue[1].trim()

                when (key) {
                    // ALERTAS RESUELTAS: Usando String.format y recursos
                    "Humedad" -> binding.valorHumedadSuelo.text = getString(R.string.format_humedad_adc, value)
                    "Temp" -> binding.valorTempAire.text = getString(R.string.format_temperatura, value)
                    "HumAire" -> binding.valorHumAire.text = getString(R.string.format_hum_aire, value)
                    "pH" -> binding.valorPh.text = value
                    "PPM" -> binding.valorPpm.text = getString(R.string.format_ppm, value)
                }
            }
        }
    }
}