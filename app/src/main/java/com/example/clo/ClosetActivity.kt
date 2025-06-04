package com.example.clo

import android.graphics.Paint
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ClosetActivity : AppCompatActivity() {
    private lateinit var topButton: Button
    private lateinit var bottomButton: Button
    private lateinit var shoesButton: Button
    private lateinit var accessoriesButton: Button
    private lateinit var clothesRecyclerView: RecyclerView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_closet)

        // Initialize views
        topButton = findViewById(R.id.button_top)
        bottomButton = findViewById(R.id.button_bottom)
        shoesButton = findViewById(R.id.button_shoes)
        accessoriesButton = findViewById(R.id.button_accessories)
        clothesRecyclerView = findViewById(R.id.recycler_view_clothes)

        // Set up RecyclerView with Grid Layout (4 columns)
        clothesRecyclerView.layoutManager = GridLayoutManager(this, 4)

        // Set click listeners for category buttons
        topButton.setOnClickListener {
            resetAllUnderlines()
            topButton.paintFlags = topButton.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            // TODO: Load top clothes items
        }

        bottomButton.setOnClickListener {
            resetAllUnderlines()
            bottomButton.paintFlags = bottomButton.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            // TODO: Load bottom clothes items
        }

        shoesButton.setOnClickListener {
            resetAllUnderlines()
            shoesButton.paintFlags = shoesButton.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            // TODO: Load shoes items
        }

        accessoriesButton.setOnClickListener {
            resetAllUnderlines()
            accessoriesButton.paintFlags = accessoriesButton.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            // TODO: Load accessories items
        }

        // Set default selection to tops
        topButton.performClick()
    }

    private fun resetAllUnderlines() {
        topButton.paintFlags = topButton.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
        bottomButton.paintFlags = bottomButton.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
        shoesButton.paintFlags = shoesButton.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
        accessoriesButton.paintFlags = accessoriesButton.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
    }
} 