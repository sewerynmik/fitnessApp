import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.ui.semantics.text
import androidx.recyclerview.widget.RecyclerView
import com.example.aplikacjafitness.R
import com.example.aplikacjafitness.Route
import kotlin.text.toDoubleOrNull

class RoutesAdapter(private val routes: List<Route>) : RecyclerView.Adapter<RoutesAdapter.RouteViewHolder>() {

    class RouteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateTextView: TextView = itemView.findViewById(R.id.routeDate)
        val distanceTextView: TextView = itemView.findViewById(R.id.routeDistance)
        val timeTextView: TextView = itemView.findViewById(R.id.routeTime)
        val kcalTextView: TextView = itemView.findViewById(R.id.routeKcal)
        val avgSpeedTextView: TextView = itemView.findViewById(R.id.routeAvgSpeed)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_route, parent, false)
        return RouteViewHolder(view)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        val route = routes[position]
        Log.d("RoutesAdapter", "Route: $route")

        holder.dateTextView.text = route.date
        holder.distanceTextView.text = "Distance: ${route.distance} km"
        holder.timeTextView.text = "Time: ${route.time}"
        val caloriesBurned = (route.distance * 60).toInt()
        holder.kcalTextView.text = "Calories: $caloriesBurned"

        val parts = route.time.split(":")
        val seconds = parts[0].toIntOrNull() ?: 0
        val minutes = parts[1].toIntOrNull() ?: 0
        val totalSeconds = minutes * 60 + seconds
        val averageSpeed = (route.distance / totalSeconds) * 3600

        holder.avgSpeedTextView.text = "Speed: ${String.format("%.2f", averageSpeed)} km/h"
    }


    override fun getItemCount(): Int {
        return routes.size
    }
}
