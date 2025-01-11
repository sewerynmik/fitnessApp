package com.example.aplikacjafitness

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.text.format.DateUtils.formatElapsedTime
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class MapActivity : BaseActivity(), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var tracking = false
    private val routePoints = mutableListOf<LatLng>()
    private var totalDistance = 0.0
    private lateinit var map: GoogleMap
    private var startTime: Long = 0L
    private var endTime: Long = 0L

    @SuppressLint("CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapView) as SupportMapFragment
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mapFragment.getMapAsync(this)

        // Inicjalizacja LocationRequest
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L // Czas w milisekundach pomiędzy aktualizacjami
        ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                val newPoint = LatLng(location.latitude, location.longitude)

                if (routePoints.isNotEmpty()) {
                    val lastPoint = routePoints.last()
                    val distance = calculateDistance(lastPoint, newPoint)
                    totalDistance += distance

                    map.addPolyline(
                        PolylineOptions().add(lastPoint, newPoint).color(Color.RED).width(5f)
                    )
                }

                routePoints.add(newPoint)
                map.moveCamera(CameraUpdateFactory.newLatLng(newPoint))
            }
        }

        val infoFrame = findViewById<FrameLayout>(R.id.info)
        val timeTextView = findViewById<TextView>(R.id.timeTextView)
        val distanceTextView = findViewById<TextView>(R.id.distanceTextView)
        val resumeButton = findViewById<Button>(R.id.resumeRun)
        val endButton = findViewById<Button>(R.id.endRun)


        findViewById<Button>(R.id.startRun).setOnClickListener {
            tracking = !tracking
            if (tracking) {
                routePoints.clear()
                totalDistance = 0.0
                startTime = System.currentTimeMillis()
                (it as Button).text = "Stop"
                startTracking()
                infoFrame.visibility = View.VISIBLE
                resumeButton.visibility = View.INVISIBLE
                endButton.visibility = View.INVISIBLE

                // Pokazanie pól tekstowych
                timeTextView.visibility = View.VISIBLE
                distanceTextView.visibility = View.VISIBLE
                updateInfo(timeTextView, distanceTextView)
            } else {
                endTime = System.currentTimeMillis()
                (it as Button).visibility = View.INVISIBLE
                resumeButton.visibility = View.VISIBLE
                endButton.visibility = View.VISIBLE

                // Ukrycie pól tekstowych
                timeTextView.visibility = View.INVISIBLE
                distanceTextView.visibility = View.INVISIBLE
                stopTracking()
            }
        }

        resumeButton.setOnClickListener {
            tracking = true
            startTime = System.currentTimeMillis() - (endTime - startTime) // Kontynuacja czasu
            startTracking()

            // Zarządzanie widocznością
            findViewById<Button>(R.id.startRun).visibility = View.VISIBLE
            findViewById<Button>(R.id.startRun).text = "Stop"
            resumeButton.visibility = View.INVISIBLE
            endButton.visibility = View.INVISIBLE

            // Ukrycie pól tekstowych
            findViewById<TextView>(R.id.timeTextView).visibility = View.VISIBLE
            findViewById<TextView>(R.id.distanceTextView).visibility = View.VISIBLE

            // Ponowne uruchomienie aktualizacji informacji
            updateInfo(findViewById(R.id.timeTextView), findViewById(R.id.distanceTextView))
        }

        endButton.setOnClickListener {
            tracking = false
            infoFrame.visibility = View.INVISIBLE

            // Ukrycie pól tekstowych
            timeTextView.visibility = View.INVISIBLE
            distanceTextView.visibility = View.INVISIBLE
            showSummary()
        }


        // bottom nav
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.menuBottom)
        if (bottomNavigationView != null) {
            setupBottomNavigation(bottomNavigationView)
            bottomNavigationView.selectedItemId = R.id.map
        }
    }

    private fun updateInfo(timeTextView: TextView, distanceTextView: TextView) {
        val handler = android.os.Handler(Looper.getMainLooper())
        val updateRunnable = object : Runnable {
            @SuppressLint("SetTextI18n")
            override fun run() {
                if (tracking) {
                    val elapsedTime = (System.currentTimeMillis() - startTime) / 1000 // sekundy
                    timeTextView.text = "Czas: ${formatElapsedTime(elapsedTime)}"
                    distanceTextView.text = "Dystans: ${"%.2f".format(totalDistance)} km"
                    handler.postDelayed(this, 1000) // Aktualizacja co sekundę
                } else {
                    handler.removeCallbacks(this) // Zatrzymaj, jeśli tracking == false
                }
            }

        }
        handler.post(updateRunnable)
    }


    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
            return
        }

        map.isMyLocationEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val userLocation = LatLng(location.latitude, location.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f)) // Zoom do użytkownika
            }
        }
    }

    private fun startTracking() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d("MapActivity", "Dystans: ${totalDistance.roundToInt()} metrów")
    }

    private fun calculateDistance(start: LatLng, end: LatLng): Double {
        val radius = 6371e3 // Earth radius in meters
        val lat1 = Math.toRadians(start.latitude)
        val lat2 = Math.toRadians(end.latitude)
        val deltaLat = Math.toRadians(end.latitude - start.latitude)
        val deltaLon = Math.toRadians(end.longitude - start.longitude)

        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
                cos(lat1) * cos(lat2) *
                sin(deltaLon / 2) * sin(deltaLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return radius * c / 1000.0 // Return distance in kilometers
    }

    private fun showSummary() {
        val elapsedTime = (endTime - startTime) / 1000.0 // Czas w sekundach
        val averageSpeed = if (elapsedTime > 0) totalDistance / (elapsedTime / 3600) else 0.0 // km/h

        val intent = Intent(this, SummaryActivity::class.java).apply {
            putExtra("distance", totalDistance)
            putExtra("time", elapsedTime)
            putExtra("averageSpeed", averageSpeed)
        }
        startActivity(intent)
    }

}