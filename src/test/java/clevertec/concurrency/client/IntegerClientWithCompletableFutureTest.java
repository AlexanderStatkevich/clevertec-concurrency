package clevertec.concurrency.client;

import clevertec.concurrency.server.IntegerServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegerClientWithCompletableFutureTest {

    private final int QUANTITY = 10;
    private final int THREAD_QUANTITY = 5;

    @Mock
    private IntegerServer integerServer;
    private IntegerClient integerClient;

    @BeforeEach
    void setUp() {
        integerClient = new IntegerClient(QUANTITY, THREAD_QUANTITY, integerServer);
    }

    @Test
    void checkClientSendRequestShouldReturnCorrectSumOfAccumulatorListSizes() {
        for (int i = 1; i <= QUANTITY; i++) {
            when(integerServer.process(new Request(i))).thenReturn(new Response(i));
        }
        Integer actual = integerClient.send();
        Integer expected = (1 + QUANTITY) * (QUANTITY / 2);
        assertThat(actual).isEqualTo(expected);
    }
}
