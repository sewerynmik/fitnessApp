package com.example.aplikacjafitness

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.semantics.text
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), SensorEventListener {

    private val sensorManager: SensorManager by lazy {
        getSystemService(SENSOR_SERVICE) as SensorManager
    }

    private var isSensorAvailable = false
    private var isPermissionGranted = false

    private var sensor: Sensor? = null

    private val counterFlow = MutableStateFlow(0)

    lateinit var progressBar: ProgressBar

    lateinit var Distance: TextView
    lateinit var Calories: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)

        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        isSensorAvailable = sensor != null

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                1)
        } else {
            isPermissionGranted = true
            registerSensorListener()
        }
        //progress bar
        progressBar = findViewById(R.id.circularProgressBar)
        val stepCounterText = findViewById<TextView>(R.id.stepCounterText)
        val dailyStepGoal = 6000

        val Distance = findViewById<TextView>(R.id.dataDis)
        val Calories = findViewById<TextView>(R.id.dataCal)

        lifecycleScope.launch {
            counterFlow.collect { count ->
                runOnUiThread {
                    stepCounterText.text = "$count"
                    val progress = (count.toFloat() / dailyStepGoal * 100).toInt()
                    progressBar.progress = if (progress > 100) 100 else progress
                    val dyst = (count * 0.7 * 1000).toInt()
                    val cal = (count * 0.04).toInt()
                    Distance.text = "Dystans przebyty: $dyst km"
                    Calories.text = "Kalorie spalone: $cal kcal"
                }
            }
        }





    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                isPermissionGranted = true
                registerSensorListener()
            } else {
                // Handle permission denial
                Log.d("MainActivity", "Permission denied")
            }
        }
    }

    private fun registerSensorListener() {
        if (isSensorAvailable && isPermissionGranted) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST)
            Log.d("MainActivity", "Step detector sensor registered")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        sensorEvent?.let { event ->
            if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                lifecycleScope.launch(Dispatchers.Main) {
                    counterFlow.value++
                }
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // Not yet implemented
    }


}