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
import androidx.compose.ui.semantics.text
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
import com.github.mikephil.charting.data.DataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener


class Progress : AppCompatActivity(), OnChartValueSelectedListener {

    private lateinit var lineChart: LineChart
    private lateinit var dbHelper: DatabaseHelper

    private lateinit var profilePic: CircleImageView

    private lateinit var weightInput: EditText

    private lateinit var weightProg: TextView
    private lateinit var progressWeight: TextView
    private lateinit var progressWeight2: TextView
    private lateinit var bmiProgress: TextView
    private lateinit var dateProgress: TextView

    private lateinit var arrowTopProg: ImageView

    private var dates: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.progress)

        dbHelper = DatabaseHelper(this)
        lineChart = findViewById(R.id.lineChartProgress)
        setupLineChart()
        loadLineChartData()

        profilePic = findViewById(R.id.profilePicProg)
        loadProfilePictureForButton()

        weightProg = findViewById(R.id.weightProg)
        progressWeight = findViewById(R.id.progressWeight)
        progressWeight2 = findViewById(R.id.progressWeight2)
        bmiProgress = findViewById(R.id.bmiProgress)
        dateProgress = findViewById(R.id.dateProgress)
        arrowTopProg = findViewById(R.id.arrowTopProg)

        lineChart.setOnChartValueSelectedListener(this)

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
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.xAxis.setDrawGridLines(false)
        lineChart.xAxis.granularity = 1f
        lineChart.xAxis.labelRotationAngle = -45f
        lineChart.axisLeft.isEnabled = false
        lineChart.axisRight.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.setHighlightPerTapEnabled(false)
        lineChart.setHighlightPerDragEnabled(false)
        lineChart.setOnChartValueSelectedListener(null)
        lineChart.setScaleEnabled(false)
        lineChart.setPinchZoom(false)
        lineChart.isDragEnabled = true

        lineChart.setOnChartGestureListener(object : OnChartGestureListener {
            override fun onChartGestureStart(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {}
            override fun onChartGestureEnd(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {
                snapToNearestDot()
            }

            override fun onChartLongPressed(me: MotionEvent?) {}
            override fun onChartDoubleTapped(me: MotionEvent?) {}
            override fun onChartSingleTapped(me: MotionEvent?) {}

            override fun onChartFling(me1: MotionEvent?, me2: MotionEvent?, velocityX: Float, velocityY: Float) {}

            override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {}
            override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {
                updateDataOnScroll()
            }
        })
    }

    private fun loadLineChartData() {
        val entries = ArrayList<Entry>()
        val userId = Utils.getUserIdFromSharedPreferences(this)
        val (weights, initialDates) = dbHelper.getWeightProgress(userId)

        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

        dates = initialDates.sorted()
        val sortedData = dates.zip(weights).sortedBy { it.first }
        val parsedData = initialDates.mapIndexedNotNull { index, date ->
            try {
                dateFormat.parse(date) to weights[index]
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.first }

        val sortedDates = parsedData.map { dateFormat.format(it.first) }
        val sortedWeights = sortedData.map { it.second }

        val valueToDateMap = mutableMapOf<Float, String>()
        for (i in sortedWeights.indices) {
            entries.add(Entry(i.toFloat(), sortedWeights[i]))
            valueToDateMap[i.toFloat()] = sortedDates[i]
        }

        val dataSet = LineDataSet(entries, "Weight")
        dataSet.color = Color.GREEN
        dataSet.circleRadius = 5f
        dataSet.circleColors = listOf(Color.GREEN)
        dataSet.setDrawCircles(true)
        dataSet.setDrawValues(false)
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.argb(255, 0, 255, 0), Color.argb(0, 0, 255, 0))
        )
        dataSet.setDrawFilled(true)
        dataSet.fillDrawable = gradientDrawable

        val lineData = LineData(dataSet)
        lineChart.data = lineData

        val extraSpace = 3.5f
        lineChart.xAxis.setAxisMinimum(-extraSpace)
        lineChart.xAxis.setAxisMaximum(entries.size.toFloat() + extraSpace - 1f)

        lineChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return valueToDateMap[value] ?: ""
            }
        }

        val today = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        val targetIndex = sortedDates.indexOf(today).takeIf { it != -1 } ?: sortedDates.size - 1

        if (targetIndex in 0 until entries.size) {
            lineChart.setVisibleXRangeMaximum(7f)
            lineChart.moveViewToX(targetIndex.toFloat())
        } else {
            lineChart.moveViewToX(0f)
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
        val dbHelper = DatabaseHelper(this)

        val data = dbHelper.getDataForDate(indicatedDate)
        val userData = dbHelper.getUserData(Utils.getUserIdFromSharedPreferences(this))

        val weight = if (data.weight > 0) data.weight else userData.weight
        val height = userData.height
        val initialWeight = userData.weight

        val bmi = if (height > 0) {
            weight / ((height / 100.0) * (height / 100.0))
        } else {
            0f
        }

        weightProg.text = "$weight kg"

        val userId = Utils.getUserIdFromSharedPreferences(this)
        val allWeights = dbHelper.getWeightProgress(userId).first

        val progressFromLast = if (allWeights.size > 1) {
            weight - allWeights[allWeights.size - 2]
        } else {
            weight - initialWeight
        }
        progressWeight.text = "Progress from last weight: ${String.format("%.1f", progressFromLast)} kg"

        val progressFromBeginning = if (allWeights.isNotEmpty()) {
            weight - allWeights[0]
        } else {
            0f
        }
        progressWeight2.text = "Progress from beginning: ${String.format("%.1f", progressFromBeginning)} kg"

        bmiProgress.text = String.format("%.1f", bmi)
    }



    override fun onValueSelected(e: Entry, h: Highlight) {

        val x = e.x
        val transformer = lineChart.getTransformer(LEFT)
        val arrowX = transformer.getPixelForValues(x, 0f).x
        arrowTopProg.x = arrowX.toFloat() - arrowTopProg.width / 2f

        val indicatedDate = dates[x.toInt()]
        updateChartData(indicatedDate)
    }

    override fun onNothingSelected() {
        TODO("Not yet implemented")//tu nic ma nie byc :)
    }

    private fun updateDataOnScroll() {
        val visibleXRange = lineChart.visibleXRange
        val centerX = (lineChart.lowestVisibleX + lineChart.highestVisibleX) / 2f

        val closestEntry = lineChart.data.getDataSetByIndex(0)
            ?.getEntryForXValue(centerX, Float.NaN, DataSet.Rounding.CLOSEST)

        if (closestEntry != null) {
            val dateIndex = closestEntry.x.toInt()
            if (dateIndex in dates.indices) {
                val date = dates[dateIndex]
                val weight = closestEntry.y
                updateChartData(date, weight)
            }
        }
    }

    private fun updateChartData(date: String, weight: Float) {
        weightProg.text = "$weight kg"
        dateProgress.text = "$date"
        bmiProgress.text = "bmi"
    }

    private fun snapToNearestDot() {
        val currentViewCenter = lineChart.lowestVisibleX + (lineChart.highestVisibleX - lineChart.lowestVisibleX) / 2f

        val closestEntryIndex = dates.indices.minByOrNull { index ->
            Math.abs(index - currentViewCenter)
        }

        closestEntryIndex?.let { index ->
            lineChart.centerViewToAnimated(
                index.toFloat(),
                0f,
                YAxis.AxisDependency.LEFT,
                300
            )
        }
    }


}