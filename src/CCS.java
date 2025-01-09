import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CCS {

    private final ExecutorService clientHandlerPool;

    public static void main(String[] args) {
        //todo make checks for incorrect input of arguments

        new CCS(5058);
    }

    public CCS(int port) {
        //creating variables
        clientHandlerPool = Executors.newCachedThreadPool();

        //starting methods
        new Thread(this::startStatisticsReporter).start();
        new Thread(() -> createUDPSocket(port)).start();
        createTCPSocket(port);
    }

    public void createUDPSocket(int port) {
        try {
            DatagramSocket udpSocket = new DatagramSocket(5058);
            byte[] buffer = new byte[12];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());

                if (message.equals("CCS DISCOVER")) {
                    byte[] responseMessage = "CCS FOUND".getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(responseMessage, responseMessage.length, packet.getAddress(), packet.getPort());
                    udpSocket.send(responsePacket);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createTCPSocket(int port) {
        try {
            ServerSocket tcpSocket = new ServerSocket(5058);
            while (true) {
                Socket clientSocket = tcpSocket.accept();
                clientHandlerPool.execute(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleClient(Socket clientSocket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

            String request;
            while ((request = reader.readLine()) != null) {
                String[] tokens = request.split(" ");

                if (tokens.length != 3) {
                    writer.println("ERROR");
                    continue;
                }

                //parsing information from the message
                String operation = tokens[0];
                int arg1;
                int arg2;
                try {
                    arg1 = Integer.parseInt(tokens[1]);
                    arg2 = Integer.parseInt(tokens[2]);
                }
                catch (NumberFormatException e) {
                    writer.println("ERROR");
                    continue;
                }

                //calculating information
                int result;
                switch (operation) {
                    case "ADD": {
                        result = arg1 + arg2;
                        break;
                    }
                    case "SUB": {
                        result = arg1 - arg2;
                        break;
                    }
                    case "MUL": {
                        result = arg1 * arg2;
                        break;
                    }
                    case "DIV": {
                        if (arg2 == 0) {
                            writer.println("ERROR");
                            continue;
                        }
                        result = arg1 / arg2;
                        break;
                    }
                    default: {
                        writer.println("ERROR");
                        continue;
                    }
                }
                writer.println(result);
                System.out.println(clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort() + " — " + request + " is " + result);
                //todo make statistics writing
                synchronized (this) {

                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startStatisticsReporter() {
        while (true) {
            try {
                Thread.sleep(10000);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }

            synchronized (this) {

            }
        }
    }
}
