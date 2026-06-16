package com.example.todoaccesible.ui.specialist.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.todoaccesible.R
import com.example.todoaccesible.data.model.SpecialistDiagnostic
import com.google.android.material.chip.Chip

class DiagnosticAdapter(
    private var diagnostics: List<SpecialistDiagnostic>,
    private val onItemClick: (SpecialistDiagnostic) -> Unit
) : RecyclerView.Adapter<DiagnosticAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvProjectName: TextView = view.findViewById(R.id.tvProjectName)
        val tvClientName: TextView = view.findViewById(R.id.tvClientName)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val chipStatus: Chip = view.findViewById(R.id.chipStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diagnostic, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = diagnostics[position]
        holder.tvProjectName.text = item.projectName
        holder.tvClientName.text = "Cliente: ${item.clientName}"
        holder.tvDate.text = "Fecha: ${item.date}"
        holder.chipStatus.text = item.status.label
        
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = diagnostics.size

    fun updateData(newData: List<SpecialistDiagnostic>) {
        diagnostics = newData
        notifyDataSetChanged()
    }
}
