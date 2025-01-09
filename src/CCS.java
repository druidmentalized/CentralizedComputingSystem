import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class CCS {

    private final ExecutorService clientHandlerPool;
    private final ArrayList<String> connectedClients = new ArrayList<>();

    //global statistics variables
    private final AtomicInteger newConnectedClientsOverall = new AtomicInteger(0);
    private final AtomicInteger computedRequestsOverall = new AtomicInteger(0);
    private final AtomicInteger incorrectOperationsOverall = new AtomicInteger(0);
    private final AtomicInteger valuesComputedSumOverall = new AtomicInteger(0);
    private final Map<String, AtomicInteger> operationStatsOverall = new ConcurrentHashMap<>();

    //last 10 seconds statistics variables
    private final AtomicInteger newConnectedClients = new AtomicInteger(0);
    private final AtomicInteger computedRequests = new AtomicInteger(0);
    private final AtomicInteger incorrectOperations = new AtomicInteger(0);
    private final AtomicInteger valuesComputedSum = new AtomicInteger(0);
    private final Map<String, AtomicInteger> operationStats = new ConcurrentHashMap<>();


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
        for (String operation : new String[]{"ADD", "SUB", "MUL", "DIV"}) {
            operationStatsOverall.put(operation, new AtomicInteger(0));
            operationStats.put(operation, new AtomicInteger(0));
        }

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
                if (!connectedClients.contains(clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort())) {
                    newConnectedClients.incrementAndGet();
                    synchronized (connectedClients) {
                        connectedClients.add(clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
                    }
                }
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
                    incorrectOperations.incrementAndGet();
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
                    incorrectOperations.incrementAndGet();
                    System.out.println(clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort() + " — " + request + " is ERROR");
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
                    incorrectOperations.incrementAndGet();
                    System.out.println(clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort() +
                            " -- " + request + " is ERROR (" + e.getMessage() + ")");
                    continue;
                }

                writer.println(result);
                System.out.println(clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort() + " -- " + request + " is " + result);

                computedRequests.incrementAndGet();
                operationStats.get(operation).incrementAndGet();
                valuesComputedSum.addAndGet(result);
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

            newConnectedClientsOverall.addAndGet(newConnectedClients.get());
            computedRequestsOverall.addAndGet(computedRequests.get());
            incorrectOperationsOverall.addAndGet(incorrectOperations.get());
            valuesComputedSumOverall.addAndGet(valuesComputedSum.get());
            for (String operation : operationStats.keySet()) {
                operationStatsOverall.put(operation, new AtomicInteger(operationStatsOverall.get(operation).addAndGet(operationStats.get(operation).get())));
            }


            synchronized (this) {
                System.out.println("---------- Statistics report ----------");
                System.out.println("Total connected clients: " + newConnectedClientsOverall.get());
                System.out.println("Total computed requests: " + computedRequestsOverall.get());
                System.out.println("Total computed values: " + valuesComputedSumOverall.get());
                System.out.println("Total incorrect operations: " + incorrectOperationsOverall.get());
                operationStatsOverall.forEach((operation, count) -> System.out.println("Total " + operation + ": " + count));
                System.out.println("---------- Last 10 seconds statistics report ----------");
                System.out.println("Last connected clients: " + newConnectedClients.get());
                System.out.println("Last computed requests: " + computedRequests.get());
                System.out.println("Last computed values: " + valuesComputedSum.get());
                System.out.println("Last incorrect operations: " + incorrectOperations.get());
                operationStats.forEach((operation, count) -> System.out.println("Last " + operation + ": " + count));
            }

            newConnectedClients.set(0);
            computedRequests.set(0);
            incorrectOperations.set(0);
            valuesComputedSum.set(0);
            operationStats.replaceAll((o, v) -> new AtomicInteger(0));
        }
    }
}
