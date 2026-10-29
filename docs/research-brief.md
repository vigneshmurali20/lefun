# Watch 7 + Lefun Health — Custom Companion App Research Brief

**Status:** Discovery / reverse-engineering  
**Last updated:** 2026-10-29  
**Target device:** Smartwatch shown as `Watch 7` in Android Bluetooth and Lefun Health  
**Companion app:** Lefun Health  
**Primary goal:** Build an independent Android companion app with a better interface and features, communicating directly with the watch where technically possible.

---

## 1. Executive summary

The project is to investigate how the user's `Watch 7` communicates with the Lefun Health Android app, then build a custom companion app around the protocol we can verify.

The recommended strategy is:

1. Treat Lefun Health as a **reference implementation**, not as a framework we can assume is reusable.
2. Observe Bluetooth Low Energy (BLE) communication during controlled app actions.
3. Map the watch's GATT services and characteristics, then identify commands and responses.
4. Build a small independent Android prototype that verifies one operation.
5. Expand to a polished companion app only as each feature is confirmed to work.

The user has supplied two Android bug-report ZIP files and a screenshot of the connected watch in Lefun Health. The first report's Bluetooth capture was reported as older than the bug report; a later report was described as containing a fresh HCI capture with thousands of packets. **The actual packet-level protocol has not yet been documented in this brief.** Previous conversational summaries described a packet count and traffic categories, but those should be revalidated directly against the ZIP before being treated as forensic conclusions.

---

## 2. Confirmed information

### Device and app observations

- The watch appears in Android Bluetooth as **`Watch 7`**.
- It is paired/connected to **Lefun Health** on an Android phone.
- The screenshot of the Lefun Health device page shows:
  - Device name: `Watch 7`
  - A Bluetooth MAC address (intentionally not repeated here)
  - A battery indicator
  - A steps count
  - Calories and distance fields
  - A generic watch illustration
- The phone appears to be a Vivo device based on the Android report filename/model identifier `I2501i`, but confirm the exact commercial model if it matters.
- The user enabled Bluetooth HCI snoop logging and generated Android bug reports.
- One uploaded report was named:
  `bugreport-I2501i-BP2A.250605.031.A3_V000L1-2026-10-29-13-00-04.zip`
- A later report was named:
  `bugreport-I2501i-BP2A.250605.031.A3_V000L1-2026-10-29-13-10-59.zip`
- A previous analysis of the first report identified Lefun package ID `com.tjd.tjdmainS2` and a class/component string `com.tjd.lefun.sdk.device.work.DeviceService`. These are useful leads but should be rechecked against the actual report/APK before implementation.

### Not yet confirmed

- The exact watch manufacturer, chipset, SoC, RAM, flash size, firmware and Bluetooth stack.
- Whether this specific `Watch 7` is the same hardware as any similarly named Z7 Ultra or other market variant.
- Whether the watch uses a JieLi/JL7012A6S chipset.
- Whether the FT100-related reverse-engineering project is compatible with this watch.
- The UUIDs and semantics of the watch's GATT services and characteristics.
- Whether its protocol supports independent reads/writes, heart-rate collection, history sync, notifications or watch-face transfer.
- Whether the watch supports custom firmware or a full replacement OS.

**Do not treat product listings for similarly named watches as proof of this watch's internals.**

---

## 3. Research leads to verify

These resources were mentioned during the investigation. They are leads, not proof of compatibility:

- OpenWearOTA: https://github.com/HackSucks/OpenWearOTA  
  A project for OTA/firmware work on certain supported inexpensive smartwatches. Verify chipset and protocol compatibility before attempting any flashing.

- FT100 BLE reverse engineering: https://github.com/0xless/FT100_fitness_bracelet_reversing  
  Includes reverse-engineering artifacts for an FT100 bracelet. Potentially useful as a methodological reference, but it targets a different named device.

- FT100 technical write-up: https://lessonsec.com/posts/reversing_the_ft100_ble_fitness_bracelet/  
  Background on the FT100 investigation.

- Fogg: https://github.com/MiguelDLM/Fogg  
  Reverse-engineering tooling for certain JieLi/HK89-related devices and watch-face workflows. Compatibility with this Watch 7 is unknown.

- Android BLE overview: https://developer.android.com/develop/connectivity/bluetooth/ble/ble-overview  
  Official Android BLE development reference.

- Android Bluetooth debugging: https://source.android.com/docs/core/connect/bluetooth/verifying_debugging  
  Official guidance on Bluetooth debugging and HCI snoop logs.

