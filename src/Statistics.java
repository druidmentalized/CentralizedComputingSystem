import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Statistics {
    private final AtomicInteger newConnectedClients = new AtomicInteger(0);
    private final AtomicInteger computedRequests = new AtomicInteger(0);
    private final AtomicInteger incorrectOperations = new AtomicInteger(0);
    private final AtomicInteger valuesComputedSum = new AtomicInteger(0);
    private final Map<String, AtomicInteger> operationStats = new ConcurrentHashMap<>();

    public Statistics() {
        for (String operation : new String[]{"ADD", "SUB", "MUL", "DIV"}) {
            operationStats.put(operation, new AtomicInteger(0));
        }
    }

    public void incrementNewConnectedClients() {
        newConnectedClients.incrementAndGet();
    }

    public void incrementComputedRequests() {
        computedRequests.incrementAndGet();
    }

    public void incrementIncorrectOperations() {
        incorrectOperations.incrementAndGet();
    }

    public void addValuesComputedSum(int value) {
        valuesComputedSum.addAndGet(value);
    }

    public void incrementOperationStats(String operation) {
        operationStats.get(operation).incrementAndGet();
    }

    public synchronized void summarize(Statistics stats) {
        newConnectedClients.getAndAdd(stats.newConnectedClients.get());
        computedRequests.getAndAdd(stats.computedRequests.get());
        incorrectOperations.getAndAdd(stats.incorrectOperations.get());
        valuesComputedSum.getAndAdd(stats.valuesComputedSum.get());
        operationStats.forEach((key, value) -> value.addAndGet(stats.operationStats.get(key).get()));
    }

    public synchronized void clear() {
        newConnectedClients.set(0);
        computedRequests.set(0);
        incorrectOperations.set(0);
        valuesComputedSum.set(0);
        operationStats.replaceAll((k, v) -> new AtomicInteger(0));
    }

    public void print(String title) {
        System.out.println(title + " connected clients: " + newConnectedClients.get());
        System.out.println(title + " computed requests: " + computedRequests.get());
        System.out.println(title + " computed values: " + valuesComputedSum.get());
        System.out.println(title + " incorrect operations: " + incorrectOperations.get());
        operationStats.forEach((operation, count) -> System.out.println(title + " " + operation + ": " + count));
    }
}
