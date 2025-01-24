package com.example.aplikacjafitness

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import com.example.aplikacjafitness.Utils.getUserIdFromSharedPreferences
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
import kotlinx.coroutines.delay


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
                Log.d("DEBUG", "onChartTranslate: Chart is being translated, dX=$dX, dY=$dY")
                updateDataOnScroll(sortedDates)
            }

        })
    }

    private fun loadLineChartData(dates: List<String>) {
        val entries = ArrayList<Entry>()
        val userId = Utils.getUserIdFromSharedPreferences(this)
        val (weights, initialDates, hours) = dbHelper.getWeightProgress(userId)

        // Sparuj daty z godzinami dla unikalności
        val dateTimePairs = initialDates.zip(hours)
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())

        val parsedData = dateTimePairs.mapIndexedNotNull { index, pair ->
            try {
                dateFormat.parse("${pair.first} ${pair.second}")?.let { it to weights[index] }
            } catch (e: Exception) {
                Log.e("Progress2", "Failed to parse date/time: ${pair.first} ${pair.second}", e)
                null
            }
        }.sortedBy { it.first }

        val sortedDates = parsedData.map { dateFormat.format(it.first) }
        val sortedWeights = parsedData.map { it.second }

        val valueToDateMap = mutableMapOf<Float, String>()
        for (i in sortedWeights.indices) {
            entries.add(Entry(i.toFloat(), sortedWeights[i]))
            valueToDateMap[i.toFloat()] = sortedDates[i]
        }

        weightsList = sortedWeights
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
            updateChartData(sortedDates[targetIndex], sortedWeights)
        } else {
            lineChart.moveViewToX(0f)
        }
    }

    private fun showAddProgressPopup() {
        val popupView = layoutInflater.inflate(R.layout.popup_add_progress, null)
        val photoStatusTextView: TextView = popupView.findViewById(R.id.photoStatusTextView)

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
            Handler(Looper.getMainLooper()).postDelayed({
                photoStatusTextView.text = "Photo added successfully"
                photoStatusTextView.visibility = View.VISIBLE
            }, 1000)
        }

        val cameraButton: Button = popupView.findViewById(R.id.cameraButton)
        cameraButton.setOnClickListener {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)

            Handler(Looper.getMainLooper()).postDelayed({
                photoStatusTextView.text = "Photo added successfully"
                photoStatusTextView.visibility = View.VISIBLE
            }, 1000)
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
            val hour = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

            dbHelper.insertWeightProgress(userId, currentDate, weight, photoFileName,hour)

            loadLineChartData(sortedDates)
            initChart(weightsList, sortedDates)
            popupWindow.dismiss()
            finish()
            startActivity(intent)

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
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            photoFileName = saveImageToInternalStorage(imageBitmap)
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

                return fileName
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
        return null
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): String {
        val fileName = "progress_photo_${System.currentTimeMillis()}.jpg"
        val file = File(filesDir, fileName)
        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
        }
        return fileName
    }
    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val REQUEST_IMAGE_CAPTURE = 2
    }

    private fun loadProfilePictureForButton() {
        val userId = getUserIdFromSharedPreferences(this)
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT prof_pic FROM users WHERE id = ?", arrayOf(userId.toString()))

        if (cursor.moveToFirst()) {
            val picName = cursor.getString(cursor.getColumnIndexOrThrow("prof_pic"))
            if (!picName.isNullOrEmpty()) {
                val file = File(filesDir, picName)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    profilePic.setImageBitmap(bitmap)
                } else {
                    // File not found, log an error and load the default image
                    Log.e("loadProfilePicture", "Profile picture file not found: $picName")
                    profilePic.setImageResource(R.drawable.person)
                }
            } else {
                // picName is null or empty, load the default image
                profilePic.setImageResource(R.drawable.person)
            }
        } else {
            // No user found, load the default image
            profilePic.setImageResource(R.drawable.person)
        }

        cursor.close()

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
            Log.d("DEBUG", "Date: $date, dateIndex: $dateIndex, sortedDates: $sortedDates")

            if (dateIndex in weightsList.indices && dateIndex > 0) {
                val firstWeight = weightsList[0]
                val daysAgo = calculateDaysAgo(sortedDates[0], date)
                var progressCompared = weightsList[dateIndex] - firstWeight
                var index2 = false
                if (progressCompared < 0) {
                    index2 = true
                    progressCompared *= -1
                }

                Log.d("DEBUG", "FirstWeight: $firstWeight, ProgressCompared: $progressCompared, DaysAgo: $daysAgo")

                val progressText = "${String.format("%.1f", progressCompared)} kg\nCompared with $daysAgo days ago\n (${sortedDates[0]})"
                val spannableString = SpannableString(progressText)

                val numberEndIndex = progressText.indexOf(" kg")
                spannableString.setSpan(
                    ForegroundColorSpan(if (index2) Color.GREEN else Color.RED),
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
                Log.e("DEBUG", "No valid data for progress calculation.")
                SpannableString("No progress data available")
            }
        }

    }

    override fun onValueSelected(e: Entry, h: Highlight) {
        val index = e.x.toInt()
        if (index in weightsList.indices) {
            val userId = Utils.getUserIdFromSharedPreferences(this)
            val (weights, dates, hours) = dbHelper.getWeightProgress(userId)

            // Pobierz datę i godzinę dla wybranego punktu
            val selectedDate = dates[index]
            val selectedHour = hours[index]

            // Aktualizuj dane w interfejsie na podstawie wybranej godziny
            updateChartDataWithHour(selectedDate, selectedHour, weights[index])

            // Pobierz zdjęcie związane z wybraną datą i godziną
            val imageFilePath = dbHelper.getImageForDate(userId, selectedDate, selectedHour)
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

    private fun updateChartDataWithHour(date: String, hour: String, weight: Float) {
        Log.d("DEBUG", "updateChartDataWithHour: Updating with Date=$date, Hour=$hour, Weight=$weight")

        val userId = Utils.getUserIdFromSharedPreferences(this)
        val userData = dbHelper.getUserData(userId)

        val height = userData.height
        val bmi = if (height > 0) {
            weight / ((height / 100.0) * (height / 100.0)).toFloat()
        } else {
            0f
        }

        // Aktualizuj pola w interfejsie
        weightProg.text = "$weight kg"
        dateProgress.text = "$date $hour"

        bmiProgress.text = when {
            bmi >= 30 -> "BMI: %.1f (Obese)".format(bmi)
            bmi >= 25 -> "BMI: %.1f (Overweight)".format(bmi)
            bmi >= 18.5 -> "BMI: %.1f (Normal)".format(bmi)
            else -> "BMI: %.1f (Underweight)".format(bmi)
        }

        Log.d("DEBUG", "updateChartDataWithHour: BMI updated to $bmi")
    }

    private fun updateDataOnScroll(sortedDates: List<String>) {
        val centerX = (lineChart.lowestVisibleX + lineChart.highestVisibleX) / 2f
        val closestEntry = lineChart.data.getDataSetByIndex(0)
            ?.getEntryForXValue(centerX, Float.NaN, DataSet.Rounding.CLOSEST)

        closestEntry?.let { entry ->
            val index = entry.x.toInt()

            if (index in weightsList.indices) {
                val userId = Utils.getUserIdFromSharedPreferences(this)
                val (weights, dates, hours) = dbHelper.getWeightProgress(userId)

                val selectedDate = dates[index]
                val selectedHour = hours[index]

                Log.d("DEBUG", "updateDataOnScroll: Date=$selectedDate, Hour=$selectedHour, Weight=${weights[index]}")

                // Aktualizuj dane w interfejsie użytkownika
                updateChartDataWithHour(selectedDate, selectedHour, weights[index])
            } else {
                Log.e("DEBUG", "updateDataOnScroll: Index $index is out of bounds!")
            }
        } ?: Log.e("DEBUG", "updateDataOnScroll: closestEntry is null")
    }

    private fun snapToNearestDot() {
        val centerX = (lineChart.lowestVisibleX + lineChart.highestVisibleX) / 2f
        val closestEntry = lineChart.data.getDataSetByIndex(0)
            ?.getEntryForXValue(centerX, Float.NaN, DataSet.Rounding.CLOSEST)

        closestEntry?.let { entry ->
            val index = entry.x.toInt()

            // Pobranie pełnej informacji (data + godzina) dla punktu
            if (index in weightsList.indices) {
                val userId = Utils.getUserIdFromSharedPreferences(this)
                val (weights, dates, hours) = dbHelper.getWeightProgress(userId)

                val selectedDate = dates[index]
                val selectedHour = hours[index]

                Log.d("DEBUG", "Snap to: Date=$selectedDate, Hour=$selectedHour, Weight=${weights[index]}")

                // Aktualizacja postępu dla daty i godziny
                updateChartDataWithHour(selectedDate, selectedHour, weights[index])

                // Ustaw widok na wybrany punkt
                lineChart.centerViewToAnimated(entry.x, entry.y, YAxis.AxisDependency.LEFT, 300)
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
            Log.d("DEBUG", "StartDate: $startDate, EndDate: $endDate, ParsedStart: $start, ParsedEnd: $end")

            if (start != null && end != null) {
                val differenceInMillis = end.time - start.time
                (differenceInMillis / (1000 * 60 * 60 * 24)).toInt()
            } else {
                Log.e("DEBUG", "Error parsing dates in calculateDaysAgo.")
                0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("DEBUG", "Exception in calculateDaysAgo: ${e.message}")
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