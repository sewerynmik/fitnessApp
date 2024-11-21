package com.example.aplikacjafitness

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.semantics.text
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.text.toIntOrNull

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

    private lateinit var sharedPreferences: SharedPreferences
    private var loginTimestamp: Long = 0
    private var isLoggedInFlag: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // zrobione ze nie wylogowywuje z apki przez 24h po pomyslnym zalogowaniu(nie dziala)

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        loginTimestamp = sharedPreferences.getLong("LOGIN_TIMESTAMP", 0)
        isLoggedInFlag = sharedPreferences.getBoolean("IS_LOGGED_IN", false)
        val savedStepCount = sharedPreferences.getInt("STEP_COUNT", 0)
        counterFlow.value = savedStepCount
        val dailyStepGoalString = sharedPreferences.getString("DAILY_STEP_GOAL", "6000")
        val dailyStepGoal = dailyStepGoalString?.toIntOrNull() ?: 6000

        if (!(isLoggedInFlag && System.currentTimeMillis() - loginTimestamp < 24 * 60 * 60 * 1000)) {
            startActivity(Intent(this, Login::class.java))
            finish()
            return

        }
        setContentView(R.layout.activity_main)

        val name = findViewById<TextView>(R.id.NameAndSurrView)
        val nameReg = sharedPreferences.getString("NAME", "Name")
        val surReg = sharedPreferences.getString("SURNAME", "Surname")
        name.text = "$nameReg $surReg"

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

        progressBar = findViewById(R.id.circularProgressBar)
        val stepCounterText = findViewById<TextView>(R.id.stepCounterText)

        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        val Distance = findViewById<TextView>(R.id.dataDis)
        val Calories = findViewById<TextView>(R.id.dataCal)

        lifecycleScope.launch {
            counterFlow.collect { count ->
                runOnUiThread {
                    stepCounterText.text = "$count"
                    val progress = (count.toFloat() / dailyStepGoal * 100).toInt()
                    progressBar.progress = if (progress > 100) 100 else progress
                    val dyst = (count * 0.7 / 1000).toFloat()
                    val cal = (count * 0.04).toInt()
                    val roundedDyst = String.format("%.2f", dyst)
                    Distance.text = "Dystans przebyty: $roundedDyst km"
                    Calories.text = "Kalorie spalone: $cal kcal"

                    sharedPreferences.edit { putInt("STEP_COUNT", count) }
                }
            }
        }

        val ProfileButton: ImageButton = findViewById(R.id.profileBtn)

        ProfileButton.setOnClickListener {
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
        }

    }

    override fun onPause() {
        super.onPause()
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("STEP_COUNT", counterFlow.value)
        editor.apply()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                isPermissionGranted = true
                registerSensorListener()
            } else {
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