package clevertec.concurrency.client;

import clevertec.concurrency.server.IntegerServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static clevertec.concurrency.util.Constants.CHANGE_TO_INCLUSIVE_UPPER_BOUND;
import static clevertec.concurrency.util.Constants.SHUTDOWN_SERVER_VALUE;

public class IntegerClientWithCompletableFuture {

    private final int quantity;
    private final IntegerServer server;
    private final Random random = new Random();
    private final List<Integer> integers;
    private final AtomicInteger clientAccumulator = new AtomicInteger(0);
    private final Lock lock = new ReentrantLock();
    private final ExecutorService executorServiceGetParameter;
    private final ExecutorService executorServiceResponse;
    private final ExecutorService executorServiceAccumulate;

    public IntegerClientWithCompletableFuture(int quantity, int threadQuantity, IntegerServer server) {
        this.quantity = quantity;
        integers = IntStream.range(1, quantity + CHANGE_TO_INCLUSIVE_UPPER_BOUND)
                .boxed()
                .collect(Collectors.toList());
        this.server = server;
        executorServiceGetParameter = Executors.newFixedThreadPool(threadQuantity);
        executorServiceResponse = Executors.newFixedThreadPool(threadQuantity);
        executorServiceAccumulate = Executors.newFixedThreadPool(threadQuantity);
    }

    public Integer send() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            CompletableFuture<Void> voidCompletableFuture = CompletableFuture.supplyAsync(this::getRequestParameter, executorServiceGetParameter)
                    .thenApplyAsync(x -> {
                        Response response = server.process(new Request(x));
                        return response.value();
                    }, executorServiceResponse)
                    .thenAcceptAsync(x -> {
                        clientAccumulator.accumulateAndGet(x, Integer::sum);
                    }, executorServiceAccumulate);
            futures.add(voidCompletableFuture);
        }
        futures.forEach(CompletableFuture::join);
        executorServiceGetParameter.shutdown();
        executorServiceResponse.shutdown();
        executorServiceAccumulate.shutdown();
        server.process(new Request(SHUTDOWN_SERVER_VALUE));
        return clientAccumulator.get();
    }

    private Integer getRequestParameter() {
        lock.lock();
        try {
            int size = integers.size();
            int randomIndex = size == 1
                    ? 0
                    : random.nextInt(size - 1);
            return integers.remove(randomIndex);
        } finally {
            lock.unlock();
        }
    }
}
