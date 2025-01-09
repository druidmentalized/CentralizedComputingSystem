import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class CCS {

    private final ExecutorService clientHandlerPool;

    //global statistics variables
    private final Statistics overallStatistics = new Statistics();

    //last 10 seconds statistics variables
    private final Statistics lastStatistics = new Statistics();


    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Not correct arguments! Correct usage java -jar CCS.jar <port>");
            System.exit(1);
        }

        int port = 0;
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

        new CCS(port);
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
        try (DatagramSocket udpSocket = new DatagramSocket(port)) {
            byte[] buffer = new byte[12];
            System.out.println("UDP socket created on port: " + port);
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Message: " + message + ". Received from " + packet.getAddress().getHostAddress() + ":" + packet.getPort());

                if (message.equals("CCS DISCOVER")) {
                    byte[] responseMessage = "CCS FOUND".getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(responseMessage, responseMessage.length, packet.getAddress(), packet.getPort());
                    udpSocket.send(responsePacket);
                }
            }
        } catch (IOException e) {
            System.err.println("Error during UDP communication: " + e.getMessage());
        }
    }

    public void createTCPSocket(int port) {
        try (ServerSocket tcpSocket = new ServerSocket(port)) {
            System.out.println("TCP socket created on port: " + port);
            while (true) {
                Socket clientSocket = tcpSocket.accept();
                lastStatistics.incrementNewConnectedClients();
                clientHandlerPool.execute(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Error during TCP communication: " + e.getMessage());
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
                    lastStatistics.incrementIncorrectOperations();
                    System.out.println(clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort() + " — " + request + " is ERROR");
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
                    lastStatistics.incrementIncorrectOperations();
                    System.out.println(clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort() + " -- " + request + " is ERROR");
                    continue;
                }

                //calculating information
                int result;
                try {
                    switch (operation) {
                        case "ADD":
                            result = Math.addExact(arg1, arg2);
                            break;
                        case "SUB":
                            result = Math.subtractExact(arg1, arg2);
                            break;
                        case "MUL":
                            result = Math.multiplyExact(arg1, arg2);
                            break;
                        case "DIV":
                            if (arg2 == 0) {
                                throw new ArithmeticException("Division by zero");
                            }
                            result = arg1 / arg2;
                            break;
                        default:
                            throw new UnsupportedOperationException("Unknown operation: " + operation);
                    }
                } catch (ArithmeticException | UnsupportedOperationException e) {
                    writer.println("ERROR");
                    lastStatistics.incrementIncorrectOperations();
                    System.out.println(clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort() +
                            " -- " + request + " is ERROR (" + e.getMessage() + ")");
                    continue;
                }

                writer.println(result);
                System.out.println(clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort() + " -- " + request + " is " + result);

                lastStatistics.incrementComputedRequests();
                lastStatistics.incrementOperationStats(operation);
                lastStatistics.addValuesComputedSum(result);
            }
        }
        catch (IOException e) {
            System.err.println("Error during client handling: " + e.getMessage());
        }
    }

    private void startStatisticsReporter() {
        while (true) {
            try {
                Thread.sleep(10000);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Problem during statistics thread sleeping: " + e.getMessage());
            }

            overallStatistics.summarize(lastStatistics);

            synchronized (this) {
                System.out.println("---------- Statistics report ----------");
                overallStatistics.print("Total");
                System.out.println("---------- Last 10 seconds statistics report ----------");
                lastStatistics.print("Last");
            }

            lastStatistics.clear();
        }
    }
}
