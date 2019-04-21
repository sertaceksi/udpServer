import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;

public class Server extends Thread {
    private DatagramSocket socket;
    private boolean running;
    private byte[] receiverBuf = new byte[Constant.MESSAGE_SIZE];
    private int lastSequence = 0;
    private Map<Integer, Integer> nackCount;

    Server() throws SocketException {
        socket = new DatagramSocket(Constant.PORT);
        nackCount = new HashMap<Integer, Integer>();
    }

    public void run() {
        byte[] senderBuf;
        running = true;
        try {
            while (running) {
                DatagramPacket packet = new DatagramPacket(receiverBuf, receiverBuf.length);
                socket.receive(packet);
                ByteBuffer wrapped = ByteBuffer.wrap(packet.getData());
                Integer clientId = wrapped.getInt(Constant.CLIENT_INDEX);
                Integer sequenceId = wrapped.getInt(Constant.SEQUENCE_INDEX);
                Integer value = wrapped.getInt(Constant.VALUE_INDEX);
                ByteBuffer byteBuffer = null;
                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                if (lastSequence == 0 || lastSequence + 1 == sequenceId) {
                    if (isCheckSumTrue(packet.getData())) {
                        byteBuffer = generateServerResponsePacket(clientId, sequenceId, Constant.ACK_VALUE);
                        createValuesFile(clientId, sequenceId, value);
                    } else if (generateNackRecordOnServer(sequenceId)) {
                        byteBuffer = generateServerResponsePacket(clientId, sequenceId, Constant.NACK_VALUE);
                    }
                } else if (generateNackRecordOnServer(sequenceId)) {
                    byteBuffer = generateServerResponsePacket(clientId, lastSequence + 1, Constant.NACK_VALUE);
                }
                if (byteBuffer != null) {
                    senderBuf = byteBuffer.array();
                    packet = new DatagramPacket(senderBuf, senderBuf.length, address, port);
                    socket.send(packet);
                }

            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private boolean generateNackRecordOnServer(Integer sequenceId) {
        boolean generateNackRecord = false;
        if (!nackCount.containsKey(sequenceId)) {
            nackCount.put(sequenceId, 1);
            generateNackRecord = true;
        } else if (nackCount.get(sequenceId) < Constant.MAX_TRY) {
            int temp = nackCount.get(sequenceId);
            temp++;
            nackCount.put(sequenceId, temp);
            generateNackRecord = true;
        }
        return generateNackRecord;
    }

    private void createValuesFile(Integer clientId, Integer sequenceId, Integer value) throws IOException {
        String fileName = clientId.toString() + Constant.VALUES_FILE_EXTENSION;
        FileWriter fileWriter;
        if (lastSequence == 0) {
            fileWriter = new FileWriter(fileName);
        } else {
            fileWriter = new FileWriter(fileName, true);
        }
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.write(newLine(clientId, sequenceId, value));
        bufferedWriter.close();
    }

    private String newLine(Integer clientId, Integer sequenceId, Integer value) {
        return clientId + "," + sequenceId + "," + value + "\n";
    }

    private boolean isCheckSumTrue(byte[] dataArray) {
        ByteBuffer wrapped = ByteBuffer.wrap(dataArray);
        CRC32 checksum = new CRC32();
        byte[] clientIdBytes = ByteBuffer.allocate(Constant.CLIENT_SIZE).putInt(wrapped.getInt(Constant.CLIENT_INDEX)).array();
        byte[] sequenceIdBytes = ByteBuffer.allocate(Constant.SEQUENCE_SIZE).putInt(wrapped.getInt(Constant.SEQUENCE_INDEX)).array();
        byte[] valueBytes = ByteBuffer.allocate(Constant.VALUE_SIZE).putInt(wrapped.getInt(Constant.VALUE_INDEX)).array();
        checksum.update(clientIdBytes);
        checksum.update(sequenceIdBytes);
        checksum.update(valueBytes);
        return checksum.getValue() == wrapped.getLong(Constant.CHECKSUM_INDEX);
    }

    private ByteBuffer generateServerResponsePacket(Integer clientId, Integer sequenceId, byte ack) {
        byte[] clientIdBytes = ByteBuffer.allocate(Constant.CLIENT_SIZE).putInt(clientId).array();
        byte[] sequenceIdBytes = ByteBuffer.allocate(Constant.SEQUENCE_SIZE).putInt(sequenceId).array();
        ByteBuffer byteBuffer = ByteBuffer.allocate(Constant.NOTIFY_MESSAGE_SIZE);
        byteBuffer.put(clientIdBytes);
        byteBuffer.put(sequenceIdBytes);
        byte[] ackNumBytes;
        ackNumBytes = ByteBuffer.allocate(Constant.NOTIFICATION_SIZE).put(ack).array();
        byteBuffer.put(ackNumBytes);
        return byteBuffer;
    }
}