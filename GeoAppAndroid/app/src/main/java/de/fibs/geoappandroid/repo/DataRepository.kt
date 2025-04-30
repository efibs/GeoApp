package de.fibs.geoappandroid.repo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.Date

class DataRepository private constructor() {
    private val _collecting = MutableLiveData(false)
    val collecting: LiveData<Boolean> = _collecting

    fun setCollecting(coll: Boolean) {
        _collecting.postValue(coll)
    }

    private val _frequency = MutableLiveData(DEFAULT_FREQUENCY)
    val frequency: LiveData<Long> = _frequency

    fun setFrequency(freq: Long) {
        _frequency.postValue(freq)
    }

    val bufferSize = 100000
    private val _currentBufferSize = MutableLiveData(0)
    val currentBufferSize: LiveData<Int> = _currentBufferSize

    fun setCurrentBufferSize(s: Int) {
        _currentBufferSize.postValue(s)
    }

    private val _lastUpdateTime = MutableLiveData<Date?>(null)
    val lastSensorUpdateTime: LiveData<Date?> = _lastUpdateTime

    fun setLastSensorUpdateTime(newDate: Date) {
        _lastUpdateTime.postValue(newDate)
    }

    companion object {
        @Volatile
        private var INSTANCE: DataRepository? = null

        fun getInstance(): DataRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DataRepository().also { INSTANCE = it }
            }
        }

        public var DEFAULT_FREQUENCY = 10L;
    }
}