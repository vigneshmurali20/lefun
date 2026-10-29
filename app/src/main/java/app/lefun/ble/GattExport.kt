package app.lefun.ble

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Plain-text dump of the GATT table and event log, for sharing into the research notes (redact the MAC first). */
fun exportReport(services: List<ServiceInfo>, log: List<LogEntry>): String = buildString {
    appendLine("# GATT services")
    services.forEach { s ->
        appendLine("service ${s.uuid}")
        s.characteristics.forEach { appendLine("  char ${it.uuid} [${it.properties.joinToString(",")}]") }
    }
    appendLine()
    appendLine("# Event log")
    val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    log.forEach { appendLine("${fmt.format(Date(it.timeMillis))} ${it.direction} ${it.uuid} ${it.hex}") }
}
