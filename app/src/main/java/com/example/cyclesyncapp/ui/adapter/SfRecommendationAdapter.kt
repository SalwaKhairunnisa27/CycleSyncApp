package com.example.cyclesyncapp.ui.adapter

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.cyclesyncapp.R
import com.example.cyclesyncapp.ui.dashboard.SmartFoodActivity.RecommendationItem

class SfRecommendationAdapter(
    private var items: List<RecommendationItem> = emptyList()
) : RecyclerView.Adapter<SfRecommendationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSfRecName: TextView = view.findViewById(R.id.tvSfRecName)
        val tvSfRecBadge: TextView = view.findViewById(R.id.tvSfRecBadge)
        val tvSfRecReason: TextView = view.findViewById(R.id.tvSfRecReason)
        val layoutSfRecNutrients: LinearLayout = view.findViewById(R.id.layoutSfRecNutrients)
    }

    fun submitList(newItems: List<RecommendationItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_sf_recommendation, parent, false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvSfRecName.text = item.name
        holder.tvSfRecReason.text = item.reason

        // Set up recommendation badge text and background
        holder.tvSfRecBadge.text = item.badgeText
        holder.tvSfRecBadge.setBackgroundResource(
            when (item.badgeColorType) {
                "GOLD" -> R.drawable.bg_pill_gold
                "GREEN" -> R.drawable.bg_pill_green
                "BLUE" -> R.drawable.bg_pill_blue
                else -> R.drawable.bg_pill_soft
            }
        )

        // Set up nutrient badges dynamically
        holder.layoutSfRecNutrients.removeAllViews()
        for (nutrient in item.nutrients) {
            val tvNutrient = TextView(holder.itemView.context).apply {
                text = nutrient
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
                setTextColor(ContextCompat.getColor(context, R.color.p))
                setBackgroundResource(R.drawable.bg_badge_nutrient)
                setPadding(14, 8, 14, 8)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 16, 0)
                }
                layoutParams = lp
            }
            holder.layoutSfRecNutrients.addView(tvNutrient)
        }
    }

    override fun getItemCount(): Int = items.size
}
