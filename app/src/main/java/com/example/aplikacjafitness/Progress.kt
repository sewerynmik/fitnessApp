package com.example.aplikacjafitness

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.color
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class Progress : AppCompatActivity(){

    private lateinit var lineChart: LineChart
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.progress)

        dbHelper = DatabaseHelper(this)
        lineChart = findViewById(R.id.lineChartProgress)
        setupLineChart()
        loadLineChartData()

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
        val userId = 1 // zmienic to potem na getUserIdFromSharedPreferences(this)
        val (weights, dates) = dbHelper.getWeightProgress(userId)

        for (i in 0 until weights.size) {
            entries.add(Entry(i.toFloat(), weights[i]))
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

        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.xAxis.granularity = 1f
        lineChart.xAxis.setDrawLabels(true)
        lineChart.xAxis.setDrawGridLines(false)

        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.axisLeft.isEnabled = false
        lineChart.axisRight.isEnabled = false
        lineChart.setScaleEnabled(false)
        lineChart.setPinchZoom(false)

        dataSet.setColors(*ColorTemplate.MATERIAL_COLORS)
        dataSet.setDrawValues(false)
        dataSet.setDrawCircles(false)
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.argb(255, 0, 255, 0), Color.argb(0, 0, 255, 0))
        )

        dataSet.setDrawFilled(true)
        dataSet.fillDrawable = gradientDrawable

        lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(dates)

        lineChart.invalidate()
    }

    private fun showAddProgressPopup() {
        val popupView = layoutInflater.inflate(R.layout.popup_add_progress, null)
        val popupWindow = PopupWindow(popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true)

        val weightInput: EditText = popupView.findViewById(R.id.weightInput)
        val addPhotoButton: android.widget.Button = popupView.findViewById(R.id.addPhotoButton)

        addPhotoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri = data.data
        }
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }
}