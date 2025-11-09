package com.example.travelcompanion.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travelcompanion.R
import com.example.travelcompanion.model.TripActivity
import com.squareup.picasso.Picasso


class TripActivityAdapter(
    private var activities: List<TripActivity> = emptyList(),
    private val onItemClick: (TripActivity) -> Unit

) : RecyclerView.Adapter<TripActivityAdapter.ActivityViewHolder>() {

    // Modifica inner class per passare la lambda
    inner class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val noteText: TextView = itemView.findViewById(R.id.activityNote)
        val photo: ImageView = itemView.findViewById(R.id.activityPhoto)

        fun bind(activity: TripActivity) {
            noteText.text = activity.note ?: ""

            if (activity.photoPath != null) {
                photo.visibility = View.VISIBLE
                Picasso.get().load(activity.photoPath).into(photo)
            } else {
                photo.visibility = View.GONE
            }

            // Imposta il click sull'intera riga
            itemView.setOnClickListener {
                onItemClick(activity)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        // Chiama la funzione bind che hai già scritto
        holder.bind(activities[position])
    }

    override fun getItemCount(): Int = activities.size

    fun setActivities(newList: List<TripActivity>) {
        activities = newList
        notifyDataSetChanged()
    }
}
