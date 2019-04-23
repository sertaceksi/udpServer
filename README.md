# UDP Server
This is a Server application which listens udp port 4445. 
Server accepts a binary payload which contains 20 bytes.Once server receives a binary payload, sends a response acknowledge message.

Acceptable binary payload:
<4 bytes, integer client identifier><4 bytes, integer sequence number><4 bytes, integer value><8 bytes, CRC32 checksum of the first 12 bytes of the payload>

If an acceptable data arrived on udp port server,it creates values and sum files for this data.
Server accepts data in order of sequence numbers. In case of a data with a larger sequence number is arrived, server asks for data with expected sequence number. 


There is 3 types of file in Server:
1.Missed Files: Named as "[client id].missed.txt". Once server receives a binary payload with larger sequence number than expected, 
    server asks 3 times to the client for data which contains expected sequence number.After 3 times if the message is not received by sever, it creates missed file.
2.Sum Files: Named as "[client id].sum.txt". Once the server accepts a binary payload, it creates or updates sum file for this specific client. 
3.Values Files: Named as "[client id].values.txt". Once server receives a binary payload server creates or append values file for the client.

## Prerequisites
JDK or JRE  1.8 or Higher

## Built With
* [Maven](https://maven.apache.org/) - Dependency Management

## References
* https://www.baeldung.com/udp-in-java
* https://www.pegaxchange.com/2018/01/23/simple-udp-server-and-client-socket-java/
* https://examples.javacodegeeks.com/core-java/util/zip/calculate-crc32-checksum-for-byte-array/

## Author
Evren Sertac Eksi

