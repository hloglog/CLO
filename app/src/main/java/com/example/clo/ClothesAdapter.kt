package com.example.clo

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.clo.databinding.ItemClothingBinding

class ClothesAdapter : ListAdapter<ClosetActivity.ClothingItem, ClothesAdapter.ClothingViewHolder>(ClothingDiffCallback()) {
    companion object {
        private const val TAG = "ClothesAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClothingViewHolder {
        val binding = ItemClothingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ClothingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ClothingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ClothingViewHolder(private val binding: ItemClothingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ClosetActivity.ClothingItem) {
            Log.d(TAG, "Loading image from URL: ${item.imageUrl}")
            
            val requestOptions = RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)

            Glide.with(binding.root.context)
                .load(item.imageUrl)
                .apply(requestOptions)
                .into(binding.clothingImage)
        }
    }

    private class ClothingDiffCallback : DiffUtil.ItemCallback<ClosetActivity.ClothingItem>() {
        override fun areItemsTheSame(oldItem: ClosetActivity.ClothingItem, newItem: ClosetActivity.ClothingItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ClosetActivity.ClothingItem, newItem: ClosetActivity.ClothingItem): Boolean {
            return oldItem == newItem
        }
    }
} 