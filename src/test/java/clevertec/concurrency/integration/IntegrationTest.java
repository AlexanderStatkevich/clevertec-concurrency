package clevertec.concurrency.integration;

import clevertec.concurrency.client.IntegerClient;
import clevertec.concurrency.client.IntegerClientWithCompletableFuture;
import clevertec.concurrency.server.IntegerServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class IntegrationTest {

    private final int QUANTITY = 10;
    private final int THREAD_QUANTITY = 5;

    private IntegerServer server;

    private IntegerClient client;

    @BeforeEach
    void setUp() {
        server = new IntegerServer();
        client = new IntegerClient(QUANTITY, THREAD_QUANTITY, server);
    }

    @Test
    void checkClientSendRequestShouldReturnCorrectSumOfAccumulatorListSizes() {
        Integer actual = client.send();
        Integer expected = (1 + QUANTITY) * (QUANTITY / 2);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void checkClientWithCompletableFutureSendRequestShouldReturnCorrectSumOfAccumulatorListSizes() {
        IntegerClientWithCompletableFuture client = new IntegerClientWithCompletableFuture(QUANTITY, THREAD_QUANTITY, server);
        Integer actual = client.send();
        Integer expected = (1 + QUANTITY) * (QUANTITY / 2);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void checkThatTaskEvaluatesFasterInMultithreading() {
        IntegerServer integerServerForSoloClient = new IntegerServer();
        IntegerClient soloIntegerClient = new IntegerClient(QUANTITY, 1, integerServerForSoloClient);

        IntegerServer integerServerForMultiClient = new IntegerServer();
        IntegerClient multiIntegerClient = new IntegerClient(QUANTITY, 5, integerServerForMultiClient);

        long beforeSolo = System.nanoTime();
        soloIntegerClient.send();
        long afterSolo = System.nanoTime();
        long soloTime = afterSolo - beforeSolo;

        long beforeMulti = System.nanoTime();
        multiIntegerClient.send();
        long afterMulti = System.nanoTime();
        long multiTime = beforeMulti - afterMulti;

        assertThat(soloTime).isGreaterThan(multiTime);
    }

    @Test
    void checkAllRequestsHaveSend() {
        client.send();
        int actualSize = client.getIntegers().size();
        assertThat(actualSize).isEqualTo(0);
    }

    @Test
    void checkAccumulatorOnServerConsistentAfterRequest() {
        List<Integer> initialList = IntStream
                .range(1, QUANTITY + 1)
                .boxed()
                .toList();
        client.send();

        List<Integer> serverAccumulator = server.getServerAccumulator();
        assertThat(serverAccumulator.containsAll(initialList)).isTrue();
    }

}
