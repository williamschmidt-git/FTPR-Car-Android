package com.example.myapitest

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapitest.adapter.CarAdapter
import com.example.myapitest.database.DatabaseBuilder
import com.example.myapitest.database.model.UserLocation
import com.example.myapitest.databinding.ActivityMainBinding
import com.example.myapitest.model.Car
import com.example.myapitest.service.Result
import com.example.myapitest.service.RetroFitClient
import com.example.myapitest.service.safeApiCall
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestLocationPermission()
        setupView()

        // 1- Criar tela de Login com algum provedor do Firebase (Telefone, Google)
        //      Cadastrar o Seguinte celular para login de test: +5511912345678
        //      Código de verificação: 101010

        // 2- Criar Opção de Logout no aplicativo

        // 3- Integrar API REST /car no aplicativo
        //      API será disponibilida no Github
        //      JSON Necessário para salvar e exibir no aplicativo
        //      O Image Url deve ser uma foto armazenada no Firebase Storage
        //      { "id": "001", "imageUrl":"https://image", "year":"2020/2020", "name":"Gaspar", "licence":"ABC-1234", "place": {"lat": 0, "long": 0} }

        // Opcionalmente trabalhar com o Google Maps ara enviar o place
    }

    override fun onResume() {
        super.onResume()
        fetchItems()
    }

    private fun setupView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = true
            fetchItems()
        }

        binding.addCta.setOnClickListener {
            navigateToCarView()
        }
    }

    private fun navigateToCarView() {
        startActivity(NewCarActivity.newIntent(this))
    }

    private fun requestLocationPermission() {
        // TODO
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationPermissionLauncher = registerForActivityResult(ActivityResultContracts
            .RequestPermission()) { isGranted ->
                if (isGranted) {
                    getLastLocation()
                } else {
                    Toast.makeText(
                        this,
                        "Permissão de localização negada",
                        Toast.LENGTH_SHORT)
                        .show()
                }
        }

        checkLocationPermissionAndRequest()
    }

    private fun checkLocationPermissionAndRequest() {
        when {
            checkSelfPermission(ACCESS_FINE_LOCATION) == PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED -> {
                        getLastLocation()
                    }
            shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION) -> {
                locationPermissionLauncher.launch(ACCESS_FINE_LOCATION)
            }
            shouldShowRequestPermissionRationale(ACCESS_COARSE_LOCATION) -> {
                locationPermissionLauncher.launch(ACCESS_COARSE_LOCATION)
            }
            else -> {
                locationPermissionLauncher.launch(ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun getLastLocation() {
        if (
            ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED
            ) {
                requestLocationPermission()
                return
        }
        fusedLocationProviderClient.lastLocation.addOnCompleteListener { task: Task<Location> ->
            if(task.isSuccessful) {
                val location = task.result
                Log.d("Hello world", "Lat: ${location.latitude} Long: ${location.longitude}")
                val userLocation =
                    UserLocation(latitude = location.latitude, longitude = location.longitude)

                CoroutineScope(Dispatchers.IO).launch {
                    DatabaseBuilder.getInstance()
                        .userLocationDao()
                        .insert(userLocation)
                }
            } else {
                Toast.makeText(this, "Erro desconhecido", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchItems() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall {
                RetroFitClient .apiService.getCars()
            }
            withContext(Dispatchers.Main) {
                binding.swipeRefreshLayout.isRefreshing = false
                when(result) {
                    is Result.Error -> {
                        Toast.makeText(
                            this@MainActivity,
                            "Erro ao buscar carros",
                            Toast.LENGTH_SHORT)
                            .show()
                    }
                    is Result.Success<*> -> {
                        val adapter = CarAdapter(result.data as List<Car>) { car ->

                            startActivity(CarDetailActivity.newIntent(this@MainActivity, car.id))
                        }
                        binding.recyclerView.adapter = adapter
                    }
                }
            }
        }
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, MainActivity::class.java)
    }
}
