package com.example.aplikacjafitness

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.text
import java.time.LocalDate
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.text.toFloatOrNull
import com.github.mikephil.charting.data.DataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

import java.time.format.DateTimeFormatter

import com.google.android.material.bottomnavigation.BottomNavigationView



class Progress : BaseActivity(), OnChartValueSelectedListener {

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

    private var photoFileName: String? = null


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

        val (weights, initialSortedDates) = dbHelper.getWeightProgress(userId)
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.getDefault())
        sortedDates = initialSortedDates.sortedBy {
            LocalDate.parse(it, formatter)
        }
        val sort = sortedDates.last()


        lineChart.setOnChartValueSelectedListener(this)

        setupLineChart(sortedDates)
        loadLineChartData(sortedDates)

        profilePic = findViewById(R.id.profilePicProg)
        loadProfilePictureForButton()

        val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        updateChartData(currentDate,weightsList)


        val addButton: ImageButton = findViewById(R.id.addButton)
        addButton.setOnClickListener {
            showAddProgressPopup()
        }
        if (sortedDates.isNotEmpty() && weights.isNotEmpty()) {
            val latestDate = sortedDates.last()

            updateChartData(latestDate,weightsList)

            initChart(weights, sortedDates)
        } else {
            weightProg.text = "No weight data"
            dateProgress.text = "No date available"
            bmiProgress.text = "BMI: -"
            progressWeight.text = "No progress data"
            progressWeight2.text = "No progress available"
        }

        // bottom nav
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.menuBottom)
        if (bottomNavigationView != null) {
            setupBottomNavigation(bottomNavigationView)
            bottomNavigationView.selectedItemId = R.id.workout
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
        lineChart.isHighlightPerTapEnabled = true
        lineChart.isHighlightPerDragEnabled = true
        lineChart.setOnChartValueSelectedListener(this)
        lineChart.setScaleEnabled(false)
        lineChart.setPinchZoom(false)
        lineChart.isDragEnabled = true
        lineChart.isClickable = true
        lineChart.isEnabled = true


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
        val valueToDateMap = mutableMapOf<Float, String>()
        for (i in weights.indices) {
            entries.add(Entry(i.toFloat(), weights[i]))
            valueToDateMap[i.toFloat()] = sortedDates[i]
        }
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
            updateChartData(sortedDates[targetIndex],weights)
        } else {
            lineChart.moveViewToX(0f)
        }

    }

    private fun showAddProgressPopup() {
        val popupView = layoutInflater.inflate(R.layout.popup_add_progress, null)

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val popupWidth = (screenWidth * 0.85).toInt()

        val popupWindow = PopupWindow(
            popupView,
            popupWidth,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 10f
            setBackgroundDrawable(ColorDrawable(Color.WHITE))
        }

        weightInput = popupView.findViewById(R.id.weightInput)
        weightInput.hint = "Enter your weight (kg)"
        weightInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        val addPhotoButton: Button = popupView.findViewById(R.id.addPhotoButton)
        val cancelButton: Button = popupView.findViewById(R.id.cancelButton)
        val saveButton: Button = popupView.findViewById(R.id.saveButton)

        addPhotoButton.text = "Add Photo"
        cancelButton.text = "Cancel"
        saveButton.text = "Save"
        
        addPhotoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        cancelButton.setOnClickListener {
            popupWindow.dismiss()
        }

        saveButton.setOnClickListener {
            val weight = weightInput.text.toString().toFloatOrNull()
            if (weight == null || weight <= 0) {
                Toast.makeText(this, "Please enter a valid weight!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
            val userId = Utils.getUserIdFromSharedPreferences(this)

            dbHelper.insertWeightProgress(userId, currentDate, weight, photoFileName)

            loadLineChartData(sortedDates)
            initChart(weightsList, sortedDates)
            popupWindow.dismiss()
        }

        val dimBackground = ColorDrawable(Color.BLACK).apply { alpha = 100 }
        popupWindow.setBackgroundDrawable(dimBackground)
        popupWindow.isFocusable = true
        popupWindow.isOutsideTouchable = true
        popupWindow.update()

        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri = data.data
            photoFileName = saveImageToInternalStorage(selectedImageUri, weightInput).toString()
        }
    }

    private fun saveImageToInternalStorage(imageUri: Uri?, weightInput: EditText): String? {
        imageUri?.let { uri ->
            try {
                val contentResolver = contentResolver

                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    return null
                }

                val fileName = "progress_photo_${System.currentTimeMillis()}.jpg"
                val filesDir = filesDir
                val file = File(filesDir, fileName)

                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                    outputStream.flush()
                }

                inputStream.close()

                val userId = Utils.getUserIdFromSharedPreferences(this)
                val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
                val weight = weightInput.text.toString().toFloatOrNull() ?: 0f
                dbHelper.insertWeightProgress(userId, currentDate, weight, fileName)

                return fileName
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
        return null
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

        val dateIndex = sortedDates.indexOf(date)
        val weight = if (dateIndex in weightsList.indices) {
            weightsList[dateIndex]
        } else {
            if (data.weight > 0) data.weight else userData.weight
        }
        val height = userData.height

        val bmi = if (height > 0) {
            weight / ((height / 100.0) * (height / 100.0)).toFloat()
        } else {
            0f
        }

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

            val previousDate = sortedDates[index - 1]

            val progressText = "${String.format("%.1f", dailyProgress)} kg\n Progress ($previousDate)\n "
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
            val progressStartIndex = progressText.indexOf("\n") + 1
            spannableString.setSpan(
                RelativeSizeSpan(0.7f),
                progressStartIndex,
                progressText.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            progressWeight.text = spannableString
        } else {
            progressWeight.text = "No progress data"
            progressWeight.setTextColor(Color.GRAY)
        }

        progressWeight2.text = run {
            val dateIndex = sortedDates.indexOf(date)
            if (dateIndex in weightsList.indices && dateIndex > 0) {
                val firstWeight = weightsList[0]
                val daysAgo = calculateDaysAgo(sortedDates[0], date)
                var progressCompared = weightsList[dateIndex] - firstWeight
                var index2 = false
                if (progressCompared < 0) {
                    index2 = true
                    progressCompared *= -1
                }

                val progressText = "${String.format("%.1f", progressCompared)} kg\nCompared with $daysAgo days ago\n (${sortedDates[0]})"
                val spannableString = SpannableString(progressText)

                val numberEndIndex = progressText.indexOf(" kg")
                spannableString.setSpan(
                    ForegroundColorSpan(if (index2 == true) Color.GREEN else Color.RED),
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

                val comparedStartIndex = progressText.indexOf("Compared")
                spannableString.setSpan(
                    RelativeSizeSpan(0.7f),
                    comparedStartIndex,
                    progressText.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                spannableString
            } else {
                SpannableString("No progress data available")
            }
        }
    }

    override fun onValueSelected(e: Entry, h: Highlight) {
        val x = e.x.toInt()
        if (x in sortedDates.indices) {
            val selectedDate = sortedDates[x]
            updateChartData(selectedDate, weightsList)

            val userId = Utils.getUserIdFromSharedPreferences(this)
            val imageFilePath = dbHelper.getImageForDate(userId, selectedDate)
            if (!imageFilePath.isNullOrEmpty()) {
                showImageDialog(imageFilePath)
            } else {
                Toast.makeText(this, "No image available for this date", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showImageDialog(imagePath: String) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_image)
        val imageView = dialog.findViewById<ImageView>(R.id.imageView)

        val file = File(filesDir, imagePath)
        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            imageView.setImageBitmap(bitmap)
        } else {
            imageView.setImageResource(R.drawable.person)
        }

        dialog.findViewById<Button>(R.id.closeButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onNothingSelected() {

    }

    private fun updateDataOnScroll(sortedDates: List<String>) {
        val centerX = (lineChart.lowestVisibleX + lineChart.highestVisibleX) / 2f
        val closestEntry = lineChart.data.getDataSetByIndex(0)
            ?.getEntryForXValue(centerX, Float.NaN, DataSet.Rounding.CLOSEST)

        closestEntry?.let { entry ->
            val index = entry.x.toInt()
            if (index in sortedDates.indices) {
                val date = sortedDates[index]
                updateChartData(date,weightsList)
            }
        }
    }

    private fun snapToNearestDot() {
        val centerX = (lineChart.lowestVisibleX + lineChart.highestVisibleX) / 2f
        val closestEntry = lineChart.data.getDataSetByIndex(0)
            ?.getEntryForXValue(centerX, Float.NaN, DataSet.Rounding.CLOSEST)
        if (closestEntry != null) {
            val dateIndex = closestEntry.x.toInt()
            if (dateIndex in sortedDates.indices) {
                val date = sortedDates[dateIndex]
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
        lineChart.isClickable = true
        lineChart.isEnabled = true
        lineChart.invalidate()
    }





}