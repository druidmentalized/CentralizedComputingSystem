import java.io.*;
import java.net.*;
import java.util.*;

public class CCSClient {
    private static final String DISCOVER_MESSAGE = "CCS DISCOVER";
    private static final String DISCOVER_RESPONSE = "CCS FOUND";
    private static final int BUFFER_SIZE = 1024;
    private static int port;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java CCSClient <port>");
            System.exit(1);
        }

        try {
            port = Integer.parseInt(args[0]);
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Port must be between 1 and 65535.");
            }
        } catch (NumberFormatException e) {
            System.err.println("Error: Port must be a valid integer.");
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }

        try {
            // Discover server
            InetAddress serverAddress = discoverServer(port);
            if (serverAddress == null) {
                System.err.println("Server not found.");
                System.exit(1);
            }

            System.out.println("Server discovered at: " + serverAddress.getHostAddress() + ":" + port);

            // Connect to server via TCP
            try (Socket tcpSocket = new Socket(serverAddress, port);
                 BufferedReader in = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
                 BufferedWriter out = new BufferedWriter(new OutputStreamWriter(tcpSocket.getOutputStream()))) {

                System.out.println("Connected to the server.");

                // Continuously send requests to the server
                sendManyRequests(out, in);
            }
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    private static InetAddress discoverServer(int port) {
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            udpSocket.setBroadcast(true);
            broadcastValue(udpSocket, DISCOVER_MESSAGE);

            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);

            udpSocket.setSoTimeout(5000); // 5-second timeout
            try {
                udpSocket.receive(responsePacket);
                String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
                if (response.equals(DISCOVER_RESPONSE)) {
                    return responsePacket.getAddress(); // Return the server's IP address
                }
            } catch (SocketTimeoutException e) {
                System.err.println("Discovery timed out.");
            }
        } catch (IOException e) {
            System.err.println("Error during service discovery: " + e.getMessage());
        }
        return null;
    }

    private static List<InetAddress> getBroadcastAddress() {
        List<InetAddress> addresses = new ArrayList<>();
        try {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (networkInterface.isUp()) {
                    for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                        InetAddress broadcast = interfaceAddress.getBroadcast();
                        if (broadcast != null) {
                            addresses.add(broadcast);
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return addresses;
    }

    private static <T> void broadcastValue(DatagramSocket socket, T value) {
        byte[] buff = String.valueOf(value).getBytes();

        for (InetAddress addr : getBroadcastAddress()) {
            DatagramPacket packet = new DatagramPacket(buff, buff.length, addr, port);
            try {
                socket.send(packet);
                System.out.println("Sent discovery message to: " + addr.getHostAddress());
            } catch (IOException e) {
                System.err.println("Error broadcasting to: " + addr.getHostAddress() + " - " + e.getMessage());
            }
        }
    }

    private static void sendManyRequests(BufferedWriter out, BufferedReader in) throws IOException {
        Random random = new Random();
        String[] operations = {"ADD", "SUB", "MUL", "DIV"};

        int requestCount = 0; // Track how many requests were sent
        while (true) { // Infinite loop to send requests continuously
            String operation = operations[random.nextInt(operations.length)];
            int arg1 = random.nextInt(100); // Random number between 0-99
            int arg2 = random.nextInt(100); // Random number between 0-99

            if (operation.equals("DIV") && arg2 == 0) {
                arg2 = 0; // Avoid division by zero
            }

            String request = operation + " " + 10 + " " + 0;
            System.out.println("Sending request: " + request);

            out.write(request + "\n");
            out.flush();

            String response = in.readLine();
            System.out.println("Server response: " + response);

            requestCount++;

            // Optionally, stop after sending a specific number of requests
            if (requestCount >= 50) { // Example: stop after 50 requests
                break;
            }

            try {
                Thread.sleep(500); // Sleep for 500ms between requests (optional)
            } catch (InterruptedException e) {
                System.err.println("Interrupted while waiting between requests.");
                break;
            }
        }
    }
}

