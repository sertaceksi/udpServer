import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

public class Server extends Thread {
    private DatagramSocket socket;
    private boolean running;
    private byte[] receiverBuf = new byte[20];
    private int lastSequence = 0;

    public Server() throws SocketException {
        socket = new DatagramSocket(4445);
    }

    public void run() {
        byte[] senderBuf;
        running = true;
        try {
            while (running) {
                DatagramPacket packet = new DatagramPacket(receiverBuf, receiverBuf.length);
                socket.receive(packet);
                ByteBuffer wrapped = ByteBuffer.wrap(packet.getData());
                Integer clientId = wrapped.getInt(0);
                Integer sequenceId = wrapped.getInt(4);
                Integer value = wrapped.getInt(8);
                ByteBuffer byteBuffer;
                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                if (lastSequence == 0 || lastSequence + 1 == sequenceId) {
                    if (isCheckSumTrue(packet.getData())) {
                        byteBuffer = generateServerResponsePacket(packet, clientId, sequenceId, (byte) 1);
                        createValuesFile(clientId, sequenceId, value);
                    } else {
                        byteBuffer = generateServerResponsePacket(packet, clientId, sequenceId, (byte) 0);
                    }
                } else {
                    byteBuffer = generateServerResponsePacket(packet, clientId, lastSequence+1, (byte) 0);
                }
                senderBuf = byteBuffer.array();
                packet = new DatagramPacket(senderBuf, senderBuf.length, address, port);
                socket.send(packet);

            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void createValuesFile(Integer clientId, Integer sequenceId, Integer value) throws IOException {
        String fileName = clientId.toString() + ".values.txt";
        FileWriter fileWriter = new FileWriter(fileName,true);
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
        byte[] clientIdBytes = ByteBuffer.allocate(4).putInt(wrapped.getInt(0)).array();
        byte[] sequenceIdBytes = ByteBuffer.allocate(4).putInt(wrapped.getInt(4)).array();
        byte[] valueBytes = ByteBuffer.allocate(4).putInt(wrapped.getInt(8)).array();
        checksum.update(clientIdBytes);
        checksum.update(sequenceIdBytes);
        checksum.update(valueBytes);
        return checksum.getValue() == wrapped.getLong(12);
    }

    private ByteBuffer generateServerResponsePacket(DatagramPacket packet, Integer clientId, Integer sequenceId, byte ack) {
        byte[] clientIdBytes = ByteBuffer.allocate(4).putInt(clientId).array();
        byte[] sequenceIdBytes = ByteBuffer.allocate(4).putInt(sequenceId).array();

        ByteBuffer byteBuffer = ByteBuffer.allocate(9);
        byteBuffer.put(clientIdBytes);
        byteBuffer.put(sequenceIdBytes);
        byte[] ackNumBytes;
        if (isCheckSumTrue(packet.getData())) {
            ackNumBytes = ByteBuffer.allocate(1).put((byte) 1).array();
            byteBuffer.put(ackNumBytes);
        } else {
            ackNumBytes = ByteBuffer.allocate(1).put((byte) 0).array();
            byteBuffer.put(ackNumBytes);
        }
        return byteBuffer;
    }
}