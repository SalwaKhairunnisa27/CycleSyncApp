package com.example.cyclesyncapp.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cyclesyncapp.data.local.entity.RecommendationEntity
import com.example.cyclesyncapp.databinding.ItemRecommendationBinding

class RecommendationAdapter : ListAdapter<RecommendationEntity, RecommendationAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(private val binding: ItemRecommendationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: RecommendationEntity) {
            binding.apply {
                tvCategory.text = item.category
                tvTitle.text = item.title
                tvDescription.text = item.description
                tvBenefit.text = "Manfaat: ${item.benefit}"

                // Opsional: Ubah warna kategori berdasarkan tipe (FOOD/EXERCISE)
                if (item.type == "FOOD") {
                    tvCategory.setTextColor(root.context.getColor(android.R.color.holo_green_dark))
                } else {
                    tvCategory.setTextColor(root.context.getColor(android.R.color.holo_blue_dark))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecommendationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    object DiffCallback : DiffUtil.ItemCallback<RecommendationEntity>() {
        override fun areItemsTheSame(oldItem: RecommendationEntity, newItem: RecommendationEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RecommendationEntity, newItem: RecommendationEntity): Boolean {
            return oldItem == newItem
        }
    }
}