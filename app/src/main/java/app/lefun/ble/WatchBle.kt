package app.lefun.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * Protocol-agnostic BLE transport: scan, connect, discover, read, subscribe.
 * Deliberately has no write API yet — writes wait until a command is understood from captures.
 * Callers must hold BLUETOOTH_SCAN / BLUETOOTH_CONNECT (see MainActivity).
 */
@SuppressLint("MissingPermission")
class WatchBle(context: Context) {
    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    private val _devices = MutableStateFlow<List<ScanDevice>>(emptyList())
    val devices: StateFlow<List<ScanDevice>> = _devices

    private val _state = MutableStateFlow(ConnectionState.Disconnected)
    val state: StateFlow<ConnectionState> = _state

    private val _services = MutableStateFlow<List<ServiceInfo>>(emptyList())
    val services: StateFlow<List<ServiceInfo>> = _services

    private val _log = MutableStateFlow<List<LogEntry>>(emptyList())
    val log: StateFlow<List<LogEntry>> = _log

    private val _battery = MutableStateFlow<Int?>(null)
    val battery: StateFlow<Int?> = _battery

    private var gatt: BluetoothGatt? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val d = ScanDevice(result.device.name ?: result.scanRecord?.deviceName, result.device.address, result.rssi)
            _devices.update { list -> list.filterNot { it.address == d.address } + d }
        }
    }

    fun startScan() {
        _devices.value = emptyList()
        adapter?.bluetoothLeScanner?.startScan(scanCallback)
    }

    fun stopScan() {
        adapter?.bluetoothLeScanner?.stopScan(scanCallback)
    }

    fun connect(context: Context, address: String) {
        stopScan()
        val device = adapter?.getRemoteDevice(address) ?: return
        _state.value = ConnectionState.Connecting
        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    fun disconnect() {
        gatt?.disconnect()
    }

    fun read(service: UUID, characteristic: UUID) {
        gatt?.getService(service)?.getCharacteristic(characteristic)?.let { gatt?.readCharacteristic(it) }
    }

    fun setNotify(service: UUID, characteristic: UUID, enabled: Boolean) {
        val g = gatt ?: return
        val c = g.getService(service)?.getCharacteristic(characteristic) ?: return
        g.setCharacteristicNotification(c, enabled)
        val cccd = c.getDescriptor(Uuids.CCCD) ?: return
        val value = when {
            !enabled -> BluetoothGattDescriptorValues.DISABLE
            c.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0 -> BluetoothGattDescriptorValues.NOTIFY
            else -> BluetoothGattDescriptorValues.INDICATE
        }
        g.writeDescriptor(cccd, value)
        record("SUBSCRIBE", c.uuid, value)
    }

    fun clearLog() { _log.value = emptyList() }

    private fun record(direction: String, uuid: UUID, bytes: ByteArray) {
        val entry = LogEntry(System.currentTimeMillis(), direction, uuid.toString(), bytes.toHex())
        _log.update { it + entry }
    }

    private fun handleValue(c: BluetoothGattCharacteristic, value: ByteArray, direction: String) {
        record(direction, c.uuid, value)
        if (c.uuid == Uuids.BATTERY_LEVEL && value.isNotEmpty()) _battery.value = value[0].toInt() and 0xFF
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _state.value = ConnectionState.Connected
                g.discoverServices()
            } else {
                _state.value = ConnectionState.Disconnected
                _services.value = emptyList()
                g.close()
                gatt = null
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            _services.value = g.services.map { s ->
                ServiceInfo(s.uuid, s.characteristics.map { CharInfo(it.uuid, propertyNames(it.properties)) })
            }
            g.getService(Uuids.BATTERY_SERVICE)?.getCharacteristic(Uuids.BATTERY_LEVEL)?.let { g.readCharacteristic(it) }
        }

        override fun onCharacteristicRead(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) handleValue(c, value, "READ")
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray) {
            handleValue(c, value, "NOTIFY")
        }
    }
}

private object BluetoothGattDescriptorValues {
    val NOTIFY = android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
    val INDICATE = android.bluetooth.BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
    val DISABLE = android.bluetooth.BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
}

fun propertyNames(p: Int): List<String> = buildList {
    if (p and BluetoothGattCharacteristic.PROPERTY_READ != 0) add("READ")
    if (p and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) add("WRITE")
    if (p and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) add("WRITE_NO_RESP")
    if (p and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) add("NOTIFY")
    if (p and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) add("INDICATE")
}
