package com.example.aplikacjafitness

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Button
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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

        findViewById<Button>(R.id.startRun).setOnClickListener {
            tracking = !tracking
            if (tracking) {
                routePoints.clear()
                totalDistance = 0.0
                (it as Button).text = "Stop"
                startTracking()
            } else {
                (it as Button).text = "Start"
                stopTracking()
            }
        }
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
}