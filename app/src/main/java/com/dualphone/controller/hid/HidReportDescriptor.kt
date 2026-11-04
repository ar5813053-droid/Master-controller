package com.dualphone.controller.hid

/**
 * Standard Game Pad HID Report Descriptor (Bluetooth HID Device).
 *
 * Report ID 1, 8 data bytes (plus report id when framed by stack):
 *  Byte 0: buttons 1-8  (A B X Y L1 R1 Select Start)  bit0=A …
 *  Byte 1: buttons 9-12 (L2 R2 Home) + 4-bit hat (low nibble)
 *  Byte 2: Left X   (0–255, center 128)
 *  Byte 3: Left Y   (0–255, center 128, inverted Y for screen coords)
 *  Byte 4: Right X  (0–255, center 128)
 *  Byte 5: Right Y  (0–255, center 128)
 *  Byte 6: Left trigger  (0–255)
 *  Byte 7: Right trigger (0–255)
 *
 * Compatible with Android InputDevice SOURCE_GAMEPAD / SOURCE_JOYSTICK hosts.
 */
object HidReportDescriptor {

    const val REPORT_ID: Byte = 0x01
    const val REPORT_SIZE = 8

    /**
     * Binary HID descriptor bytes registered with [android.bluetooth.BluetoothHidDevice].
     */
    val DESCRIPTOR: ByteArray = byteArrayOf(
        0x05, 0x01,             // Usage Page (Generic Desktop)
        0x09, 0x05,             // Usage (Game Pad)
        0xA1.toByte(), 0x01,    // Collection (Application)
        0x85.toByte(), REPORT_ID, //   Report ID (1)

        // Buttons 1–12
        0x05, 0x09,             //   Usage Page (Button)
        0x19, 0x01,             //   Usage Minimum (Button 1)
        0x29, 0x0C,             //   Usage Maximum (Button 12)
        0x15, 0x00,             //   Logical Minimum (0)
        0x25, 0x01,             //   Logical Maximum (1)
        0x75, 0x01,             //   Report Size (1)
        0x95.toByte(), 0x0C,    //   Report Count (12)
        0x81.toByte(), 0x02,    //   Input (Data,Var,Abs)

        // 4 bit padding to complete byte 1 high nibble later used by hat in some layouts
        // Actually we pack hat in same byte — use 4 bits padding after 12 buttons = 16 bits = 2 bytes
        0x75, 0x01,             //   Report Size (1)
        0x95.toByte(), 0x04,    //   Report Count (4) padding
        0x81.toByte(), 0x03,    //   Input (Const,Var,Abs)

        // Hat switch (optional alternate — we encode hat as buttons only for simplicity;
        // keep 4 constant bits already above). For better D-pad, add hat collection:
        0x05, 0x01,             //   Usage Page (Generic Desktop)
        0x09, 0x39,             //   Usage (Hat switch)
        0x15, 0x00,             //   Logical Minimum (0)
        0x25, 0x07,             //   Logical Maximum (7)
        0x35, 0x00,             //   Physical Minimum (0)
        0x46, 0x3B, 0x01,       //   Physical Maximum (315)
        0x65, 0x14,             //   Unit (Eng Rot:Degrees)
        0x75, 0x04,             //   Report Size (4)
        0x95.toByte(), 0x01,    //   Report Count (1)
        0x81.toByte(), 0x42,    //   Input (Data,Var,Abs,Null State)
        0x65, 0x00,             //   Unit (None)
        0x75, 0x04,             //   Report Size (4)
        0x95.toByte(), 0x01,    //   Report Count (1)
        0x81.toByte(), 0x03,    //   Input (Const)

        // X, Y, Z, Rz (left X/Y, right X/Y) — 8-bit axes
        0x05, 0x01,             //   Usage Page (Generic Desktop)
        0x09, 0x30,             //   Usage (X)
        0x09, 0x31,             //   Usage (Y)
        0x09, 0x32,             //   Usage (Z)
        0x09, 0x35,             //   Usage (Rz)
        0x15, 0x00,             //   Logical Minimum (0)
        0x26, 0xFF.toByte(), 0x00, // Logical Maximum (255)
        0x75, 0x08,             //   Report Size (8)
        0x95.toByte(), 0x04,    //   Report Count (4)
        0x81.toByte(), 0x02,    //   Input (Data,Var,Abs)

        // Rx, Ry as triggers (0–255)
        0x09, 0x33,             //   Usage (Rx)
        0x09, 0x34,             //   Usage (Ry)
        0x15, 0x00,
        0x26, 0xFF.toByte(), 0x00,
        0x75, 0x08,
        0x95.toByte(), 0x02,
        0x81.toByte(), 0x02,    //   Input (Data,Var,Abs)

        0xC0.toByte()           // End Collection
    )

    /** Report payload length excluding Report ID (stack may prepend ID). */
    const val PAYLOAD_LENGTH = 11 // 2 button bytes + 1 hat nibble packing handled in encoder as 11
    // Encoder builds: buttons(2) + hat byte packing + 4 axes + 2 triggers = we use fixed 8-byte compact
    // See HidReportEncoder for the actual wire format matched to a simpler descriptor.

    /**
     * Compact 8-byte descriptor that matches [HidReportEncoder] output.
     * Preferred for Android host compatibility.
     */
    val COMPACT_DESCRIPTOR: ByteArray = byteArrayOf(
        0x05, 0x01, 0x09, 0x05, 0xA1.toByte(), 0x01,
        0x85.toByte(), REPORT_ID,
        // 16 buttons
        0x05, 0x09, 0x19, 0x01, 0x29, 0x10,
        0x15, 0x00, 0x25, 0x01, 0x75, 0x01, 0x95.toByte(), 0x10,
        0x81.toByte(), 0x02,
        // X Y Z Rz
        0x05, 0x01,
        0x09, 0x30, 0x09, 0x31, 0x09, 0x32, 0x09, 0x35,
        0x15, 0x00, 0x26, 0xFF.toByte(), 0x00,
        0x75, 0x08, 0x95.toByte(), 0x04,
        0x81.toByte(), 0x02,
        // Rx Ry triggers
        0x09, 0x33, 0x09, 0x34,
        0x15, 0x00, 0x26, 0xFF.toByte(), 0x00,
        0x75, 0x08, 0x95.toByte(), 0x02,
        0x81.toByte(), 0x02,
        0xC0.toByte()
    )
}
