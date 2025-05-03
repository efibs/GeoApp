package de.fibs.geoappandroid.ui.home

import android.content.Context
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import de.fibs.geoappandroid.BR
import de.fibs.geoappandroid.repo.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class HomeViewModel(context: Context) : BaseObservable() {

    private val repo = SettingsRepository.getInstance(context)

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

    private var _sensorFrequency: Long = runBlocking {
        repo.sensorFrequency.first()
    }
    @get:Bindable
    var sensorFrequency: String
        get() = _sensorFrequency.toString()
        set(value) {
            if (value.isEmpty()) {
                return;
            }
            _sensorFrequency = value.toLong()
            notifyPropertyChanged(BR.sensorFrequency)
            CoroutineScope(Dispatchers.IO).launch {
                repo.setSensorFrequency(value.toLong())
            }
        }

    private var _sendFrequency: Long = runBlocking {
        repo.sendFrequency.first()
    }
    @get:Bindable
    var sendFrequency: String
        get() = _sendFrequency.toString()
        set(value) {
            if (value.isEmpty()) {
                return
            }
            _sendFrequency = value.toLong()
            notifyPropertyChanged(BR.sendFrequency)
            CoroutineScope(Dispatchers.IO).launch {
                repo.setSendFrequency(value.toLong())
            }
        }

    @get:Bindable
    var connectTimeoutText: String
        get() = repo.httpClientConnectTimeoutMillis.value.toString()
        set(value) {
            if (value.isBlank()) {
                return
            }
            val lVal = value.toLong()
            if (lVal == repo.httpClientConnectTimeoutMillis.value) {
                return
            }
            repo.setHttpClientConnectTimeoutMillis(lVal)
            notifyPropertyChanged(BR.connectTimeoutText)
        }

    @get:Bindable
    var writeTimeoutText: String
        get() = repo.httpClientWriteTimeoutMilliseconds.value.toString()
        set(value) {
            if (value.isBlank()) {
                return
            }
            val lVal = value.toLong()
            if (lVal == repo.httpClientWriteTimeoutMilliseconds.value) {
                return
            }
            repo.setHttpClientWriteTimeoutMillis(lVal)
            notifyPropertyChanged(BR.writeTimeoutText)
        }

    @get:Bindable
    var readTimeoutText: String
        get() = repo.httpClientReadTimeoutMilliseconds.value.toString()
        set(value) {
            if (value.isBlank()) {
                return
            }
            val lVal = value.toLong()
            if (lVal == repo.httpClientReadTimeoutMilliseconds.value) {
                return
            }
            repo.setHttpClientReadTimeoutMillis(lVal)
            notifyPropertyChanged(BR.readTimeoutText)
        }

    @get:Bindable
    val sendErrorText: String
        get() = repo.sendErrorText.value?.toString() ?: "---"

    @get:Bindable
    val bufferCurrent = repo.currentBufferSize

    @get:Bindable
    val bufferMax = repo.bufferSize

    @get:Bindable
    val bufferText: String
        get() = "${repo.currentBufferSize.value ?: 0} / ${repo.bufferSize}"

    @get:Bindable
    val lastSensorUpdateTimeText: String
        get() = repo.lastSensorUpdateTime.value?.toString() ?: "---"

    @get:Bindable
    val lastRequestDurationText: String
        get() = "${repo.lastRequestDurationMilliseconds.value ?: "---"} ms"

    init {
        repo.currentBufferSize.observeForever {
            notifyPropertyChanged(BR.bufferCurrent)
            notifyPropertyChanged(BR.bufferText)
        }

        repo.lastSensorUpdateTime.observeForever {
            notifyPropertyChanged(BR.lastSensorUpdateTimeText)
        }

        repo.lastRequestDurationMilliseconds.observeForever{
            notifyPropertyChanged(BR.lastRequestDurationText)
        }

        repo.sendErrorText.observeForever{
            notifyPropertyChanged(BR.sendErrorText)
        }
    }
}