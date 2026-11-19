package kata5;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebServiceTest {
    private final WebService service = new WebService("r-test", "localhost", 0, "http://localhost:8500");

    @Test
    void returnsEmptyArrayForZeroCount() {
        assertEquals(0, service.randomNumber(0).length);
    }

    @Test
    void returnsRequestedCountWithinExpectedRange() {
        int[] values = service.randomNumber(20);
        assertEquals(20, values.length);
        for (int value : values) {
            assertTrue(value >= 0 && value < 100);
        }
    }
}
