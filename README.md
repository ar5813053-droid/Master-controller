# DualPhone Controller

**Full release – Wi-Fi + Wi-Fi Direct + Bluetooth RFCOMM**

Personal-use wireless gamepad: install the same APK on two Android phones.

| Phone | Mode |
|-------|------|
| A | **Controller Mode** – landscape virtual gamepad, streams input |
| B | **Gaming Mode** – authenticates session, receives binary input |

No internet, accounts, ads, or cloud.

## Connection methods

### 1. Wi-Fi Hotspot / Existing LAN
1. Gaming phone → **Gaming Mode** → **Wi-Fi** (Start).
2. Note pairing string / QR: `IP:TCP_PORT:TOKEN`.
3. Controller on same network (or connected to gaming phone’s hotspot).
4. Controller Mode → **Connect** → enter pairing string.

Local IP is discovered at runtime (no fixed gateway assumption).

### 2. Wi-Fi Direct
- Gaming Mode → Discover peers when hardware supports it.
- After group formation, use group-owner IP with the same TCP/UDP ports and token.
- If P2P is unavailable, use hotspot/LAN instead.

### 3. Bluetooth RFCOMM (app-to-app)
- Service UUID: `a1b2c3d4-e5f6-7890-abcd-ef1234567890`
- Gaming: **Bluetooth** start → note BT pairing token.
- Bond devices in system Bluetooth settings first.
- Controller: enter `BT:AA:BB:CC:DD:EE:FF:TOKEN` (or use bonded list + token).
- Length-prefixed binary frames on the RFCOMM stream (same packet layout as Wi-Fi).

**Not Bluetooth HID.** Both phones must run DualPhone Controller.

## Input protocol

46-byte little-endian binary frames (not JSON):

version · type · sessionId · sequence · timestamp · Lx Ly Rx Ry · bitmask · LT RT

- Target ~120 Hz over Wi-Fi UDP; ~60 Hz over Bluetooth RFCOMM
- Latest-state joysticks (no stale queue buildup)
- Session-bound packets; foreign sessions rejected
- `RELEASE_ALL` on disconnect (no stuck buttons)

## Game input backend – honest limitation

Ordinary Android apps **cannot** inject synthetic gamepad HID events into other games without system privileges, a custom ROM, or a carefully designed AccessibilityService.

This release ships:

- **LiveInputTesterBackend** – receiver UI shows live sticks/buttons/triggers
- Clean `GameInputBackend` interface for a future supported injection path

It does **not** claim universal control of third-party games.

## Diagnostics

Packets sent/received, loss estimate, update rate, connection type, session id.  
RTT is shown only when measured; otherwise “Not measured (one-way only)”.

## Build

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

GitHub Actions artifact: **DualPhoneController-FullRelease-APK**

## Automated tests

- Binary protocol encode/decode, NaN/version rejection
- Session auth accept/reject
- Sequence / loss estimate
- TCP+UDP loopback
- Bluetooth length-prefix framing
- Joystick deadzone, multi-button state, layout serialization
- Transport exclusive switching logic

## Manual two-phone checklist

Devices (example): POCO X3 Pro (controller), Redmi A2 (receiver).

- [ ] Wi-Fi same LAN: connect, dual sticks + buttons, disconnect → RELEASE_ALL
- [ ] Gaming phone hotspot: controller joins hotspot, pairs via QR/string
- [ ] Wi-Fi Direct: discover, group, TCP/UDP (if hardware allows)
- [ ] Bluetooth: bond in system settings, BT receiver + `BT:MAC:TOKEN`
- [ ] Switch transports (stop previous before starting another)
- [ ] Landscape controller + layout editor save/load
- [ ] App background/foreground while connected
- [ ] Reconnect with new token after stop

Physical radio tests require real devices; unit/loopback tests run in CI.

## Version

`1.0.0-full` (versionCode 3)

## Architecture packages

`core` · `input` · `transport` (wifi, bluetooth, protocol) · `receiver` · `hid` · `database` · `preferences` · `ui.*`
