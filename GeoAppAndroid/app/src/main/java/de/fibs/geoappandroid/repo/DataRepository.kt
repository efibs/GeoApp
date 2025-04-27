package de.fibs.geoappandroid.repo

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class DataRepository private constructor() {
    private val _collecting = MutableLiveData(false)
    val collecting: LiveData<Boolean> = _collecting

    fun setCollecting(coll: Boolean) {
        _collecting.postValue(coll)
    }

    private val _frequency = MutableLiveData(60L)
    val frequency: LiveData<Long> = _frequency

    fun setFrequency(freq: Long) {
        _frequency.postValue(freq)
    }

    companion object {
        @Volatile
        private var INSTANCE: DataRepository? = null

        fun getInstance(): DataRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DataRepository().also { INSTANCE = it }
            }
        }
    }
}