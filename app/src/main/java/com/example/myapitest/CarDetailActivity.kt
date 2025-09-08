package com.example.myapitest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapitest.databinding.ActivityCarDetailBinding
import com.example.myapitest.model.Car
import com.example.myapitest.model.CarResponse
import com.example.myapitest.service.RetroFitClient
import com.example.myapitest.service.safeApiCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.myapitest.service.Result
import com.example.myapitest.ui.loadUrl
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class CarDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityCarDetailBinding
    private lateinit var car: Car

    private lateinit var mMap: GoogleMap
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarDetailBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        setupView()
        loadItem()
        setupGoogleMap()
    }

    override fun onMapReady(map: GoogleMap) {
        mMap = map

        if(::car.isInitialized) {
            loadItemLocationInGoogleMap()
        }
    }

    private fun loadItemLocationInGoogleMap() {
        car.place.apply {
            binding.googleMapContent.visibility = View.VISIBLE
            val latLong = LatLng(car.place.lat, car.place.long)
            mMap.addMarker(
                MarkerOptions()
                .position(latLong)
                .title(car.name))
            mMap.moveCamera(
                CameraUpdateFactory
                .newLatLngZoom(latLong, 17f)
            )
        }
    }

    private fun setupGoogleMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
//        binding.deleteCTA.setOnClickListener {
//            deleteItem()
//        }
//        binding.editCTA.setOnClickListener {
//            editItem()
//        }
    }

    private fun loadItem(){
        val itemId = intent.getStringExtra(ARG_ID) ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetroFitClient.apiService.getCar(itemId) }

            withContext(Dispatchers.Main) {
                when(result) {
                    is Result.Error -> handleError()
                    is Result.Success -> {
                        car = (result.data as CarResponse).value
                        handleSuccess()
                    }
                }
            }
        }
    }

    private fun deleteItem() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetroFitClient.apiService.deleteCar(car.id) }

            withContext(Dispatchers.Main) {
                when(result) {
                    is Result.Error -> {
                        Toast.makeText(this@CarDetailActivity, "Erro ao deletar", Toast.LENGTH_SHORT).show()
                    }
                    is Result.Success<*> -> {
                        Toast.makeText(this@CarDetailActivity, "Deletado com sucesso", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }
    }

    private fun handleError() {
//        TODO()
    }

    private fun handleSuccess() {
        binding.name.text = car.name
        binding.image.loadUrl(car.imageUrl)
        binding.license.text = car.licence
        binding.year.setText(car.year)
        binding.deleteCTA.setOnClickListener {
            deleteItem()
        }
        loadItemLocationInGoogleMap()
    }

    companion object {
        private const val ARG_ID  = "arg_id"
        fun newIntent(context: Context, itemId: String) = Intent(context, CarDetailActivity::class.java).apply {
            putExtra(ARG_ID, itemId)
        }
    }
}