package kata5;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebServiceTest {
    private final WebService service = new WebService("t-test", "localhost", 0, "http://localhost:8500");

    @Test
    void convertsFreezingPointFromFahrenheitToKelvin() {
        assertEquals(273, service.fahrenheitToKelvin(32));
    }

    @Test
    void convertsRoomTemperatureFromFahrenheitToKelvin() {
        assertEquals(298, service.fahrenheitToKelvin(77));
    }

    @Test
    void convertsBoilingPointFromFahrenheitToKelvin() {
        assertEquals(373, service.fahrenheitToKelvin(212));
    }

    @Test
    void convertsNegativeFortyFahrenheitToKelvin() {
        assertEquals(233, service.fahrenheitToKelvin(-40));
    }
}
