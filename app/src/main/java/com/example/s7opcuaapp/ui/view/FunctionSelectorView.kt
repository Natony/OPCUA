package com.example.s7opcuaapp.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.ArrayAdapter
import com.example.s7opcuaapp.R
import com.example.s7opcuaapp.databinding.FunctionSelectorViewBinding

class FunctionSelectorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val binding = FunctionSelectorViewBinding.inflate(
        LayoutInflater.from(context), this)

    var onRun: (() -> Unit)? = null

    init {
        orientation = HORIZONTAL
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.FunctionSelectorView)
            val entriesId = a.getResourceId(R.styleable.FunctionSelectorView_entries, 0)
            val selected = a.getInteger(R.styleable.FunctionSelectorView_selectedIndex, 0)
            a.recycle()

            if (entriesId != 0) {
                val arr = resources.getStringArray(entriesId)
                binding.spinnerFunc.adapter =
                    ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, arr)
                binding.spinnerFunc.setSelection(selected)
            }

            binding.btnRun.setOnClickListener { onRun?.invoke() }
        }
    }

    fun getSelectedIndex(): Int = binding.spinnerFunc.selectedItemPosition
}
