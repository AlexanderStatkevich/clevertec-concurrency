package clevertec.concurrency.server;

import clevertec.concurrency.client.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class IntegerServerTest {

    private final int QUANTITY = 10;

    private IntegerServer integerServer;

    @BeforeEach
    void setUp() {
        integerServer = new IntegerServer();
    }

    @Test
    void checkThatServerReturnCorrectAccumulatorSize() {
        for (int i = 1; i < QUANTITY; i++) {
            int actualSize = integerServer.process(new Request(i)).value();
            assertThat(actualSize).isEqualTo(i);
        }
    }

    @Test
    void checkAccumulatorOnServerConsistentAfterRequest() {
        List<Integer> initialList = IntStream.range(1, 10)
                .boxed()
                .map(Request::new)
                .peek(integerServer::process)
                .map(Request::value)
                .toList();
        List<Integer> serverAccumulator = integerServer.getServerAccumulator();
        assertThat(serverAccumulator.containsAll(initialList)).isTrue();
    }
}
