package de.fibs.geoappandroid.ui.settings

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
import java.text.DateFormat
import java.util.Locale

class SettingsViewModel(context: Context) : BaseObservable()  {
    private val repo = SettingsRepository.getInstance(context)

    private var _apiEndpoint: String = runBlocking {
        repo.apiEndpoint.first()
    }
    @get:Bindable
    var apiEndpoint: String
        get() = _apiEndpoint
        set(value) {
            if (value == _apiEndpoint) {
                return;
            }

            _apiEndpoint = value
            notifyPropertyChanged(BR.apiEndpoint)
            CoroutineScope(Dispatchers.IO).launch {
                repo.setApiEndpoint(value)
            }
        }

    private var _apiSendDataToken: String = runBlocking {
        repo.token.first()
    }
    @get:Bindable
    var apiSendDataToken: String
        get() = _apiSendDataToken
        set(value) {
            if (value == _apiSendDataToken) {
                return;
            }

            _apiSendDataToken = value
            CoroutineScope(Dispatchers.IO).launch {
                repo.setToken(value)
                notifyPropertyChanged(BR.apiSendDataToken)
                notifyPropertyChanged(BR.tokenExpirationDate)
                notifyPropertyChanged(BR.tokenIsExpired)
            }
        }

    @get:Bindable
    var tokenExpirationDate: String = "--"
        get() {
            val formatter = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM, // date style
                DateFormat.SHORT,  // time style
                Locale.getDefault()
            )

            val expirationDate = runBlocking {
                repo.tokenExpirationDate.first()
            }

            if (expirationDate == null) {
                return "--"
            }

            return formatter.format(expirationDate)
        }

    @get:Bindable
    var tokenIsExpired: Boolean = false
        get() = runBlocking {
            repo.tokenIsExpired.first()
        }
}