- nRF Connect for Mobile: https://www.nordicsemi.com/Products/Development-tools/nRF-Connect-for-mobile  
  Can inspect BLE advertisements, GATT services and characteristics.

- Lefun firmware-upgrade help: https://lefunapp.com/how-to-upgrade-bracelet-firmware/  
  Reference for app/device firmware-update behavior; exact UI may vary.

Before relying on any repository, review its README, supported hardware list, license, and current status.

---

## 4. Product vision

Build an attractive, privacy-conscious companion app that improves on the Lefun Health user experience while supporting only capabilities verified on the actual watch.

### Candidate features

| Feature | Feasibility status | Notes |
|---|---|---|
| Clean dashboard | High | UI can be designed independently; live data depends on protocol. |
| Battery status | To verify | Visible in Lefun Health; must determine how it is obtained. |
| Steps / calories / distance | To verify | Shown in the app; determine whether values come from watch, phone, or calculations. |
| Heart-rate measurement | To verify | Requires a documented or reverse-engineered command/notification flow. |
| Sleep/activity history | To verify | Historical sync may use additional proprietary commands. |
| Local history and charts | High after data access | Store only measurements the watch actually supplies. |
| Export to CSV/JSON | High after data access | Use normalized local records. |
| Notification forwarding | To verify | Depends on supported protocol and Android notification access. |
| Alarms / settings | To verify | Each operation needs protocol testing. |
| Custom watch faces | Unknown | Requires knowledge of image format and transfer protocol. |
| Firmware modification | High risk / separate project | Do not attempt before identifying hardware, bootloader and recovery path. |
| Wear OS / Android installation | Very unlikely on typical low-cost watch hardware | Not the initial goal. |

### Product principles

- Build an independent client; do not require Lefun Health to remain running once protocol compatibility is established.
- Local-first storage by default.
- No cloud account required for basic functionality.
- Keep protocol-specific code separate from UI and data storage.
- Do not advertise unsupported or unverified health capabilities.
- Treat watch measurements as consumer-device data, not medical-grade readings.

---

## 5. Recommended architecture

```text
Android app (Kotlin + Jetpack Compose)
|
+-- UI layer
|   +-- Dashboard
|   +-- Trends / history
|   +-- Device connection and status
|   +-- Settings / diagnostics
|
+-- Domain layer
|   +-- Connect / disconnect
|   +-- Read device information
|   +-- Read battery
|   +-- Sync measurements (only verified commands)
|   +-- Configure supported settings
|
+-- BLE transport layer
|   +-- Scan and connect
|   +-- GATT service discovery
|   +-- Characteristic read/write
|   +-- Notifications / indications
|   +-- Timeouts and reconnects
|
+-- Watch protocol adapter
|   +-- UUID map
|   +-- Packet framing
|   +-- Command encoding
|   +-- Response parsing
|   +-- Checksums / sequence numbers (if required)
|
+-- Local persistence (Room)
    +-- Device metadata
    +-- Measurements
    +-- Sync history
    +-- Debug events
```

### Suggested stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Bluetooth:** Android BLE / GATT APIs
- **Storage:** Room
- **Asynchronous work:** Kotlin coroutines and Flow
- **Charts:** A maintained Compose-compatible chart library
- **Testing:** Unit tests for packet parsing; integration tests against the physical watch

Do not build a backend unless a later feature genuinely needs cloud sync or cross-device access.

---

## 6. Reverse-engineering plan

### Phase 0 — Preserve the evidence

1. Keep both original bug-report ZIPs unchanged.
2. Record which capture corresponds to which test and timestamp.
3. Keep bug reports private: they can contain device identifiers, installed-app data and system activity.
4. Avoid publishing the raw Bluetooth MAC address or raw report publicly.
5. Re-extract and validate the HCI capture from the newest report before making packet-level claims.

### Phase 1 — Discover GATT services

Use nRF Connect for Mobile on Android:

1. Connect to `Watch 7`.
2. Save screenshots/export details for all discovered services and characteristics.
3. Record UUIDs and available properties:
   - Read
   - Write
   - Write without response
   - Notify
   - Indicate
4. If possible, save a GATT database export or screen recording of discovery.
5. Do not write arbitrary values or trigger firmware update commands.

Standard services such as Device Information or Battery Service may be present, but do not assume they exist until observed.

### Phase 2 — Capture controlled sessions

Keep HCI snoop logging enabled and capture separate tests, ideally generating a bug report or retrieving the log after each:

