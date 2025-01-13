package com.example.aplikacjafitness

import RoutesAdapter
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class SummaryActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        val dbHelper = DatabaseHelper(this)
        val userId = Utils.getUserIdFromSharedPreferences(this)
        val routes = getRoutesFromDatabase(dbHelper, userId)

        val recyclerView: RecyclerView = findViewById(R.id.routesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = RoutesAdapter(routes)

        // bottom nav
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.menuBottom)
        if (bottomNavigationView != null) {
            setupBottomNavigation(bottomNavigationView)
            bottomNavigationView.selectedItemId = R.id.summary
        }
    }

    private fun getRoutesFromDatabase(dbHelper: DatabaseHelper, userId: Int): List<Route> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT date, distance, time FROM routes WHERE user_id = ? ORDER BY date DESC",
            arrayOf(userId.toString())
        )

        val routes = mutableListOf<Route>()
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val outputFormat = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())

        while (cursor.moveToNext()) {
            val rawDate = cursor.getString(cursor.getColumnIndexOrThrow("date"))
            val distance = cursor.getFloat(cursor.getColumnIndexOrThrow("distance"))
            val time = cursor.getString(cursor.getColumnIndexOrThrow("time"))

            // Formatowanie daty
            val formattedDate = try {
                val parsedDate = inputFormat.parse(rawDate)
                outputFormat.format(parsedDate ?: rawDate)
            } catch (e: Exception) {
                rawDate // Jeśli wystąpi błąd, użyj daty bez formatowania
            }

            routes.add(Route(formattedDate, distance, time))
        }
        cursor.close()

        return routes
    }
}
