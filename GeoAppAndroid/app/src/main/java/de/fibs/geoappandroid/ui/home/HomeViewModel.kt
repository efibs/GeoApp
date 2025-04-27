package de.fibs.geoappandroid.ui.home

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import de.fibs.geoappandroid.BR
import de.fibs.geoappandroid.repo.DataRepository
import de.fibs.geoappandroid.repo.DataRepository.Companion.DEFAULT_FREQUENCY


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
        get() = (repo.frequency.value ?: DEFAULT_FREQUENCY).toString()
        set(value) {
            if (value.isEmpty()) {
                return;
            }
            repo.setFrequency(value.toLong())
            notifyPropertyChanged(BR.frequency)
        }

    @get:Bindable
    val bufferCurrent = repo.currentBufferSize

    @get:Bindable
    val bufferMax = repo.bufferSize

    @get:Bindable
    val bufferText: String
        get() = "${repo.currentBufferSize.value ?: 0} / ${repo.bufferSize}"

    init {
        repo.currentBufferSize.observeForever {
            notifyPropertyChanged(BR.bufferCurrent)
            notifyPropertyChanged(BR.bufferText)
        }
    }
}