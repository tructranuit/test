package com.example.testprogress

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView

class CustomProgressBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private var progressBar: ProgressBar
    private var progressText: TextView
    private var progressHexagon: ImageView

    init {
        LayoutInflater.from(context).inflate(R.layout.view_custom_progress_bar, this, true)
        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)
        progressHexagon = findViewById(R.id.progress)

        // Set initial progress
        setProgress(50)
    }

    @SuppressLint("SetTextI18n")
    fun setProgress(progress: Int) {
        progressBar.progress = progress
        progressText.text = "$progress%"
        updateProgressPosition()
    }

    fun setMax(max: Int) {
        progressBar.max = max
        updateProgressPosition()
    }

    private fun updateProgressPosition() {
        val progress = progressBar.progress
        val max = progressBar.max
        val width = progressBar.width
        val hexagonWidth = progressHexagon.width

        val progressRatio = progress.toFloat() / max
        var leftMargin = (width * progressRatio - hexagonWidth / 2).toInt()

        // Adjust the left margin to keep the hexagon within bounds
        if (leftMargin < 0) {
            leftMargin = 0
        } else if (leftMargin + hexagonWidth > width) {
            leftMargin = width - hexagonWidth
        }

        val params = progressHexagon.layoutParams as LayoutParams
        params.leftMargin = leftMargin
        progressHexagon.layoutParams = params
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            updateProgressPosition()
        }
    }
}