- **Capture A:** Bluetooth connection only; do not open Lefun Health.
- **Capture B:** Open Lefun Health and connect/sync.
- **Capture C:** Open the battery/device-information page.
- **Capture D:** Trigger a heart-rate measurement, if available.
- **Capture E:** Trigger step/history sync.
- **Capture F:** Change one harmless setting, if supported.

Write down the exact local time for each action. A bug report may contain a capture from an earlier session, so verify timestamps and packet activity before assuming the relevant traffic is present.

HCI logs generally show transport-level BLE packets, not source code or friendly operation names. Some application payloads may need to be reconstructed from ATT/GATT traffic. Encryption, missing capture windows or vendor-specific behavior may limit visibility.

### Phase 3 — Analyze the traffic

For the relevant capture:

1. Extract the HCI snoop log from the ZIP.
2. Open it with Wireshark or another appropriate Bluetooth trace tool.
3. Filter for BLE ATT/GATT traffic and the watch's connection.
4. Identify:
   - Connection and service discovery
   - Characteristic reads
   - Characteristic writes
   - Notification/indication subscriptions
   - Notifications received after a particular action
5. Compare controlled captures to isolate changes.
6. Build a table of observed UUIDs, directions, byte sequences, timestamps and likely purpose.
7. Mark every interpretation as **observed**, **probable**, or **unknown**.

Do not infer the chipset from the Bluetooth name or one UUID alone.

### Phase 4 — Inspect Lefun Health implementation (optional)

The app package previously observed was `com.tjd.tjdmainS2`, with a class string resembling `com.tjd.lefun.sdk.device.work.DeviceService`.

Potential methods:
- Inspect app metadata and permissions.
- Determine whether the APK can be obtained legitimately from the user's device or official distribution.
- Inspect manifest, package resources and class names with standard Android APK inspection tools.
- Use static analysis to locate BLE service/characteristic UUIDs and packet parsers if present.
- If necessary, use runtime instrumentation only on the user's own device/app and only where permitted.

The app may use native libraries, obfuscation or dynamically loaded code. Static inspection is a lead, not a guarantee. Respect applicable terms and avoid redistributing proprietary app code.

### Phase 5 — Prototype one safe operation

Start with the simplest verified function, such as:
- Reading a standard Battery Service characteristic, if available; or
- Reading Device Information; or
- One confirmed proprietary read-only request.

Then implement:
1. BLE scan and connection.
2. GATT discovery.
3. One read/notification flow.
4. Parsing and UI display.
5. Disconnect/reconnect handling.
6. Tests and diagnostic logging.

Only implement writes once the command is understood and the operation is safe.

---

## 7. Security, privacy and safety

- Do not publish bug reports or BLE captures containing identifiers.
- Redact device addresses and unrelated personal information before sharing logs.
- Avoid sending arbitrary data to unknown characteristics.
- Do not flash firmware or erase device storage without confirmed chipset compatibility, an appropriate image, and a recovery path.
- Treat heart rate, sleep and activity data as sensitive personal information.
- If any protocol commands are discovered, document their evidence and confidence rather than treating guesses as facts.

---

## 8. Current progress and open questions

### Completed

- Identified the app name: Lefun Health.
- Identified Bluetooth device display name: `Watch 7`.
- Captured screenshot showing connected state, device name, battery indicator and basic activity metrics.
- User enabled Android Bluetooth HCI snoop logging.
- Two Android bug-report ZIPs were uploaded.
- The later report was described in the conversation as containing a fresh HCI capture with thousands of packets; exact packet count and contents still need direct revalidation.

### Open questions

1. What are the watch's actual GATT service and characteristic UUIDs?
2. Does it expose standard Battery Service or Device Information Service?
3. What data is sourced from the watch versus computed by Lefun Health or the phone?
4. Which proprietary commands handle sync, heart rate, history and settings?
5. Does the FT100 or JieLi research apply to this exact watch?
6. What chipset is physically installed?
7. Which features can the independent app support reliably?

### Next action

**Connect with nRF Connect and export the GATT service/characteristic list.** In parallel, validate and extract the newest HCI capture from the second bug report. Do not attempt firmware flashing at this stage.

---

## 9. Definition of the first milestone

The first milestone is achieved when the custom Android prototype can:

- Find and connect to the physical `Watch 7`.
- Discover its GATT services.
- Read one verified value or receive one verified notification.
- Display that result in the custom app.
- Disconnect and reconnect safely.

After that, we can prioritize dashboard metrics, sync history, watch settings and visual improvements based on verified protocol support.

**Core decision:** Use Lefun Health as a behavioral reference, use BLE captures to discover the protocol, and build an independent client. Do not assume Lefun is a reusable framework or that the watch can run a different OS.
