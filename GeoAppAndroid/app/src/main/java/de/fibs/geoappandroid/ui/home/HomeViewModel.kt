package de.fibs.geoappandroid.ui.home

import androidx.databinding.Bindable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    val text = MutableLiveData("This is home Fragment")

    private val _collecting = MutableLiveData(false)

    @get:Bindable
    var collecting: Boolean
        get() = _collecting.value ?: false
        set(value) {
            _collecting.value = value
            if (value) {
                text.value = "Enabled"
            } else {
                text.value = "Disabled"
            }
        }
}