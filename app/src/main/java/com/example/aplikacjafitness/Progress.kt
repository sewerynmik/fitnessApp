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
import androidx.compose.ui.semantics.text
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

    private lateinit var weightsList: List<Float>


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

//okej
        val (weights, initialSortedDates) = dbHelper.getWeightProgress(userId)
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.getDefault())
        sortedDates = initialSortedDates.sortedBy {
            LocalDate.parse(it, formatter)
        }
        val sort = sortedDates.last()

        Log.d("SortedDates", "sorted dates: $sortedDates")
        Log.d("SortedDates", "sort: $sort")

        lineChart.setOnChartValueSelectedListener(this)

        setupLineChart(sortedDates)
        loadLineChartData(sortedDates)

        profilePic = findViewById(R.id.profilePicProg)
        loadProfilePictureForButton()

        val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        Log.d("Progress", "current date: $currentDate")
        updateChartData(currentDate,weightsList)

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
            updateChartData(latestDate,weightsList)

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

    private fun loadLineChartData(dates: List<String>) {
        val entries = ArrayList<Entry>()
        val userId = Utils.getUserIdFromSharedPreferences(this)
        val (weights, initialDates) = dbHelper.getWeightProgress(userId)

        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

        Log.d("Progress2", "Initial data - Dates: $initialDates, Weights: $weights")

        val parsedData = initialDates.mapIndexedNotNull { index, date ->
            try {
                dateFormat.parse(date)?.let { it to weights[index] }
            } catch (e: Exception) {
                Log.e("Progress2", "Failed to parse date: $date", e)
                null
            }
        }.sortedBy { it.first }

        val sortedDates = parsedData.map { dateFormat.format(it.first) }
        val sortedWeights = parsedData.map { it.second }
    Log.d("Progress2", "sorted datesss: $sortedDates + $sortedWeights")
        val valueToDateMap = mutableMapOf<Float, String>()
        for (i in weights.indices) {
            entries.add(Entry(i.toFloat(), weights[i]))
            valueToDateMap[i.toFloat()] = sortedDates[i]
        }//powrut
        Log.d("Progress2", "entries: $entries")
        weightsList = weights
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
            updateChartData(sortedDates[targetIndex],weights)
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

        loadLineChartData(sortedDates)
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

    private fun updateChartData(date: String,weights: List<Float>) {
        val userId = Utils.getUserIdFromSharedPreferences(this)
        val data = dbHelper.getDataForDate(date)
        val userData = dbHelper.getUserData(userId)
    Log.d("Progress", "w updatechartdata: $date + $data")

        var weight = if (data.weight > 0) data.weight else userData.weight
        val height = userData.height

        val bmi = if (height > 0) {
            weight / ((height / 100.0) * (height / 100.0)).toFloat()
        } else {
            0f
        }
        Log.d("Weightlista","wsg $weightsList")

    weightProg.text = {
        val dateIndex = sortedDates.indexOf(date)
        if (dateIndex in weightsList.indices) {
            "${weightsList[dateIndex]} kg"
        } else {
            dbHelper.getLastRecordedWeightBeforeDate(userId, date).toString()
        }
    }().toString()

        dateProgress.text = date

        bmiProgress.text = when {
            bmi >= 30 -> "BMI: %.1f (Obese)".format(bmi)
            bmi >= 25 -> "BMI: %.1f (Overweight)".format(bmi)
            bmi >= 18.5 -> "BMI: %.1f (Normal)".format(bmi)
            else -> "BMI: %.1f (Underweight)".format(bmi)
        }

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
// tutaj zmienic kololki
        progressWeight2.text = {
            val dateIndex = sortedDates.indexOf(date)
            if (dateIndex in weightsList.indices && dateIndex > 0) {
                val firstWeight = weightsList[0]
                val daysAgo = calculateDaysAgo(sortedDates[0], date)
                val progressCompared = weightsList[dateIndex] - firstWeight
                val progressText = "Compared with $daysAgo days ago:\n ${String.format("%.1f", progressCompared)} kg"
                val spannableString = SpannableString(progressText)

                val numberStartIndex = progressText.indexOf("\n ") + 2
                val numberEndIndex = progressText.indexOf(" kg")
                spannableString.setSpan(
                    ForegroundColorSpan(if (progressCompared < 0) Color.GREEN else Color.RED),
                    numberStartIndex,
                    numberEndIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                spannableString.setSpan(
                    RelativeSizeSpan(2f),
                    numberStartIndex,
                    numberEndIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                spannableString
            } else {
                "No progress data available"
            }
        }().toString()
    }

    override fun onValueSelected(e: Entry, h: Highlight) {
        val x = e.x.toInt()
        Log.d("SelDate", "x: $x")
        if (x in sortedDates.indices) {
            val selectedDate = sortedDates[x]
            Log.d("SelDate", "selected date: $selectedDate")
            updateChartData(selectedDate,weightsList)

            val transformer = lineChart.getTransformer(YAxis.AxisDependency.LEFT)
            val arrowX = transformer.getPixelForValues(x.toFloat(), 0f).x
            Log.d("Progress2","Strzalka: $arrowX")
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
                Log.d("Progress", "Tu git: $sortedDates")
                updateChartData(date,weightsList)
            }
        }
    }


    private fun snapToNearestDot() {
        val centerX = (lineChart.lowestVisibleX + lineChart.highestVisibleX) / 2f
Log.d("Progress2", "centerX: $centerX")
        val closestEntry = lineChart.data.getDataSetByIndex(0)
            ?.getEntryForXValue(centerX, Float.NaN, DataSet.Rounding.CLOSEST)
        Log.d("Progress2","closestEntry: $closestEntry")
        if (closestEntry != null) {
            val dateIndex = closestEntry.x.toInt()
            if (dateIndex in dates.indices) {
                val date = dates[dateIndex]
                Log.d("Progress", "date2: $date")
                updateChartData(date,weightsList)
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
Log.d("Progress2", "wyswietlanie dobrze: $entries")
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