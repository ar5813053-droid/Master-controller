# DualPhone Controller – Milestone 1

**Android Foundation & UI**

One Android application that can be installed on two phones:

- **Controller Mode** – landscape virtual gamepad with full local input testing
- **Gaming Mode** – receiver dashboard (wireless transport arrives in later milestones)

## Features in Milestone 1

- Home dashboard with role and connection status
- Full-screen landscape virtual controller (joysticks, D-pad, A/B/X/Y, L1/L2/R1/R2, Start/Select/Home)
- Simultaneous multi-touch, deadzone, sensitivity, haptic feedback
- Functional local layout editor (drag, resize, opacity, save/load via Room)
- Settings persisted with DataStore
- Clean MVVM architecture prepared for Wi-Fi Hotspot, Wi-Fi Direct and Bluetooth RFCOMM
- Unit tests for input math, button state, layout serialization
- GitHub Actions workflow that builds and uploads a debug APK

## Requirements

- minSdk 28
- Kotlin + Jetpack Compose + Material 3
- Gradle Kotlin DSL

## Build (GitHub Actions)

1. Push this repository to GitHub
2. Actions → **Build Debug APK** runs on every push and can be triggered manually
3. Download the artifact named **DualPhoneController-Milestone1-APK**

No Android Studio or PC is required to obtain a debug APK.

## Project structure (packages)

```
com.dualphone.controller
├── core            # GamepadState, ControllerButton, layouts, settings models
├── input           # GamepadInputManager, joystick & button trackers
├── ui              # Compose screens & theme
├── preferences     # DataStore SettingsRepository
├── database        # Room layout persistence
├── transport       # InputTransport, WiFi, Bluetooth, PacketProtocol interfaces
├── receiver        # InputReceiver interface
├── hid             # GameInputBackend interface
└── ...
```

## Local development

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## License

Personal-use project. No accounts, ads or cloud services.
