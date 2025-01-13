import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.aplikacjafitness.R
import com.example.aplikacjafitness.Route

class RoutesAdapter(private val routes: List<Route>) : RecyclerView.Adapter<RoutesAdapter.RouteViewHolder>() {

    class RouteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateTextView: TextView = itemView.findViewById(R.id.routeDate)
        val distanceTextView: TextView = itemView.findViewById(R.id.routeDistance)
        val timeTextView: TextView = itemView.findViewById(R.id.routeTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_route, parent, false)
        return RouteViewHolder(view)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        val route = routes[position]
        holder.dateTextView.text = route.date
        holder.distanceTextView.text = "Dystans: ${route.distance} km"
        holder.timeTextView.text = "Czas: ${route.time}"
    }

    override fun getItemCount(): Int {
        return routes.size
    }
}
