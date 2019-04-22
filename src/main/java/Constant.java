class Constant {
    final static Integer PORT = 4445;
    final static Integer CLIENT_INDEX = 0;
    final static Integer SEQUENCE_INDEX = 4;
    final static Integer VALUE_INDEX = 8;
    final static Integer CHECKSUM_INDEX = 12;
    final static Integer CLIENT_SIZE = 4;
    final static Integer SEQUENCE_SIZE = 4;
    final static Integer VALUE_SIZE = 4;
    final static Integer NOTIFICATION_SIZE = 1;
    final static Integer NOTIFY_MESSAGE_SIZE = 9;
    final static Integer MESSAGE_SIZE = 20;
    final static byte ACK_VALUE = 1;
    final static byte NACK_VALUE = 1;
    final static Integer MAX_TRY = 3;
    final static String VALUES_FILE_EXTENSION = ".values.txt";
    final static String MISSED_FILE_EXTENSION = ".missed.txt";
    final static String SUM_FILE_EXTENSION = ".sum.txt";
}
