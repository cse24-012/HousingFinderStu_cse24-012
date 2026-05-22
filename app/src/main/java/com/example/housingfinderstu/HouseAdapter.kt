package com.example.housingfinderstu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
//bridge between the data and ui system
class HouseAdapter(
    private var houses: List<HouseEntity>,
    private val onItemClick: (HouseEntity) -> Unit
) : RecyclerView.Adapter<HouseAdapter.HouseViewHolder>() {

    fun updateList(newHouses: List<HouseEntity>) {
        houses = newHouses
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HouseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_house, parent, false)
        return HouseViewHolder(view)
    }

    override fun onBindViewHolder(holder: HouseViewHolder, position: Int) {
        holder.bind(houses[position])
        holder.itemView.setOnClickListener { onItemClick(houses[position]) }
    }

    override fun getItemCount() = houses.size
//house item views used in the main page
    class HouseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivHouseImage: ImageView = itemView.findViewById(R.id.ivHouseImage)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
//use of data binding
        fun bind(house: HouseEntity) {
            tvTitle.text = house.title
            tvPrice.text = "P${house.price}/month"
            tvLocation.text = house.location

            val resourceId = itemView.context.resources.getIdentifier(
                house.imageName, "drawable", itemView.context.packageName
            )
            if (resourceId != 0) {
                ivHouseImage.setImageResource(resourceId)
            } else {
                ivHouseImage.setImageResource(R.drawable.ic_house_placeholder)
            }
        }
    }
}