package com.example.aplikacjafitness

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.core.app.ActivityCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class MapActivity : BaseActivity() {

    private lateinit var mapView: MapView
    private lateinit var locationManager: LocationManager
    private var currentLocationMarker: Marker? = null
    private val routePoints = mutableListOf<GeoPoint>()
    private var polyline: Polyline? = null

    private var isTracking: Boolean = false
    private var totalDistance: Double = 0.0
    private var startTime: Long = 0
    private var elapsedTime: Long = 0

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_map)

        mapView = findViewById(R.id.map)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(GeoPoint(52.237049, 21.017532))

        checkLocationPermissions()

        val centerButton = findViewById<Button>(R.id.MapCenter)
        centerButton.setOnClickListener {
            centerOnCurrentLocation()
        }

        val startRunButton = findViewById<Button>(R.id.startRun)
        startRunButton.setOnClickListener {
            if (!isTracking) {
                startRunButton.text = "Stop"
                startRun()
            } else {
                startRunButton.text = "Start"
                stopRun()
            }
        }

        // bottom nav
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.menuBottom)
        if (bottomNavigationView != null) {
            setupBottomNavigation(bottomNavigationView)
            bottomNavigationView.selectedItemId = R.id.map
        }
    }

    override fun onResume() {
        super.onResume()

        checkLocationPermissions()
    }

    private fun checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
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
            getCurrentLocation()
            centerOnCurrentLocation()
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
                getCurrentLocation()
            } else {
                Log.e("MapActivity", "Uprawnienia lokalizacji zostały odrzucone.")
            }
        }
    }

    private fun getCurrentLocation() {
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0,
                0f,
                object : LocationListener {
                    @SuppressLint("UseCompatLoadingForDrawables")
                    override fun onLocationChanged(location: Location) {
                        val currentGeoPoint = GeoPoint(location.latitude, location.longitude)

                        currentLocationMarker?.let { mapView.overlays.remove(it) }

                        currentLocationMarker = Marker(mapView).apply {
                            position = currentGeoPoint
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = resources.getDrawable(R.drawable.ic_map_marker, null)
                        }

                        mapView.overlays.add(currentLocationMarker)

                        if (isTracking) {

                            if (routePoints.isNotEmpty()) {
                                val lastPoint = routePoints.last()
                                val result = FloatArray(1)

                                Location.distanceBetween(
                                    lastPoint.latitude, lastPoint.longitude,
                                    currentGeoPoint.latitude, currentGeoPoint.longitude,
                                    result)

                                totalDistance += result[0]
                            }

                            routePoints.add(currentGeoPoint)
                            drawRoute()

                        }

                    }

                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
            )
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun centerOnCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { location ->
                val currentGeoPoint = GeoPoint(location.latitude, location.longitude)
                mapView.controller.animateTo(currentGeoPoint)
            } ?: Log.e("MapActivity", "Nie można pobrać ostatniej znanej lokalizacji.")
        } else {
            Log.e("MapActivity", "Brak uprawnień do lokalizacji.")
        }
    }

    @Suppress("DEPRECATION")
    private fun drawRoute(){
        polyline?.let { mapView.overlays.remove(it) }

        polyline = Polyline(mapView).apply {
            setPoints(routePoints)
            color = resources.getColor(R.color.blue, null)
            width = 8f
        }

        mapView.overlays.add(polyline)
    }

    private fun startRun(){
        isTracking = true
        routePoints.clear()
        totalDistance = 0.0
        startTime = System.currentTimeMillis()
        elapsedTime = 0
    }

    private fun stopRun(){
        isTracking = false
        elapsedTime = System.currentTimeMillis() - startTime
        val averageSpeed = totalDistance / (elapsedTime / 1000.0 / 60.0)
        Log.d("MapActivity", "Total distance: $totalDistance meters")
        Log.d("MapActivity", "Elapsed time: $elapsedTime milliseconds")
        Log.d("MapActivity", "Average speed: $averageSpeed meters per minute")
    }
}