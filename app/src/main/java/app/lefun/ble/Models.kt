package app.lefun.ble

import java.util.UUID

enum class ConnectionState { Disconnected, Connecting, Connected }

data class ScanDevice(val name: String?, val address: String, val rssi: Int)

data class CharInfo(val uuid: UUID, val properties: List<String>)

data class ServiceInfo(val uuid: UUID, val characteristics: List<CharInfo>)

/** One observed BLE event, kept so sessions can be exported and compared against HCI captures. */
data class LogEntry(val timeMillis: Long, val direction: String, val uuid: String, val hex: String)

object Uuids {
    val BATTERY_SERVICE: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    val BATTERY_LEVEL: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
    val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
}
