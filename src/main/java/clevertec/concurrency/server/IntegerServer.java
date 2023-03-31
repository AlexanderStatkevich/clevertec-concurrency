package clevertec.concurrency.server;

import clevertec.concurrency.client.Request;
import clevertec.concurrency.client.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class IntegerServer {
    private final Lock lock = new ReentrantLock();
    private final List<Integer> serverAccumulator = new ArrayList<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public Response process(Request request) {
        if (request.value() == -1) {
            executorService.shutdown();
            return new Response(-1);
        }
        try {
            return executorService.submit(() -> getResponse(request)).get();
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
    }

    private Response getResponse(Request request) {
        sleepRandomTime();

        lock.lock();
        try {
            serverAccumulator.add(request.value());
            return new Response(serverAccumulator.size());
        } finally {
            lock.unlock();
        }
    }

    private void sleepRandomTime() {
        try {
            Thread.sleep((long) (100 + Math.random() * 900));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Integer> getServerAccumulator() {
        return serverAccumulator;
    }
}
