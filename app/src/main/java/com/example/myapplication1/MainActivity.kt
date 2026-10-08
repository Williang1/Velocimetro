package com.example.myapplication1

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

class MainActivity : AppCompatActivity() {

    private lateinit var tvVelocidade: TextView
    private lateinit var tvStatusGps: TextView

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Mantém a tela ligada enquanto o velocímetro estiver em uso
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        tvVelocidade = findViewById(R.id.tvVelocidade)
        tvStatusGps = findViewById(R.id.tvStatusGps)

        // Provedor de Localização de alta eficiência do Google
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Configura requisições de GPS a cada 1 segundo
        configurarRequisicaoGps()

        // Callback disparado a cada nova coordenada recebida
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    atualizarVelocidade(location)
                }
            }
        }

        verificarPermissaoEIniciar()
    }

    private fun configurarRequisicaoGps() {
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, // Utiliza GPS e sensores para alta precisão
            1000 // Atualiza a cada 1000 ms (1 segundo)
        ).apply {
            setMinUpdateIntervalMillis(500) // Intervalo mínimo entre atualizações
        }.build()
    }

    private fun atualizarVelocidade(location: Location) {
        if (location.hasSpeed()) {
            val velocidadeMs = location.speed // Velocidade padrão do Android em m/s
            var velocidadeKmh = velocidadeMs * 3.6f // Conversão para km/h

            // Filtro para zerar oscilações com o carro parado
            if (velocidadeKmh < 1.5f) {
                velocidadeKmh = 0f
            }

            // Exibe em formato 3 dígitos (ex: 000, 015, 080)
            val velocidadeFormatada = String.format("%03d", velocidadeKmh.toInt())
            tvVelocidade.text = velocidadeFormatada
            tvStatusGps.text = "GPS Conectado | Precisão: ±${location.accuracy.toInt()}m"
            tvStatusGps.setTextColor(android.graphics.Color.parseColor("#00FF66"))
        } else {
            tvStatusGps.text = "Calculando velocidade..."
            tvStatusGps.setTextColor(android.graphics.Color.parseColor("#FFCC00"))
        }
    }

    private fun verificarPermissaoEIniciar() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            iniciarRastreamentoGps()
        }
    }

    private fun iniciarRastreamentoGps() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            tvStatusGps.text = "Conectando ao GPS..."
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                iniciarRastreamentoGps()
            } else {
                Toast.makeText(this, "Permissão de GPS necessária para o velocímetro!", Toast.LENGTH_LONG).show()
                tvStatusGps.text = "Permissão de GPS negada."
                tvStatusGps.setTextColor(android.graphics.Color.RED)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        verificarPermissaoEIniciar()
    }

    override fun onPause() {
        super.onPause()
        // Desliga atualizações em segundo plano para economizar bateria
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
