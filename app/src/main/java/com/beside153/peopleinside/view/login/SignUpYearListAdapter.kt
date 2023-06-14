package com.beside153.peopleinside.view.login

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beside153.peopleinside.databinding.ItemSignUpBirthYearBinding
import com.beside153.peopleinside.model.login.BirthYearModel

class SignUpYearListAdapter(private val onYearItemClick: (item: BirthYearModel) -> Unit) :
    ListAdapter<BirthYearModel, SignUpYearListAdapter.YearItemViewHolder>(YearItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): YearItemViewHolder {
        val binding = ItemSignUpBirthYearBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val viewHolder = YearItemViewHolder(binding)
        viewHolder.itemView.setOnClickListener {
            val position = viewHolder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onYearItemClick(getItem(position))
            }
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: YearItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class YearItemViewHolder(private val binding: ItemSignUpBirthYearBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BirthYearModel) {
            binding.item = item
        }
    }
}

private class YearItemDiffCallback : DiffUtil.ItemCallback<BirthYearModel>() {
    override fun areItemsTheSame(oldItem: BirthYearModel, newItem: BirthYearModel): Boolean {
        return oldItem.year == newItem.year
    }

    override fun areContentsTheSame(oldItem: BirthYearModel, newItem: BirthYearModel): Boolean {
        return oldItem == newItem
    }
}
