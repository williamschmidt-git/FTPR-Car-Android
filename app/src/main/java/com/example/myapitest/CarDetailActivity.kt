package com.example.myapitest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
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

class CarDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCarDetailBinding
    private lateinit var car: Car
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarDetailBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        setupView()
        loadItem()
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

    private fun handleError() {
//        TODO()
    }

    private fun handleSuccess() {
        binding.name.text = car.name
        binding.image.loadUrl(car.imageUrl)
        binding.license.text = car.licence
        binding.year.setText(car.year)
    }

    companion object {
        private const val ARG_ID  = "arg_id"
        fun newIntent(context: Context, itemId: String) = Intent(context, CarDetailActivity::class.java).apply {
            putExtra(ARG_ID, itemId)
        }
    }
}