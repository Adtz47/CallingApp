package com.example.callingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class CallLogAdapter(private var callLogs: List<CallLogItem>) :
    RecyclerView.Adapter<CallLogAdapter.CallLogViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION
    private var onItemSelectedListener: ((String) -> Unit)? = null
    private var filteredCallLogs = callLogs

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallLogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_call_log_adapter, parent, false)
        return CallLogViewHolder(view)
    }

    override fun onBindViewHolder(holder: CallLogViewHolder, position: Int) {
        val callLog = filteredCallLogs[position]
        holder.bind(callLog)

        // Handle selection state
        holder.itemView.isSelected = position == selectedPosition
        holder.itemView.setBackgroundColor(
            if (position == selectedPosition)
                ContextCompat.getColor(holder.itemView.context, R.color.selected_item_background)
            else
                ContextCompat.getColor(holder.itemView.context, android.R.color.transparent)
        )
    }

    override fun getItemCount(): Int = filteredCallLogs.size

    inner class CallLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val numberTextView: TextView = itemView.findViewById(R.id.textViewNumber)
        private val typeTextView: TextView = itemView.findViewById(R.id.textViewType)
        private val dateTextView: TextView = itemView.findViewById(R.id.textViewDate)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    setSelectedItem(position)
                }
            }
        }

        fun bind(callLog: CallLogItem) {
            // Show contact name if available, otherwise show number
            numberTextView.text = callLog.contactName ?: callLog.number
            typeTextView.text = callLog.type
            dateTextView.text = formatDate(callLog.date)
        }
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(timestamp)
    }

    fun setOnItemSelectedListener(listener: (String) -> Unit) {
        onItemSelectedListener = listener
    }

    private fun setSelectedItem(position: Int) {
        val previousSelected = selectedPosition
        selectedPosition = position

        // Notify previous and new positions to update their appearance
        notifyItemChanged(previousSelected)
        notifyItemChanged(selectedPosition)

        // Notify the activity about the selection
        onItemSelectedListener?.invoke(filteredCallLogs[position].number)
    }

    fun getSelectedNumber(): String? {
        return if (selectedPosition != RecyclerView.NO_POSITION) {
            filteredCallLogs[selectedPosition].number
        } else {
            null
        }
    }

    fun filter(query: String) {
        filteredCallLogs = if (query.isEmpty()) {
            callLogs
        } else {
            callLogs.filter { callLog ->
                val matchesNumber = callLog.number.contains(query, ignoreCase = true)
                val matchesName = callLog.contactName?.contains(query, ignoreCase = true) == true
                matchesNumber || matchesName
            }
        }
        selectedPosition = RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }

    fun updateData(newCallLogs: List<CallLogItem>) {
        callLogs = newCallLogs
        filteredCallLogs = newCallLogs
        selectedPosition = RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }
}