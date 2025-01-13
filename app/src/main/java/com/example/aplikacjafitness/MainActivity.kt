package com.example.aplikacjafitness

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.bottomnavigation.BottomNavigationView
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date


class MainActivity : BaseActivity(), SensorEventListener {

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

        loadLastRoute()

        // bottom nav
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.menuBottom)
        if (bottomNavigationView != null) {
            setupBottomNavigation(bottomNavigationView)
            bottomNavigationView.selectedItemId = R.id.main
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

    private fun loadLastRoute() {
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase

        // Zapytanie o najnowszy bieg
        val query = "SELECT id, date, distance, time FROM routes ORDER BY id DESC LIMIT 1"
        val cursor = db.rawQuery(query, null)

        // Odwołania do widoków
        val distanceTextView: TextView = findViewById(R.id.biegi)
        val avgSpeedTextView: TextView = findViewById(R.id.biegii)
        val caloriesTextView: TextView = findViewById(R.id.biiegi)
        val speedTextView: TextView = findViewById(R.id.biiiegii)
        val noRunTextView: TextView = findViewById(R.id.yeti)

        if (cursor.moveToFirst()) {
            // Pobieramy dane z kursora
            val distance = cursor.getFloat(cursor.getColumnIndexOrThrow("distance"))
            val time = cursor.getString(cursor.getColumnIndexOrThrow("time"))

            // Wyliczamy dodatkowe dane
            val caloriesBurned = (distance * 60).toInt()
            val parts = time.split(":")
            val seconds = parts[0].toIntOrNull() ?: 0
            val minutes = parts[1].toIntOrNull() ?: 0
            val totalSeconds = minutes * 60 + seconds
            val averageSpeed = (distance / totalSeconds) * 3600


            // Formatowanie i ustawianie danych w widokach
            distanceTextView.text = "Distance: %.2f km".format(distance)
            avgSpeedTextView.text = "Average speed: %.2f km/h".format(averageSpeed)
            caloriesTextView.text = "Calories loss: $caloriesBurned kcal"
            speedTextView.text = "Time: ${time}".format(averageSpeed)

            // Ukrycie komunikatu "No run yet"
            noRunTextView.visibility = View.GONE
        } else {
            // Jeśli brak danych, wyświetlamy komunikat
            noRunTextView.visibility = View.VISIBLE
            distanceTextView.text = ""
            avgSpeedTextView.text = ""
            caloriesTextView.text = ""
            speedTextView.text = ""
        }

        cursor.close()
        db.close()
    }

}