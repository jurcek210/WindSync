package com.example.wind

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.wind.databinding.RecyclerViewRowEventBinding

class EventAdapter(
    private val events: MutableList<EventItem>
) : RecyclerView.Adapter<EventAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: RecyclerViewRowEventBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RecyclerViewRowEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val e = events[position]

        holder.binding.tvTopic.text = e.topic
        holder.binding.tvMessage.text = e.message
        holder.binding.tvMeta.text =
            "${formatTime(e.timestamp)} • ${"%.5f".format(e.lat)}, ${"%.5f".format(e.lon)}"

        val iconRes = if (e.topic.startsWith("veter/")) {
            R.drawable.wind
        } else {
            R.drawable.windmill
        }

        holder.binding.imgType.setImageResource(iconRes)
    }

    override fun getItemCount(): Int = events.size

    fun updateData(newList: List<EventItem>) {
        events.clear()
        events.addAll(newList)
        notifyDataSetChanged()
    }

    private fun formatTime(iso: String): String {
        return if (iso.length >= 16) iso.substring(0, 16).replace("T", " ")
        else iso
    }
}
