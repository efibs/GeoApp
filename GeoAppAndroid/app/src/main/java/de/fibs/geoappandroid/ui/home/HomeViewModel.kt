package de.fibs.geoappandroid.ui.home

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import de.fibs.geoappandroid.BR
import de.fibs.geoappandroid.repo.DataRepository


class HomeViewModel : BaseObservable() {

    private val repo = DataRepository.getInstance()

    @get:Bindable
    val collectingText: String
        get() {
            val collecting = repo.collecting.value ?: false
            return if (collecting) "Collecting data" else "Collection stopped"
        }

    @get:Bindable
    var collecting: Boolean
        get() = repo.collecting.value ?: false
        set(value) {
            repo.setCollecting(value)
            notifyPropertyChanged(BR.collectingText)
            notifyPropertyChanged(BR.collecting)
        }

    @get:Bindable
    var frequency: String
        get() = (repo.frequency.value ?: 60).toString()
        set(value) {
            if (value.isEmpty()) {
                return;
            }
            repo.setFrequency(value.toLong())
            notifyPropertyChanged(BR.frequency)
        }
}