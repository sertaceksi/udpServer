import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.zip.CRC32;


class ServerTest {
    private int clientId;
    private int sequenceId;
    private int value;

    @Test
    void responseTest() throws IOException {
        clientId = generateRandomIntegerInRange();
        sequenceId = generateRandomIntegerInRange();
        value = generateRandomIntegerInRange();
        DatagramSocket socket = new DatagramSocket();
        sendPacket(socket);
        ackNackTest(socket, (byte) 1);
        sequenceId = sequenceId + 2;
        for (int k = 0; k < 3; k++) {
            sendPacket(socket);
            ackNackTest(socket, (byte) 0);
        }
        String fileName = clientId + Constant.MISSED_FILE_EXTENSION;
        File f = new File(fileName);
        Assert.assertTrue(f.exists());

        BufferedReader b = new BufferedReader(new FileReader(fileName));
        String strLine;

        while ((strLine = b.readLine()) != null) {
        }
        Assert.assertEquals(Integer.parseInt(strLine), sequenceId);
    }

    private void ackNackTest(DatagramSocket socket, byte i) throws IOException {
        ByteBuffer ackWrapped = receivePacket(socket);
        Assert.assertEquals(ackWrapped.getInt(Constant.CLIENT_INDEX), clientId);
        Assert.assertEquals(ackWrapped.getInt(Constant.SEQUENCE_INDEX), sequenceId);
        Assert.assertEquals(ackWrapped.get(Constant.NOTIFICATION_INDEX), i);
    }

    private ByteBuffer receivePacket(DatagramSocket socket) throws IOException {
        byte[] receiverBuf = new byte[Constant.NOTIFY_MESSAGE_SIZE];
        DatagramPacket responsePacket = new DatagramPacket(receiverBuf, receiverBuf.length);
        socket.receive(responsePacket);
        return ByteBuffer.wrap(responsePacket.getData());
    }

    private void sendPacket(DatagramSocket socket) throws IOException {
        InetAddress address = InetAddress.getByName(Constant.HOSTNAME);
        ByteBuffer byteBuffer = generateClientPacket(clientId, sequenceId, value);
        byte[] senderBuf = byteBuffer.array();
        DatagramPacket packet = new DatagramPacket(senderBuf, senderBuf.length, address, Constant.PORT);
        socket.send(packet);
    }

    private ByteBuffer generateClientPacket(int clientId, int sequenceId, int value) {
        byte[] clientIdBytes = ByteBuffer.allocate(Constant.CLIENT_SIZE).putInt(clientId).array();
        byte[] sequenceIdBytes = ByteBuffer.allocate(Constant.SEQUENCE_SIZE).putInt(sequenceId).array();
        byte[] valueBytes = ByteBuffer.allocate(Constant.VALUE_SIZE).putInt(value).array();
        byte[] checkSumBytes = calculateChecksum(clientIdBytes, sequenceIdBytes, valueBytes);

        ByteBuffer byteBuffer = ByteBuffer.allocate(Constant.MESSAGE_SIZE);
        byteBuffer.put(clientIdBytes);
        byteBuffer.put(sequenceIdBytes);
        byteBuffer.put(valueBytes);
        byteBuffer.put(checkSumBytes);

        return byteBuffer;
    }

    private byte[] calculateChecksum(byte[] clientIdBytes, byte[] sequenceIdBytes, byte[] valueBytes) {
        CRC32 checksum = new CRC32();
        checksum.update(clientIdBytes);
        checksum.update(sequenceIdBytes);
        checksum.update(valueBytes);
        return ByteBuffer.allocate(Constant.CHECKSUM_SIZE).putLong(checksum.getValue()).array();
    }

    private static int generateRandomIntegerInRange() {
        if (Constant.RAND_MIN_LIMIT >= Constant.RAND_MAX_LIMIT) {
            throw new IllegalArgumentException("max must be greater than min");
        }
        Random r = new Random();
        return r.nextInt((Constant.RAND_MAX_LIMIT - Constant.RAND_MIN_LIMIT) + 1) + Constant.RAND_MIN_LIMIT;
    }
}
