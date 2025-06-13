package com.example.s7opcuaapp.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.example.s7opcuaapp.R
import com.example.s7opcuaapp.databinding.BoolControlItemBinding

/**
 * A custom view that displays a boolean control with a label and toggles icons.
 */
class BoolControlItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: BoolControlItemBinding =
        BoolControlItemBinding.inflate(LayoutInflater.from(context), this)

    var labelText: String? = null
        set(value) {
            field = value
            binding.tvLabel.text = value
        }

    var iconOnRes: Int = 0
        set(value) {
            field = value
            binding.ivIconOn.setImageResource(value)
        }

    var iconOffRes: Int = 0
        set(value) {
            field = value
            binding.ivIconOff.setImageResource(value)
        }

    var value: Boolean = false
        set(value) {
            field = value
            updateIconState()
        }

    init {
        // Read custom attributes
        attrs?.let {
            context.obtainStyledAttributes(it, R.styleable.BoolControlItemView).apply {
                labelText = getString(R.styleable.BoolControlItemView_labelText)
                iconOnRes = getResourceId(R.styleable.BoolControlItemView_iconOn, 0)
                iconOffRes = getResourceId(R.styleable.BoolControlItemView_iconOff, 0)
                // <-- use the renamed attribute initialBoolValue
                value = getBoolean(R.styleable.BoolControlItemView_initialBoolValue, false)
                recycle()
            }
        }

        // Initialize the icons to the correct state
        updateIconState()
    }

    private fun updateIconState() {
        binding.ivIconOn.alpha  = if (value) 1f else 0f
        binding.ivIconOff.alpha = if (value) 0f else 1f
    }
}
