package app.lefun.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.lefun.ble.ConnectionState
import app.lefun.ble.exportReport

@Composable
fun LefunApp(vm: WatchViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf("Dashboard", "Devices", "Explorer")
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            titles.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
        }
        when (tab) {
            0 -> Dashboard(vm)
            1 -> Devices(vm)
            else -> Explorer(vm)
        }
    }
}

@Composable
private fun Dashboard(vm: WatchViewModel) {
    val state by vm.ble.state.collectAsState()
    val battery by vm.ble.battery.collectAsState()
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Watch 7", style = MaterialTheme.typography.titleLarge)
                Text("Connection: $state")
                Text("Battery: ${battery?.let { "$it%" } ?: "unknown"}")
            }
        }
        Text(
            "Steps, heart rate and history appear here once their commands are verified from captures.",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (state == ConnectionState.Connected) OutlinedButton(onClick = { vm.ble.disconnect() }) { Text("Disconnect") }
    }
}

@Composable
private fun Devices(vm: WatchViewModel) {
    val devices by vm.ble.devices.collectAsState()
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.ble.startScan() }) { Text("Scan") }
            OutlinedButton(onClick = { vm.ble.stopScan() }) { Text("Stop") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(devices.sortedByDescending { it.rssi }, key = { it.address }) { d ->
                Card(Modifier.fillMaxWidth().clickable { vm.connect(d.address) }) {
                    Column(Modifier.padding(12.dp)) {
                        Text(d.name ?: "(unnamed)", style = MaterialTheme.typography.titleMedium)
                        Text("${d.address}  ${d.rssi} dBm", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun Explorer(vm: WatchViewModel) {
    val services by vm.ble.services.collectAsState()
    val log by vm.ble.log.collectAsState()
    val context = LocalContext.current
    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, exportReport(services, log))
                    context.startActivity(Intent.createChooser(send, "Export GATT report"))
                }) { Text("Export") }
                OutlinedButton(onClick = { vm.ble.clearLog() }) { Text("Clear log") }
            }
        }
        services.forEach { s ->
            item { Text("Service ${s.uuid}", style = MaterialTheme.typography.titleSmall) }
            items(s.characteristics, key = { "${s.uuid}/${it.uuid}" }) { c ->
                Row(Modifier.fillMaxWidth().padding(start = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(c.uuid.toString(), style = MaterialTheme.typography.bodySmall)
                        Text(c.properties.joinToString(" · "), style = MaterialTheme.typography.labelSmall)
                    }
                    if ("READ" in c.properties) OutlinedButton(onClick = { vm.ble.read(s.uuid, c.uuid) }) { Text("Read") }
                    if ("NOTIFY" in c.properties || "INDICATE" in c.properties)
                        OutlinedButton(onClick = { vm.ble.setNotify(s.uuid, c.uuid, true) }) { Text("Sub") }
                }
            }
        }
        item { Text("Log", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp)) }
        items(log.takeLast(200).reversed()) { e ->
            Text("${e.direction} ${e.uuid.take(8)} ${e.hex}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
