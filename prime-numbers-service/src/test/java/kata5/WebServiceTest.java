package kata5;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebServiceTest {
    private final WebService service = new WebService("p-test", "localhost", 0, "http://localhost:8500");

    @Test
    void rejectsNonPrimeEdgeValues() {
        assertFalse(service.isPrime(-5));
        assertFalse(service.isPrime(0));
        assertFalse(service.isPrime(1));
    }

    @Test
    void detectsPrimeNumbers() {
        assertTrue(service.isPrime(2));
        assertTrue(service.isPrime(3));
        assertTrue(service.isPrime(7));
        assertTrue(service.isPrime(13));
        assertTrue(service.isPrime(97));
    }

    @Test
    void detectsCompositeNumbers() {
        assertFalse(service.isPrime(4));
        assertFalse(service.isPrime(9));
        assertFalse(service.isPrime(15));
        assertFalse(service.isPrime(100));
    }
}
