package kata5;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebServiceTest {
    private final List<Books> sampleBooks = List.of(
            new Books("1", "1984", "George Orwell", "1949", Genre.DYSTOPIAN),
            new Books("2", "Dune", "Frank Herbert", "1965", Genre.SCI_FI),
            new Books("8", "The Hitchhiker's Guide to the Galaxy", "Douglas Adams", "1979", Genre.SCI_FI)
    );

    private final WebService service = new WebService(
            "b-test", "localhost", 0, "http://localhost:8500", "v1", sampleBooks
    );

    @Test
    void filtersBooksByGenre() {
        List<Books> sciFi = service.findByGenre(Genre.SCI_FI);
        assertEquals(2, sciFi.size());
        assertEquals("Dune", sciFi.get(0).title());
    }

    @Test
    void returnsEmptyListWhenGenreHasNoBooks() {
        assertTrue(service.findByGenre(Genre.MEME).isEmpty());
    }

    @Test
    void findsBookByTitleIgnoringCase() {
        Optional<Books> book = service.findByTitle("dune");
        assertTrue(book.isPresent());
        assertEquals("Frank Herbert", book.get().author());
    }

    @Test
    void returnsEmptyOptionalWhenTitleDoesNotExist() {
        assertTrue(service.findByTitle("Unknown").isEmpty());
    }
}
