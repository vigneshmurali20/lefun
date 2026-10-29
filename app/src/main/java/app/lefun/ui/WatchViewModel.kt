package app.lefun.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import app.lefun.ble.WatchBle

class WatchViewModel(app: Application) : AndroidViewModel(app) {
    val ble = WatchBle(app)

    fun connect(address: String) = ble.connect(getApplication(), address)

    override fun onCleared() {
        ble.stopScan()
        ble.disconnect()
    }
}
