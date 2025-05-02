package de.fibs.geoappandroid.ui.bindings

import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.databinding.BindingAdapter

@BindingAdapter("booleanColor")
fun setBooleanColor(textView: TextView, bool: Boolean) {
    val color = if (bool) Color.GREEN else Color.RED
    textView.setTextColor(color)
}