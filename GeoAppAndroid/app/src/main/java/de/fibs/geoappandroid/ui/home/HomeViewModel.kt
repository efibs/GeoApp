package de.fibs.geoappandroid.ui.home

import android.os.Handler
import android.os.Looper
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import de.fibs.geoappandroid.BR


class HomeViewModel : BaseObservable() {

    @get:Bindable
    val text: String
        get() {
            val collecting = _collecting
            return if (collecting) "Collecting data" else "Collection stopped"
        }

    private var _collecting = false

    @get:Bindable
    var collecting: Boolean
        get() = _collecting
        set(value) {
            _collecting = value
            notifyPropertyChanged(BR.text)
            notifyPropertyChanged(BR.collecting)

            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                collecting = !collecting
            }, 2000L)
        }
}