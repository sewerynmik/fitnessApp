package com.example.aplikacjafitness

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.add
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.intl.Locale
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.text.format
import kotlin.text.toIntOrNull
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import android.graphics.Color
import androidx.core.text.color
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File
import kotlin.io.path.exists


class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var db: SQLiteDatabase

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
    private var lastResetTimestamp: Long = 0

    private var lastSensorTimestamp = 0L
    private val debounceTime = 500L

    private lateinit var profileBtn: CircleImageView

    private lateinit var lineChart: LineChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        dbHelper = DatabaseHelper(this)
        db = dbHelper.writableDatabase

        // zrobione ze nie wylogowywuje z apki przez 24h po pomyslnym zalogowaniu(nie dziala)

        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        loginTimestamp = sharedPreferences.getLong("LOGIN_TIMESTAMP", 0)
        isLoggedInFlag = sharedPreferences.getBoolean("IS_LOGGED_IN", false)
        lastResetTimestamp = sharedPreferences.getLong("LAST_RESET_TIMESTAMP", 0)

        counterFlow.value = 0
        val dailyStepGoalString = sharedPreferences.getString("DAILY_STEP_GOAL", "6000")
        val dailyStepGoal = dailyStepGoalString?.toIntOrNull() ?: 6000
        val today = SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault()).format(Date())
        val userId = getUserIdFromSharedPreferences()
        val stepsToday = dbHelper.getStepsForToday(today, userId)

        if (stepsToday != -1) {
            counterFlow.value = stepsToday
        } else {
            counterFlow.value = 0
        }

        val currentTime = System.currentTimeMillis()
        if (!isSameDay(currentTime, lastResetTimestamp)) {
            resetStepCount()
            lastResetTimestamp = currentTime
            sharedPreferences.edit { putLong("LAST_RESET_TIMESTAMP", lastResetTimestamp) }
        }

        if (!(isLoggedInFlag && System.currentTimeMillis() - loginTimestamp < 24 * 60 * 60 * 1000)) {
            startActivity(Intent(this, Login::class.java))
            finish()
            return

        }
        setContentView(R.layout.activity_main)

        lineChart = findViewById<LineChart>(R.id.lineChart)
        setupLineChart()
        loadLineChartData()

        val name = findViewById<TextView>(R.id.NameAndSurrView)
        val savedEmail = sharedPreferences.getString("EMAIL", "")
        val dbHelper = DatabaseHelper(this)
        val cursor = dbHelper.readableDatabase.rawQuery("SELECT name, surname FROM users WHERE email = ?", arrayOf(savedEmail))
        if (cursor.moveToFirst()) {
            val nameReg = cursor.getString(0)
            val surReg = cursor.getString(1)
            name.text = "$nameReg $surReg"
        } else {
            name.text = "Name Surname"
        }

        cursor.close()


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

        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)

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
                    Distance.text = "Distance: $roundedDyst km"
                    Calories.text = "Calories: $cal kcal"

                    sharedPreferences.edit { putInt("STEP_COUNT", count) }
                    val currentDate = SimpleDateFormat("dd-MM-yyyy",
                        java.util.Locale.getDefault()).format(Date())
                    val userId = getUserIdFromSharedPreferences()
                    dbHelper.updateDailySteps(db, currentDate, count, userId)
                }

            }
        }

        profileBtn = findViewById(R.id.profileBtn)
        loadProfilePictureForButton()
        profileBtn.setOnClickListener {
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
        }

        val ProgressButton: ImageButton = findViewById(R.id.progressBtn)

        ProgressButton.setOnClickListener {
            val intent = Intent(this, Progress::class.java)
            startActivity(intent)
        }

    }

    override fun onResume() {
        super.onResume()
        if (counterFlow.value == 0) {
            val savedStepCount = sharedPreferences.getInt("STEP_COUNT", 0)
            counterFlow.value = savedStepCount
        }
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences.edit { putInt("STEP_COUNT", counterFlow.value) }
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
                val currentTimestamp = System.currentTimeMillis()
                if (currentTimestamp - lastSensorTimestamp > debounceTime) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        counterFlow.value++
                        sharedPreferences.edit { putInt("STEP_COUNT", counterFlow.value) }
                    }
                    lastSensorTimestamp = currentTimestamp
                }
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // Not yet implemented
    }

    private fun resetStepCount() {
        counterFlow.value = 0
        sharedPreferences.edit { putInt("STEP_COUNT", 0) }
    }

    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val calendar1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val calendar2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
                calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR)
    }

    fun getUserIdFromSharedPreferences(): Int {
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val savedEmail = sharedPreferences.getString("EMAIL", "")
        val dbHelper = DatabaseHelper(this)
        val cursor = dbHelper.readableDatabase.rawQuery("SELECT id FROM users WHERE email = ?", arrayOf(savedEmail))
        var userId = -1
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(0)
        }
        cursor.close()
        return userId
    }

    private fun setupLineChart() {
        lineChart.description.isEnabled = false
        lineChart.setTouchEnabled(true)
        lineChart.setDragEnabled(true)
        lineChart.setScaleEnabled(true)
        lineChart.setPinchZoom(true)
        lineChart.setDrawGridBackground(false)
    }

    private fun loadLineChartData() {
        val entries = ArrayList<Entry>()
        val userId = getUserIdFromSharedPreferences()
        val last7DaysSteps = dbHelper.getLast7DaysSteps(userId)
        val xAxisLabels = ArrayList<String>()
        val averageSteps = last7DaysSteps.average().toFloat()
        val maxValue = last7DaysSteps.maxOrNull()?.toFloat() ?: 0f

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd", java.util.Locale.getDefault())

        calendar.add(Calendar.DAY_OF_YEAR, -6)

        for (i in 0 until last7DaysSteps.size) {
            xAxisLabels.add(dateFormat.format(calendar.time))
            entries.add(Entry(i.toFloat(), last7DaysSteps[i].toFloat()))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val dataSet = LineDataSet(entries, "Steps")
        dataSet.setColors(*ColorTemplate.MATERIAL_COLORS)
        dataSet.setDrawValues(false)
        dataSet.setDrawCircles(false)
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.argb(255, 0, 0, 255), Color.argb(0, 0, 0, 255))
        )

        dataSet.setDrawFilled(true)
        dataSet.fillDrawable = gradientDrawable

        val averageLineDataSet = LineDataSet(listOf(
            Entry(0f, averageSteps),
            Entry(last7DaysSteps.size.toFloat() - 1, averageSteps)
        ), "Average")

        averageLineDataSet.color = Color.RED
        averageLineDataSet.enableDashedLine(10f, 10f, 0f)
        averageLineDataSet.setDrawCircles(false)

        lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(xAxisLabels)
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.xAxis.granularity = 1f
        lineChart.xAxis.setDrawLabels(true)
        lineChart.xAxis.setDrawGridLines(false)
        lineChart.axisLeft.axisMaximum = maxValue * 1.3f
        lineChart.axisLeft.axisMinimum = 2f

        lineChart.axisLeft.isEnabled = true
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.axisLeft.isEnabled = false
        lineChart.axisRight.isEnabled = false
        lineChart.setTouchEnabled(false)
        lineChart.isDragEnabled = false
        lineChart.setScaleEnabled(false)
        lineChart.setPinchZoom(false)

        val dataSets = ArrayList<ILineDataSet>()
        dataSets.add(dataSet)
        dataSets.add(averageLineDataSet)

        val data = LineData(dataSets)
        lineChart.data = data

        lineChart.invalidate()
    }

    private fun loadProfilePictureForButton() {
        val file = File(filesDir, "profile_picture.jpg")
        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            profileBtn.setImageBitmap(bitmap)
        } else {

            profileBtn.setImageResource(R.drawable.person)
        }
    }

}