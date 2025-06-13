package com.example.s7opcuaapp.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.example.s7opcuaapp.R
import com.example.s7opcuaapp.databinding.RunPointCardBinding

class RunPointCardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val binding = RunPointCardBinding.inflate(
        LayoutInflater.from(context), this, true
    )

    init {
        orientation = VERTICAL
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.RunPointCardView)
            binding.tvTitle.text =
                a.getString(R.styleable.RunPointCardView_titleText) ?: ""
            a.recycle()
        }
    }

    var startX: String
        get() = binding.etX.text.toString()
        set(v) { binding.etX.setText(v) }
    var startY: String
        get() = binding.etY.text.toString()
        set(v) { binding.etY.setText(v) }
    var startZ: String
        get() = binding.etZ.text.toString()
        set(v) { binding.etZ.setText(v) }
}
