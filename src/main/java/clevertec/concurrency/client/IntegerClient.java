package clevertec.concurrency.client;

import clevertec.concurrency.server.IntegerServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static clevertec.concurrency.util.Constants.CHANGE_TO_INCLUSIVE_UPPER_BOUND;
import static clevertec.concurrency.util.Constants.SHUTDOWN_SERVER_VALUE;

public class IntegerClient {
    private final Random random = new Random();
    private final Lock lock = new ReentrantLock();
    private final ExecutorService executorService;
    private int quantity;
    private IntegerServer server;
    private List<Integer> integers;


    public IntegerClient(int quantity, int threadQuantity, IntegerServer server) {
        this.quantity = quantity;
        integers = IntStream.range(1, quantity + CHANGE_TO_INCLUSIVE_UPPER_BOUND)
                .boxed()
                .collect(Collectors.toList());
        this.server = server;
        executorService = Executors.newFixedThreadPool(threadQuantity);
    }

    public Integer send() {
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            Callable<Integer> callable = () -> {
                Integer value = getRequestParameter();
                Response response = server.process(new Request(value));
                return response.value();
            };
            tasks.add(callable);
        }
        try {
            List<Future<Integer>> futures = executorService.invokeAll(tasks);
            executorService.shutdown();
            server.process(new Request(SHUTDOWN_SERVER_VALUE));

            return futures.stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .reduce(Integer::sum).orElseThrow();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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

    public List<Integer> getIntegers() {
        return integers;
    }
}
