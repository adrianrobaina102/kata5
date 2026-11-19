package kata5;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class WebServiceTest {
    private final WebService service = new WebService("f-test", "localhost", 0, "http://localhost:8500");

    @Test
    void returnsEmptyArrayForZeroTerms() {
        assertArrayEquals(new int[0], service.fibonacci(0));
    }

    @Test
    void returnsFirstTermForOne() {
        assertArrayEquals(new int[]{0}, service.fibonacci(1));
    }

    @Test
    void returnsFibonacciSequenceForSevenTerms() {
        assertArrayEquals(new int[]{0, 1, 1, 2, 3, 5, 8}, service.fibonacci(7));
    }
}
