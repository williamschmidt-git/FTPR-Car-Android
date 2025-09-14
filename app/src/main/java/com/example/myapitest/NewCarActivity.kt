package com.example.myapitest

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import com.example.myapitest.databinding.NewCarLayoutBinding
import com.example.myapitest.model.Car
import com.example.myapitest.model.CarLocation
import com.example.myapitest.service.Result
import com.example.myapitest.service.RetroFitClient
import com.example.myapitest.service.safeApiCall
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID

class NewCarActivity: AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: NewCarLayoutBinding
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var  imageUri: Uri
    private var selectedMarker: Marker? = null
    private var imageFile: File? = null

    private val cameraLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if(it.resultCode == RESULT_OK) {
            imageFile?.let {
                uploadImageToFirebase()
            }
        }
    }

    private fun uploadImageToFirebase() {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("images/${UUID.randomUUID()}")
        val baos = ByteArrayOutputStream()
        val imageBitMap = BitmapFactory.decodeFile(imageFile!!.path)
        imageBitMap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        onLoadingImage(true)

        imageRef.putBytes(data)
            .addOnFailureListener {
                onLoadingImage(false)
                Toast.makeText(this, "Erro ao enviar imagem", Toast.LENGTH_SHORT).show()
            }
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    binding.imageUrl.setText(uri.toString())
                }
                    .addOnCompleteListener {
                        onLoadingImage(false)
                    }
            }
    }

    private fun onLoadingImage(isLoading: Boolean) {
        binding.loadImageProgress.isVisible = isLoading
        binding.takePictureCta.isEnabled = !isLoading
        binding.saveCta.isEnabled = !isLoading
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = NewCarLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        setupView()
        setupGoogleMap()
    }

    private fun setupGoogleMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }


    override fun onMapReady(p0: GoogleMap) {
        mMap = p0
        binding.mapContent.visibility = View.VISIBLE
        mMap.setOnMapClickListener { latLng ->
            selectedMarker?.remove()
            selectedMarker = mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .draggable(true)
                    .title("Lat ${latLng.latitude} Long: ${latLng.longitude}")
            )
        }

        getDeviceLocation()
    }

    private fun getDeviceLocation() {
        if (
            ContextCompat.checkSelfPermission
                (
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
            ) {
            loadCurrentLocation()
        } else {
            requestCameraPermission()
        }
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.CAMERA),
            REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        when(requestCode) {
            REQUEST_CODE -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    openCamera()
                } else {
                    Toast.makeText(this, "Permissão da câmera negada", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        imageUri = createImageUri()
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)

        cameraLauncher.launch(intent)
    }

    private fun createImageUri(): Uri {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd-HHmmss").format(Date())
        val imageFileName = "JPEG_${timeStamp}_}"
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        imageFile = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )

        return FileProvider.getUriForFile(
            this,
            "com.example.myapitest.fileprovider",
            imageFile!!
        )

    }

    @SuppressLint("MissingPermission")
    private fun loadCurrentLocation() {
        mMap.isMyLocationEnabled = true
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            val currentLocationLatLng = LatLng(location.latitude, location.longitude)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocationLatLng, 15f))
        }
    }
    private fun setupView(){
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.saveCta.setOnClickListener {
            onSave()
        }

        binding.takePictureCta.setOnClickListener {
            onTakePicture()
        }
    }

    private fun onTakePicture() {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestCameraPermission()
        }
    }
    private fun onSave() {
        if (!validateform()) return

        saveData()
    }

    private fun saveData() {
        val name = binding.name.text.toString()
        val imageUrl = binding.imageUrl.text.toString()
        val licence = binding.license.text.toString()
        val year = binding.year.text.toString()
        val place = selectedMarker?.position?.let { position ->
            CarLocation(
                lat = position.latitude,
                long = position.longitude,
            )

        } ?: throw IllegalArgumentException("Usuário deveria ter a localizaćão neste ponto.")

        CoroutineScope(Dispatchers.IO).launch {
            val carValue = Car(
                SecureRandom().nextInt().toString(),
                imageUrl,
                year,
                name,
                licence,
                place
            )

            Log.d("NewCarActivity", "onSave: $carValue")

            val result = safeApiCall {
                RetroFitClient.apiService.addCar(carValue)
            }

            withContext(Dispatchers.Main) {
                when(result) {
                    is Result.Error -> {
                        Toast.makeText(
                            this@NewCarActivity,
                            "Erro ao salvar",
                            Toast.LENGTH_SHORT).show()
                    }
                    is Result.Success -> {
                        Toast.makeText(
                            this@NewCarActivity,
                            "Carro criado com sucesso",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun validateform(): Boolean {
        if(binding.name.text.toString().isBlank()) {
            Toast.makeText(this, "Nome é obrigatório", Toast.LENGTH_SHORT).show()
            return false;
        }

        if(binding.imageUrl.text.toString().isBlank()) {
            Toast.makeText(this, "Imagem é obrigatório", Toast.LENGTH_SHORT).show()
            return false;
        }

        if(binding.license.text.toString().isBlank()) {
            Toast.makeText(this, "Placa é obrigatório", Toast.LENGTH_SHORT).show()
            return false;
        }

        if(binding.year.text.toString().isBlank()) {
            Toast.makeText(this, "Ano é obrigatório", Toast.LENGTH_SHORT).show()
            return false;
        }

        if(selectedMarker == null) {
            Toast.makeText(this, "Localizaćão é obrigatório", Toast.LENGTH_SHORT).show()
            return false;
        }

        return true
    }

    companion object {
        const val REQUEST_CODE = 101
        fun newIntent(context: Context) = Intent(context, NewCarActivity::class.java)
    }
}