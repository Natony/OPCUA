package com.example.s7opcuaapp.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.example.s7opcuaapp.R
import com.example.s7opcuaapp.databinding.IntControlItemBinding

class IntControlItemView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val binding = IntControlItemBinding.inflate(
        LayoutInflater.from(context), this
    )

    var onClick: (() -> Unit)? = null

    init {
        orientation = HORIZONTAL
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.IntControlItemView)
            binding.tvLabel.text =
                a.getString(R.styleable.IntControlItemView_labelText) ?: ""
            val iconOn = a.getResourceId(R.styleable.IntControlItemView_iconOn, 0)
            val iconOff = a.getResourceId(R.styleable.IntControlItemView_iconOff, 0)
            val init = a.getInteger(R.styleable.IntControlItemView_initialValue, 0)
            a.recycle()

            binding.tvValue.text = init.toString()
            binding.ivIcon.setImageResource(if (init != 0) iconOn else iconOff)

            setOnClickListener {
                onClick?.invoke()
            }
        }
    }

    fun setValue(v: Int) {
        binding.tvValue.text = v.toString()
        // bạn có thể set lại icon tuỳ v != 0
    }
}
