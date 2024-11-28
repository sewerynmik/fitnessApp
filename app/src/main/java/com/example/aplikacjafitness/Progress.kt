package com.example.aplikacjafitness

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.DisplayMetrics
import android.util.Log
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
import java.time.LocalDate
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
import java.time.format.DateTimeFormatter


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

    private lateinit var sortedDates: List<String>

    private lateinit var arrowTopProg: ImageView

    private var dates: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.progress)

        val userId = Utils.getUserIdFromSharedPreferences(this)


        weightProg = findViewById(R.id.weightProg)
        progressWeight = findViewById(R.id.progressWeight)
        progressWeight2 = findViewById(R.id.progressWeight2)
        bmiProgress = findViewById(R.id.bmiProgress)
        dateProgress = findViewById(R.id.dateProgress)
        arrowTopProg = findViewById(R.id.arrowTopProg)

        dbHelper = DatabaseHelper(this)
        lineChart = findViewById(R.id.lineChartProgress)

//tutaj blad
        val (weights, initialSortedDates) = dbHelper.getWeightProgress(userId)
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.getDefault())
        val sortedDates = initialSortedDates.sortedBy {
            LocalDate.parse(it, formatter)
        }
        val sort = sortedDates.last()

        Log.d("SortedDates", "sorted dates: $sortedDates")
        Log.d("SortedDates", "sort: $sort")

        lineChart.setOnChartValueSelectedListener(this)

        setupLineChart(sortedDates)
        loadLineChartData()

        profilePic = findViewById(R.id.profilePicProg)
        loadProfilePictureForButton()

        val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        Log.d("Progress", "current date: $currentDate")
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
        if (sortedDates.isNotEmpty() && weights.isNotEmpty()) {
            val latestDate = sortedDates.last()
            //tu okej
            updateChartData(latestDate)

            initChart(weights, sortedDates)
        } else {
            weightProg.text = "No weight data"
            dateProgress.text = "No date available"
            bmiProgress.text = "BMI: -"
            progressWeight.text = "No progress data"
            progressWeight2.text = "No progress available"
        }

    }

    private fun setupLineChart(sortedDates: List<String>) {
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
                updateDataOnScroll(sortedDates)
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
            Log.d("Sorted", "sorted index: $sortedDates")
            //tu okej
            updateChartData(sortedDates[targetIndex])
        } else {
            lineChart.moveViewToX(0f)
        }

       // lineChart.invalidate()
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
//zle przekazuje date
    private fun updateChartData(date: String) {
        val userId = Utils.getUserIdFromSharedPreferences(this)
        Log.d("Progress", "w updatechartdata: $date")
        val data = dbHelper.getDataForDate(date)
        val userData = dbHelper.getUserData(userId)

        val weight = if (data.weight > 0) data.weight else userData.weight
        val height = userData.height

        val bmi = if (height > 0) {
            weight / ((height / 100.0) * (height / 100.0)).toFloat()
        } else {
            0f
        }

        weightProg.text = if (data.weight > 0) "$weight kg" else dbHelper.getLastRecordedWeightBeforeDate(userId, date).toString()
        dateProgress.text = date
        bmiProgress.text = if (bmi > 0) String.format("BMI: %.1f", bmi) else "BMI: -"

        val (weights, initialSortedDates) = dbHelper.getWeightProgress(userId)

        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val parsedDates = initialSortedDates.mapNotNull {
            try {
                dateFormat.parse(it)
            } catch (e: Exception) {
                null
            }
        }
        val sortedParsedDates = parsedDates.sorted()
        val sortedDates = sortedParsedDates.map { dateFormat.format(it) }

        val progressFromLastDay = calculateDailyProgress(sortedDates, weights)

        var index = sortedDates.indexOf(date)
        var isWeightLoss = false

        if (index > 0) {
            var dailyProgress = progressFromLastDay[index - 1]
            if (dailyProgress < 0) {
                dailyProgress = dailyProgress * -1
                isWeightLoss = true
            }

            val progressText = "${String.format("%.1f", dailyProgress)} kg\n Progress"
            val spannableString = SpannableString(progressText)

            val numberEndIndex = progressText.indexOf(" kg")
            spannableString.setSpan(
                ForegroundColorSpan(if (isWeightLoss) Color.GREEN else Color.RED),
                0,
                numberEndIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                RelativeSizeSpan(2f),
                0,
                numberEndIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            progressWeight.text = spannableString
        } else {
            progressWeight.text = "No progress data"
            progressWeight.setTextColor(Color.GRAY)
        }

        if (weights.isNotEmpty()) {
            val firstWeight = weights[0]
            val daysAgo = calculateDaysAgo(sortedDates[0], date)
            val progressCompared = weight - firstWeight
            progressWeight2.text = "Compared with $daysAgo days ago:\n ${String.format("%.1f", progressCompared)} kg"
        } else {
            progressWeight2.text = "No progress data available"
        }
    }

    override fun onValueSelected(e: Entry, h: Highlight) {
        val x = e.x.toInt()
        if (x in sortedDates.indices) {
            val selectedDate = sortedDates[x]
            Log.d("SelDate", "selected date: $selectedDate")
            updateChartData(selectedDate)

            val transformer = lineChart.getTransformer(YAxis.AxisDependency.LEFT)
            val arrowX = transformer.getPixelForValues(x.toFloat(), 0f).x
            arrowTopProg.x = (arrowX - arrowTopProg.width / 2f).toFloat()
        }
    }

    override fun onNothingSelected() {
        TODO("Not yet implemented")//tu nic ma nie byc :)
    }

    private fun updateDataOnScroll(sortedDates: List<String>) {
        val centerX = (lineChart.lowestVisibleX + lineChart.highestVisibleX) / 2f
        val closestEntry = lineChart.data.getDataSetByIndex(0)
            ?.getEntryForXValue(centerX, Float.NaN, DataSet.Rounding.CLOSEST)

        closestEntry?.let { entry ->
            val index = entry.x.toInt()
            if (index in sortedDates.indices) {
                val date = sortedDates[index]
                Log.d("Progress", "date: $date")
                updateChartData(date)
            }
        }
    }


    private fun snapToNearestDot() {
        val centerX = (lineChart.lowestVisibleX + lineChart.highestVisibleX) / 2f

        val closestEntry = lineChart.data.getDataSetByIndex(0)
            ?.getEntryForXValue(centerX, Float.NaN, DataSet.Rounding.CLOSEST)
//tutaj jest problem na pozniej pozdrawiiam po hrze z wozniakiem w Fortniet
        if (closestEntry != null) {
            val dateIndex = closestEntry.x.toInt()
            if (dateIndex in sortedDates.indices) {
                val date = sortedDates[dateIndex]
                Log.d("Progress", "date2: $date")
                updateChartData(date)
                lineChart.centerViewToAnimated(dateIndex.toFloat(), 0f, YAxis.AxisDependency.LEFT, 300)
            }
        }
    }

    private fun calculateDailyProgress(dates: List<String>, weights: List<Float>): List<Float> {
        val dailyProgress = mutableListOf<Float>()

        for (i in 1 until weights.size) {
            val progress = weights[i] - weights[i - 1]
            dailyProgress.add(progress)
        }

        return dailyProgress
    }

    private fun calculateDaysAgo(startDate: String, endDate: String): Int {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        return try {
            val start = dateFormat.parse("$startDate UTC")
            val end = dateFormat.parse("$endDate UTC")
            Log.d("DaysAgo", "Start: $start, End: $end")

            if (start != null && end != null) {
                val differenceInMillis = end.time - start.time
                (differenceInMillis / (1000 * 60 * 60 * 24)).toInt()
            } else {
                0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    private fun initChart(weights: List<Float>, dates: List<String>) {
        val entries = weights.mapIndexed { index, weight ->
            Entry(index.toFloat(), weight)
        }

        val dataSet = LineDataSet(entries, "Weight Progress").apply {
            color = Color.GREEN
            valueTextColor = Color.BLACK
            valueTextSize = 10f
            circleRadius = 5f
            circleColors = listOf(Color.GREEN)
            setDrawCircles(true)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER

            val gradientDrawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.argb(255, 0, 255, 0), Color.argb(0, 0, 255, 0))
            )

            setDrawFilled(true)

            fillDrawable = gradientDrawable
        }


        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.description.isEnabled = false
        lineChart.invalidate()
    }





}