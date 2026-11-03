package com.dualphone.controller

import com.dualphone.controller.core.ControllerButton
import com.dualphone.controller.core.GamepadState
import com.dualphone.controller.transport.protocol.BinaryPacketProtocol
import com.dualphone.controller.transport.protocol.SessionAuth
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * In-process TCP auth + UDP packet loopback (no Android framework required).
 */
class NetworkingLoopbackTest {

    @Test
    fun `tcp auth and udp input round trip`() {
        val pairingToken = SessionAuth.generatePairingToken()
        val sessionIdHolder = AtomicReference(0)
        val latch = CountDownLatch(1)
        val received = AtomicReference<GamepadState?>(null)

        // Receiver thread
        val server = ServerSocket(0)
        val tcpPort = server.localPort
        val udpSocket = DatagramSocket(0)
        val udpPort = udpSocket.localPort

        Thread {
            try {
                val client = server.accept()
                val input = client.getInputStream()
                val output = client.getOutputStream()
                val authBuf = ByteArray(SessionAuth.AUTH_FRAME_SIZE)
                var offset = 0
                while (offset < authBuf.size) {
                    val n = input.read(authBuf, offset, authBuf.size - offset)
                    if (n < 0) break
                    offset += n
                }
                val req = SessionAuth.decodeAuthRequest(authBuf)
                assertThat(req).isNotNull()
                assertThat(req!!.pairingToken).isEqualTo(pairingToken)
                val sessionId = SessionAuth.generateSessionId()
                sessionIdHolder.set(sessionId)
                output.write(SessionAuth.encodeAuthResponse(sessionId, true, udpPort))
                output.flush()

                // Receive one UDP packet
                val buf = ByteArray(64)
                val dp = DatagramPacket(buf, buf.size)
                udpSocket.soTimeout = 3000
                udpSocket.receive(dp)
                val decoded = BinaryPacketProtocol.decodePacket(dp.data.copyOf(dp.length))
                assertThat(decoded).isNotNull()
                assertThat(decoded!!.sessionId).isEqualTo(sessionId)
                received.set(decoded.state)
                latch.countDown()
                client.close()
            } catch (e: Exception) {
                e.printStackTrace()
                latch.countDown()
            } finally {
                try { server.close() } catch (_: Exception) {}
                try { udpSocket.close() } catch (_: Exception) {}
            }
        }.start()

        // Controller side
        Thread.sleep(50)
        val socket = Socket("127.0.0.1", tcpPort)
        socket.getOutputStream().write(SessionAuth.encodeAuthRequest(pairingToken, "LoopbackPhone"))
        socket.getOutputStream().flush()
        val respBuf = ByteArray(SessionAuth.AUTH_FRAME_SIZE)
        var off = 0
        while (off < respBuf.size) {
            val n = socket.getInputStream().read(respBuf, off, respBuf.size - off)
            if (n < 0) break
            off += n
        }
        val resp = SessionAuth.decodeAuthResponse(respBuf)
        assertThat(resp).isNotNull()
        assertThat(resp!!.accepted).isTrue()

        val state = GamepadState(
            leftStickX = 0.33f,
            leftStickY = -0.66f,
            buttonsBitmask = ControllerButton.B.bit,
            leftTrigger = 1f
        )
        val packet = BinaryPacketProtocol.encodeInputState(resp.sessionId, 1, state)
        val sendSocket = DatagramSocket()
        sendSocket.send(
            DatagramPacket(packet, packet.size, InetAddress.getByName("127.0.0.1"), resp.udpPort)
        )
        sendSocket.close()
        socket.close()

        assertThat(latch.await(4, TimeUnit.SECONDS)).isTrue()
        val got = received.get()
        assertThat(got).isNotNull()
        assertThat(got!!.leftStickX).isWithin(0.01f).of(0.33f)
        assertThat(got.leftStickY).isWithin(0.01f).of(-0.66f)
        assertThat(got.isButtonPressed(ControllerButton.B)).isTrue()
        assertThat(got.leftTrigger).isWithin(0.01f).of(1f)
    }

    @Test
    fun `wrong pairing token is rejected`() {
        val server = ServerSocket(0)
        val port = server.localPort
        val latch = CountDownLatch(1)
        val accepted = AtomicReference<Boolean?>(null)

        Thread {
            try {
                val client = server.accept()
                val buf = ByteArray(SessionAuth.AUTH_FRAME_SIZE)
                var o = 0
                while (o < buf.size) {
                    val n = client.getInputStream().read(buf, o, buf.size - o)
                    if (n < 0) break
                    o += n
                }
                val req = SessionAuth.decodeAuthRequest(buf)
                val ok = req != null && req.pairingToken == 12345
                client.getOutputStream().write(SessionAuth.encodeAuthResponse(0, ok, 0))
                client.getOutputStream().flush()
                accepted.set(ok)
                client.close()
            } finally {
                server.close()
                latch.countDown()
            }
        }.start()

        Thread.sleep(30)
        val socket = Socket("127.0.0.1", port)
        socket.getOutputStream().write(SessionAuth.encodeAuthRequest(99999, "Bad"))
        socket.getOutputStream().flush()
        val respBuf = ByteArray(SessionAuth.AUTH_FRAME_SIZE)
        var o = 0
        while (o < respBuf.size) {
            val n = socket.getInputStream().read(respBuf, o, respBuf.size - o)
            if (n < 0) break
            o += n
        }
        val resp = SessionAuth.decodeAuthResponse(respBuf)
        socket.close()
        latch.await(3, TimeUnit.SECONDS)
        assertThat(resp).isNotNull()
        assertThat(resp!!.accepted).isFalse()
        assertThat(accepted.get()).isFalse()
    }
}
