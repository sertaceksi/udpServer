import java.io.*;
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
    private byte[] receiverBuf = new byte[Constant.MESSAGE_SIZE];
    private int lastSequence = 0;
    private Map<Integer, Integer> nackCount;
    private int clientId;
    private int sequenceId;
    private int value;

    Server() throws SocketException {
        socket = new DatagramSocket(Constant.PORT);
        nackCount = new HashMap<Integer, Integer>();
    }

    public void run() {
        byte[] senderBuf;
        try {
            while (true) {
                DatagramPacket packet = new DatagramPacket(receiverBuf, receiverBuf.length);
                socket.receive(packet);
                ByteBuffer wrapped = ByteBuffer.wrap(packet.getData());
                clientId = wrapped.getInt(Constant.CLIENT_INDEX);
                sequenceId = wrapped.getInt(Constant.SEQUENCE_INDEX);
                value = wrapped.getInt(Constant.VALUE_INDEX);
                ByteBuffer byteBuffer;
                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                if (lastSequence == 0 || lastSequence + 1 == sequenceId) {
                    if (isCheckSumTrue(packet.getData())) {
                        byteBuffer = generateResponseBuffer(Constant.ACK_VALUE);
                        createValuesFile();
                        updateOrCreateSumFile();
                    } else {
                        generateNackCount();
                        byteBuffer = generateResponseBuffer(Constant.NACK_VALUE);
                    }
                } else {
                    generateNackCount();
                    byteBuffer = generateResponseBuffer(Constant.NACK_VALUE);
                }
                if (byteBuffer != null) {
                    senderBuf = byteBuffer.array();
                    packet = new DatagramPacket(senderBuf, senderBuf.length, address, port);
                    socket.send(packet);
                }
            }
        } catch (IOException e) {
            socket.close();
            e.printStackTrace();
        }
    }


    private void generateNackCount() throws IOException {
        if (!nackCount.containsKey(sequenceId)) {
            nackCount.put(sequenceId, 1);
        } else if (nackCount.get(sequenceId) < Constant.MAX_TRY) {
            int temp = nackCount.get(sequenceId);
            temp++;
            nackCount.put(sequenceId, temp);
        }
        if (nackCount.get(sequenceId).equals(Constant.MAX_TRY)) {
            nackCount.put(sequenceId, 0);
            generateMissedFile();
        }
    }

    private void generateMissedFile() throws IOException {
        String fileName = clientId + Constant.MISSED_FILE_EXTENSION;
        FileWriter fileWriter;
        fileWriter = new FileWriter(fileName, true);
        lastSequence++;
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.write(newLine(sequenceId));
        bufferedWriter.close();
    }

    private void createValuesFile() throws IOException {
        String fileName = clientId + Constant.VALUES_FILE_EXTENSION;
        FileWriter fileWriter;
        if (lastSequence == 0) {
            fileWriter = new FileWriter(fileName);
            lastSequence = sequenceId;
        } else {
            fileWriter = new FileWriter(fileName, true);
            lastSequence++;
        }
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.write(newLine(clientId, sequenceId, value));
        bufferedWriter.close();
    }

    private void updateOrCreateSumFile() {
        try {
            String fileName = clientId + Constant.SUM_FILE_EXTENSION;
            String upToDateLine = getUpToDateLine(fileName);
            FileWriter fileWriter;
            fileWriter = new FileWriter(fileName, false);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(upToDateLine);
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private String getUpToDateLine(String fileName) {
        String firstLine = readSumFile(fileName);
        String[] arr = firstLine.split(",");
        int prevSumOfValue = Integer.parseInt(arr[0]);
        int prevNoOfValues = Integer.parseInt(arr[1]);
        int upToDateSumOfValue = (prevSumOfValue + value) / (++prevNoOfValues);
        return upToDateSumOfValue + "," + prevNoOfValues;
    }

    private String readSumFile(String fileName) {
        String strLine = "0,0";
        try {
            BufferedReader b = new BufferedReader(new FileReader(fileName));
            while (b.readLine() != null) {
                strLine = b.readLine();
            }
        } catch (FileNotFoundException e) {
            System.err.println("File not found");
        } catch (IOException e) {
            System.err.println("Unable to read the file.");
        }
        return strLine;
    }

    private String newLine(int sequenceId) {
        return "\n" + sequenceId;
    }

    private String newLine(int clientId, int sequenceId, int value) {
        return "\n" + clientId + "," + sequenceId + "," + value;
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

    private ByteBuffer generateResponseBuffer(byte ack) {
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