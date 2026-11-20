package kata5;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebServiceTest {
    private final WebService service = new WebService("d-test", "localhost", 0, "http://localhost:8500");

    @Test
    void convertsZeroKilometersToMiles() {
        assertEquals(0.0, service.kilometersToMiles(0.0), 1e-6);
    }

    @Test
    void convertsOneMileInKilometersToOneMile() {
        assertEquals(1.0, service.kilometersToMiles(1.609344), 1e-6);
    }

    @Test
    void convertsTenKilometersToMiles() {
        assertEquals(6.213712, service.kilometersToMiles(10.0), 1e-6);
    }
}
