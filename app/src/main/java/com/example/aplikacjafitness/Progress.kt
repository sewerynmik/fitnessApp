package com.example.aplikacjafitness

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.text.toFloatOrNull
import com.github.mikephil.charting.components.YAxis.AxisDependency.LEFT


class Progress : AppCompatActivity(){

    private lateinit var lineChart: LineChart
    private lateinit var dbHelper: DatabaseHelper

    private lateinit var profilePic: CircleImageView

    private lateinit var weightInput: EditText

    private lateinit var weightProg: TextView
    private lateinit var progressWeight: TextView
    private lateinit var progressWeight2: TextView
    private lateinit var bmiProgress: TextView

    private var dates: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.progress)


        profilePic = findViewById(R.id.profilePicProg)
        loadProfilePictureForButton()

        weightProg = findViewById(R.id.weightProg)
        progressWeight = findViewById(R.id.progressWeight)
        progressWeight2 = findViewById(R.id.progressWeight2)
        bmiProgress = findViewById(R.id.bmiProgress)

        dbHelper = DatabaseHelper(this)
        lineChart = findViewById(R.id.lineChartProgress)
        setupLineChart()
        loadLineChartData()

        val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        updateChartData(currentDate)

        val homeButton: ImageButton = findViewById(R.id.main)
        homeButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val addButton: ImageButton = findViewById(R.id.addButton)
        addButton.setOnClickListener {
            showAddProgressPopup()
        }

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
        val userId = Utils.getUserIdFromSharedPreferences(this)
        val (weights, initialDates) = dbHelper.getWeightProgress(userId)

        val sortedData = initialDates.zip(weights).sortedBy { it.first }
        val sortedDates = sortedData.map { it.first }
        val sortedWeights = sortedData.map { it.second }

        dates = sortedDates

        val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        val currentDateIndex = initialDates.indexOf(currentDate)

        val lastDayIndex = dates.lastIndex
        val visibleRange = 7f // Adjust as needed
        val centerX = currentDateIndex.toFloat()
        lineChart.moveViewToX(centerX - visibleRange / 2f)

        val valueToDateMap = mutableMapOf<Float, String>()
        val initialEntries = sortedWeights.takeLast(7)

        for (i in 0 until weights.size) {
            entries.add(Entry(i.toFloat(), weights[i]))
            valueToDateMap[i.toFloat()] = initialDates[i]
        }

        val dataSet = LineDataSet(entries, "Weight")
        dataSet.color = Color.GREEN
        dataSet.circleRadius = 5f
        dataSet.circleColors = listOf(Color.GREEN)
        dataSet.setDrawCircles(true)


        val dataSets = ArrayList<ILineDataSet>()
        dataSets.add(dataSet)

        val data = LineData(dataSets)
        lineChart.data = data

        lineChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val dateString = valueToDateMap[value]
                if (dateString != null) {
                    val date = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(dateString)
                    return SimpleDateFormat("dd-MM", Locale.getDefault()).format(date)
                }
                return ""
            }
        }

        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.xAxis.granularity = 1f
        lineChart.xAxis.setDrawLabels(true)
        lineChart.xAxis.setDrawGridLines(false)
        lineChart.xAxis.setAxisMinimum(0f)
        lineChart.xAxis.setAxisMaximum(currentDateIndex.toFloat())
        lineChart.xAxis.textSize = 10f
        lineChart.setVisibleXRangeMaximum(visibleRange)
        lineChart.moveViewToX(lastDayIndex.toFloat())
        lineChart.xAxis.labelRotationAngle = -45f

        val initialVisibleRange = minOf(visibleRange, lastDayIndex.toFloat() + 1) // Ensure at least one day is visible
        lineChart.moveViewToX(lastDayIndex.toFloat() - initialVisibleRange / 2f + 0.5f)


        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.axisLeft.isEnabled = false
        lineChart.axisRight.isEnabled = false
        lineChart.setScaleEnabled(false)
        lineChart.setPinchZoom(false)
        lineChart.setHighlightPerTapEnabled(false)

        dataSet.setColors(*ColorTemplate.MATERIAL_COLORS)
        dataSet.setDrawValues(false)
        dataSet.setDrawCircles(true)
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.argb(255, 0, 255, 0), Color.argb(0, 0, 255, 0))
        )

        dataSet.setDrawFilled(true)
        dataSet.fillDrawable = gradientDrawable

        val arrowTopProg: ImageView = findViewById(R.id.arrowTopProg)

        arrowTopProg.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Start dragging
                }
                MotionEvent.ACTION_MOVE -> {
                    // Update arrow position
                    val x = event.x
                    val indicatedDate = getDateFromXCoordinate(x) // Calculate indicated date
                    updateChartData(indicatedDate) // Update chart data
                }
                MotionEvent.ACTION_UP -> {
                    // Stop dragging
                }
            }
            true
        }


        lineChart.invalidate()
    }

    private fun showAddProgressPopup() {
        val popupView = layoutInflater.inflate(R.layout.popup_add_progress, null)

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val popupWidth = (screenWidth * 0.8).toInt()

        val popupWindow = PopupWindow(popupView,
            popupWidth,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true)

        weightInput = popupView.findViewById(R.id.weightInput)
        val addPhotoButton: Button = popupView.findViewById(R.id.addPhotoButton)
        val cancelButton: Button = popupView.findViewById(R.id.cancelButton)
        val saveButton: Button = popupView.findViewById(R.id.saveButton)

        addPhotoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        cancelButton.setOnClickListener {
            popupWindow.dismiss()
        }

        saveButton.setOnClickListener {
            val weight = weightInput.text.toString().toFloatOrNull() ?: 0f
            val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
            val userId = Utils.getUserIdFromSharedPreferences(this)

            popupWindow.dismiss()
        }

        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0)

        loadLineChartData()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri = data.data
            saveImageToInternalStorage(selectedImageUri, weightInput)
        }
    }

    private fun saveImageToInternalStorage(imageUri: Uri?, weightInput: EditText) {
        imageUri?.let { uri ->
            val contentResolver = contentResolver
            val inputStream = contentResolver.openInputStream(uri)
            val fileName = "progress_photo_${System.currentTimeMillis()}.jpg"
            val file = File(filesDir, fileName)

            try {
                val outputStream = FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                outputStream.close()
                inputStream?.close()

                val userId = Utils.getUserIdFromSharedPreferences(this)
                val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
                val weight = weightInput.text.toString().toFloatOrNull() ?: 0f
                dbHelper.insertWeightProgress(userId, currentDate, weight, fileName)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    private fun loadProfilePictureForButton() {
        val file = File(filesDir, "profile_picture.jpg")
        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            profilePic.setImageBitmap(bitmap)
        } else {

            profilePic.setImageResource(R.drawable.person)
        }
    }

    private fun updateChartData(indicatedDate: String) {
        // Retrieve data for indicated date
        val data = dbHelper.getDataForDate(indicatedDate)

        // Format data
        val weight = data.weight // Assuming your data object has a weight property
        val bmi = data.weight // zrobic bmi

        // Update text views
        weightProg.text = "Weight: $weight"
        progressWeight.text = "Progress from last weight: ..." // Calculate progress
        progressWeight2.text = "Progress from beginning: ..." // Calculate progress
        bmiProgress.text = "BMI: $bmi"
    }

    private fun getDateFromXCoordinate(x: Float): String {
        // Get the x-axis value corresponding to the touch event's x-coordinate
        val transformer = lineChart.getTransformer(LEFT)

        val position = floatArrayOf(x, 0f)
        val values = lineChart.getValuesByTouchPoint(x, 0f, YAxis.AxisDependency.LEFT)
        val xValue = values.x

        // Find the closest date in your dates list to the x-axis value
        val closestDateIndex = dates.indexOfFirst {
            val dateValue = dates.indexOf(it).toFloat()
            Math.abs(dateValue - xValue) < 0.5f // Adjust the tolerance as needed
        }

        // Return the date if found, otherwise return an empty string or handle the error
        return if (closestDateIndex != -1) {
            dates[closestDateIndex]
        } else {
            "" // Or handle the error appropriately
        }
    }

}