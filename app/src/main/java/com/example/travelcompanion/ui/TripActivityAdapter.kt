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
    private var activities: List<TripActivity> = emptyList()
) : RecyclerView.Adapter<TripActivityAdapter.ActivityViewHolder>() {

    inner class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val noteText: TextView = itemView.findViewById(R.id.activityNote)
        val photo: ImageView = itemView.findViewById(R.id.activityPhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val activity = activities[position]
        holder.noteText.text = activity.note ?: ""

        if (activity.photoPath != null) {
            holder.photo.visibility = View.VISIBLE
            Picasso.get().load(activity.photoPath).into(holder.photo)
        } else {
            holder.photo.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = activities.size

    fun setActivities(newList: List<TripActivity>) {
        activities = newList
        notifyDataSetChanged()
    }
}
