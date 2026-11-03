# DualPhone Controller

**Milestone 2 – Wi-Fi Connectivity & Low-Latency Input Transport**

One Android application installed on two phones:

- **Controller Mode** – landscape virtual gamepad that streams input over Wi-Fi
- **Gaming Mode** – receiver that authenticates a session and accepts UDP input packets

No internet or cloud server is required.

## Milestone 2 features

- TCP session establishment + pairing token authentication
- UDP live gamepad state at up to ~120 Hz
- Versioned binary packet protocol (not JSON)
- Session-bound packets; foreign sessions rejected
- RELEASE_ALL on disconnect (no stuck buttons)
- Wi-Fi LAN / hotspot workflow
- Wi-Fi Direct discovery & group formation helpers
- Pairing QR code + manual IP:PORT:TOKEN entry
- Live diagnostics (packets, loss estimate, update rate)
- All Milestone 1 UI, layout editor, settings, and tests preserved

## Quick two-phone test (hotspot / same Wi-Fi)

1. **Gaming phone**
   - Open **Gaming Mode** → **Start Receiver**
   - Note the pairing string (or scan the QR): `IP:TCP_PORT:TOKEN`
   - Optionally enable a Wi-Fi hotspot and connect the controller phone to it

2. **Controller phone**
   - Open **Controller Mode** → **Connect**
   - Enter the pairing string → **Connect**
   - Operate sticks/buttons; Gaming phone diagnostics should show packets and live input

3. **Disconnect**
   - Use **Disconnect** on either side
   - Receiver applies RELEASE_ALL and is ready for a new session

### Wi-Fi Direct

- On Gaming Mode, if hardware supports it, use **Discover peers**
- After the P2P group forms, use the group-owner IP with the same TCP/UDP ports and pairing token
- If Wi-Fi Direct is unavailable, hotspot/LAN still works

### Existing Wi-Fi network

- Both phones on the same LAN
- Gaming phone shows its local IP in the pairing string
- Controller connects to that IP (no fixed gateway assumption)

## Protocol (summary)

| Field | Size | Notes |
|-------|------|--------|
| version | 1 | currently `1` |
| type | 1 | INPUT_STATE, RELEASE_ALL, HEARTBEAT, AUTH_* |
| sessionId | 4 | bound after TCP auth |
| sequence | 4 | monotonic |
| timestampMs | 8 | wall clock |
| sticks / triggers | f32 x 6 | normalized |
| buttons | u32 bitmask | |

Total INPUT_STATE frame: **46 bytes**.

## Architecture

```
transport/
  protocol/   BinaryPacketProtocol, SessionAuth
  wifi/       TcpUdpInputTransport, WiFiDirectManager, NetworkUtils
  ConnectionDiagnostics
```

Interfaces from Milestone 1 remain the boundary for future Bluetooth RFCOMM.

## Build

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

GitHub Actions uploads artifact **DualPhoneController-Milestone2-APK**.

## Tests

- Binary encode/decode, invalid rejection, NaN rejection
- Session auth accept/reject
- Sequence / loss estimate logic
- In-process TCP+UDP loopback
- Existing M1 joystick, button, layout tests

## Not in this milestone

- Bluetooth RFCOMM
- Injecting input into third-party games (HID / accessibility backend)
- Claimed one-way latency numbers without device measurement

## Version

`1.0.0-milestone2` (versionCode 2)
