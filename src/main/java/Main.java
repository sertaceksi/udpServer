import java.net.SocketException;

public class Main {
    public static void main(String[] args) throws SocketException {
        Server server = new Server();
        System.out.println("Server is up and running.");
        server.start();
    }
}
